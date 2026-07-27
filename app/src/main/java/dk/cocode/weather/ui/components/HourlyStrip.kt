package dk.cocode.weather.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dk.cocode.weather.data.Forecast
import dk.cocode.weather.data.HourRow
import dk.cocode.weather.domain.Units
import dk.cocode.weather.domain.Wmo
import dk.cocode.weather.ui.icons.WeatherIconView
import dk.cocode.weather.ui.theme.LocalPalette

/** The rows to show for a given day, plus which one (if any) is "Now". */
data class HourWindow(val rows: List<HourRow>, val nowAt: Int, val note: String)

/**
 * Today means the next 24 hours rolling from the current hour; any other day means
 * that calendar day's own rows. Ported from `hoursForDay` in render.js.
 */
fun hoursForDay(forecast: Forecast, dayIndex: Int, units: Units): HourWindow {
    if (dayIndex == 0) {
        val start = nowIndex(forecast)
        return HourWindow(
            rows = forecast.hourly.drop(start).take(24),
            nowAt = 0,
            note = "next 24 hours",
        )
    }
    val day = forecast.daily.getOrNull(dayIndex) ?: return HourWindow(emptyList(), -1, "")
    val key = units.dayKey(day.time)
    return HourWindow(
        rows = forecast.hourly.filter { units.dayKey(it.time) == key },
        nowAt = -1,
        note = units.longDate(day.time),
    )
}

@Composable
fun HourlyStrip(
    forecast: Forecast,
    dayIndex: Int,
    units: Units,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val window = hoursForDay(forecast, dayIndex, units)
    val listState = rememberLazyListState()

    // Switching day should show that day from its start, not wherever the previous
    // day happened to be scrolled to.
    LaunchedEffect(dayIndex) { listState.scrollToItem(0) }

    Column(modifier) {
        SectionTitle("Hour by hour", window.note)
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(window.rows, key = { _, h -> h.time }) { i, h ->
                val isNow = i == window.nowAt
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .width(64.dp)
                        .height(118.dp)
                        .background(
                            if (isNow) palette.tile else palette.tile2,
                            RoundedCornerShape(14.dp),
                        )
                        .padding(vertical = 10.dp),
                ) {
                    Text(
                        text = if (isNow) "Now" else units.hourLabel(h.time),
                        color = if (isNow) palette.fg else palette.fgDim,
                        fontSize = 12.sp,
                        fontWeight = if (isNow) FontWeight.Bold else FontWeight.Normal,
                    )
                    Spacer(Modifier.height(6.dp))
                    WeatherIconView(
                        icon = Wmo.icon(h.weatherCode, h.isDay),
                        modifier = Modifier.size(30.dp),
                        tint = palette.fg,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = units.temp(h.temperature) + "°",
                        color = palette.fg,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = units.percent(h.precipitationProbability),
                        // A near-zero chance is dimmed rather than hidden, so the
                        // column keeps a constant height and the strip stays level.
                        color = if ((h.precipitationProbability ?: 0.0) < 5) palette.fgDim
                        else palette.wet,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}
