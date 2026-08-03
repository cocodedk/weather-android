package dk.cocode.weather.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dk.cocode.weather.data.DeviceLocation
import dk.cocode.weather.data.ForecastRepository
import dk.cocode.weather.data.LocationPermissionMissing
import dk.cocode.weather.data.Place
import dk.cocode.weather.data.WeatherStore
import dk.cocode.weather.widget.WeatherWidgetProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WeatherViewModel(app: Application) : AndroidViewModel(app) {

    private val store = WeatherStore(app)
    private val repo = ForecastRepository(store)

    private val _state = MutableStateFlow(WeatherUiState())
    val state: StateFlow<WeatherUiState> = _state.asStateFlow()

    private val searcher = PlaceSearch(viewModelScope)
    val search: StateFlow<SearchUiState> = searcher.state

    private var loadJob: Job? = null

    /** Signals the UI to launch the system permission dialog. */
    private val _permissionRequest = MutableStateFlow(false)
    val permissionRequest: StateFlow<Boolean> = _permissionRequest.asStateFlow()

    init {
        viewModelScope.launch {
            val prefs = store.prefs.first()
            val selected = prefs.places.firstOrNull { it.key == prefs.selectedKey }
                ?: prefs.places.first()
            _state.update {
                it.copy(
                    places = prefs.places,
                    selected = selected,
                    imperial = prefs.imperial,
                    theme = prefs.theme,
                    use24Hour = android.text.format.DateFormat.is24HourFormat(app),
                )
            }
            refresh()
        }
    }

    // ---------- forecast ----------

    fun refresh() {
        val place = _state.value.selected ?: return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val loaded = repo.load(place)
                _state.update {
                    it.copy(
                        forecast = loaded.forecast,
                        stale = loaded.stale,
                        loading = false,
                        error = null,
                        // A shorter forecast (or a new place) must not leave the
                        // selector pointing past the end of the list.
                        dayIndex = it.dayIndex.coerceIn(0, (loaded.forecast.daily.size - 1).coerceAtLeast(0)),
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(loading = false, error = e.message ?: "Could not load the forecast")
                }
            }
        }
    }

    fun selectDay(index: Int) {
        val daily = _state.value.forecast?.daily ?: return
        if (index in daily.indices) _state.update { it.copy(dayIndex = index) }
    }

    // ---------- places ----------

    fun selectPlace(place: Place) {
        if (place.key == _state.value.selected?.key) return
        // dayIndex resets: "Wednesday" in the old city is not the row the user wants
        // to keep staring at after switching to a new one.
        _state.update { it.copy(selected = place, forecast = null, dayIndex = 0, stale = false) }
        viewModelScope.launch {
            // Notify only after the write commits — the widget re-reads the store,
            // and poking it first would just make it redraw the old place.
            store.saveSelected(place.key)
            notifyWidgets()
        }
        refresh()
    }

    /**
     * The widget follows the app's selected place and unit preference, so anything
     * that changes either has to poke it — otherwise it shows the old city until
     * its next half-hourly tick.
     */
    private fun notifyWidgets() =
        WeatherWidgetProvider.notifyDataChanged(getApplication())

    /** Adds a searched place (if new), selects it, and persists the list. */
    fun addPlace(place: Place) {
        val current = _state.value.places
        val existing = current.firstOrNull { it.key == place.key }
        val places = if (existing != null) current else current + place
        _state.update { it.copy(places = places) }
        viewModelScope.launch { store.savePlaces(places) }
        selectPlace(existing ?: place)
        searcher.clear()
    }

    fun removePlace(place: Place) {
        val places = _state.value.places.filterNot { it.key == place.key }
        // Never leave the app with nothing to show.
        if (places.isEmpty()) {
            _state.update { it.copy(message = "Keep at least one location") }
            return
        }
        _state.update { it.copy(places = places) }
        viewModelScope.launch { store.savePlaces(places) }
        if (_state.value.selected?.key == place.key) selectPlace(places.first())
    }

    // ---------- device location ----------

    fun useDeviceLocation() {
        val app = getApplication<Application>()
        if (!DeviceLocation.hasPermission(app)) {
            _permissionRequest.value = true
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(locating = true, error = null) }
            try {
                val place = DeviceLocation.current(app)
                // Replace any previous device entry rather than stacking one per fix.
                val places = _state.value.places.filterNot { it.isDeviceLocation } + place
                _state.update { it.copy(places = places, locating = false) }
                store.savePlaces(places)
                selectPlace(place)
                searcher.clear()
            } catch (e: LocationPermissionMissing) {
                _state.update { it.copy(locating = false) }
                _permissionRequest.value = true
            } catch (e: Exception) {
                _state.update {
                    it.copy(locating = false, message = e.message ?: "Could not get your location")
                }
            }
        }
    }

    fun onPermissionResult(granted: Boolean) {
        _permissionRequest.value = false
        if (granted) useDeviceLocation()
        else _state.update { it.copy(message = "Location permission denied") }
    }

    // ---------- search ----------

    fun onQueryChange(query: String) = searcher.onQueryChange(query)

    // ---------- preferences ----------

    fun toggleUnits() {
        val imperial = !_state.value.imperial
        _state.update { it.copy(imperial = imperial) }
        viewModelScope.launch {
            store.saveImperial(imperial)
            notifyWidgets()   // after the write, or the widget re-reads the old value
        }
    }

    fun cycleTheme() {
        val next = when (_state.value.theme) {
            WeatherStore.THEME_AUTO -> WeatherStore.THEME_DAY
            WeatherStore.THEME_DAY -> WeatherStore.THEME_NIGHT
            else -> WeatherStore.THEME_AUTO
        }
        _state.update { it.copy(theme = next) }
        viewModelScope.launch { store.saveTheme(next) }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(
                modelClass: Class<T>,
                extras: androidx.lifecycle.viewmodel.CreationExtras,
            ): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                return WeatherViewModel(app) as T
            }
        }
    }
}
