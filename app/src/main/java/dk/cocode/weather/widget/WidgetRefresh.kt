package dk.cocode.weather.widget

import dk.cocode.weather.data.ForecastRepository
import dk.cocode.weather.data.WeatherStore
import kotlinx.coroutines.withTimeoutOrNull

/**
 * One whole refresh, from loading to drawing, against a clock that [startedNanos] (a
 * System.nanoTime() value taken when the broadcast was handed over) started: the fetch gets until
 * [fetchMs] on it, the whole refresh until [overallMs]. When time is up it draws nothing more.
 * Until it has a result to draw it draws nothing, so a refresh that runs out of time, fails or is
 * cancelled leaves every widget showing what it showed before. Only a widget that has never been
 * drawn gets something first: the clickable placeholder, so it can be tapped whatever happens next.
 */
suspend fun <V> refreshWidget(
    ticket: Long,
    publisher: WidgetPublisher,
    readPrefs: suspend () -> WeatherStore.Prefs,
    repo: ForecastRepository,
    surface: WidgetSurface<V>,
    startedNanos: Long = System.nanoTime(),
    overallMs: Long = WIDGET_REFRESH_MS,
    fetchMs: Long = WIDGET_FETCH_MS,
) {
    val deadline = startedNanos + overallMs * 1_000_000
    withTimeoutOrNull(maxOf(0L, (deadline - System.nanoTime()) / 1_000_000)) {
        publisher.showPlaceholders(ticket, surface, deadline)
        val found = loadForWidget(readPrefs, repo, startedNanos + fetchMs * 1_000_000)
        publisher.publish(ticket, found, readPrefs, surface, deadline)
    }
}
