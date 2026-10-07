package dk.cocode.weather.widget

import dk.cocode.weather.data.ForecastRepository
import dk.cocode.weather.data.Place
import dk.cocode.weather.data.WeatherStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicLong

/**
 * The broadcast that starts a refresh is held open with goAsync(), and Android gives it about ten
 * seconds before it counts as stuck. A whole refresh gets [WIDGET_REFRESH_MS]; the fetch inside
 * it gets [WIDGET_FETCH_MS], which leaves time to draw the saved forecast when the fetch gives up.
 */
const val WIDGET_REFRESH_MS = 9_000L
const val WIDGET_FETCH_MS = 7_000L

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

/** Where a refresh draws. [V] is what the platform draws with (RemoteViews in the app). */
interface WidgetSurface<V> {
    fun build(found: WidgetLoad): V

    /** What a widget shows while a refresh works on it. */
    fun loading(): V

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
     * stand in for an older one that was asked about other widgets). True when it drew every
     * widget. Waiting for the lock can be cancelled but drawing cannot, so a refresh that has a
     * [deadlineNanos] (a System.nanoTime() value) looks at the clock itself: before it builds the
     * views and before each widget, and stops drawing once the time is up.
     */
    suspend fun <V> publish(
        ticket: Long,
        found: WidgetLoad,
        readPrefs: suspend () -> WeatherStore.Prefs,
        surface: WidgetSurface<V>,
        deadlineNanos: Long? = null,
    ): Boolean = lock.withLock {
        if (ticket != newest.get() || readPrefs().selected != found.place || timeIsUp(deadlineNanos)) {
            return@withLock false
        }
        val views = surface.build(found)
        for (id in surface.allIds()) {
            if (timeIsUp(deadlineNanos)) return@withLock false
            surface.update(id, views)
        }
        true
    }

    /**
     * Shows the "loading" views on [ids] while a refresh works, unless a newer refresh has started
     * (its result, or its own "loading", must not be overwritten by an older one that is late) or
     * the time is up. Looks at both before each widget, under the same lock as [publish].
     */
    suspend fun <V> showLoading(
        ticket: Long,
        ids: IntArray,
        surface: WidgetSurface<V>,
        deadlineNanos: Long? = null,
    ) = lock.withLock {
        val views = surface.loading()
        for (id in ids) {
            if (ticket != newest.get() || timeIsUp(deadlineNanos)) return@withLock
            surface.update(id, views)
        }
    }

    private fun timeIsUp(deadlineNanos: Long?) = deadlineNanos != null && System.nanoTime() - deadlineNanos > 0
}

/**
 * One whole refresh, from loading to drawing, against a clock that [startedNanos] (a
 * System.nanoTime() value taken when the broadcast was handed over) started: the fetch gets until
 * [fetchMs] on it, the whole refresh until [overallMs]. When time is up it draws nothing more.
 * It first shows "loading" on [requestedIds], the widgets the refresh was asked about.
 */
suspend fun <V> refreshWidget(
    ticket: Long,
    publisher: WidgetPublisher,
    readPrefs: suspend () -> WeatherStore.Prefs,
    repo: ForecastRepository,
    surface: WidgetSurface<V>,
    requestedIds: IntArray = IntArray(0),
    startedNanos: Long = System.nanoTime(),
    overallMs: Long = WIDGET_REFRESH_MS,
    fetchMs: Long = WIDGET_FETCH_MS,
) {
    val deadline = startedNanos + overallMs * 1_000_000
    withTimeoutOrNull(maxOf(0L, (deadline - System.nanoTime()) / 1_000_000)) {
        publisher.showLoading(ticket, requestedIds, surface, deadline)
        val found = loadForWidget(readPrefs, repo, startedNanos + fetchMs * 1_000_000)
        publisher.publish(ticket, found, readPrefs, surface, deadline)
    }
}
