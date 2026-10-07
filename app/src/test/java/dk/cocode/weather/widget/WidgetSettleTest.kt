package dk.cocode.weather.widget

import dk.cocode.weather.data.Forecast
import dk.cocode.weather.data.ForecastApi
import dk.cocode.weather.data.ForecastCache
import dk.cocode.weather.data.ForecastRepository
import dk.cocode.weather.data.Place
import dk.cocode.weather.data.WeatherStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

/** What a widget shows while a refresh works, and how a refresh that cannot finish ends. */
class WidgetSettleTest {
    private val copenhagen = Place(name = "Copenhagen", latitude = 55.68, longitude = 12.57)
    private val forecast: Forecast = ForecastApi.parse("{}")

    private class SavedCache(private val saved: Forecast?) : ForecastCache {
        override suspend fun cacheForecast(cacheKey: String, body: String) {}
        override suspend fun cachedForecast(cacheKey: String): Forecast? = saved
    }

    private fun prefs() = WeatherStore.Prefs(
        places = listOf(copenhagen), selectedKey = copenhagen.key, imperial = false, theme = "auto",
    )

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
    fun showingLoadingStopsAtTheDeadlineWidgetByWidgetAndThoseItDrewOnEndUnavailable() = runBlocking {
        val calls = AtomicInteger()
        val surface = FakeSurface(intArrayOf(1, 2, 3), beforeUpdate = { if (calls.getAndIncrement() == 0) Thread.sleep(600) })
        val publisher = WidgetPublisher()

        refreshWidget(
            publisher.begin(), publisher, { prefs() }, ForecastRepository(SavedCache(null)) { forecast to "{}" },
            surface, requestedIds = intArrayOf(1, 2, 3), overallMs = 300,
        )

        // "loading" only reached widget 1 before time was up, and that is the widget that is settled.
        assertEquals(listOf(1 to "loading", 1 to "unavailable"), surface.updates)
    }

    @Test
    fun aRefreshThatRunsOutOfTimeEndsOnUnavailableNotLoading() = runBlocking {
        val surface = FakeSurface(intArrayOf(7))
        val publisher = WidgetPublisher()
        val stuckPrefs: suspend () -> WeatherStore.Prefs = { delay(5_000); prefs() }
        val started = System.nanoTime()

        refreshWidget(
            publisher.begin(), publisher, stuckPrefs, ForecastRepository(SavedCache(forecast)) { forecast to "{}" },
            surface, requestedIds = intArrayOf(7), overallMs = 400,
        )
        val tookMs = (System.nanoTime() - started) / 1_000_000

        assertEquals(listOf(7 to "loading", 7 to "unavailable"), surface.updates)
        assertTrue("took $tookMs ms", tookMs < 3_000)
    }

    @Test
    fun aRefreshThatFailsWhileDrawingStillEndsOnUnavailable() = runBlocking {
        val inner = FakeSurface(intArrayOf(7))
        val failing = object : WidgetSurface<String> by inner {
            override fun build(found: WidgetLoad): String = throw IllegalStateException("cannot build")
        }
        val publisher = WidgetPublisher()

        try {
            refreshWidget(
                publisher.begin(), publisher, { prefs() }, ForecastRepository(SavedCache(null)) { forecast to "{}" },
                failing, requestedIds = intArrayOf(7),
            )
            fail("the failure must not be swallowed")
        } catch (e: IllegalStateException) {
            assertEquals("cannot build", e.message)
        }

        assertEquals(listOf(7 to "loading", 7 to "unavailable"), inner.updates)
    }

    @Test
    fun aCancelledRefreshStillEndsOnUnavailableEvenIfAnotherDrawHoldsTheLock() = runBlocking {
        val loadingShown = CompletableDeferred<Unit>()
        val surface = FakeSurface(intArrayOf(7), beforeUpdate = { loadingShown.complete(Unit) })
        val publisher = WidgetPublisher()
        val neverAnswers = ForecastRepository(SavedCache(null)) { CompletableDeferred<Nothing>().await() }
        val ticket = publisher.begin()
        val refresh = launch(Dispatchers.Default) {
            refreshWidget(ticket, publisher, { prefs() }, neverAnswers, surface, requestedIds = intArrayOf(7))
        }
        withTimeout(5_000) { loadingShown.await() }

        // Something else is in the middle of a draw, holding the lock, when the refresh is cancelled.
        val inDraw = CompletableDeferred<Unit>()
        val release = CountDownLatch(1)
        val busy = FakeSurface(intArrayOf(9), beforeUpdate = { inDraw.complete(Unit); release.await() })
        try {
            val holder = async(Dispatchers.Default) { publisher.showLoading(ticket, intArrayOf(9), busy) }
            withTimeout(5_000) { inDraw.await() }

            refresh.cancel()
            delay(300) // long enough for the cancelled refresh to reach its last step and wait for the lock
            release.countDown()
            withTimeout(5_000) { refresh.join(); holder.await() }
        } finally {
            release.countDown()
        }

        assertEquals(listOf(7 to "loading", 7 to "unavailable"), surface.updates)
    }

    @Test
    fun aNewerRefreshOwnsTheWidgetSoAnOlderOneDoesNotSettleOverIt() = runBlocking {
        val surface = FakeSurface(intArrayOf(7))
        val publisher = WidgetPublisher()
        val older = publisher.begin()
        publisher.showLoading(older, intArrayOf(7), surface)
        val newer = publisher.begin()
        val ready = WidgetLoad.Ready(copenhagen, ForecastRepository.Loaded(forecast, stale = false), imperial = false)
        publisher.publish(newer, ready, { prefs() }, surface)

        publisher.settle(older, intArrayOf(7), surface)

        assertEquals(listOf(7 to "loading", 7 to "Copenhagen"), surface.updates)
    }

    @Test
    fun settlingDrawsNothingOnceItsTimeIsUp() = runBlocking {
        val surface = FakeSurface(intArrayOf(7))
        val publisher = WidgetPublisher()

        publisher.settle(publisher.begin(), intArrayOf(7), surface, deadlineNanos = System.nanoTime() - 1)

        assertEquals(emptyList<Pair<Int, String>>(), surface.updates)
    }
}
