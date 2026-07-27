package dk.cocode.weather.data

/**
 * A place the app can show weather for. [timezone] is an IANA id used to ask
 * Open-Meteo for wall-clock times in that location, not the phone's.
 */
data class Place(
    val name: String,
    val country: String = "",
    val admin1: String = "",
    val latitude: Double,
    val longitude: Double,
    val timezone: String = "auto",
    /** True for the entry backed by the device GPS rather than a saved search. */
    val isDeviceLocation: Boolean = false,
) {
    /** "Central Jutland, Denmark" — the sub-heading under the place name. */
    val region: String
        get() = listOf(admin1, country).filter { it.isNotBlank() }.joinToString(", ")

    /** Identity for saved-list dedupe: coordinates rounded to ~1 km. */
    val key: String
        get() = if (isDeviceLocation) "device"
        else "%.2f,%.2f".format(java.util.Locale.US, latitude, longitude)
}

data class Current(
    val time: String?,
    val temperature: Double?,
    val humidity: Double?,
    val apparentTemperature: Double?,
    val isDay: Boolean,
    val precipitation: Double?,
    val weatherCode: Int?,
    val windSpeed: Double?,
    val windDirection: Double?,
    val pressure: Double?,
)

data class HourRow(
    val time: String,
    val temperature: Double?,
    val weatherCode: Int?,
    val precipitationProbability: Double?,
    val isDay: Boolean,
    val windSpeed: Double?,
)

data class DayRow(
    val time: String,
    val weatherCode: Int?,
    val temperatureMax: Double?,
    val temperatureMin: Double?,
    val sunrise: String?,
    val sunset: String?,
    val precipitationSum: Double?,
    val precipitationProbabilityMax: Double?,
    val windSpeedMax: Double?,
    val uvIndexMax: Double?,
)

data class Forecast(
    val fetchedAt: Long,
    /**
     * The location's UTC offset including DST, straight from the API. Lets the app
     * show the selected city's own clock even when the phone sits in another zone.
     */
    val utcOffsetSeconds: Int,
    val current: Current,
    val hourly: List<HourRow>,
    val daily: List<DayRow>,
)
