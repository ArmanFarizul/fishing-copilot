package com.fishingcopilot.data.remote

import com.fishingcopilot.marine.MarineHour
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenMeteoWeatherClientTest {
    private val marineBody = """
        {"hourly":{"time":[1790208000,1790211600],
          "wave_height":[0.5,0.6],"wave_period":[5.1,5.3],"wave_direction":[40,45],
          "ocean_current_velocity":[0.8,null],"ocean_current_direction":[180,185],
          "sea_surface_temperature":[29.7,29.6]}}
    """.trimIndent()
    private val weatherBody = """
        {"hourly":{"time":[1790208000,1790211600,1790215200],
          "wind_speed_10m":[7.0,8.0,9.0],"wind_gusts_10m":[12.0,13.0,14.0],"wind_direction_10m":[90,95,100],
          "pressure_msl":[1010.1,1009.8,1009.5]}}
    """.trimIndent()

    @Test
    fun `merges both APIs by hour in knots and keeps hours only one API has`() = runTest {
        val urls = mutableListOf<String>()
        val client = OpenMeteoWeatherClient { url ->
            urls += url
            if ("marine-api" in url) marineBody else weatherBody
        }

        val hours = client.marineHours(1.325, 103.44)

        assertEquals(
            MarineHour(1_790_208_000_000, 0.5, 5.1, 40.0, 7.0, 12.0, 90.0, 1010.1, 0.8, 180.0, 29.7),
            hours[0]
        )
        assertEquals(null, hours[1].currentKn)
        assertEquals(MarineHour(1_790_215_200_000, windKn = 9.0, gustKn = 14.0, windDirection = 100.0, pressureHpa = 1009.5), hours[2])
        urls.forEach { url ->
            listOf("wind_speed_unit=kn", "timeformat=unixtime", "past_days=1", "latitude=1.325").forEach {
                assertTrue("missing $it in $url", it in url)
            }
        }
    }
}
