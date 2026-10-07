package dk.cocode.weather.ui

import dk.cocode.weather.data.Place
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaceSelectionTest {
    private val copenhagen = Place(name = "Copenhagen", latitude = 55.68, longitude = 12.57)
    private val tokyo = Place(name = "Tokyo", latitude = 35.68, longitude = 139.69)
    private val here = Place(name = "Here", latitude = 55.0, longitude = 12.0, isDeviceLocation = true)
    private val moved = here.copy(name = "There", latitude = 35.0, longitude = 139.0)

    /** What the selection did to the outside world. */
    private class Effects {
        val savedPlaces = mutableListOf<List<Place>>()
        val savedKeys = mutableListOf<String>()
        var widgetPokes = 0
        var loads = 0
    }

    private fun CoroutineScope.selectionOver(state: MutableStateFlow<WeatherUiState>, fx: Effects) =
        PlaceSelection(
            state = state,
            scope = this,
            savePlaces = { fx.savedPlaces += it },
            saveSelected = { fx.savedKeys += it },
            notifyWidgets = { fx.widgetPokes++ },
            reload = { fx.loads++ },
        )

    private suspend fun CoroutineScope.settle() = coroutineContext.job.children.toList().joinAll()

    private fun showing(selected: Place, places: List<Place>) =
        MutableStateFlow(WeatherUiState(places = places, selected = selected))

    @Test
    fun aNewFixForTheSelectedDeviceEntryLoadsTheForecastAndTellsTheWidget() = runBlocking {
        val fx = Effects()
        val state = showing(here, listOf(copenhagen, here))

        selectionOver(state, fx).useFix(moved)
        settle()

        assertEquals(1, fx.loads)
        assertEquals(1, fx.widgetPokes)
        assertEquals(listOf("device"), fx.savedKeys)
        assertEquals(listOf(listOf(copenhagen, moved)), fx.savedPlaces)
        assertEquals(moved, state.value.selected)
    }

    @Test
    fun theSameFixAgainLoadsNothingAndTellsNoOne() = runBlocking {
        val fx = Effects()
        val state = showing(here, listOf(copenhagen, here))

        selectionOver(state, fx).useFix(here.copy())
        settle()

        assertEquals(0, fx.loads)
        assertEquals(0, fx.widgetPokes)
        assertEquals(emptyList<String>(), fx.savedKeys)
    }

    @Test
    fun aFixWhileAnotherPlaceIsSelectedSelectsTheDeviceEntry() = runBlocking {
        val fx = Effects()
        val state = showing(tokyo, listOf(tokyo, here))

        selectionOver(state, fx).useFix(moved)
        settle()

        assertEquals(1, fx.loads)
        assertEquals(1, fx.widgetPokes)
        assertEquals(moved, state.value.selected)
        assertEquals(listOf(tokyo, moved), state.value.places)
    }

    @Test
    fun pickingTheSavedPlaceAlreadyShownLoadsNothingAndTellsNoOne() = runBlocking {
        val fx = Effects()
        val state = showing(copenhagen, listOf(copenhagen, tokyo))

        selectionOver(state, fx).select(copenhagen)
        settle()

        assertEquals(0, fx.loads)
        assertEquals(0, fx.widgetPokes)
        assertEquals(emptyList<String>(), fx.savedKeys)
    }

    @Test
    fun pickingAnotherSavedPlaceLoadsItAndTellsTheWidgetAfterSavingTheChoice() = runBlocking {
        val fx = Effects()
        val state = showing(copenhagen, listOf(copenhagen, tokyo))

        selectionOver(state, fx).select(tokyo)
        settle()

        assertEquals(1, fx.loads)
        assertEquals(1, fx.widgetPokes)
        assertEquals(listOf(tokyo.key), fx.savedKeys)
        assertEquals(tokyo, state.value.selected)
    }
}
