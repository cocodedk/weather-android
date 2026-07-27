package dk.cocode.weather.domain

import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Unit conversion + formatting, ported from the Tizen app's js/units.js.
 *
 * Metric is the source of truth (Open-Meteo is asked for degC and m/s); imperial
 * is derived on the client so switching never needs a refetch.
 *
 * [use24Hour] comes from the device setting, resolved at the UI edge so this
 * class stays free of Android imports.
 */
class Units(
    val imperial: Boolean = false,
    private val use24Hour: Boolean = true,
    private val locale: Locale = Locale.getDefault(),
) {

    /** A wall-clock instant with no timezone attached. See [parseLocal]. */
    data class Wall(val y: Int, val mo: Int, val d: Int, val h: Int, val mi: Int)

    // --- temperature ---

    fun tempValue(c: Double?): Double? =
        if (c == null) null else if (imperial) c * 9 / 5 + 32 else c

    fun temp(c: Double?): String = tempValue(c)?.roundToInt()?.toString() ?: "--"

    fun tempUnit(): String = if (imperial) "°F" else "°C"

    fun tempFull(c: Double?): String = temp(c) + tempUnit()

    // --- wind: the API gives m/s ---

    fun wind(ms: Double?): String {
        if (ms == null) return "--"
        val v = if (imperial) ms * 2.236936 else ms
        val n = if (v < 10) String.format(locale, "%.1f", v) else v.roundToInt().toString()
        return n + if (imperial) " mph" else " m/s"
    }

    fun bearing(deg: Double?): String {
        if (deg == null) return ""
        return COMPASS[((deg / 22.5).roundToInt()) % 16]
    }

    // --- precipitation: the API gives mm ---

    fun precip(mm: Double?): String {
        if (mm == null) return "--"
        if (imperial) return String.format(locale, "%.2f in", mm / 25.4)
        return if (mm < 10) String.format(locale, "%.1f mm", mm)
        else "${mm.roundToInt()} mm"
    }

    fun percent(p: Double?): String = if (p == null) "--" else "${p.roundToInt()}%"

    fun pressure(hpa: Double?): String = if (hpa == null) "--" else "${hpa.roundToInt()} hPa"

    // --- time ---

    /**
     * Open-Meteo returns local wall-clock strings ("2026-07-26T22:15") already in
     * the requested location's timezone. Parse the digits directly instead of
     * going through Date/Instant, so the *phone's* timezone cannot shift them.
     *
     * This matters more here than it did on the TV: the user can pick a city in
     * any timezone, and its forecast must read in that city's local time, not the
     * phone's.
     */
    fun parseLocal(iso: String?): Wall? {
        if (iso == null) return null
        val m = ISO.find(iso) ?: return null
        val (y, mo, d, h, mi) = m.destructured
        return Wall(
            y.toInt(), mo.toInt(), d.toInt(),
            if (h.isEmpty()) 0 else h.toInt(),
            if (mi.isEmpty()) 0 else mi.toInt(),
        )
    }

    private fun pad2(n: Int): String = if (n < 10) "0$n" else n.toString()

    fun clock(iso: String?): String {
        val t = parseLocal(iso) ?: return "--:--"
        return formatTime(t.h, t.mi)
    }

    fun hourLabel(iso: String?): String {
        val t = parseLocal(iso) ?: return "--"
        return if (use24Hour) "${pad2(t.h)}:00" else shortHour12(t.h)
    }

    private fun formatTime(h: Int, mi: Int): String {
        if (use24Hour) return "${pad2(h)}:${pad2(mi)}"
        val suffix = if (h < 12) "am" else "pm"
        val h12 = when {
            h % 12 == 0 -> 12
            else -> h % 12
        }
        return "$h12:${pad2(mi)} $suffix"
    }

    private fun shortHour12(h: Int): String {
        val suffix = if (h < 12) "am" else "pm"
        val h12 = if (h % 12 == 0) 12 else h % 12
        return "$h12$suffix"
    }

    private fun date(iso: String?): LocalDate? {
        val t = parseLocal(iso) ?: return null
        return runCatching { LocalDate.of(t.y, t.mo, t.d) }.getOrNull()
    }

    fun weekday(iso: String?): String =
        date(iso)?.dayOfWeek?.getDisplayName(TextStyle.FULL, locale) ?: ""

    fun weekdayShort(iso: String?): String =
        date(iso)?.dayOfWeek?.getDisplayName(TextStyle.SHORT, locale) ?: ""

    fun dateLabel(iso: String?): String {
        val d = date(iso) ?: return ""
        return "${d.dayOfMonth} ${d.month.getDisplayName(TextStyle.SHORT, locale)}"
    }

    fun longDate(iso: String?): String {
        val d = date(iso) ?: return ""
        return "${weekday(iso)} ${d.dayOfMonth} " +
            "${d.month.getDisplayName(TextStyle.SHORT, locale)} ${d.year}"
    }

    /** Stable yyyy-MM-dd key for grouping hourly rows into days. */
    fun dayKey(iso: String?): String {
        val t = parseLocal(iso) ?: return ""
        return "${t.y}-${pad2(t.mo)}-${pad2(t.d)}"
    }

    companion object {
        private val COMPASS = listOf(
            "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
            "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW",
        )
        private val ISO = Regex("""^(\d{4})-(\d{2})-(\d{2})(?:[T ](\d{2}):(\d{2}))?""")
    }
}
