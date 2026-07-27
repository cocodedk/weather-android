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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dk.cocode.weather.data.Place
import dk.cocode.weather.ui.components.DailyList
import dk.cocode.weather.ui.components.Hero
import dk.cocode.weather.ui.components.HourlyStrip
import dk.cocode.weather.ui.components.LocationSheet
import dk.cocode.weather.ui.components.StatsGrid
import dk.cocode.weather.ui.components.WeatherTopBar
import dk.cocode.weather.ui.theme.LocalPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    state: WeatherUiState,
    search: SearchUiState,
    onRefresh: () -> Unit,
    onSelectDay: (Int) -> Unit,
    onQueryChange: (String) -> Unit,
    onPickPlace: (Place) -> Unit,
    onRemovePlace: (Place) -> Unit,
    onUseDeviceLocation: () -> Unit,
    onToggleUnits: () -> Unit,
    onCycleTheme: () -> Unit,
    onAddWidget: () -> Unit,
    onMessageShown: () -> Unit,
) {
    val snackbars = remember { SnackbarHostState() }
    var sheetOpen by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbars.showSnackbar(it)
            onMessageShown()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbars) },
        topBar = {
            WeatherTopBar(
                state = state,
                onOpenLocations = { sheetOpen = true },
                onToggleUnits = onToggleUnits,
                onCycleTheme = onCycleTheme,
                onRefresh = onRefresh,
                onAddWidget = onAddWidget,
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.loading,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            ScreenBody(state, onSelectDay, onRefresh)
        }
    }

    if (sheetOpen) {
        LocationSheet(
            search = search,
            saved = state.places,
            selectedKey = state.selected?.key,
            locating = state.locating,
            onQueryChange = onQueryChange,
            onPick = { onPickPlace(it); sheetOpen = false },
            onRemove = onRemovePlace,
            onUseDeviceLocation = onUseDeviceLocation,
            onDismiss = { sheetOpen = false },
        )
    }
}

@Composable
private fun ScreenBody(
    state: WeatherUiState,
    onSelectDay: (Int) -> Unit,
    onRefresh: () -> Unit,
) {
    val palette = LocalPalette.current
    val forecast = state.forecast

    when {
        // A stale cached forecast still renders normally; only the status line says so.
        forecast != null -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(8.dp))
            Hero(forecast, state.dayIndex, state.units)
            Spacer(Modifier.height(18.dp))
            StatsGrid(forecast, state.dayIndex, state.units)
            Spacer(Modifier.height(10.dp))
            HourlyStrip(forecast, state.dayIndex, state.units)
            Spacer(Modifier.height(20.dp))
            DailyList(forecast, state.dayIndex, state.units, onSelectDay)
            StatusLine(state)
            // Clears the gesture/navigation bar so the last row is not cut off.
            Spacer(Modifier.height(16.dp))
            Spacer(Modifier.navigationBarsPadding())
        }

        state.loading -> Centered { CircularProgressIndicator(color = palette.accent) }

        state.error != null -> Centered {
            Text("Could not load the forecast", color = palette.fg, fontSize = 17.sp)
            Spacer(Modifier.height(6.dp))
            Text(state.error, color = palette.fgDim, fontSize = 13.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(14.dp))
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, "Retry", tint = palette.accent)
            }
        }
    }
}

@Composable
private fun StatusLine(state: WeatherUiState) {
    val palette = LocalPalette.current
    val forecast = state.forecast ?: return
    val text = if (state.stale) {
        "Offline — showing data from ${state.units.clock(forecast.current.time)}"
    } else {
        "Updated ${state.units.clock(forecast.current.time)}  ·  Open-Meteo"
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
