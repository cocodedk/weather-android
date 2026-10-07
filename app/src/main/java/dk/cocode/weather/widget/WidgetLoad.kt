package dk.cocode.weather.widget

import dk.cocode.weather.data.ForecastRepository
import dk.cocode.weather.data.Place
import dk.cocode.weather.data.WeatherStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicLong

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

/** Loads the forecast for the place the app has selected. [readPrefs] reads the saved choice. */
suspend fun loadForWidget(
    readPrefs: suspend () -> WeatherStore.Prefs,
    repo: ForecastRepository,
): WidgetLoad {
    val prefs = readPrefs()
    val place = prefs.selected ?: return WidgetLoad.NoPlace
    return try {
        WidgetLoad.Ready(place, repo.load(place), prefs.imperial)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        WidgetLoad.Unavailable(place)
    }
}

/**
 * Widget refreshes run side by side, so they can finish out of order. Each takes a number when it
 * starts, and drawing happens under one lock that re-checks that number and the saved selection.
 * A refresh that is no longer the newest, or whose place is no longer the selected one, draws
 * nothing (the refresh that change set off draws instead). One that passes the check finishes
 * drawing before the next refresh can check, so an old place never overwrites a newer one.
 */
class WidgetPublisher {
    private val newest = AtomicLong()
    private val lock = Mutex()

    /** Call when a refresh starts. */
    fun begin(): Long = newest.incrementAndGet()

    /** Calls [draw] with [found] unless it is out of date. True when it drew. */
    suspend fun publish(
        ticket: Long,
        found: WidgetLoad,
        readPrefs: suspend () -> WeatherStore.Prefs,
        draw: (WidgetLoad) -> Unit,
    ): Boolean = lock.withLock {
        if (ticket != newest.get() || readPrefs().selected != found.place) return@withLock false
        draw(found)
        true
    }
}
