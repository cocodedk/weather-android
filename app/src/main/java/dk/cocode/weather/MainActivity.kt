package dk.cocode.weather

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dk.cocode.weather.ui.WeatherScreen
import dk.cocode.weather.ui.WeatherViewModel
import dk.cocode.weather.ui.theme.WeatherTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val vm: WeatherViewModel = viewModel(factory = WeatherViewModel.Factory)
            val state by vm.state.collectAsStateWithLifecycle()
            val search by vm.search.collectAsStateWithLifecycle()
            val needsPermission by vm.permissionRequest.collectAsStateWithLifecycle()

            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { grants ->
                // Coarse is enough for a city forecast, so either grant counts.
                vm.onPermissionResult(grants.values.any { it })
            }

            // The ViewModel cannot show a system dialog, so it raises a flag and the
            // Activity — which owns the result contract — launches it.
            LaunchedEffect(needsPermission) {
                if (needsPermission) {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                        )
                    )
                }
            }

            WeatherTheme(isNight = state.isNight) {
                WeatherScreen(
                    state = state,
                    search = search,
                    onRefresh = vm::refresh,
                    onSelectDay = vm::selectDay,
                    onQueryChange = vm::onQueryChange,
                    onPickPlace = vm::addPlace,
                    onRemovePlace = vm::removePlace,
                    onUseDeviceLocation = vm::useDeviceLocation,
                    onToggleUnits = vm::toggleUnits,
                    onCycleTheme = vm::cycleTheme,
                    onMessageShown = vm::consumeMessage,
                )
            }
        }
    }
}
