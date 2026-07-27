package dk.cocode.weather.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.Locale
import kotlin.coroutines.resume

/** Raised when the caller must ask for (or the user has refused) location access. */
class LocationPermissionMissing : Exception("Location permission not granted")

/** Raised when permission exists but no usable fix could be obtained. */
class LocationUnavailable(message: String) : Exception(message)

/**
 * Device GPS via the platform LocationManager.
 *
 * Deliberately not Play Services' fused provider: LocationManager ships with
 * Android, so the app needs no Google dependency and still works on a device
 * without Play Services. For a city-level forecast the extra accuracy of the
 * fused provider buys nothing.
 */
object DeviceLocation {

    private const val FRESH_ENOUGH_MS = 10 * 60 * 1000L
    private const val FIX_TIMEOUT_MS = 20_000L

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Resolves the device's position to a [Place]. Prefers a recent cached fix so
     * the common case is instant, and only waits on the radios when there is
     * nothing usable.
     */
    suspend fun current(context: Context): Place {
        if (!hasPermission(context)) throw LocationPermissionMissing()

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: throw LocationUnavailable("Location service unavailable")

        val cached = lastKnown(lm)
        if (cached != null && System.currentTimeMillis() - cached.time < FRESH_ENOUGH_MS) {
            return toPlace(context, cached)
        }

        val fresh = try {
            withTimeout(FIX_TIMEOUT_MS) { awaitFix(lm) }
        } catch (e: TimeoutCancellationException) {
            null
        }

        // A stale fix still beats no forecast — the user moved at most a little
        // since, and the alternative is an error screen.
        val best = fresh ?: cached
            ?: throw LocationUnavailable("Could not get a location fix. Is location turned on?")
        return toPlace(context, best)
    }

    /** Most recent fix any enabled provider already has, without powering up the radios. */
    @Suppress("MissingPermission")
    private fun lastKnown(lm: LocationManager): Location? =
        listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            .mapNotNull { provider ->
                runCatching { lm.getLastKnownLocation(provider) }.getOrNull()
            }
            .maxByOrNull { it.time }

    @Suppress("MissingPermission")
    private suspend fun awaitFix(lm: LocationManager): Location? =
        suspendCancellableCoroutine { cont ->
            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
                .filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }

            if (providers.isEmpty()) {
                cont.resume(null)
                return@suspendCancellableCoroutine
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requestModern(lm, providers, cont)
            } else {
                requestLegacy(lm, providers, cont)
            }
        }

    /** API 31+: getCurrentLocation cancels itself, so there is nothing to unregister. */
    @Suppress("MissingPermission")
    @androidx.annotation.RequiresApi(Build.VERSION_CODES.S)
    private fun requestModern(
        lm: LocationManager,
        providers: List<String>,
        cont: CancellableContinuation<Location?>,
    ) {
        val signal = android.os.CancellationSignal()
        cont.invokeOnCancellation { runCatching { signal.cancel() } }
        // Ask every enabled provider at once and take whichever answers first.
        providers.forEach { provider ->
            runCatching {
                lm.getCurrentLocation(provider, signal, callbackExecutor) { loc ->
                    if (loc != null && cont.isActive) cont.resume(loc)
                }
            }
        }
    }

    /** Shared, so a burst of location requests cannot spawn a thread per call. */
    private val callbackExecutor: java.util.concurrent.Executor by lazy {
        java.util.concurrent.Executors.newSingleThreadExecutor { r ->
            Thread(r, "location-callback").apply { isDaemon = true }
        }
    }

    /** API 26-30: a one-shot listener that unregisters itself on the first fix. */
    @Suppress("MissingPermission")
    private fun requestLegacy(
        lm: LocationManager,
        providers: List<String>,
        cont: CancellableContinuation<Location?>,
    ) {
        lateinit var listener: LocationListener
        listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                runCatching { lm.removeUpdates(listener) }
                if (cont.isActive) cont.resume(location)
            }

            // Required on API < 30 or the listener is never registered on some OEM builds.
            override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        cont.invokeOnCancellation { runCatching { lm.removeUpdates(listener) } }
        providers.forEach { provider ->
            runCatching {
                lm.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
            }
        }
    }

    /**
     * Names the fix. Geocoder is best-effort: it needs a backend service that some
     * devices and ROMs lack, so a failure degrades to coordinates rather than
     * blocking the forecast, which only ever needed the numbers.
     */
    private suspend fun toPlace(context: Context, loc: Location): Place =
        withContext(Dispatchers.IO) {
            val named = if (android.location.Geocoder.isPresent()) {
                runCatching {
                    @Suppress("DEPRECATION")
                    android.location.Geocoder(context, Locale.getDefault())
                        .getFromLocation(loc.latitude, loc.longitude, 1)
                        ?.firstOrNull()
                }.getOrNull()
            } else null

            val name = named?.locality
                ?: named?.subAdminArea
                ?: named?.adminArea
                ?: String.format(Locale.US, "%.3f, %.3f", loc.latitude, loc.longitude)

            Place(
                name = name,
                country = named?.countryName.orEmpty(),
                admin1 = named?.adminArea?.takeIf { it != name }.orEmpty(),
                latitude = loc.latitude,
                longitude = loc.longitude,
                timezone = "auto",
                isDeviceLocation = true,
            )
        }
}
