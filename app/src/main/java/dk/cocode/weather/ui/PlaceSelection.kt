package dk.cocode.weather.ui

import dk.cocode.weather.data.Place
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * What picking a place, or taking a new device fix, sets going: the screen's state changes, the
 * choice is saved, the widget is told and the forecast is loaded again. The edges (storage, the
 * widget, the loader) come in as functions, so tests can fake them.
 */
class PlaceSelection(
    private val state: MutableStateFlow<WeatherUiState>,
    private val scope: CoroutineScope,
    private val savePlaces: suspend (List<Place>) -> Unit,
    private val saveSelected: suspend (String) -> Unit,
    private val notifyWidgets: () -> Unit,
    private val reload: () -> Unit,
) {

    /** Shows [place] and loads its forecast, unless the screen already shows exactly that place. */
    fun select(place: Place) {
        if (state.value.isShowing(place)) return
        state.update { it.withSelectedPlace(place) }
        scope.launch {
            // Notify only after the write commits — the widget re-reads the store,
            // and poking it first would just make it redraw the old place.
            saveSelected(place.key)
            notifyWidgets()
        }
        reload()
    }

    /**
     * Keeps [fix] as the one device-location entry and shows it. Another fix for the entry that is
     * already selected is a new place (the key stays "device", the coordinates change), so it
     * loads a new forecast too.
     */
    suspend fun useFix(fix: Place) {
        val places = withDevicePlace(state.value.places, fix)
        state.update { it.copy(places = places, locating = false) }
        savePlaces(places)
        select(fix)
    }
}
