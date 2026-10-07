package dk.cocode.weather.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
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
    private const val ERROR_BODY_BYTES = 2 * 1024

    /**
     * Where the blocking work runs, apart from the caller. A blocked socket read ignores coroutine
     * cancellation, and the read timeout starts again with every byte, so the caller must not
     * wait for the read: it gives up on its own when cancelled or after [totalTimeoutMs], and
     * the worker checks, at every step, that the caller still wants the answer.
     */
    private val reads = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun getString(url: String, totalTimeoutMs: Long = TOTAL_TIMEOUT_MS): String =
        withTimeoutOrNull(totalTimeoutMs) {
            suspendCancellableCoroutine<String> { cont ->
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    // No single wait may be longer than the whole request is allowed to take.
                    connectTimeout = minOf(CONNECT_TIMEOUT_MS.toLong(), totalTimeoutMs).toInt()
                    readTimeout = minOf(READ_TIMEOUT_MS.toLong(), totalTimeoutMs).toInt()
                    setRequestProperty("Accept", "application/json")
                    // Open-Meteo asks non-browser clients to identify themselves.
                    setRequestProperty("User-Agent", "dk.cocode.weather/1.0 (Android)")
                }
                // Closing the connection ends a blocked read. It is done on a worker thread: disconnect()
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
     * Connects, reads the status and reads the body, checking before each step and after every
     * chunk that the caller still wants the answer ([wanted] turns false when it was cancelled or
     * its time ran out). Disconnecting from another thread does not end a blocked read on every
     * HttpURLConnection, and does nothing at all if the worker has not connected yet.
     */
    private fun read(conn: HttpURLConnection, wanted: () -> Boolean): String {
        fun stopIfUnwanted() {
            if (!wanted()) throw IOException("The caller gave up")
        }
        stopIfUnwanted()
        conn.connect()
        stopIfUnwanted()
        val code = conn.responseCode
        stopIfUnwanted()
        if (code !in 200..299) {
            // Read the error body too: Open-Meteo explains bad parameters there.
            val detail = conn.errorStream?.let { readChunks(it, wanted, ERROR_BODY_BYTES) }.orEmpty()
            throw IOException("HTTP $code${if (detail.isBlank()) "" else ": ${detail.take(200)}"}")
        }
        return readChunks(conn.inputStream, wanted, Int.MAX_VALUE)
    }

    /** Reads in chunks, stopping at [maxBytes] or as soon as [wanted] turns false. */
    private fun readChunks(input: InputStream, wanted: () -> Boolean, maxBytes: Int): String {
        val out = ByteArrayOutputStream()
        val chunk = ByteArray(CHUNK_BYTES)
        input.use {
            while (out.size() < maxBytes) {
                val n = it.read(chunk)
                if (n < 0) break
                out.write(chunk, 0, n)
                if (!wanted()) throw IOException("The caller gave up")
            }
        }
        return out.toString("UTF-8")
    }
}
