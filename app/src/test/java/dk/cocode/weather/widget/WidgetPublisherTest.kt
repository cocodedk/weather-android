package dk.cocode.weather.widget

import dk.cocode.weather.data.ForecastApi
import dk.cocode.weather.data.ForecastRepository
import dk.cocode.weather.data.Place
import dk.cocode.weather.data.WeatherStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Collections
import java.util.concurrent.CountDownLatch

class WidgetPublisherTest {
    private val spotA = Place(name = "A", latitude = 55.0, longitude = 12.0, isDeviceLocation = true)
    private val spotB = spotA.copy(name = "B", latitude = 35.0, longitude = 139.0)

    private fun ready(place: Place) = WidgetLoad.Ready(
        place, ForecastRepository.Loaded(ForecastApi.parse("{}"), stale = false), imperial = false,
    )

    private fun prefs(selected: Place) = WeatherStore.Prefs(
        places = listOf(selected), selectedKey = selected.key, imperial = false, theme = "auto",
    )

    /** What reached the home screen, in order. */
    private val drawn = Collections.synchronizedList(mutableListOf<Place?>())
    private val draw: (WidgetLoad) -> Unit = { drawn += it.place }

    @Test
    fun aRefreshThatPassedTheCheckFinishesDrawingBeforeANewerOneDraws() = runBlocking {
        val publisher = WidgetPublisher()
        var saved = prefs(spotA)
        val drawing = CompletableDeferred<Unit>()
        val resume = CountDownLatch(1)

        val ticketA = publisher.begin()
        val refreshA = async(Dispatchers.Default) {
            // A passes the check, then stalls in the middle of drawing.
            publisher.publish(ticketA, ready(spotA), { saved }) { found ->
                drawing.complete(Unit)
                resume.await()
                draw(found)
            }
        }
        withTimeout(5_000) { drawing.await() }

        saved = prefs(spotB) // the phone moves on
        val ticketB = publisher.begin()
        val refreshB = async(Dispatchers.Default) { publisher.publish(ticketB, ready(spotB), { saved }, draw) }
        Thread.sleep(200) // B is ready to draw while A is still mid-draw
        resume.countDown() // A resumes
        refreshA.await()
        refreshB.await()

        assertEquals(listOf<Place?>(spotA, spotB), drawn.toList()) // B is the last on screen
    }

    @Test
    fun aRefreshThatIsNoLongerTheNewestDrawsNothing() = runBlocking {
        val publisher = WidgetPublisher()
        val ticketA = publisher.begin()
        val ticketB = publisher.begin() // a newer refresh has started

        assertFalse(publisher.publish(ticketA, ready(spotA), { prefs(spotA) }, draw))
        assertTrue(publisher.publish(ticketB, ready(spotA), { prefs(spotA) }, draw))
        assertEquals(listOf<Place?>(spotA), drawn.toList())
    }

    @Test
    fun aRefreshForAPlaceThatIsNoLongerSelectedDrawsNothing() = runBlocking {
        val publisher = WidgetPublisher()
        val ticket = publisher.begin()

        assertFalse(publisher.publish(ticket, ready(spotA), { prefs(spotB) }, draw))
        assertEquals(emptyList<Place?>(), drawn.toList())
    }

    @Test
    fun theNewestRefreshForTheSelectedPlaceDraws() = runBlocking {
        val publisher = WidgetPublisher()
        assertTrue(publisher.publish(publisher.begin(), ready(spotA), { prefs(spotA) }, draw))
        assertEquals(listOf<Place?>(spotA), drawn.toList())
    }
}
