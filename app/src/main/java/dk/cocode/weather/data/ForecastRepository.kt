package dk.cocode.weather.data

/**
 * Fetch-with-fallback. Mirrors the Tizen app's rule: a network failure is only an
 * error if there is no cached reading for that place to fall back on.
 */
class ForecastRepository(private val store: WeatherStore) {

    data class Loaded(val forecast: Forecast, val stale: Boolean)

    suspend fun load(place: Place): Loaded {
        return try {
            val (forecast, body) = ForecastApi.fetch(place)
            store.cacheForecast(place.key, body)
            Loaded(forecast, stale = false)
        } catch (e: Exception) {
            val cached = store.cachedForecast(place.key)
                ?: throw e // nothing to show — let the caller surface the real cause
            Loaded(cached, stale = true)
        }
    }
}
