package dk.cocode.weather.widget

import dk.cocode.weather.data.ForecastRepository
import dk.cocode.weather.data.Place
import dk.cocode.weather.data.WeatherStore
import kotlinx.coroutines.CancellationException

/** What a widget refresh found, before any view is built from it. */
sealed interface WidgetLoad {
    /** Nothing is saved to show. */
    data object NoPlace : WidgetLoad

    /** No network and no cached forecast for the place. */
    data object Unavailable : WidgetLoad

    /**
     * The app selected another place, or took a new device fix, while this loaded. Drawing it
     * would put the old place back on the home screen; the refresh that change set off draws.
     */
    data object Superseded : WidgetLoad

    data class Ready(val place: Place, val loaded: ForecastRepository.Loaded, val imperial: Boolean) : WidgetLoad
}

/** Loads the forecast for the place the app has selected. [readPrefs] reads the saved choice. */
suspend fun loadForWidget(
    readPrefs: suspend () -> WeatherStore.Prefs,
    repo: ForecastRepository,
): WidgetLoad {
    val prefs = readPrefs()
    val place = prefs.selected ?: return WidgetLoad.NoPlace

    val result: WidgetLoad = try {
        WidgetLoad.Ready(place, repo.load(place), prefs.imperial)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        WidgetLoad.Unavailable
    }

    return if (readPrefs().selected == place) result else WidgetLoad.Superseded
}
