package dk.cocode.weather.domain

/**
 * WMO weather-interpretation codes -> condition + icon id. The words for a condition
 * live in string resources (see ui/Labels.kt), so this file stays free of Android.
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

/** What a WMO code says about the sky. Each value has its own string resource. */
enum class Condition {
    CLEAR_SKY, MAINLY_CLEAR, PARTLY_CLOUDY, OVERCAST, FOG, FREEZING_FOG,
    LIGHT_DRIZZLE, DRIZZLE, DENSE_DRIZZLE, FREEZING_DRIZZLE,
    LIGHT_RAIN, RAIN, HEAVY_RAIN, FREEZING_RAIN,
    LIGHT_SNOW, SNOW, HEAVY_SNOW, SNOW_GRAINS,
    LIGHT_SHOWERS, SHOWERS, VIOLENT_SHOWERS, SNOW_SHOWERS, HEAVY_SNOW_SHOWERS,
    THUNDERSTORM, HEAVY_THUNDERSTORM, THUNDERSTORM_HAIL, UNKNOWN
}

/** The risk band a UV index falls in. */
enum class UvBand { LOW, MODERATE, HIGH, VERY_HIGH, EXTREME }

object Wmo {

    private val CODES: Map<Int, Pair<Condition, WeatherIcon>> = mapOf(
        0 to (Condition.CLEAR_SKY to WeatherIcon.CLEAR),
        1 to (Condition.MAINLY_CLEAR to WeatherIcon.CLEAR),
        2 to (Condition.PARTLY_CLOUDY to WeatherIcon.PARTLY),
        3 to (Condition.OVERCAST to WeatherIcon.CLOUDY),
        45 to (Condition.FOG to WeatherIcon.FOG),
        48 to (Condition.FREEZING_FOG to WeatherIcon.FOG),
        51 to (Condition.LIGHT_DRIZZLE to WeatherIcon.DRIZZLE),
        53 to (Condition.DRIZZLE to WeatherIcon.DRIZZLE),
        55 to (Condition.DENSE_DRIZZLE to WeatherIcon.DRIZZLE),
        56 to (Condition.FREEZING_DRIZZLE to WeatherIcon.SLEET),
        57 to (Condition.FREEZING_DRIZZLE to WeatherIcon.SLEET),
        61 to (Condition.LIGHT_RAIN to WeatherIcon.RAIN),
        63 to (Condition.RAIN to WeatherIcon.RAIN),
        65 to (Condition.HEAVY_RAIN to WeatherIcon.RAIN),
        66 to (Condition.FREEZING_RAIN to WeatherIcon.SLEET),
        67 to (Condition.FREEZING_RAIN to WeatherIcon.SLEET),
        71 to (Condition.LIGHT_SNOW to WeatherIcon.SNOW),
        73 to (Condition.SNOW to WeatherIcon.SNOW),
        75 to (Condition.HEAVY_SNOW to WeatherIcon.SNOW),
        77 to (Condition.SNOW_GRAINS to WeatherIcon.SNOW),
        80 to (Condition.LIGHT_SHOWERS to WeatherIcon.SHOWERS),
        81 to (Condition.SHOWERS to WeatherIcon.SHOWERS),
        82 to (Condition.VIOLENT_SHOWERS to WeatherIcon.SHOWERS),
        85 to (Condition.SNOW_SHOWERS to WeatherIcon.SNOW),
        86 to (Condition.HEAVY_SNOW_SHOWERS to WeatherIcon.SNOW),
        95 to (Condition.THUNDERSTORM to WeatherIcon.THUNDER),
        97 to (Condition.HEAVY_THUNDERSTORM to WeatherIcon.THUNDER),
        96 to (Condition.THUNDERSTORM_HAIL to WeatherIcon.THUNDER),
        99 to (Condition.THUNDERSTORM_HAIL to WeatherIcon.THUNDER),
    )

    /** Icons that have a distinct night variant. */
    private val NIGHT = mapOf(
        WeatherIcon.CLEAR to WeatherIcon.CLEAR_NIGHT,
        WeatherIcon.PARTLY to WeatherIcon.PARTLY_NIGHT,
    )

    fun condition(code: Int?): Condition = CODES[code]?.first ?: Condition.UNKNOWN

    fun icon(code: Int?, isDay: Boolean): WeatherIcon {
        val id = CODES[code]?.second ?: WeatherIcon.CLOUDY
        return if (!isDay) NIGHT[id] ?: id else id
    }

    /** UV index -> the risk band the WHO publishes it under. Null when the index is unknown. */
    fun uvBand(uv: Double?): UvBand? = when {
        uv == null -> null
        uv < 3 -> UvBand.LOW
        uv < 6 -> UvBand.MODERATE
        uv < 8 -> UvBand.HIGH
        uv < 11 -> UvBand.VERY_HIGH
        else -> UvBand.EXTREME
    }
}
