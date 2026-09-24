package com.fishingcopilot.data.remote

import com.fishingcopilot.tide.SeaLevelSample
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.LocalDate

class OpenMeteoMarineClientTest {
    @Test
    fun `parses unix seconds into millis and skips hours without a value`() {
        val body = """
            {"latitude":1.29,"hourly_units":{"time":"unixtime","sea_level_height_msl":"m"},
             "hourly":{"time":[1789862400,1789866000,1789869600],"sea_level_height_msl":[0.86,null,0.03]}}
        """.trimIndent()

        assertEquals(
            listOf(SeaLevelSample(1_789_862_400_000L, 0.86), SeaLevelSample(1_789_869_600_000L, 0.03)),
            OpenMeteoMarineClient.parseSeaLevel(body)
        )
    }

    @Test
    fun `requests hourly sea level in unix time for the given dates`() = runTest {
        var requested = ""
        val client = OpenMeteoMarineClient { url ->
            requested = url
            """{"hourly":{"time":[],"sea_level_height_msl":[]}}"""
        }

        client.seaLevel(1.325, 103.44, LocalDate.of(2025, 9, 20), LocalDate.of(2026, 9, 19))

        assertTrue(requested.startsWith("https://marine-api.open-meteo.com/v1/marine?"))
        listOf(
            "latitude=1.325", "longitude=103.44", "hourly=sea_level_height_msl",
            "timeformat=unixtime", "start_date=2025-09-20", "end_date=2026-09-19"
        ).forEach { assertTrue("missing $it in $requested", it in requested) }
    }

    @Test(expected = IOException::class)
    fun `surfaces an Open-Meteo error body as IOException`() {
        OpenMeteoMarineClient.parseSeaLevel("""{"error":true,"reason":"Parameter 'start_date' is out of range"}""")
    }
}
