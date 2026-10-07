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
        override suspend fun cacheForecast(placeKey: String, body: String) {}
        override suspend fun cachedForecast(placeKey: String): Forecast? = cached
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
}
