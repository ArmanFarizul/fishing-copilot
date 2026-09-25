package com.fishingcopilot.data.remote

import com.fishingcopilot.data.satellite.SatelliteRepositoryTest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException
import java.time.LocalDate

class SatelliteClientTest {
    @Test
    fun `parses the pipeline layout`() {
        val data = SatelliteClient.parse(SatelliteRepositoryTest.document(sst = 29.5))
        assertEquals(2, data.grid.nlon)
        assertEquals(LocalDate.of(2026, 9, 23), data.seaTemp!!.date)
        assertEquals(listOf(29.5, null), data.seaTemp!!.values)
        assertEquals(LocalDate.of(2026, 9, 22), data.chlorophyll!!.date)
        assertNull(data.front)
        assertNull(data.clarity)
    }

    @Test(expected = IOException::class)
    fun `a layer with the wrong cell count is rejected`() {
        SatelliteClient.parse(SatelliteRepositoryTest.document(sst = 29.5).replace("[0.4,null]", "[0.4]"))
    }

    @Test(expected = IOException::class)
    fun `a non-json download is an IOException`() = runTest {
        SatelliteClient { "Not Found" }.download()
    }

    @Test(expected = IOException::class)
    fun `a bad date is an IOException`() = runTest {
        SatelliteClient { SatelliteRepositoryTest.document(sst = 29.5).replace("2026-09-23", "yesterday") }.download()
    }
}
