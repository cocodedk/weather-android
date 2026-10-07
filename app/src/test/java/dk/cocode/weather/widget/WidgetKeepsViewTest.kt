package dk.cocode.weather.widget

import dk.cocode.weather.data.Forecast
import dk.cocode.weather.data.ForecastApi
import dk.cocode.weather.data.ForecastCache
import dk.cocode.weather.data.ForecastRepository
import dk.cocode.weather.data.Place
import dk.cocode.weather.data.WeatherStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * A refresh draws only its result. One that times out, fails or is cancelled therefore calls
 * nothing on any widget, which is to say every widget keeps the view it already had.
 */
class WidgetKeepsViewTest {
    private val copenhagen = Place(name = "Copenhagen", latitude = 55.68, longitude = 12.57)
    private val forecast: Forecast = ForecastApi.parse("{}")
    private val noUpdates = emptyList<Pair<Int, String>>()

    private class SavedCache(private val saved: Forecast?) : ForecastCache {
        override suspend fun cacheForecast(cacheKey: String, body: String) {}
        override suspend fun cachedForecast(cacheKey: String): Forecast? = saved
    }

    private fun prefs() = WeatherStore.Prefs(
        places = listOf(copenhagen), selectedKey = copenhagen.key, imperial = false, theme = "auto",
    )

    private val answering = ForecastRepository(SavedCache(null)) { forecast to "{}" }

    @Test
    fun aRefreshThatTimesOutLeavesEveryWidgetUntouched() = runBlocking {
        val surface = FakeSurface(intArrayOf(1, 2, 3))
        val publisher = WidgetPublisher()
        val stuckPrefs: suspend () -> WeatherStore.Prefs = { delay(5_000); prefs() }
        val started = System.nanoTime()

        refreshWidget(publisher.begin(), publisher, stuckPrefs, answering, surface, overallMs = 400)
        val tookMs = (System.nanoTime() - started) / 1_000_000

        assertEquals(noUpdates, surface.updates)
        assertTrue("took $tookMs ms", tookMs < 3_000)
    }

    @Test
    fun aNeverDrawnWidgetWhoseRefreshTimesOutEndsOnTheClickablePlaceholder() = runBlocking {
        // Widget 2 has just been added: Android's own layout has no click actions, so it is given the placeholder.
        val surface = FakeSurface(intArrayOf(1, 2, 3), neverDrawn = setOf(2))
        val publisher = WidgetPublisher()
        val stuckPrefs: suspend () -> WeatherStore.Prefs = { delay(5_000); prefs() }

        refreshWidget(publisher.begin(), publisher, stuckPrefs, answering, surface, overallMs = 400)

        assertEquals(listOf(2 to "placeholder"), surface.updates) // widgets 1 and 3 keep their views
    }

    @Test
    fun onceAWidgetHasBeenDrawnALaterRefreshThatTimesOutLeavesItAlone() = runBlocking {
        val surface = FakeSurface(intArrayOf(1), neverDrawn = setOf(1))
        val publisher = WidgetPublisher()
        refreshWidget(publisher.begin(), publisher, { prefs() }, answering, surface)
        assertEquals(listOf(1 to "placeholder", 1 to "Copenhagen"), surface.updates)

        val stuckPrefs: suspend () -> WeatherStore.Prefs = { delay(5_000); prefs() }
        refreshWidget(publisher.begin(), publisher, stuckPrefs, answering, surface, overallMs = 400)

        assertEquals(listOf(1 to "placeholder", 1 to "Copenhagen"), surface.updates)
    }

    @Test
    fun anOlderRefreshDoesNotGiveANeverDrawnWidgetItsPlaceholder() = runBlocking {
        val surface = FakeSurface(intArrayOf(1), neverDrawn = setOf(1))
        val publisher = WidgetPublisher()
        val older = publisher.begin()
        publisher.begin() // a newer refresh owns the widgets now

        publisher.showPlaceholders(older, surface)

        assertEquals(noUpdates, surface.updates)
    }

    @Test
    fun placeholdersAreNotDrawnOnceTheTimeIsUp() = runBlocking {
        val surface = FakeSurface(intArrayOf(1), neverDrawn = setOf(1))
        val publisher = WidgetPublisher()

        publisher.showPlaceholders(publisher.begin(), surface, deadlineNanos = System.nanoTime() - 1)

        assertEquals(noUpdates, surface.updates)
    }

    @Test
    fun aRefreshThatFailsReadingTheSavedChoiceLeavesEveryWidgetUntouched() = runBlocking {
        val surface = FakeSurface(intArrayOf(1, 2, 3))
        val publisher = WidgetPublisher()

        try {
            refreshWidget(
                publisher.begin(), publisher, { throw IllegalStateException("storage failed") }, answering, surface,
            )
            fail("the failure must not be swallowed")
        } catch (e: IllegalStateException) {
            assertEquals("storage failed", e.message)
        }

        assertEquals(noUpdates, surface.updates)
    }

    @Test
    fun aRefreshThatFailsBuildingTheViewsLeavesEveryWidgetUntouched() = runBlocking {
        val inner = FakeSurface(intArrayOf(1, 2, 3))
        val failing = object : WidgetSurface<String> by inner {
            override fun build(found: WidgetLoad): String = throw IllegalStateException("cannot build")
        }
        val publisher = WidgetPublisher()

        try {
            refreshWidget(publisher.begin(), publisher, { prefs() }, answering, failing)
            fail("the failure must not be swallowed")
        } catch (e: IllegalStateException) {
            assertEquals("cannot build", e.message)
        }

        assertEquals(noUpdates, inner.updates)
    }

    @Test
    fun aCancelledRefreshLeavesEveryWidgetUntouched() = runBlocking {
        val surface = FakeSurface(intArrayOf(1, 2, 3))
        val publisher = WidgetPublisher()
        val inFetch = CompletableDeferred<Unit>()
        val neverAnswers = ForecastRepository(SavedCache(null)) {
            inFetch.complete(Unit)
            CompletableDeferred<Nothing>().await()
        }

        val refresh = launch(Dispatchers.Default) {
            refreshWidget(publisher.begin(), publisher, { prefs() }, neverAnswers, surface)
        }
        withTimeout(5_000) { inFetch.await() }
        refresh.cancelAndJoin()

        assertEquals(noUpdates, surface.updates)
    }
}
