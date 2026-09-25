package com.fishingcopilot.data.remote

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaceSearchClientTest {
    private val redang = """[{"lat":"5.7834163","lon":"103.0140756","name":"Pulau Redang",
        "display_name":"Pulau Redang, Kuala Nerus, Terengganu, Malaysia"}]"""

    @Test
    fun `parses name, detail and position`() {
        val place = PlaceSearchClient.parse(redang).single()
        assertEquals("Pulau Redang", place.name)
        assertEquals("Kuala Nerus, Terengganu, Malaysia", place.detail)
        assertEquals(5.7834163, place.latitude, 1e-9)
        assertEquals(103.0140756, place.longitude, 1e-9)
    }

    @Test
    fun `repeats come from the cache and requests are spaced`() = runTest {
        val urls = mutableListOf<String>()
        var clock = 1_000_000L
        val client = PlaceSearchClient(get = { urls += it; redang }, clock = { clock })
        client.search("Pulau Redang", "ms")
        client.search("  pulau redang ", "ms")
        assertEquals(1, urls.size)
        assertTrue(urls.single().contains("q=Pulau+Redang"))
        assertTrue(urls.single().contains("accept-language=ms"))
    }
}
