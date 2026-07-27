package dk.cocode.weather.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "weather")

/** Persisted preferences, the saved-places list, and the offline forecast cache. */
class WeatherStore(private val context: Context) {

    data class Prefs(
        val places: List<Place>,
        val selectedKey: String?,
        val imperial: Boolean,
        val theme: String,
    )

    val prefs: Flow<Prefs> = context.dataStore.data.map { p ->
        Prefs(
            places = PlaceJson.decode(p[KEY_PLACES]).ifEmpty { listOf(DEFAULT_PLACE) },
            selectedKey = p[KEY_SELECTED],
            imperial = p[KEY_IMPERIAL] ?: false,
            theme = p[KEY_THEME] ?: THEME_AUTO,
        )
    }

    suspend fun savePlaces(places: List<Place>) {
        context.dataStore.edit { it[KEY_PLACES] = PlaceJson.encode(places) }
    }

    suspend fun saveSelected(key: String) {
        context.dataStore.edit { it[KEY_SELECTED] = key }
    }

    suspend fun saveImperial(imperial: Boolean) {
        context.dataStore.edit { it[KEY_IMPERIAL] = imperial }
    }

    suspend fun saveTheme(theme: String) {
        context.dataStore.edit { it[KEY_THEME] = theme }
    }

    /**
     * The last successful response body, kept per place so switching back to a
     * city shows its own last-known reading rather than another city's.
     */
    suspend fun cacheForecast(placeKey: String, body: String) {
        context.dataStore.edit { it[cacheKey(placeKey)] = body }
    }

    suspend fun cachedForecast(placeKey: String): Forecast? {
        val body = context.dataStore.data.first()[cacheKey(placeKey)]
        if (body.isNullOrBlank()) return null
        return runCatching { ForecastApi.parse(body) }.getOrNull()
    }

    private fun cacheKey(placeKey: String) = stringPreferencesKey("cache.$placeKey")

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
