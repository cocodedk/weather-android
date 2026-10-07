package dk.cocode.weather.ui

import dk.cocode.weather.data.Place
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch

/**
 * A stand-in for the geocoding request. Like Http.getString it blocks a thread inside
 * withContext(Dispatchers.IO), so cancelling the caller takes effect only once the "network"
 * answers: [finish] is that moment, and the test decides when it comes.
 */
private class FakeLookup(private val answer: (String) -> List<Place>) {
    private val release = ConcurrentHashMap<String, CountDownLatch>()
    private val began = ConcurrentHashMap<String, CompletableDeferred<Unit>>()

    private fun latch(q: String) = release.getOrPut(q) { CountDownLatch(1) }
    private fun began(q: String) = began.getOrPut(q) { CompletableDeferred() }

    suspend fun search(q: String): List<Place> = withContext(Dispatchers.IO) {
        began(q).complete(Unit)
        latch(q).await()
        answer(q)
    }

    /** Suspends until the request for [q] is in flight. */
    suspend fun awaitInFlight(q: String) = withTimeout(5_000) { began(q).await() }

    /** The answer for [q] arrives. */
    fun finish(q: String) = latch(q).countDown()
}

class PlaceSearchTest {

    private fun place(name: String) = Place(name = name, latitude = 1.0, longitude = 2.0)

    private fun kotlinx.coroutines.CoroutineScope.searchOver(lookup: FakeLookup) =
        PlaceSearch(this, lookup = { lookup.search(it) }, debounceMs = 0)

    private fun kotlinx.coroutines.CoroutineScope.running(): List<Job> =
        coroutineContext.job.children.toList()

    @Test
    fun aKeystrokeThatReplacesARequestInFlightDoesNotLeaveSearchFailedOverTheNewResults() = runBlocking {
        val lookup = FakeLookup { listOf(place(it)) }
        val search = searchOver(lookup)

        search.onQueryChange("ab")
        val first = running().single()
        lookup.awaitInFlight("ab")
        search.onQueryChange("abc") // cancels the request for "ab" while it is on the wire
        lookup.awaitInFlight("abc")

        lookup.finish("ab") // the cancelled request returns first
        first.join()
        lookup.finish("abc")
        running().joinAll()

        val state = search.state.value
        assertFalse(state.failed)
        assertEquals(listOf(place("abc")), state.results)
        assertFalse(state.searching)
    }

    @Test
    fun emptyingTheBoxWhileARequestIsInFlightDoesNotMakeTheSearchFail() = runBlocking {
        val lookup = FakeLookup { listOf(place(it)) }
        val search = searchOver(lookup)

        search.onQueryChange("ab")
        val first = running().single()
        lookup.awaitInFlight("ab")
        search.onQueryChange("a") // too short to search: the box is cleared and the request dropped

        lookup.finish("ab")
        first.join()

        val state = search.state.value
        assertFalse(state.failed)
        assertEquals(emptyList<Place>(), state.results)
    }

    @Test
    fun anOlderRequestThatFailsLateDoesNotStickOverTheNewerAnswer() = runBlocking {
        val lookup = FakeLookup { if (it == "ab") throw IOException("HTTP 429") else listOf(place(it)) }
        val search = searchOver(lookup)

        search.onQueryChange("ab")
        val first = running().single()
        lookup.awaitInFlight("ab")
        search.onQueryChange("abc")
        lookup.awaitInFlight("abc")

        lookup.finish("ab") // the older request fails after the newer one has started
        first.join()
        lookup.finish("abc")
        running().joinAll()

        val state = search.state.value
        assertFalse(state.failed)
        assertEquals(listOf(place("abc")), state.results)
    }

    @Test
    fun aRequestThatFailsOnItsOwnStillShowsSearchFailed() = runBlocking {
        val lookup = FakeLookup { throw IOException("offline") }
        val search = searchOver(lookup)

        search.onQueryChange("ab")
        lookup.awaitInFlight("ab")
        lookup.finish("ab")
        running().joinAll()

        assertEquals(true, search.state.value.failed)
        assertFalse(search.state.value.searching)
    }
}
