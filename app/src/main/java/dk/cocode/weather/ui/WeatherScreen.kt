package dk.cocode.weather.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dk.cocode.weather.data.Place
import dk.cocode.weather.ui.components.LocationSheet
import dk.cocode.weather.ui.components.WeatherTopBar

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
    var aboutOpen by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbars.showSnackbar(it)
            onMessageShown()
        }
    }

    // A flag rather than a navigation library: the app has one screen and this one detour.
    if (aboutOpen) {
        BackHandler { aboutOpen = false }
        AboutScreen(onBack = { aboutOpen = false })
        return
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
                onOpenAbout = { aboutOpen = true },
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
