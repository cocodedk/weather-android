package dk.cocode.weather.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Minimal GET-a-string helper. Open-Meteo needs no auth, no retries beyond the
 * caller's, and no request body, so HttpURLConnection is enough and keeps a
 * networking library out of the dependency list.
 */
object Http {

    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 12_000
    private const val TOTAL_TIMEOUT_MS = 20_000L
    private const val CHUNK_BYTES = 8 * 1024

    /**
     * Where the blocking work runs, apart from the caller. A blocked socket read ignores coroutine
     * cancellation, and the read timeout starts again with every byte, so the caller must not
     * wait for the read: it gives up on its own when cancelled or after [totalTimeoutMs], and
     * closing the connection then ends the read.
     */
    private val reads = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun getString(url: String, totalTimeoutMs: Long = TOTAL_TIMEOUT_MS): String =
        withTimeoutOrNull(totalTimeoutMs) {
            suspendCancellableCoroutine<String> { cont ->
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    setRequestProperty("Accept", "application/json")
                    // Open-Meteo asks non-browser clients to identify themselves.
                    setRequestProperty("User-Agent", "dk.cocode.weather/1.0 (Android)")
                }
                // Closing the connection ends the blocked read. It is done on a worker thread: disconnect()
                // can itself wait for a read in progress, and it must not hold up whoever cancelled.
                cont.invokeOnCancellation { reads.launch { conn.disconnect() } }
                reads.launch {
                    try {
                        cont.resume(read(conn) { cont.isActive })
                    } catch (e: Throwable) {
                        cont.resumeWithException(e)
                    } finally {
                        conn.disconnect()
                    }
                }
            }
        } ?: throw IOException("The server took too long to answer")

    /**
     * Reads in chunks, so a response that trickles in is cut off as soon as the caller no longer
     * wants it ([wanted] turns false when it was cancelled or its time ran out): not every
     * HttpURLConnection ends a blocked read when it is disconnected from another thread.
     */
    private fun read(conn: HttpURLConnection, wanted: () -> Boolean): String {
        val code = conn.responseCode
        if (code !in 200..299) {
            // Read the error body too: Open-Meteo explains bad parameters there.
            val detail = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            throw IOException("HTTP $code${if (detail.isBlank()) "" else ": ${detail.take(200)}"}")
        }
        val out = ByteArrayOutputStream()
        val chunk = ByteArray(CHUNK_BYTES)
        conn.inputStream.use { input ->
            while (true) {
                val n = input.read(chunk)
                if (n < 0) break
                out.write(chunk, 0, n)
                if (!wanted()) throw IOException("The caller gave up")
            }
        }
        return out.toString("UTF-8")
    }
}
