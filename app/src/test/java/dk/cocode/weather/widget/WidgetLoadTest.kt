package dk.cocode.weather.widget

import dk.cocode.weather.data.Forecast
import dk.cocode.weather.data.ForecastApi
import dk.cocode.weather.data.ForecastCache
import dk.cocode.weather.data.ForecastRepository
import dk.cocode.weather.data.Place
import dk.cocode.weather.data.WeatherStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.concurrent.CountDownLatch

class WidgetLoadTest {
    private val copenhagen = Place(name = "Copenhagen", latitude = 55.68, longitude = 12.57)
    private val spotA = Place(name = "A", latitude = 55.0, longitude = 12.0, isDeviceLocation = true)
    private val forecast: Forecast = ForecastApi.parse("{}")

    private object NoCache : ForecastCache {
        override suspend fun cacheForecast(cacheKey: String, body: String) {}
        override suspend fun cachedForecast(cacheKey: String): Forecast? = null
    }

    private fun prefs(selected: Place?, imperial: Boolean = false) = WeatherStore.Prefs(
        places = listOfNotNull(copenhagen, selected),
        selectedKey = selected?.key,
        imperial = imperial,
        theme = "auto",
    )

    @Test
    fun nothingSavedIsNoPlace() = runBlocking {
        val repo = ForecastRepository(NoCache) { forecast to "{}" }
        assertEquals(WidgetLoad.NoPlace, loadForWidget({ prefs(null).copy(places = emptyList()) }, repo))
    }

    @Test
    fun theSelectedPlaceLoadsAsReady() = runBlocking {
        val repo = ForecastRepository(NoCache) { forecast to "{}" }
        val found = loadForWidget({ prefs(spotA, imperial = true) }, repo)
        assertEquals(WidgetLoad.Ready(spotA, ForecastRepository.Loaded(forecast, stale = false), imperial = true), found)
    }

    @Test
    fun noNetworkAndNoCacheIsUnavailable() = runBlocking {
        val repo = ForecastRepository(NoCache) { throw IOException("offline") }
        assertEquals(WidgetLoad.Unavailable(copenhagen), loadForWidget({ prefs(copenhagen) }, repo))
    }

    @Test
    fun aCancelledRefreshIsCancelledNotReportedAsUnavailable() = runBlocking {
        val inFlight = CompletableDeferred<Unit>()
        val answer = CountDownLatch(1)
        // Blocks a thread like Http.getString, so the cancel lands before the answer does.
        val repo = ForecastRepository(NoCache) {
            withContext(Dispatchers.IO) { inFlight.complete(Unit); answer.await(); forecast to "{}" }
        }

        var returned: WidgetLoad? = null
        val job = launch { returned = loadForWidget({ prefs(copenhagen) }, repo) }
        withTimeout(5_000) { inFlight.await() }
        job.cancel()
        answer.countDown()
        job.join()

        assertNull("a cancelled refresh must not draw anything", returned)
    }
}
