package dk.cocode.weather.ui

import dk.cocode.weather.data.GeocodingApi
import dk.cocode.weather.data.Place
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Type-ahead location search, kept out of [WeatherViewModel] so that typing owns
 * its own state and cannot trigger a repaint of the forecast.
 */
class PlaceSearch(
    private val scope: CoroutineScope,
    /** The lookup itself; tests pass a fake so no request leaves the machine. */
    private val lookup: suspend (String) -> List<Place> = { GeocodingApi.search(it) },
    private val debounceMs: Long = DEBOUNCE_MS,
) {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    private var job: Job? = null

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query, failed = false) }
        job?.cancel()

        if (query.trim().length < MIN_QUERY) {
            _state.update { it.copy(results = emptyList(), searching = false) }
            return
        }

        job = scope.launch {
            delay(debounceMs) // one request per pause, not one per keystroke
            _state.update { it.copy(searching = true) }
            try {
                val results = lookup(query)
                // A good answer clears any failure an older, cancelled request left behind.
                _state.update { it.copy(results = results, searching = false, failed = false) }
            } catch (e: CancellationException) {
                // The next keystroke cancelled this request. That is not a failed search, and
                // swallowing it would paint "Search failed" over the newer request's results.
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(searching = false, results = emptyList(), failed = true)
                }
            }
        }
    }

    fun clear() {
        job?.cancel()
        _state.value = SearchUiState()
    }

    private companion object {
        const val MIN_QUERY = 2
        const val DEBOUNCE_MS = 300L
    }
}
