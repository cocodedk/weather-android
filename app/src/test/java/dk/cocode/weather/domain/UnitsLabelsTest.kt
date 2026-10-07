package dk.cocode.weather.domain

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The words come in as [UnitLabels], so these tests use a stand-in for the string resources. */
class UnitsLabelsTest {
    private val labels = UnitLabels(
        compass = listOf(
            "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
            "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW",
        ),
        am = "am",
        pm = "pm",
    )
    private val metric24 = Units(labels, imperial = false, use24Hour = true, locale = Locale.US)
    private val metric12 = Units(labels, imperial = false, use24Hour = false, locale = Locale.US)

    @Test
    fun bearingUsesTheCompassLabels() {
        assertEquals("N", metric24.bearing(0.0))
        assertEquals("E", metric24.bearing(90.0))
        assertEquals("SW", metric24.bearing(225.0))
        assertEquals("N", metric24.bearing(359.0))
        assertEquals("", metric24.bearing(null))
    }

    @Test
    fun aWindSpeedCannotBeSplitFromItsUnit() {
        // A no-break space after the number, and a word joiner inside "m/s".
        assertEquals("2.7 m/⁠s", metric24.wind(2.7))
        assertEquals("12 m/⁠s", metric24.wind(12.0))
        val imperial = Units(labels, imperial = true, use24Hour = true, locale = Locale.US)
        assertEquals("6.0 mph", imperial.wind(2.7))
        assertEquals("--", metric24.wind(null))
    }

    @Test
    fun otherUnitsUseAPlainSpace() {
        assertEquals("0.0 mm", metric24.precip(0.0))
        assertEquals("1015 hPa", metric24.pressure(1015.2))
        val imperial = Units(labels, imperial = true, use24Hour = true, locale = Locale.US)
        assertEquals("0.04 in", imperial.precip(1.0))
    }

    @Test
    fun twelveHourClockUsesTheAmAndPmLabels() {
        assertEquals("12:05 am", metric12.clock("2026-10-07T00:05"))
        assertEquals("1:30 pm", metric12.clock("2026-10-07T13:30"))
        assertEquals("12am", metric12.hourLabel("2026-10-07T00:00"))
        assertEquals("11pm", metric12.hourLabel("2026-10-07T23:00"))
    }

    @Test
    fun dateTimeNamesTheDayBecauseSavedDataCanBeOld() {
        assertEquals("7 Oct, 13:30", metric24.dateTime("2026-10-07T13:30"))
        assertEquals("7 Oct, 1:30 pm", metric12.dateTime("2026-10-07T13:30"))
        assertEquals("--:--", metric24.dateTime(null))
    }

    @Test
    fun twentyFourHourClockHasNoSuffix() {
        assertEquals("13:30", metric24.clock("2026-10-07T13:30"))
        assertEquals("--:--", metric24.clock(null))
    }

    @Test
    fun conditionsMapFromWmoCodes() {
        assertEquals(Condition.CLEAR_SKY, Wmo.condition(0))
        assertEquals(Condition.THUNDERSTORM_HAIL, Wmo.condition(96))
        assertEquals(Condition.THUNDERSTORM_HAIL, Wmo.condition(99))
        assertEquals(Condition.FREEZING_DRIZZLE, Wmo.condition(57))
        assertEquals(Condition.HEAVY_THUNDERSTORM, Wmo.condition(97))
        assertEquals(WeatherIcon.THUNDER, Wmo.icon(97, isDay = true))
        assertEquals(Condition.UNKNOWN, Wmo.condition(1234))
        assertEquals(Condition.UNKNOWN, Wmo.condition(null))
    }

    @Test
    fun uvBandsFollowTheWhoRanges() {
        assertNull(Wmo.uvBand(null))
        assertEquals(UvBand.LOW, Wmo.uvBand(2.9))
        assertEquals(UvBand.MODERATE, Wmo.uvBand(3.0))
        assertEquals(UvBand.HIGH, Wmo.uvBand(6.0))
        assertEquals(UvBand.VERY_HIGH, Wmo.uvBand(8.0))
        assertEquals(UvBand.EXTREME, Wmo.uvBand(11.0))
    }
}
