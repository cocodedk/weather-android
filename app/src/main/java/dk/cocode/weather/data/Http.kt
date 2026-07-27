package dk.cocode.weather.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Minimal GET-a-string helper. Open-Meteo needs no auth, no retries beyond the
 * caller's, and no request body, so HttpURLConnection is enough and keeps a
 * networking library out of the dependency list.
 */
object Http {

    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 12_000

    suspend fun getString(url: String): String = withContext(Dispatchers.IO) {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("Accept", "application/json")
            // Open-Meteo asks non-browser clients to identify themselves.
            setRequestProperty("User-Agent", "dk.cocode.weather/1.0 (Android)")
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) {
                // Read the error body too: Open-Meteo explains bad parameters there.
                val detail = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw IOException("HTTP $code${if (detail.isBlank()) "" else ": ${detail.take(200)}"}")
            }
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
