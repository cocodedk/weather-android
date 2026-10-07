package dk.cocode.weather.data

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException
import java.util.concurrent.CountDownLatch

class ForecastRepositoryTest {
    private val place = Place(name = "Copenhagen", latitude = 55.68, longitude = 12.57)

    private val forecast = Forecast(
        fetchedAt = 0L,
        utcOffsetSeconds = 0,
        current = Current(
            time = null, temperature = null, humidity = null, apparentTemperature = null,
            isDay = true, precipitation = null, weatherCode = null, windSpeed = null,
            windDirection = null, pressure = null,
        ),
        hourly = emptyList(),
        daily = emptyList(),
    )

    private class FakeCache(private val cached: Forecast?) : ForecastCache {
        override suspend fun cacheForecast(cacheKey: String, body: String) {}
        override suspend fun cachedForecast(cacheKey: String): Forecast? = cached
    }

    /** Remembers what was cached under each key, like the real store (the body names the forecast). */
    private class KeyedCache(private val byBody: Map<String, Forecast>) : ForecastCache {
        val keys = mutableListOf<String>()
        private val stored = mutableMapOf<String, Forecast>()
        override suspend fun cacheForecast(cacheKey: String, body: String) {
            keys += cacheKey
            stored[cacheKey] = byBody.getValue(body)
        }
        override suspend fun cachedForecast(cacheKey: String): Forecast? = stored[cacheKey]
    }

    @Test
    fun aLoadCancelledByTheNextPlaceReturnsNothingNotThisPlacesCachedForecast() = runBlocking {
        val inFlight = CompletableDeferred<Unit>()
        val answer = CountDownLatch(1)
        // Blocks a thread like Http.getString, so the cancel lands before the answer does.
        val repo = ForecastRepository(FakeCache(forecast)) {
            withContext(Dispatchers.IO) {
                inFlight.complete(Unit)
                answer.await()
                forecast to "{}"
            }
        }

        var returned: ForecastRepository.Loaded? = null
        val job = launch { returned = repo.load(place) }
        withTimeout(5_000) { inFlight.await() }
        job.cancel()
        answer.countDown()
        job.join()

        assertNull("a cancelled load must not hand back a forecast", returned)
    }

    @Test
    fun aFailedFetchFallsBackToTheCachedForecastAndMarksItStale() = runBlocking {
        val repo = ForecastRepository(FakeCache(forecast)) { throw IOException("offline") }
        val loaded = repo.load(place)
        assertSame(forecast, loaded.forecast)
        assertTrue(loaded.stale)
    }

    @Test
    fun aFailedFetchWithNothingCachedSurfacesTheRealCause() = runBlocking {
        val repo = ForecastRepository(FakeCache(null)) { throw IOException("offline") }
        try {
            repo.load(place)
            fail("expected the fetch failure")
        } catch (e: IOException) {
            assertEquals("offline", e.message)
        }
    }

    // The device entry has the key "device" whichever fix it holds, so its cache must tell fixes apart.
    private val spotA = Place(name = "Here", latitude = 55.68, longitude = 12.57, isDeviceLocation = true)
    private val spotB = spotA.copy(name = "There", latitude = 35.68, longitude = 139.69)
    private val forecastA = forecast.copy(fetchedAt = 1L)

    @Test
    fun aForecastCachedForOneDeviceFixIsNotShownForAnotherWhenTheFetchFails() = runBlocking {
        val cache = KeyedCache(mapOf("A" to forecastA))
        ForecastRepository(cache) { forecastA to "A" }.load(spotA)

        val offline = ForecastRepository(cache) { throw IOException("offline") }
        try {
            offline.load(spotB)
            fail("the forecast for the old spot must not stand in for the new one")
        } catch (e: IOException) {
            assertEquals("offline", e.message)
        }
    }

    @Test
    fun aForecastCachedForTheDeviceSpotIsStillShownOfflineNearTheSameSpot() = runBlocking {
        val cache = KeyedCache(mapOf("A" to forecastA))
        ForecastRepository(cache) { forecastA to "A" }.load(spotA)

        val nearby = spotA.copy(latitude = 55.6805, longitude = 12.5702) // GPS jitter, same ~1 km
        val loaded = ForecastRepository(cache) { throw IOException("offline") }.load(nearby)
        assertSame(forecastA, loaded.forecast)
        assertTrue(loaded.stale)
    }

    @Test
    fun theSavedPlacesCacheKeyStaysItsCoordinates() {
        assertEquals("55.68,12.57", place.cacheKey)
        assertEquals("device@55.68,12.57", spotA.cacheKey)
        assertEquals("device", spotA.key)
    }
}
