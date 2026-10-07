package dk.cocode.weather.widget

import dk.cocode.weather.data.ForecastRepository
import dk.cocode.weather.data.Place
import dk.cocode.weather.data.WeatherStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

/**
 * The broadcast that starts a refresh is held open with goAsync(), and Android gives it about ten
 * seconds before it counts as stuck. All the work of a refresh gets [WIDGET_REFRESH_MS]; the fetch
 * inside it gets [WIDGET_FETCH_MS], which leaves time to draw the saved forecast when the fetch
 * gives up. A refresh that ran out of time still has until [WIDGET_SETTLE_MS] to replace the
 * "loading" it drew with a plain "unavailable", so a widget never stays on "loading".
 */
const val WIDGET_REFRESH_MS = 8_000L
const val WIDGET_FETCH_MS = 6_000L
const val WIDGET_SETTLE_MS = 9_000L

/** What a widget refresh found, before any view is built from it. */
sealed interface WidgetLoad {
    /** The place this was loaded for; null when nothing is saved. */
    val place: Place?

    /** Nothing is saved to show. */
    data object NoPlace : WidgetLoad {
        override val place: Place? = null
    }

    /** No network and no cached forecast for the place. */
    data class Unavailable(override val place: Place) : WidgetLoad

    data class Ready(
        override val place: Place,
        val loaded: ForecastRepository.Loaded,
        val imperial: Boolean,
    ) : WidgetLoad
}

/**
 * Loads the forecast for the place the app has selected. [readPrefs] reads the saved choice. A fetch
 * that has not finished by [fetchDeadlineNanos] (a System.nanoTime() value) is given up on, and the
 * saved forecast (or "unavailable") is what comes back, so the widget never stays on "loading". The
 * time left is worked out after the saved choice has been read, because that read can be slow too.
 */
suspend fun loadForWidget(
    readPrefs: suspend () -> WeatherStore.Prefs,
    repo: ForecastRepository,
    fetchDeadlineNanos: Long = System.nanoTime() + WIDGET_FETCH_MS * 1_000_000,
): WidgetLoad {
    val prefs = readPrefs()
    val place = prefs.selected ?: return WidgetLoad.NoPlace
    val fetchMs = maxOf(0L, (fetchDeadlineNanos - System.nanoTime()) / 1_000_000)
    val loaded = try {
        withTimeoutOrNull(fetchMs) { repo.load(place) } ?: repo.cached(place)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
    return if (loaded == null) WidgetLoad.Unavailable(place) else WidgetLoad.Ready(place, loaded, prefs.imperial)
}
