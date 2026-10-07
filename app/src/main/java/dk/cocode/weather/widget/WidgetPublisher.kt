package dk.cocode.weather.widget

import dk.cocode.weather.data.WeatherStore
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicLong

/** Where a refresh draws. [V] is what the platform draws with (RemoteViews in the app). */
interface WidgetSurface<V> {
    fun build(found: WidgetLoad): V

    /** What a widget shows while a refresh works on it. */
    fun loading(): V

    /** What a widget shows when a refresh ran out of time: needs no data, so it can always be built. */
    fun unavailable(): V

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

    /** The refresh that has drawn a result (the forecast, or its own "unavailable"), if any. */
    private val drewResult = AtomicLong()

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
            drewResult.set(ticket)
            surface.update(id, views)
        }
        true
    }

    /**
     * Shows the "loading" views on [ids] while a refresh works, unless a newer refresh has started
     * (its result, or its own "loading", must not be overwritten by an older one that is late) or
     * the time is up. Looks at both before each widget, under the same lock as [publish]. Returns
     * the widgets it drew on.
     */
    suspend fun <V> showLoading(
        ticket: Long,
        ids: IntArray,
        surface: WidgetSurface<V>,
        deadlineNanos: Long? = null,
    ): IntArray = lock.withLock {
        val views = surface.loading()
        val drawn = mutableListOf<Int>()
        for (id in ids) {
            if (ticket != newest.get() || timeIsUp(deadlineNanos)) break
            surface.update(id, views)
            drawn += id
        }
        drawn.toIntArray()
    }

    /**
     * The last step of a refresh that drew "loading" on [ids] and then ran out of time, failed or
     * was cancelled without drawing a result: replaces that "loading" with the "unavailable" view,
     * so the widget does not stay on it until some later refresh succeeds. It uses no stored data,
     * and draws nothing if a newer refresh has started (it owns the widget now) or this one already
     * drew a result.
     */
    suspend fun <V> settle(
        ticket: Long,
        ids: IntArray,
        surface: WidgetSurface<V>,
        deadlineNanos: Long? = null,
    ) = lock.withLock {
        if (ticket != newest.get() || drewResult.get() == ticket) return@withLock
        val views = surface.unavailable()
        for (id in ids) {
            if (timeIsUp(deadlineNanos)) return@withLock
            surface.update(id, views)
        }
    }

    private fun timeIsUp(deadlineNanos: Long?) = deadlineNanos != null && System.nanoTime() - deadlineNanos > 0
}
