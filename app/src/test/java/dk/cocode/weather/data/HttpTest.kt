package dk.cocode.weather.data

import dk.cocode.weather.SlowServer
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class HttpTest {

    @Test
    fun aNormalAnswerComesBackWhole() = runBlocking {
        SlowServer(SlowServer.Mode.OK).use { server ->
            assertEquals("""{"utc_offset_seconds":1}""", Http.getString(server.url))
        }
    }

    @Test
    fun cancellingAStalledRequestReturnsAtOnce() = runBlocking {
        SlowServer(SlowServer.Mode.SILENT).use { server ->
            val started = System.nanoTime()
            try {
                withTimeout(500) { Http.getString(server.url) }
                fail("the request cannot finish: the server never answers")
            } catch (e: TimeoutCancellationException) {
                // expected: the caller gave up
            }
            val tookMs = (System.nanoTime() - started) / 1_000_000
            // A caller that waited for the blocked read would sit there for its 12 s read timeout.
            assertTrue("gave up after $tookMs ms", tookMs < 4_000)
        }
    }

    @Test
    fun aResponseThatTricklesInIsGivenUpOnAfterTheTotalTimeout() = runBlocking {
        SlowServer(SlowServer.Mode.TRICKLE).use { server ->
            val started = System.nanoTime()
            try {
                // A safety net for the test only: the call itself must end well before it.
                withTimeout(8_000) { Http.getString(server.url, totalTimeoutMs = 500) }
                fail("the response never ends, so the call must fail")
            } catch (e: IOException) {
                assertTrue(e.message.orEmpty(), e.message.orEmpty().contains("too long"))
            }
            val tookMs = (System.nanoTime() - started) / 1_000_000
            assertTrue("gave up after $tookMs ms", tookMs < 4_000)
            assertTrue("the connection was left open", server.awaitClientGone(1_000))
        }
    }
}
