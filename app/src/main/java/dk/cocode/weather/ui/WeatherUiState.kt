package dk.cocode.weather.ui

import dk.cocode.weather.data.Forecast
import dk.cocode.weather.data.Place
import dk.cocode.weather.domain.Units

/** Everything the screen draws, in one immutable snapshot. */
data class WeatherUiState(
    val places: List<Place> = emptyList(),
    val selected: Place? = null,
    val forecast: Forecast? = null,
    /** 0 = today; indexes into [Forecast.daily], matching the Tizen day selector. */
    val dayIndex: Int = 0,
    val loading: Boolean = false,
    val stale: Boolean = false,
    val error: String? = null,
    val imperial: Boolean = false,
    val theme: String = "auto",
    val locating: Boolean = false,
    /** Mirrors the device's clock setting, read once at startup. */
    val use24Hour: Boolean = true,
    /** One-shot message for the snackbar; cleared once shown. */
    val message: String? = null,
) {
    val units: Units get() = Units(imperial = imperial, use24Hour = use24Hour)

    /** Night styling follows the selected place's own day/night, not the phone's. */
    val isNight: Boolean
        get() = when (theme) {
            "day" -> false
            "night" -> true
            else -> forecast?.current?.isDay == false && dayIndex == 0
        }
}

/** State of the location search sheet, kept separate so typing does not repaint the forecast. */
data class SearchUiState(
    val query: String = "",
    val results: List<Place> = emptyList(),
    val searching: Boolean = false,
    val error: String? = null,
)
