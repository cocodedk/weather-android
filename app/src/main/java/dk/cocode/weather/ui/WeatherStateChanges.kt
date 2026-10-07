package dk.cocode.weather.ui

import androidx.annotation.StringRes
import dk.cocode.weather.R
import dk.cocode.weather.data.Forecast
import dk.cocode.weather.data.LocationUnavailable
import dk.cocode.weather.data.Place
import dk.cocode.weather.data.WeatherStore

/*
 * The plain state changes behind WeatherViewModel: what the screen's state becomes when a
 * forecast arrives, a place is picked, added or removed, or the theme is cycled. They hold no
 * Android objects, so unit tests run them directly. The ViewModel keeps the coroutines, the
 * storage writes and the widget pokes.
 */

/** The state once a forecast has arrived. */
fun WeatherUiState.withForecast(forecast: Forecast, stale: Boolean): WeatherUiState = copy(
    forecast = forecast,
    stale = stale,
    loading = false,
    error = null,
    // A shorter forecast (or a new place) must not leave the selector pointing past the end
    // of the list.
    dayIndex = dayIndex.coerceIn(0, (forecast.daily.size - 1).coerceAtLeast(0)),
)

/**
 * The state right after picking [place]. dayIndex resets: "Wednesday" in the old city is not
 * the row the user wants to keep staring at after switching to a new one.
 */
fun WeatherUiState.withSelectedPlace(place: Place): WeatherUiState =
    copy(selected = place, forecast = null, dayIndex = 0, stale = false)

/** The saved list with [place] added unless it is already there, and the entry to select. */
fun withPlace(places: List<Place>, place: Place): Pair<List<Place>, Place> {
    val existing = places.firstOrNull { it.key == place.key }
    return (if (existing != null) places else places + place) to (existing ?: place)
}

/** The saved list without [place], or null when that would leave nothing to show. */
fun withoutPlace(places: List<Place>, place: Place): List<Place>? =
    places.filterNot { it.key == place.key }.ifEmpty { null }

/** The saved list with [place] as its one device-location entry, not one per fix. */
fun withDevicePlace(places: List<Place>, place: Place): List<Place> =
    places.filterNot { it.isDeviceLocation } + place

/** Each tap on the theme item moves Auto, then Day, then Night, then back to Auto. */
fun nextTheme(theme: String): String = when (theme) {
    WeatherStore.THEME_AUTO -> WeatherStore.THEME_DAY
    WeatherStore.THEME_DAY -> WeatherStore.THEME_NIGHT
    else -> WeatherStore.THEME_AUTO
}

/** The sentence to show when finding the device location failed. */
@StringRes
fun locationFailureMessage(e: Exception): Int =
    if (e is LocationUnavailable) e.messageRes else R.string.msg_location_failed
