package dk.cocode.weather.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** WeatherStore over a real DataStore in a temporary file, with no Android around it. */
class WeatherStoreTest {
    @get:Rule val tmp = TemporaryFolder()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var data: DataStore<Preferences>
    private lateinit var store: WeatherStore

    private val copenhagen = Place(name = "Copenhagen", latitude = 55.68, longitude = 12.57)
    private val spotA = Place(name = "A", latitude = 55.0, longitude = 12.0, isDeviceLocation = true)
    private val spotB = spotA.copy(name = "B", latitude = 35.0, longitude = 139.0)

    @Before
    fun open() {
        data = PreferenceDataStoreFactory.create(scope = scope) { File(tmp.root, "weather.preferences_pb") }
        store = WeatherStore(data)
    }

    @After
    fun close() = scope.cancel()

    /** A response body that parses to a forecast carrying [id], so tests can tell forecasts apart. */
    private fun body(id: Int) = """{"utc_offset_seconds":$id}"""

    private suspend fun cachedId(place: Place): Int? = store.cachedForecast(place.cacheKey)?.utcOffsetSeconds

    private val legacyKey = stringPreferencesKey("cache.device")

    @Test
    fun onlyTheNewestDeviceForecastIsKept() = runBlocking {
        store.savePlaces(listOf(copenhagen, spotA))
        store.cacheForecast(spotA.cacheKey, body(1))
        store.savePlaces(listOf(copenhagen, spotB)) // the phone moved
        store.cacheForecast(spotB.cacheKey, body(2))

        assertNull(cachedId(spotA))
        assertEquals(2, cachedId(spotB))
    }

    @Test
    fun aLegacyDeviceEntryIsNeverAFallbackForAPositionAndIsRemovedAtTheNextGoodFetch() = runBlocking {
        data.edit { it[legacyKey] = body(9) } // what versions before the position-keyed cache wrote
        store.savePlaces(listOf(copenhagen, spotB))

        assertNull(cachedId(spotB))

        store.cacheForecast(spotB.cacheKey, body(2))
        assertNull(data.data.first()[legacyKey])
        assertEquals(2, cachedId(spotB))
    }

    @Test
    fun savedPlaceCachesAreUntouchedByDeviceWrites() = runBlocking {
        store.savePlaces(listOf(copenhagen, spotA))
        store.cacheForecast(copenhagen.cacheKey, body(5))
        store.cacheForecast(spotA.cacheKey, body(1))
        store.savePlaces(listOf(copenhagen, spotB))
        store.cacheForecast(spotB.cacheKey, body(2))

        assertEquals(5, cachedId(copenhagen))
    }

    @Test
    fun aDeviceResultForASpotThePhoneHasLeftIsDropped() = runBlocking {
        store.savePlaces(listOf(copenhagen, spotB)) // the phone is at B now
        store.cacheForecast(spotB.cacheKey, body(2))

        store.cacheForecast(spotA.cacheKey, body(1)) // a slow request for A finishes late

        assertEquals(2, cachedId(spotB))
        assertNull(cachedId(spotA))
    }

    @Test
    fun aDeviceResultWithNoSavedDeviceEntryIsDropped() = runBlocking {
        store.cacheForecast(spotA.cacheKey, body(1))
        assertNull(cachedId(spotA))
    }

    @Test
    fun aLoadForTheOldSpotThatFinishesAfterTheNewSpotsLoadLeavesTheNewSpotsCache() = runBlocking {
        store.savePlaces(listOf(copenhagen, spotA))
        val gates = mapOf(spotA.cacheKey to CompletableDeferred<Unit>(), spotB.cacheKey to CompletableDeferred())
        val repo = ForecastRepository(store) { place ->
            gates.getValue(place.cacheKey).await()
            ForecastApi.parse(body(if (place == spotA) 1 else 2)) to body(if (place == spotA) 1 else 2)
        }

        val loadA = launch { repo.load(spotA) } // A starts
        store.savePlaces(listOf(copenhagen, spotB)) // the phone moves on
        gates.getValue(spotB.cacheKey).complete(Unit)
        repo.load(spotB) // B completes
        gates.getValue(spotA.cacheKey).complete(Unit)
        loadA.join() // A completes late

        assertEquals(2, cachedId(spotB))
        assertNotNull(store.cachedForecast(spotB.cacheKey))
        assertNull(cachedId(spotA))
    }
}
