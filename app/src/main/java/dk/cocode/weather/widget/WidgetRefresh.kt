package dk.cocode.weather.widget

import dk.cocode.weather.data.ForecastRepository
import dk.cocode.weather.data.WeatherStore
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * One whole refresh, from loading to drawing, against a clock that [startedNanos] (a
 * System.nanoTime() value taken when the broadcast was handed over) started: the fetch gets until
 * [fetchMs] on it, the whole refresh until [overallMs]. When time is up it draws nothing more.
 * It first shows "loading" on [requestedIds], the widgets the refresh was asked about, and if it
 * then ends without drawing a result (out of time, an exception, cancelled) it replaces that
 * "loading" with "unavailable" before returning, a step that gets until [settleMs].
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
    settleMs: Long = WIDGET_SETTLE_MS,
) {
    val deadline = startedNanos + overallMs * 1_000_000
    val settleDeadline = startedNanos + settleMs * 1_000_000
    var loadingOn = IntArray(0)
    try {
        withTimeoutOrNull(maxOf(0L, (deadline - System.nanoTime()) / 1_000_000)) {
            loadingOn = publisher.showLoading(ticket, requestedIds, surface, deadline)
            val found = loadForWidget(readPrefs, repo, startedNanos + fetchMs * 1_000_000)
            publisher.publish(ticket, found, readPrefs, surface, deadline)
        }
    } finally {
        if (loadingOn.isNotEmpty()) {
            // Not cancellable, so it also runs for a refresh that was cancelled; bounded by its own deadline.
            withContext(NonCancellable) {
                withTimeoutOrNull(maxOf(0L, (settleDeadline - System.nanoTime()) / 1_000_000)) {
                    publisher.settle(ticket, loadingOn, surface, settleDeadline)
                }
            }
        }
    }
}
