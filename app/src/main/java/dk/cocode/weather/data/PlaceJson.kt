package dk.cocode.weather.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Places are persisted as a JSON string inside DataStore Preferences. Hand-rolled
 * rather than reflective so the stored shape is explicit and an older install's
 * data degrades to defaults instead of throwing.
 */
object PlaceJson {

    fun encode(places: List<Place>): String {
        val arr = JSONArray()
        places.forEach { p ->
            arr.put(
                JSONObject()
                    .put("name", p.name)
                    .put("country", p.country)
                    .put("admin1", p.admin1)
                    .put("lat", p.latitude)
                    .put("lon", p.longitude)
                    .put("tz", p.timezone)
                    .put("device", p.isDeviceLocation)
            )
        }
        return arr.toString()
    }

    fun decode(s: String?): List<Place> {
        if (s.isNullOrBlank()) return emptyList()
        val arr = runCatching { JSONArray(s) }.getOrNull() ?: return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val lat = o.doubleOrNull("lat") ?: continue
                val lon = o.doubleOrNull("lon") ?: continue
                add(
                    Place(
                        name = o.stringOrNull("name") ?: continue,
                        country = o.stringOrNull("country").orEmpty(),
                        admin1 = o.stringOrNull("admin1").orEmpty(),
                        latitude = lat,
                        longitude = lon,
                        timezone = o.stringOrNull("tz") ?: "auto",
                        isDeviceLocation = o.optBoolean("device", false),
                    )
                )
            }
        }
    }
}
