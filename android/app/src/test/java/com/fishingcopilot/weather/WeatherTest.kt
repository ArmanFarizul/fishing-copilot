package com.fishingcopilot.weather

import com.fishingcopilot.data.remote.ForecastClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class WeatherTest {
    private val hour = 3_600_000L
    private val t0 = 1_790_308_800_000L // 2026-09-25 08:00 UTC

    private fun h(offsetHours: Int, sky: Sky, rain: Int = 20) = WeatherHour(t0 + offsetHours * hour, 30.0, rain, sky)

    @Test
    fun `WMO codes map to sky conditions`() {
        assertEquals(Sky.CLEAR, Sky.of(0))
        assertEquals(Sky.PARTLY_CLOUDY, Sky.of(2))
        assertEquals(Sky.CLOUDY, Sky.of(3))
        assertEquals(Sky.RAIN, Sky.of(81))
        assertEquals(Sky.HEAVY_RAIN, Sky.of(82))
        assertEquals(Sky.THUNDERSTORM, Sky.of(95))
    }

    @Test
    fun `thunderstorm within three hours wins over heavy rain`() {
        val hours = listOf(h(0, Sky.CLOUDY), h(1, Sky.HEAVY_RAIN), h(2, Sky.THUNDERSTORM), h(5, Sky.THUNDERSTORM))
        assertEquals(StormAlert(StormKind.THUNDERSTORM, t0 + 2 * hour), stormAlert(hours, t0 + 10 * 60_000))
    }

    @Test
    fun `likely rain counts as heavy, storms beyond three hours do not`() {
        assertEquals(StormKind.HEAVY_RAIN, stormAlert(listOf(h(1, Sky.RAIN, rain = 80)), t0)!!.kind)
        assertNull(stormAlert(listOf(h(1, Sky.RAIN, rain = 40), h(4, Sky.THUNDERSTORM)), t0))
    }

    @Test
    fun `the hour already under way still counts`() {
        val alert = stormAlert(listOf(h(0, Sky.THUNDERSTORM)), t0 + 20 * 60_000)!!
        assertEquals(t0 + 20 * 60_000, alert.atMillis)
    }

    @Test
    fun `parses a real Open-Meteo forecast`() {
        val f = ForecastClient.parse(javaClass.getResource("/openmeteo_forecast_2026-09-25.json")!!.readText())
        assertEquals(31.3, f.now.tempC, 0.0)
        assertEquals(Sky.CLOUDY, f.now.sky)
        assertEquals(7, f.days.size)
        // Daily times are local midnights in Malaysia, so the first day is the 25th, not the 24th in UTC.
        assertEquals(LocalDate.of(2026, 9, 25), f.days.first().date)
        assertEquals(Sky.THUNDERSTORM, f.days.first().sky)
    }
}
