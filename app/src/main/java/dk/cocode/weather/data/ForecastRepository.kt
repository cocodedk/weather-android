package dk.cocode.weather.data

import kotlinx.coroutines.CancellationException

/** Where the last good response per place is kept; [WeatherStore] in the app, a fake in tests. */
interface ForecastCache {
    suspend fun cacheForecast(cacheKey: String, body: String)
    suspend fun cachedForecast(cacheKey: String): Forecast?
}

/**
 * Fetch-with-fallback. Mirrors the Tizen app's rule: a network failure is only an
 * error if there is no cached reading for that place to fall back on.
 */
class ForecastRepository(
    private val cache: ForecastCache,
    private val fetch: suspend (Place) -> Pair<Forecast, String> = { ForecastApi.fetch(it) },
) {

    data class Loaded(val forecast: Forecast, val stale: Boolean)

    /** The saved forecast for [place], marked stale, or null when there is none. */
    suspend fun cached(place: Place): Loaded? =
        cache.cachedForecast(place.cacheKey)?.let { Loaded(it, stale = true) }

    suspend fun load(place: Place): Loaded {
        return try {
            val (forecast, body) = fetch(place)
            cache.cacheForecast(place.cacheKey, body)
            Loaded(forecast, stale = false)
        } catch (e: CancellationException) {
            // Cancelled because the user picked another place: not a network failure, so
            // do not hand back this place's cached forecast as if it were the answer.
            throw e
        } catch (e: Exception) {
            val cached = cache.cachedForecast(place.cacheKey)
                ?: throw e // nothing to show — let the caller surface the real cause
            Loaded(cached, stale = true)
        }
    }
}
