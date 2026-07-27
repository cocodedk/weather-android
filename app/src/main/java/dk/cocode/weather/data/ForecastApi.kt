package dk.cocode.weather.data

import org.json.JSONObject
import java.util.Locale

/**
 * Open-Meteo forecast client. No API key, CORS-open, metric.
 *
 * Ported from the Tizen app's js/api.js, with the one structural change this app
 * needs: latitude/longitude/timezone are arguments rather than the Copenhagen
 * constants that were baked into the TV build.
 *
 * Shapes the column-oriented API response into row objects the UI wants.
 */
object ForecastApi {

    private val CURRENT = listOf(
        "temperature_2m", "relative_humidity_2m", "apparent_temperature", "is_day",
        "precipitation", "weather_code", "wind_speed_10m", "wind_direction_10m",
        "surface_pressure",
    )
    private val HOURLY = listOf(
        "temperature_2m", "weather_code", "precipitation_probability", "is_day",
        "wind_speed_10m",
    )
    private val DAILY = listOf(
        "weather_code", "temperature_2m_max", "temperature_2m_min", "sunrise", "sunset",
        "precipitation_sum", "precipitation_probability_max", "wind_speed_10m_max",
        "uv_index_max",
    )

    fun url(place: Place): String =
        "https://api.open-meteo.com/v1/forecast" +
            "?latitude=" + fmt(place.latitude) +
            "&longitude=" + fmt(place.longitude) +
            "&current=" + CURRENT.joinToString(",") +
            "&hourly=" + HOURLY.joinToString(",") +
            "&daily=" + DAILY.joinToString(",") +
            // `auto` makes the API return wall-clock times in the *place's* zone,
            // which is what lets a city on the other side of the world read
            // correctly on a phone that never leaves Denmark.
            "&timezone=auto" +
            "&wind_speed_unit=ms&forecast_days=7"

    private fun fmt(v: Double) = String.format(Locale.US, "%.4f", v)

    /** Fetches and parses. Throws on network failure; the repository owns fallback. */
    suspend fun fetch(place: Place): Pair<Forecast, String> {
        val body = Http.getString(url(place))
        return parse(body) to body
    }

    fun parse(body: String): Forecast {
        val raw = JSONObject(body)

        val cur = raw.optJSONObject("current") ?: JSONObject()
        val current = Current(
            time = cur.stringOrNull("time"),
            temperature = cur.doubleOrNull("temperature_2m"),
            humidity = cur.doubleOrNull("relative_humidity_2m"),
            apparentTemperature = cur.doubleOrNull("apparent_temperature"),
            // The API sends 1/0; anything else (missing) is treated as daytime,
            // matching the Tizen fallback to day icons.
            isDay = (cur.intOrNull("is_day") ?: 1) != 0,
            precipitation = cur.doubleOrNull("precipitation"),
            weatherCode = cur.intOrNull("weather_code"),
            windSpeed = cur.doubleOrNull("wind_speed_10m"),
            windDirection = cur.doubleOrNull("wind_direction_10m"),
            pressure = cur.doubleOrNull("surface_pressure"),
        )

        val h = raw.optJSONObject("hourly")
        val hTime = h?.optJSONArray("time")
        val hourly = buildList {
            if (h != null && hTime != null) {
                val temp = h.optJSONArray("temperature_2m")
                val code = h.optJSONArray("weather_code")
                val pp = h.optJSONArray("precipitation_probability")
                val isDay = h.optJSONArray("is_day")
                val wind = h.optJSONArray("wind_speed_10m")
                for (i in 0 until hTime.length()) {
                    val t = hTime.stringAt(i) ?: continue
                    add(
                        HourRow(
                            time = t,
                            temperature = temp?.doubleAt(i),
                            weatherCode = code?.intAt(i),
                            precipitationProbability = pp?.doubleAt(i),
                            isDay = (isDay?.intAt(i) ?: 1) != 0,
                            windSpeed = wind?.doubleAt(i),
                        )
                    )
                }
            }
        }

        val d = raw.optJSONObject("daily")
        val dTime = d?.optJSONArray("time")
        val daily = buildList {
            if (d != null && dTime != null) {
                val code = d.optJSONArray("weather_code")
                val tmax = d.optJSONArray("temperature_2m_max")
                val tmin = d.optJSONArray("temperature_2m_min")
                val sunrise = d.optJSONArray("sunrise")
                val sunset = d.optJSONArray("sunset")
                val psum = d.optJSONArray("precipitation_sum")
                val pmax = d.optJSONArray("precipitation_probability_max")
                val wmax = d.optJSONArray("wind_speed_10m_max")
                val uv = d.optJSONArray("uv_index_max")
                for (i in 0 until dTime.length()) {
                    val t = dTime.stringAt(i) ?: continue
                    add(
                        DayRow(
                            time = t,
                            weatherCode = code?.intAt(i),
                            temperatureMax = tmax?.doubleAt(i),
                            temperatureMin = tmin?.doubleAt(i),
                            sunrise = sunrise?.stringAt(i),
                            sunset = sunset?.stringAt(i),
                            precipitationSum = psum?.doubleAt(i),
                            precipitationProbabilityMax = pmax?.doubleAt(i),
                            windSpeedMax = wmax?.doubleAt(i),
                            uvIndexMax = uv?.doubleAt(i),
                        )
                    )
                }
            }
        }

        return Forecast(
            fetchedAt = System.currentTimeMillis(),
            utcOffsetSeconds = raw.intOrNull("utc_offset_seconds") ?: 0,
            current = current,
            hourly = hourly,
            daily = daily,
        )
    }
}
