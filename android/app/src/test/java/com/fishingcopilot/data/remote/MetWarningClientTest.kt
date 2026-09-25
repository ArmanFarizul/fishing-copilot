package com.fishingcopilot.data.remote

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException
import java.time.Instant

class MetWarningClientTest {
    private val body = javaClass.getResource("/metmalaysia_warnings_2026-09-25.json")!!.readText()

    @Test
    fun `parses the feed with Malaysian local times`() {
        val warnings = MetWarningClient.parse(body)
        assertEquals(5, warnings.size)
        val land = warnings[3]
        assertEquals("Thunderstorms Warning", land.titleEn)
        // 06:25 in Malaysia is 22:25 UTC the day before.
        assertEquals(Instant.parse("2026-09-24T22:25:00Z"), land.issued)
        assertEquals(Instant.parse("2026-09-25T01:00:00Z"), land.validTo)
        assertNull(warnings[4].validTo)
    }

    @Test(expected = IOException::class)
    fun `an html error page is an IOException`() = runTest {
        MetWarningClient { "<html>502</html>" }.download()
    }

    @Test(expected = IOException::class)
    fun `a bad timestamp is an IOException`() = runTest {
        MetWarningClient { body.replace("2026-09-25T06:25:00", "soon") }.download()
    }
}
