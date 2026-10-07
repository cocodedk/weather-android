package dk.cocode.weather.ui

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dk.cocode.weather.R
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
                _state.update { it.withForecast(loaded.forecast, loaded.stale) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(loading = false, error = e.message ?: text(R.string.error_title))
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
        _state.update { it.withSelectedPlace(place) }
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
        val (places, toSelect) = withPlace(_state.value.places, place)
        _state.update { it.copy(places = places) }
        viewModelScope.launch { store.savePlaces(places) }
        selectPlace(toSelect)
        searcher.clear()
    }

    fun removePlace(place: Place) {
        // Never leave the app with nothing to show.
        val places = withoutPlace(_state.value.places, place) ?: run {
            _state.update { it.copy(message = text(R.string.msg_keep_one_location)) }
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
                val places = withDevicePlace(_state.value.places, place)
                _state.update { it.copy(places = places, locating = false) }
                store.savePlaces(places)
                selectPlace(place)
                searcher.clear()
            } catch (e: LocationPermissionMissing) {
                _state.update { it.copy(locating = false) }
                _permissionRequest.value = true
            } catch (e: Exception) {
                _state.update { it.copy(locating = false, message = text(locationFailureMessage(e))) }
            }
        }
    }

    fun onPermissionResult(granted: Boolean) {
        _permissionRequest.value = false
        if (granted) useDeviceLocation()
        else _state.update { it.copy(message = text(R.string.msg_location_denied)) }
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
        val next = nextTheme(_state.value.theme)
        _state.update { it.copy(theme = next) }
        viewModelScope.launch { store.saveTheme(next) }
    }

    private fun text(@StringRes id: Int): String = getApplication<Application>().getString(id)

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
