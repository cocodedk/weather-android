package dk.cocode.weather.widget

import dk.cocode.weather.SlowServer
import dk.cocode.weather.data.Forecast
import dk.cocode.weather.data.ForecastApi
import dk.cocode.weather.data.ForecastCache
import dk.cocode.weather.data.ForecastRepository
import dk.cocode.weather.data.Http
import dk.cocode.weather.data.Place
import dk.cocode.weather.data.WeatherStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

/** Whole refreshes, load and draw together, against the real Http client and a badly behaved server. */
class WidgetRefreshTest {
    private val copenhagen = Place(name = "Copenhagen", latitude = 55.68, longitude = 12.57)
    private val forecast: Forecast = ForecastApi.parse("{}")

    private class SavedCache(private val saved: Forecast?) : ForecastCache {
        override suspend fun cacheForecast(cacheKey: String, body: String) {}
        override suspend fun cachedForecast(cacheKey: String): Forecast? = saved
    }

    private fun prefs() = WeatherStore.Prefs(
        places = listOf(copenhagen), selectedKey = copenhagen.key, imperial = false, theme = "auto",
    )

    /** A fetch over the real Http client, against a server that sends one byte at a time for ever. */
    private fun trickling(server: SlowServer, saved: Forecast?) = ForecastRepository(SavedCache(saved)) {
        val body = Http.getString(server.url)
        ForecastApi.parse(body) to body
    }

    private suspend fun refreshAgainst(server: SlowServer, saved: Forecast?, surface: FakeSurface): Long {
        val publisher = WidgetPublisher()
        val started = System.nanoTime()
        refreshWidget(
            publisher.begin(), publisher, { prefs() }, trickling(server, saved), surface,
            overallMs = 4_000, fetchMs = 1_000,
        )
        return (System.nanoTime() - started) / 1_000_000
    }

    @Test
    fun aFetchThatNeverEndsDrawsTheSavedForecastNotLoading() = runBlocking {
        SlowServer(SlowServer.Mode.TRICKLE).use { server ->
            val surface = FakeSurface(intArrayOf(7))
            val tookMs = refreshAgainst(server, saved = forecast, surface)

            assertEquals(listOf(7 to "Copenhagen (saved)"), surface.updates)
            assertTrue("took $tookMs ms", tookMs < 3_500)
            assertTrue("the request never reached the server", server.awaitRequest(0))
            assertTrue("the connection was left open", server.awaitClientGone(1_000))
        }
    }

    @Test
    fun aFetchThatNeverEndsWithNothingSavedDrawsUnavailable() = runBlocking {
        SlowServer(SlowServer.Mode.TRICKLE).use { server ->
            val surface = FakeSurface(intArrayOf(7))
            val tookMs = refreshAgainst(server, saved = null, surface)

            assertEquals(listOf(7 to "unavailable: Copenhagen"), surface.updates)
            assertTrue("took $tookMs ms", tookMs < 3_500)
            assertTrue("the connection was left open", server.awaitClientGone(1_000))
        }
    }

    @Test
    fun aSlowPrefsReadLeavesTheFetchOnlyWhatIsLeftOfItsBudget() = runBlocking {
        SlowServer(SlowServer.Mode.TRICKLE).use { server ->
            val reads = AtomicInteger()
            val slowPrefs: suspend () -> WeatherStore.Prefs = {
                if (reads.getAndIncrement() == 0) delay(3_000) // the first read, which finds the selected place
                prefs()
            }
            val surface = FakeSurface(intArrayOf(7))
            val publisher = WidgetPublisher()
            val started = System.nanoTime()

            // The fetch may run until 3.5 s, so after a 3 s read it has half a second, and the saved
            // forecast is drawn well before the overall 5 s. A fetch budget worked out before the
            // read would run until 6.5 s, past the overall deadline, and nothing would be drawn.
            refreshWidget(
                publisher.begin(), publisher, slowPrefs, trickling(server, forecast), surface,
                overallMs = 5_000, fetchMs = 3_500,
            )
            val tookMs = (System.nanoTime() - started) / 1_000_000

            assertEquals(listOf(7 to "Copenhagen (saved)"), surface.updates)
            assertTrue("took $tookMs ms", tookMs < 4_500)
            assertTrue("the request never reached the server", server.awaitRequest(0))
            assertTrue("the connection was left open", server.awaitClientGone(1_000))
        }
    }

    @Test
    fun aRefreshShowsLoadingOnTheWidgetsItWasAskedAboutThenTheForecastOnAll() = runBlocking {
        val surface = FakeSurface(intArrayOf(1, 2))
        val publisher = WidgetPublisher()

        refreshWidget(
            publisher.begin(), publisher, { prefs() }, ForecastRepository(SavedCache(null)) { forecast to "{}" },
            surface, requestedIds = intArrayOf(1),
        )

        assertEquals(listOf(1 to "loading", 1 to "Copenhagen", 2 to "Copenhagen"), surface.updates)
    }

    @Test
    fun anOlderRefreshDoesNotShowLoadingOverANewerOnesResult() = runBlocking {
        val surface = FakeSurface(intArrayOf(1))
        val publisher = WidgetPublisher()
        val older = publisher.begin()
        val newer = publisher.begin()
        val ready = WidgetLoad.Ready(copenhagen, ForecastRepository.Loaded(forecast, stale = false), imperial = false)
        publisher.publish(newer, ready, { prefs() }, surface)

        publisher.showLoading(older, intArrayOf(1), surface) // the older refresh is late

        assertEquals(listOf(1 to "Copenhagen"), surface.updates)
    }

    @Test
    fun showingLoadingStopsAtTheDeadlineWidgetByWidget() = runBlocking {
        val calls = AtomicInteger()
        val surface = FakeSurface(intArrayOf(1, 2, 3), beforeUpdate = { if (calls.getAndIncrement() == 0) Thread.sleep(600) })
        val publisher = WidgetPublisher()

        refreshWidget(
            publisher.begin(), publisher, { prefs() }, ForecastRepository(SavedCache(null)) { forecast to "{}" },
            surface, requestedIds = intArrayOf(1, 2, 3), overallMs = 300,
        )

        assertEquals(listOf(1 to "loading"), surface.updates)
    }

    @Test
    fun aPublicationThatRunsPastTheDeadlineDoesNotDrawTheRemainingWidgets() = runBlocking {
        val calls = AtomicInteger()
        // The first widget's update blocks for longer than the whole refresh is allowed to take.
        val surface = FakeSurface(intArrayOf(1, 2, 3), beforeUpdate = { if (calls.getAndIncrement() == 0) Thread.sleep(600) })
        val publisher = WidgetPublisher()

        refreshWidget(
            publisher.begin(), publisher, { prefs() }, ForecastRepository(SavedCache(null)) { forecast to "{}" },
            surface, overallMs = 300,
        )

        assertEquals(listOf(1), surface.updates.map { it.first })
    }

    @Test
    fun aRefreshWhoseClockStartedBeforeItWasDispatchedGivesUpAtOnce() = runBlocking {
        val surface = FakeSurface(intArrayOf(1))
        val publisher = WidgetPublisher()
        val handedOver = System.nanoTime() - 20_000_000_000L // the broadcast was handed over 20 s ago

        refreshWidget(
            publisher.begin(), publisher, { prefs() }, ForecastRepository(SavedCache(null)) { forecast to "{}" },
            surface, startedNanos = handedOver,
        )

        assertEquals(emptyList<Pair<Int, String>>(), surface.updates)
    }

    @Test
    fun aRefreshStuckBehindTheLockGivesUpAtTheOverallDeadlineAndDrawsNothing() = runBlocking {
        val publisher = WidgetPublisher()
        val inDraw = CompletableDeferred<Unit>()
        val resume = CountDownLatch(1)
        val stalled = FakeSurface(intArrayOf(1), beforeUpdate = { inDraw.complete(Unit); resume.await() })
        val waiting = FakeSurface(intArrayOf(2))
        val ready = WidgetLoad.Ready(copenhagen, ForecastRepository.Loaded(forecast, stale = false), imperial = false)

        try {
            // An earlier refresh passed its check and is stuck in the middle of drawing, holding the lock.
            val holder = async(Dispatchers.Default) { publisher.publish(publisher.begin(), ready, { prefs() }, stalled) }
            withTimeout(5_000) { inDraw.await() }

            val started = System.nanoTime()
            // A safety net for the test only: the refresh itself must end before it.
            withTimeout(5_000) {
                refreshWidget(
                    publisher.begin(), publisher, { prefs() }, ForecastRepository(SavedCache(null)) { forecast to "{}" },
                    waiting, overallMs = 500,
                )
            }
            val tookMs = (System.nanoTime() - started) / 1_000_000

            assertTrue("gave up after $tookMs ms", tookMs < 3_000)
            assertEquals(emptyList<Pair<Int, String>>(), waiting.updates)
            resume.countDown()
            holder.await()
            Unit
        } finally {
            resume.countDown()
        }
    }
}
