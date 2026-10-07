package dk.cocode.weather.widget

import dk.cocode.weather.data.ForecastApi
import dk.cocode.weather.data.ForecastRepository
import dk.cocode.weather.data.Place
import dk.cocode.weather.data.WeatherStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

/** A mutex that says when somebody asks for it, before they start waiting. */
private class SpyMutex(private val onAsked: (Int) -> Unit) : Mutex by Mutex() {
    private val inner = Mutex()
    private val asks = AtomicInteger()

    override suspend fun lock(owner: Any?) {
        onAsked(asks.incrementAndGet())
        inner.lock(owner)
    }

    override fun unlock(owner: Any?) = inner.unlock(owner)
}

class WidgetPublisherTest {
    private val spotA = Place(name = "A", latitude = 55.0, longitude = 12.0, isDeviceLocation = true)
    private val spotB = spotA.copy(name = "B", latitude = 35.0, longitude = 139.0)

    private fun ready(place: Place) = WidgetLoad.Ready(
        place, ForecastRepository.Loaded(ForecastApi.parse("{}"), stale = false), imperial = false,
    )

    private fun prefs(selected: Place) = WeatherStore.Prefs(
        places = listOf(selected), selectedKey = selected.key, imperial = false, theme = "auto",
    )

    @Test
    fun aRefreshThatPassedTheCheckFinishesDrawingBeforeANewerOneDraws() = runBlocking {
        val bAsked = CompletableDeferred<Unit>()
        val publisher = WidgetPublisher(SpyMutex { count -> if (count == 2) bAsked.complete(Unit) })
        val drawn = Collections.synchronizedList(mutableListOf<Pair<Int, String>>())
        var saved = prefs(spotA)

        val inDraw = CompletableDeferred<Unit>()
        val resume = CountDownLatch(1)
        val surfaceA = FakeSurface(intArrayOf(1), log = drawn, beforeUpdate = {
            inDraw.complete(Unit)
            resume.await() // A stalls in the middle of drawing
        })
        val surfaceB = FakeSurface(intArrayOf(1), log = drawn)

        try {
            val refreshA = async(Dispatchers.Default) {
                publisher.publish(publisher.begin(), ready(spotA), { saved }, surfaceA)
            }
            withTimeout(5_000) { inDraw.await() }

            saved = prefs(spotB) // the phone moves on
            val ticketB = publisher.begin()
            val refreshB = async(Dispatchers.Default) { publisher.publish(ticketB, ready(spotB), { saved }, surfaceB) }
            // B asked for the lock while A still holds it. (Without a lock, B never asks and this fails.)
            withTimeout(5_000) { bAsked.await() }
            assertEquals(emptyList<Pair<Int, String>>(), drawn.toList()) // nothing drawn yet: A is mid-draw

            resume.countDown() // A resumes
            withTimeout(5_000) { refreshA.await(); refreshB.await() }
            assertEquals(listOf(1 to "A", 1 to "B"), drawn.toList()) // B is the last on screen
        } finally {
            resume.countDown()
        }
    }

    @Test
    fun theNewestRefreshDrawsOnEveryWidgetEvenOnesAnOlderRefreshWasAskedAbout() = runBlocking {
        val publisher = WidgetPublisher()
        val surface = FakeSurface(intArrayOf(11, 12, 13))
        val askedAboutEleven = publisher.begin()
        val askedAboutTwelve = publisher.begin()

        assertFalse(publisher.publish(askedAboutEleven, ready(spotA), { prefs(spotA) }, surface))
        assertEquals(emptyList<Pair<Int, String>>(), surface.updates)

        assertTrue(publisher.publish(askedAboutTwelve, ready(spotA), { prefs(spotA) }, surface))
        assertEquals(listOf(11 to "A", 12 to "A", 13 to "A"), surface.updates)
    }

    @Test
    fun aRefreshForAPlaceThatIsNoLongerSelectedDrawsNothing() = runBlocking {
        val publisher = WidgetPublisher()
        val surface = FakeSurface(intArrayOf(1))

        assertFalse(publisher.publish(publisher.begin(), ready(spotA), { prefs(spotB) }, surface))
        assertEquals(emptyList<Pair<Int, String>>(), surface.updates)
    }

    @Test
    fun theNewestRefreshForTheSelectedPlaceDraws() = runBlocking {
        val publisher = WidgetPublisher()
        val surface = FakeSurface(intArrayOf(1))

        assertTrue(publisher.publish(publisher.begin(), ready(spotA), { prefs(spotA) }, surface))
        assertEquals(listOf(1 to "A"), surface.updates)
    }
}
