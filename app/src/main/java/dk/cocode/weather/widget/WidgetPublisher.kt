package dk.cocode.weather.widget

import dk.cocode.weather.data.WeatherStore
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicLong

/** Where a refresh draws. [V] is what the platform draws with (RemoteViews in the app). */
interface WidgetSurface<V> {
    fun build(found: WidgetLoad): V

    /**
     * What a widget that has never been drawn shows until a result is ready. Android gives a new
     * widget its layout without click actions, so this has to be a full view that has them.
     */
    fun placeholder(): V

    /** Every widget on the home screen now, not just the ones a refresh was asked about. */
    fun allIds(): IntArray

    /** True once a full view has been drawn on [id] (the widget remembers it, so no stored data is read). */
    fun isDrawn(id: Int): Boolean

    /** Draws [views] on [id] and remembers that it has been drawn. */
    fun update(id: Int, views: V)
}

/**
 * Widget refreshes run side by side, so they can finish out of order. Each takes a number when it
 * starts, and drawing happens under one lock that re-checks that number and the saved selection.
 * A refresh that is no longer the newest, or whose place is no longer the selected one, draws
 * nothing (the refresh that change set off draws instead). One that passes the check finishes
 * drawing before the next refresh can check, so an old place never overwrites a newer one.
 *
 * A refresh draws only its result. Until then every widget keeps what it shows (the last forecast,
 * or "Forecast unavailable"), so a refresh that times out, fails or is cancelled leaves nothing
 * behind that a later refresh would have to clean up. The one exception is a widget that has never
 * been drawn: it gets the clickable placeholder first ([showPlaceholders]).
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
     * Gives every widget that has never been drawn the placeholder view, so that a new widget whose
     * first refresh runs out of time can still be tapped (Android's initial layout has no click
     * actions). Widgets that have been drawn keep their view. Needs no stored data, and follows the
     * same rules as [publish]: only the newest refresh, under the lock, stopping at the deadline.
     */
    suspend fun <V> showPlaceholders(
        ticket: Long,
        surface: WidgetSurface<V>,
        deadlineNanos: Long? = null,
    ) = lock.withLock {
        val placeholder by lazy { surface.placeholder() }
        for (id in surface.allIds()) {
            if (ticket != newest.get() || timeIsUp(deadlineNanos)) return@withLock
            if (!surface.isDrawn(id)) surface.update(id, placeholder)
        }
    }

    private fun timeIsUp(deadlineNanos: Long?) = deadlineNanos != null && System.nanoTime() - deadlineNanos > 0
}
