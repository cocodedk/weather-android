package dk.cocode.weather.data

import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale

/**
 * Open-Meteo geocoding search — the piece the Tizen app never needed, because the
 * TV build hardcoded Copenhagen. This is what lets the user pick any place.
 *
 * Free, key-less, and returns the IANA timezone alongside the coordinates, which
 * the forecast call then reuses.
 */
object GeocodingApi {

    private const val MIN_QUERY = 2

    /**
     * Returns places matching [query], best match first. A blank or one-character
     * query returns empty rather than calling the API, which rejects it anyway.
     */
    suspend fun search(query: String, limit: Int = 10): List<Place> {
        val q = query.trim()
        if (q.length < MIN_QUERY) return emptyList()

        val url = "https://geocoding-api.open-meteo.com/v1/search" +
            "?name=" + URLEncoder.encode(q, "UTF-8") +
            "&count=$limit" +
            "&language=" + Locale.getDefault().language +
            "&format=json"

        val raw = JSONObject(Http.getString(url))
        // Open-Meteo omits `results` entirely when nothing matches, rather than
        // returning an empty array.
        val arr = raw.optJSONArray("results") ?: return emptyList()

        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val name = o.stringOrNull("name") ?: continue
                val lat = o.doubleOrNull("latitude") ?: continue
                val lon = o.doubleOrNull("longitude") ?: continue
                add(
                    Place(
                        name = name,
                        country = o.stringOrNull("country").orEmpty(),
                        admin1 = o.stringOrNull("admin1").orEmpty(),
                        latitude = lat,
                        longitude = lon,
                        timezone = o.stringOrNull("timezone") ?: "auto",
                    )
                )
            }
        }
    }
}
