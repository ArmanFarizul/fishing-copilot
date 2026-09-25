package com.fishingcopilot.maps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CoordinatesTest {
    private fun assertParses(input: String, lat: Double, lon: Double) {
        val parsed = parseCoordinate(input) ?: throw AssertionError("could not parse \"$input\"")
        assertEquals("latitude of \"$input\"", lat, parsed.latitude, 1e-6)
        assertEquals("longitude of \"$input\"", lon, parsed.longitude, 1e-6)
    }

    @Test
    fun `decimal pairs`() {
        assertParses("5.35, 103.17", 5.35, 103.17)
        assertParses("5.35 103.17", 5.35, 103.17)
        assertParses("  5.35,103.17  ", 5.35, 103.17)
        assertParses("-1.5, -80.25", -1.5, -80.25)
        assertParses("5.35N 103.17E", 5.35, 103.17)
    }

    @Test
    fun `degrees and minutes as fish finders show them`() {
        assertParses("5°21.000'N 103°10.200'E", 5.35, 103.17)
        assertParses("N 5°21.000' E 103°10.200'", 5.35, 103.17)
        assertParses("5 21.0 N, 103 10.2 E", 5.35, 103.17)
    }

    @Test
    fun `degrees minutes and seconds`() {
        assertParses("5°21'00\"N 103°10'12\"E", 5.35, 103.17)
        assertParses("1°19'30\"N 103°26'24\"E", 1.325, 103.44)
    }

    @Test
    fun `Malay hemisphere letters`() {
        assertParses("5°21'U 103°10'T", 5.35, 103.1666667)
        assertParses("1.3 S, 100.5 B", -1.3, -100.5)
    }

    @Test
    fun `longitude written first is swapped`() {
        assertParses("103°10.2'E 5°21.0'N", 5.35, 103.17)
    }

    @Test
    fun `rejects what is not a coordinate`() {
        assertNull(parseCoordinate(""))
        assertNull(parseCoordinate("Pulau Redang"))
        assertNull(parseCoordinate("5.35"))
        assertNull(parseCoordinate("95, 103"))
        assertNull(parseCoordinate("5, 200"))
        assertNull(parseCoordinate("5°75'N 103°10'E"))
        assertNull(parseCoordinate("5N 6N"))
        assertNull(parseCoordinate("1, 2, 3"))
    }

    @Test
    fun `formats for a marine GPS and as decimals`() {
        assertEquals("5°21.000'N 103°10.200'E", formatDegreesMinutes(5.35, 103.17))
        assertEquals("1°19.500'S 80°15.000'W", formatDegreesMinutes(-1.325, -80.25))
        assertEquals("5.35000, 103.17000", formatDecimal(5.35, 103.17))
    }
}
