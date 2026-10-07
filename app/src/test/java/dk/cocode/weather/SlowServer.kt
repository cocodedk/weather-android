package dk.cocode.weather

import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * A local HTTP server for tests that answers badly on purpose, so the real Http client is the one
 * under test: [Mode.SILENT] sends the headers and then nothing, [Mode.TRICKLE] sends one byte at a
 * time for ever, [Mode.ERROR_TRICKLE] does that behind a "500", [Mode.ERROR_FLOOD] sends an endless
 * error body as fast as it can, [Mode.OK] answers properly.
 */
class SlowServer(private val mode: Mode) : AutoCloseable {
    enum class Mode { SILENT, TRICKLE, ERROR_TRICKLE, ERROR_FLOOD, OK }

    private val server = ServerSocket(0, 1, InetAddress.getLoopbackAddress())
    private val gone = CountDownLatch(1)
    private val requested = CountDownLatch(1)
    private val connected = CountDownLatch(1)
    private var client: Socket? = null

    val url: String get() = "http://127.0.0.1:${server.localPort}/"

    init {
        Thread({ serve() }, "slow-server").apply { isDaemon = true }.start()
    }

    /** True once a client has opened a connection, before it has sent anything. */
    fun awaitConnection(ms: Long): Boolean = connected.await(ms, TimeUnit.MILLISECONDS)

    /** True once the server has read the whole request, so the client has connected and sent it. */
    fun awaitRequest(ms: Long): Boolean = requested.await(ms, TimeUnit.MILLISECONDS)

    /** True once the client has closed its end of the connection. */
    fun awaitClientGone(ms: Long): Boolean = gone.await(ms, TimeUnit.MILLISECONDS)

    private fun serve() {
        try {
            server.accept().use { socket ->
                client = socket
                connected.countDown()
                val input = socket.getInputStream()
                val out = socket.getOutputStream()
                // Read the request up to its blank line.
                var tail = 0
                while (tail != 0x0d0a0d0a) {
                    val b = input.read()
                    if (b < 0) return
                    tail = (tail shl 8) or b
                }
                requested.countDown()
                val body = """{"utc_offset_seconds":1}"""
                val failing = mode == Mode.ERROR_TRICKLE || mode == Mode.ERROR_FLOOD
                val length = if (mode == Mode.OK) body.length else 2_000_000_000
                val status = if (failing) "500 Internal Server Error" else "200 OK"
                out.write("HTTP/1.1 $status\r\nContent-Type: application/json\r\nContent-Length: $length\r\nConnection: close\r\n\r\n".toByteArray())
                out.flush()
                when (mode) {
                    Mode.OK -> { out.write(body.toByteArray()); out.flush() }
                    Mode.SILENT -> while (input.read() >= 0) { /* wait for the client to hang up */ }
                    Mode.TRICKLE, Mode.ERROR_TRICKLE -> while (true) { out.write(' '.code); out.flush(); Thread.sleep(50) }
                    Mode.ERROR_FLOOD -> {
                        val filler = ByteArray(64 * 1024) { ' '.code.toByte() }
                        while (true) out.write(filler)
                    }
                }
            }
        } catch (_: Exception) {
            // The client hung up, or the server was closed: either way the test is over.
        } finally {
            gone.countDown()
        }
    }

    override fun close() {
        runCatching { client?.close() }
        runCatching { server.close() }
    }
}
