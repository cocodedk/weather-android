package dk.cocode.weather.ui

import dk.cocode.weather.R
import dk.cocode.weather.data.Current
import dk.cocode.weather.data.DayRow
import dk.cocode.weather.data.Forecast
import dk.cocode.weather.data.LocationUnavailable
import dk.cocode.weather.data.Place
import dk.cocode.weather.data.WeatherStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class WeatherStateChangesTest {
    private val copenhagen = Place(name = "Copenhagen", latitude = 55.68, longitude = 12.57)
    private val tokyo = Place(name = "Tokyo", latitude = 35.68, longitude = 139.69)
    private val device = Place(name = "Here", latitude = 1.0, longitude = 2.0, isDeviceLocation = true)

    private fun forecast(days: Int) = Forecast(
        fetchedAt = 0L,
        utcOffsetSeconds = 0,
        current = Current(
            time = "2026-10-07T12:00", temperature = 10.0, humidity = 50.0,
            apparentTemperature = 9.0, isDay = true, precipitation = 0.0, weatherCode = 0,
            windSpeed = 3.0, windDirection = 90.0, pressure = 1013.0,
        ),
        hourly = emptyList(),
        daily = List(days) { i ->
            DayRow(
                time = "2026-10-%02d".format(7 + i), weatherCode = 0, temperatureMax = 12.0,
                temperatureMin = 5.0, sunrise = null, sunset = null, precipitationSum = 0.0,
                precipitationProbabilityMax = 0.0, windSpeedMax = 4.0, uvIndexMax = 1.0,
            )
        },
    )

    @Test
    fun aNewForecastClearsLoadingAndErrorAndMarksStaleness() {
        val before = WeatherUiState(loading = true, error = "boom", stale = false)
        val after = before.withForecast(forecast(7), stale = true)
        assertFalse(after.loading)
        assertNull(after.error)
        assertEquals(true, after.stale)
        assertEquals(7, after.forecast?.daily?.size)
    }

    @Test
    fun aShorterForecastPullsTheSelectedDayBackIntoRange() {
        assertEquals(2, WeatherUiState(dayIndex = 6).withForecast(forecast(3), stale = false).dayIndex)
        assertEquals(3, WeatherUiState(dayIndex = 3).withForecast(forecast(7), stale = false).dayIndex)
        assertEquals(0, WeatherUiState(dayIndex = 4).withForecast(forecast(0), stale = false).dayIndex)
    }

    @Test
    fun pickingAPlaceClearsTheOldForecastAndStartsOnToday() {
        val before = WeatherUiState(
            selected = copenhagen, forecast = forecast(7), dayIndex = 3, stale = true,
        )
        val after = before.withSelectedPlace(tokyo)
        assertEquals(tokyo, after.selected)
        assertNull(after.forecast)
        assertEquals(0, after.dayIndex)
        assertFalse(after.stale)
    }

    @Test
    fun aNewPlaceIsAddedAndSelected() {
        val (places, toSelect) = withPlace(listOf(copenhagen), tokyo)
        assertEquals(listOf(copenhagen, tokyo), places)
        assertEquals(tokyo, toSelect)
    }

    @Test
    fun aPlaceAlreadySavedIsNotAddedTwiceAndTheSavedEntryIsSelected() {
        val sameSpotOtherName = copenhagen.copy(name = "København")
        val saved = listOf(copenhagen, tokyo)
        val (places, toSelect) = withPlace(saved, sameSpotOtherName)
        assertEquals(saved, places)
        assertSame(copenhagen, toSelect)
    }

    @Test
    fun removingAPlaceKeepsTheOthersButNeverTheLastOne() {
        assertEquals(listOf(tokyo), withoutPlace(listOf(copenhagen, tokyo), copenhagen))
        assertNull(withoutPlace(listOf(tokyo), tokyo))
    }

    @Test
    fun theDeviceLocationEntryIsReplacedNotStacked() {
        val older = device.copy(name = "Earlier fix")
        val places = withDevicePlace(listOf(copenhagen, older), device)
        assertEquals(listOf(copenhagen, device), places)
    }

    @Test
    fun theThemeCyclesAutoDayNightAuto() {
        assertEquals(WeatherStore.THEME_DAY, nextTheme(WeatherStore.THEME_AUTO))
        assertEquals(WeatherStore.THEME_NIGHT, nextTheme(WeatherStore.THEME_DAY))
        assertEquals(WeatherStore.THEME_AUTO, nextTheme(WeatherStore.THEME_NIGHT))
    }

    @Test
    fun locationFailuresUseTheirOwnSentenceOrTheGeneralOne() {
        assertEquals(
            R.string.msg_location_no_fix,
            locationFailureMessage(LocationUnavailable(R.string.msg_location_no_fix)),
        )
        assertEquals(R.string.msg_location_failed, locationFailureMessage(RuntimeException("x")))
    }
}
