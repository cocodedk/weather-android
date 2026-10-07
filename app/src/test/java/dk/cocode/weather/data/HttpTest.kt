package dk.cocode.weather.data

import dk.cocode.weather.SlowServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException

/**
 * Http against a local server that answers badly on purpose. Every test ends by checking that the
 * connection is closed while the server is still up, so a worker stuck in a read is not hidden by
 * the fixture closing the server.
 */
class HttpTest {

    private suspend fun giveUp(server: SlowServer, totalTimeoutMs: Long, message: String) {
        try {
            // A safety net for the test only: the call itself must end before it.
            withTimeout(8_000) { Http.getString(server.url, totalTimeoutMs) }
            fail("the call cannot finish: $message")
        } catch (e: IOException) {
            assertTrue(e.message.orEmpty(), e.message.orEmpty().contains("too long"))
        }
    }

    @Test
    fun aNormalAnswerComesBackWhole() = runBlocking {
        SlowServer(SlowServer.Mode.OK).use { server ->
            assertEquals("""{"utc_offset_seconds":1}""", Http.getString(server.url))
        }
    }

    @Test
    fun cancellingAStalledRequestReturnsAtOnceAndTheConnectionEnds() = runBlocking {
        SlowServer(SlowServer.Mode.SILENT).use { server ->
            val call = async(Dispatchers.Default) { Http.getString(server.url, totalTimeoutMs = 3_000) }
            assertTrue("the request never reached the server", server.awaitRequest(5_000))

            val started = System.nanoTime()
            call.cancelAndJoin() // the caller gives up once the server holds the request
            val tookMs = (System.nanoTime() - started) / 1_000_000
            // A caller that waited for the blocked read would sit there for its whole read timeout (3 s).
            assertTrue("cancelling took $tookMs ms", tookMs < 1_500)
            assertTrue("the connection was left open", server.awaitClientGone(8_000))
        }
    }

    @Test
    fun aResponseThatTricklesInIsGivenUpOnAfterTheTotalTimeout() = runBlocking {
        SlowServer(SlowServer.Mode.TRICKLE).use { server ->
            giveUp(server, totalTimeoutMs = 1_000, message = "the response never ends")
            assertTrue("the request never reached the server", server.awaitRequest(0))
            assertTrue("the connection was left open", server.awaitClientGone(1_000))
        }
    }

    @Test
    fun anErrorBodyThatTricklesInIsGivenUpOnToo() = runBlocking {
        SlowServer(SlowServer.Mode.ERROR_TRICKLE).use { server ->
            giveUp(server, totalTimeoutMs = 1_000, message = "the error body never ends")
            assertTrue("the request never reached the server", server.awaitRequest(0))
            assertTrue("the connection was left open", server.awaitClientGone(1_000))
        }
    }

    @Test
    fun aCappedReadTakesExactlyTheCapAndNoMore() {
        val source = ByteArrayInputStream(ByteArray(10_000) { 'x'.code.toByte() })

        val text = Http.readChunks(source, { true }, Http.ERROR_BODY_BYTES)

        assertEquals(Http.ERROR_BODY_BYTES, text.length)
        assertEquals(10_000 - Http.ERROR_BODY_BYTES, source.available()) // nothing past the cap was taken
    }

    @Test
    fun anErrorBodyIsReadOnlyUpToItsCap() = runBlocking {
        SlowServer(SlowServer.Mode.ERROR_FLOOD).use { server ->
            val started = System.nanoTime()
            try {
                Http.getString(server.url, totalTimeoutMs = 3_000)
                fail("a 500 is an error")
            } catch (e: IOException) {
                // The status, not "took too long": reading stopped at the cap instead of at the deadline.
                assertTrue(e.message.orEmpty(), e.message.orEmpty().startsWith("HTTP 500"))
            }
            val tookMs = (System.nanoTime() - started) / 1_000_000
            assertTrue("took $tookMs ms", tookMs < 2_500)
            assertTrue("the connection was left open", server.awaitClientGone(1_000))
        }
    }

    @Test
    fun aRequestCancelledBeforeItStartsNeverConnects() = runBlocking {
        SlowServer(SlowServer.Mode.OK).use { server ->
            val call = launch(Dispatchers.Default) {
                coroutineContext.job.cancel() // gone before the worker has even started
                Http.getString(server.url)
            }
            call.join()

            // Not even a connection: closing the connection later is no use if the worker connects anyway.
            assertFalse("a cancelled request connected to the server", server.awaitConnection(500))
        }
    }
}
