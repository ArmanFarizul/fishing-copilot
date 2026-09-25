package com.fishingcopilot.data.satellite

import com.fishingcopilot.data.remote.SatelliteClient
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class SatelliteRepositoryTest {
    @get:Rule val folder = TemporaryFolder()

    private val now = 1_790_220_000_000L
    private var requests = 0
    private var body = document(sst = 29.5)
    private val client = SatelliteClient {
        requests++
        body
    }

    private fun cacheFile() = File(folder.root, "satellite/daily_marine_fronts.json")

    @Test
    fun `downloads once, serves the cache, and refetches after six hours`() = runTest {
        val file = cacheFile()
        assertTrue(SatelliteRepository(file, client, { now }).refreshIfStale())
        assertEquals(1, requests)

        assertFalse(SatelliteRepository(file, client, { now + 5 * 3_600_000 }).refreshIfStale())
        assertEquals(1, requests)

        body = document(sst = 30.5)
        val repo = SatelliteRepository(file, client, { now + 7 * 3_600_000 })
        assertTrue(repo.refreshIfStale())
        assertEquals(2, requests)
        assertEquals(30.5, repo.data.first()!!.data.seaTemp!!.values[0]!!, 0.0)
    }

    @Test
    fun `reads an earlier download without the network`() = runTest {
        val file = cacheFile()
        SatelliteRepository(file, client, { now }).refreshIfStale()
        val cached = SatelliteRepository(file, SatelliteClient { throw IOException("offline") }, { now }).data.first()!!
        assertEquals(29.5, cached.data.seaTemp!!.values[0]!!, 0.0)
        assertEquals(now, cached.fetchedAtMillis)
    }

    @Test
    fun `a broken download keeps the previous file`() = runTest {
        val file = cacheFile()
        SatelliteRepository(file, client, { now }).refreshIfStale()
        body = "<html>rate limited</html>"
        try {
            SatelliteRepository(file, client, { now + 7 * 3_600_000 }).refreshIfStale()
            fail("expected IOException")
        } catch (e: IOException) {
            // expected
        }
        assertEquals(29.5, SatelliteRepository(file, client, { now }).data.first()!!.data.seaTemp!!.values[0]!!, 0.0)
    }

    @Test
    fun `no cache and no download is empty`() = runTest {
        assertNull(SatelliteRepository(cacheFile(), client, { now }).data.first())
    }

    companion object {
        fun document(sst: Double) = """
            {"version":1,"generated_utc":"2026-09-24T23:09:12Z",
             "grid":{"lat0":1.0,"lon0":103.0,"step":0.25,"nlat":1,"nlon":2,"order":"row-major"},
             "layers":{
               "sst_c":{"date":"2026-09-23","dataset":"METOFFICE-GLO-SST-L4-NRT-OBS-SST-V2","values":[$sst,null]},
               "chl_mg_m3":{"date":"2026-09-22","dataset":"x","values":[0.4,null]}
             }}
        """.trimIndent()
    }
}
