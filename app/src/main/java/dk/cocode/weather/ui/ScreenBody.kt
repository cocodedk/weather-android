package dk.cocode.weather.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dk.cocode.weather.R
import dk.cocode.weather.domain.Units
import dk.cocode.weather.ui.components.DailyList
import dk.cocode.weather.ui.components.Hero
import dk.cocode.weather.ui.components.HourlyStrip
import dk.cocode.weather.ui.components.StatsGrid
import dk.cocode.weather.ui.theme.LocalPalette

@Composable
fun ScreenBody(
    state: WeatherUiState,
    onSelectDay: (Int) -> Unit,
    onRefresh: () -> Unit,
) {
    val palette = LocalPalette.current
    val forecast = state.forecast
    val units = rememberUnits(state)

    when {
        // A stale cached forecast still renders normally; only the status line says so.
        forecast != null -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(8.dp))
            Hero(forecast, state.dayIndex, units)
            Spacer(Modifier.height(18.dp))
            StatsGrid(forecast, state.dayIndex, units)
            Spacer(Modifier.height(10.dp))
            HourlyStrip(forecast, state.dayIndex, units)
            Spacer(Modifier.height(20.dp))
            DailyList(forecast, state.dayIndex, units, onSelectDay)
            StatusLine(state, units)
            // Clears the gesture/navigation bar so the last row is not cut off.
            Spacer(Modifier.height(16.dp))
            Spacer(Modifier.navigationBarsPadding())
        }

        state.loading -> Centered { CircularProgressIndicator(color = palette.accent) }

        state.error != null -> Centered {
            Text(stringResource(R.string.error_title), color = palette.fg, fontSize = 17.sp)
            Spacer(Modifier.height(6.dp))
            // The system's own error text (state.error) is kept in the state for debugging but
            // is not shown: "HTTP 500: {...}" tells a customer nothing they can act on.
            Text(
                stringResource(R.string.error_detail),
                color = palette.fgDim,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(14.dp))
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, stringResource(R.string.error_retry), tint = palette.accent)
            }
        }
    }
}

@Composable
private fun StatusLine(state: WeatherUiState, units: Units) {
    val palette = LocalPalette.current
    val forecast = state.forecast ?: return
    val time = forecast.current.time
    val text = if (state.stale) {
        // The date matters here: saved data can be days old, and a bare clock time would
        // look recent.
        stringResource(R.string.status_not_updated, units.dateTime(time))
    } else {
        stringResource(R.string.status_updated, units.clock(time))
    }
    Text(
        text = text,
        color = if (state.stale) palette.accent else palette.fgDim,
        fontSize = 12.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
    )
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp),
        ) { content() }
    }
}
