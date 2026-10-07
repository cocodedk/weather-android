package dk.cocode.weather.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "weather")

/** Persisted preferences, the saved-places list, and the offline forecast cache. */
class WeatherStore(private val data: DataStore<Preferences>) : ForecastCache {

    constructor(context: Context) : this(context.dataStore)

    data class Prefs(
        val places: List<Place>,
        val selectedKey: String?,
        val imperial: Boolean,
        val theme: String,
    ) {
        /** The place the app (and so the widget) shows: the saved selection, else the first place. */
        val selected: Place? get() = places.firstOrNull { it.key == selectedKey } ?: places.firstOrNull()
    }

    val prefs: Flow<Prefs> = data.data.map { p ->
        Prefs(
            places = PlaceJson.decode(p[KEY_PLACES]).ifEmpty { listOf(DEFAULT_PLACE) },
            selectedKey = p[KEY_SELECTED],
            imperial = p[KEY_IMPERIAL] ?: false,
            theme = p[KEY_THEME] ?: THEME_AUTO,
        )
    }

    suspend fun savePlaces(places: List<Place>) {
        data.edit { it[KEY_PLACES] = PlaceJson.encode(places) }
    }

    suspend fun saveSelected(key: String) {
        data.edit { it[KEY_SELECTED] = key }
    }

    suspend fun saveImperial(imperial: Boolean) {
        data.edit { it[KEY_IMPERIAL] = imperial }
    }

    suspend fun saveTheme(theme: String) {
        data.edit { it[KEY_THEME] = theme }
    }

    /**
     * The last successful response body, kept per place ([Place.cacheKey]) so switching back to
     * a city shows its own last-known reading rather than another city's.
     */
    override suspend fun cacheForecast(cacheKey: String, body: String) {
        data.edit { prefs ->
            // The device entry gets a new key at every new spot; keep the latest one only, so
            // moving around does not leave a forecast behind per place (this also drops the
            // plain "device" entry older versions wrote). A result for a spot the phone has
            // already left (a slow widget refresh, say) is dropped instead: it must neither
            // delete the newer spot's forecast nor take its place. Checked inside this edit, so
            // no write can slip in between the check and the change.
            if (cacheKey.startsWith(Place.DEVICE_KEY)) {
                val here = PlaceJson.decode(prefs[KEY_PLACES]).firstOrNull { it.isDeviceLocation }
                if (here?.cacheKey != cacheKey) return@edit
                prefs.asMap().keys
                    .filter { it.name.startsWith(cachePref(Place.DEVICE_KEY).name) }
                    .forEach { prefs.remove(it) }
            }
            prefs[cachePref(cacheKey)] = body
        }
    }

    override suspend fun cachedForecast(cacheKey: String): Forecast? {
        val body = data.data.first()[cachePref(cacheKey)]
        if (body.isNullOrBlank()) return null
        return runCatching { ForecastApi.parse(body) }.getOrNull()
    }

    private fun cachePref(cacheKey: String) = stringPreferencesKey("cache.$cacheKey")

    companion object {
        const val THEME_AUTO = "auto"
        const val THEME_DAY = "day"
        const val THEME_NIGHT = "night"

        /** Where the Tizen app started, kept as the first-run default. */
        val DEFAULT_PLACE = Place(
            name = "Copenhagen",
            country = "Denmark",
            admin1 = "Capital Region",
            latitude = 55.6761,
            longitude = 12.5683,
            timezone = "Europe/Copenhagen",
        )

        private val KEY_PLACES = stringPreferencesKey("places")
        private val KEY_SELECTED = stringPreferencesKey("selected")
        private val KEY_IMPERIAL = booleanPreferencesKey("imperial")
        private val KEY_THEME = stringPreferencesKey("theme")
    }
}
