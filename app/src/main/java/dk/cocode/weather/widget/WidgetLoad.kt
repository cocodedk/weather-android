package dk.cocode.weather.widget

import dk.cocode.weather.data.ForecastRepository
import dk.cocode.weather.data.Place
import dk.cocode.weather.data.WeatherStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import java.util.concurrent.atomic.AtomicLong

/**
 * How long a refresh may fetch. The broadcast that started it is held open with goAsync(), and
 * Android gives it about ten seconds before it counts as stuck.
 */
const val WIDGET_DEADLINE_MS = 8_000L

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
 * that has not finished within [deadlineMs] is given up on, and the saved forecast (or "unavailable")
 * is what comes back, so the widget never stays on "loading".
 */
suspend fun loadForWidget(
    readPrefs: suspend () -> WeatherStore.Prefs,
    repo: ForecastRepository,
    deadlineMs: Long = WIDGET_DEADLINE_MS,
): WidgetLoad {
    val prefs = readPrefs()
    val place = prefs.selected ?: return WidgetLoad.NoPlace
    val loaded = try {
        withTimeout(deadlineMs) { repo.load(place) }
    } catch (e: TimeoutCancellationException) {
        repo.cached(place)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
    return if (loaded == null) WidgetLoad.Unavailable(place) else WidgetLoad.Ready(place, loaded, prefs.imperial)
}

/** Where a refresh draws. [V] is what the platform draws with (RemoteViews in the app). */
interface WidgetSurface<V> {
    fun build(found: WidgetLoad): V

    /** Every widget on the home screen now, not just the ones a refresh was asked about. */
    fun allIds(): IntArray

    fun update(id: Int, views: V)
}

/**
 * Widget refreshes run side by side, so they can finish out of order. Each takes a number when it
 * starts, and drawing happens under one lock that re-checks that number and the saved selection.
 * A refresh that is no longer the newest, or whose place is no longer the selected one, draws
 * nothing (the refresh that change set off draws instead). One that passes the check finishes
 * drawing before the next refresh can check, so an old place never overwrites a newer one.
 */
class WidgetPublisher(private val lock: Mutex = Mutex()) {
    private val newest = AtomicLong()

    /** Call when a refresh starts. */
    fun begin(): Long = newest.incrementAndGet()

    /**
     * Draws [found] on every widget of [surface] unless it is out of date (the newest refresh may
     * stand in for an older one that was asked about other widgets). True when it drew.
     */
    suspend fun <V> publish(
        ticket: Long,
        found: WidgetLoad,
        readPrefs: suspend () -> WeatherStore.Prefs,
        surface: WidgetSurface<V>,
    ): Boolean = lock.withLock {
        if (ticket != newest.get() || readPrefs().selected != found.place) return@withLock false
        val views = surface.build(found)
        surface.allIds().forEach { surface.update(it, views) }
        true
    }
}
