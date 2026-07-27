package dk.cocode.weather.domain

/**
 * WMO weather-interpretation codes -> label + icon id.
 * Reference: https://open-meteo.com/en/docs (weather_code)
 *
 * Ported from the Tizen app's js/wmo.js. The icon ids are the same strings, so
 * both apps describe the same condition with the same artwork.
 */
enum class WeatherIcon {
    CLEAR, CLEAR_NIGHT, PARTLY, PARTLY_NIGHT, CLOUDY, FOG,
    DRIZZLE, RAIN, SHOWERS, SLEET, SNOW, THUNDER,
    WIND, DROP, THERMO, GAUGE, SUNRISE, UV
}

object Wmo {

    private val CODES: Map<Int, Pair<String, WeatherIcon>> = mapOf(
        0 to ("Clear sky" to WeatherIcon.CLEAR),
        1 to ("Mainly clear" to WeatherIcon.CLEAR),
        2 to ("Partly cloudy" to WeatherIcon.PARTLY),
        3 to ("Overcast" to WeatherIcon.CLOUDY),
        45 to ("Fog" to WeatherIcon.FOG),
        48 to ("Freezing fog" to WeatherIcon.FOG),
        51 to ("Light drizzle" to WeatherIcon.DRIZZLE),
        53 to ("Drizzle" to WeatherIcon.DRIZZLE),
        55 to ("Dense drizzle" to WeatherIcon.DRIZZLE),
        56 to ("Freezing drizzle" to WeatherIcon.SLEET),
        57 to ("Freezing drizzle" to WeatherIcon.SLEET),
        61 to ("Light rain" to WeatherIcon.RAIN),
        63 to ("Rain" to WeatherIcon.RAIN),
        65 to ("Heavy rain" to WeatherIcon.RAIN),
        66 to ("Freezing rain" to WeatherIcon.SLEET),
        67 to ("Freezing rain" to WeatherIcon.SLEET),
        71 to ("Light snow" to WeatherIcon.SNOW),
        73 to ("Snow" to WeatherIcon.SNOW),
        75 to ("Heavy snow" to WeatherIcon.SNOW),
        77 to ("Snow grains" to WeatherIcon.SNOW),
        80 to ("Light showers" to WeatherIcon.SHOWERS),
        81 to ("Showers" to WeatherIcon.SHOWERS),
        82 to ("Violent showers" to WeatherIcon.SHOWERS),
        85 to ("Snow showers" to WeatherIcon.SNOW),
        86 to ("Heavy snow showers" to WeatherIcon.SNOW),
        95 to ("Thunderstorm" to WeatherIcon.THUNDER),
        96 to ("Thunderstorm, hail" to WeatherIcon.THUNDER),
        99 to ("Thunderstorm, hail" to WeatherIcon.THUNDER),
    )

    /** Icons that have a distinct night variant. */
    private val NIGHT = mapOf(
        WeatherIcon.CLEAR to WeatherIcon.CLEAR_NIGHT,
        WeatherIcon.PARTLY to WeatherIcon.PARTLY_NIGHT,
    )

    fun label(code: Int?): String = CODES[code]?.first ?: "Unknown"

    fun icon(code: Int?, isDay: Boolean): WeatherIcon {
        val id = CODES[code]?.second ?: WeatherIcon.CLOUDY
        return if (!isDay) NIGHT[id] ?: id else id
    }

    /** UV index -> the risk band the WHO publishes it under. */
    fun uvBand(uv: Double?): String = when {
        uv == null -> ""
        uv < 3 -> "Low"
        uv < 6 -> "Moderate"
        uv < 8 -> "High"
        uv < 11 -> "Very high"
        else -> "Extreme"
    }
}
