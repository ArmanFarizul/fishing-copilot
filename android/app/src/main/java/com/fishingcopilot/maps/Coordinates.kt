package com.fishingcopilot.maps

import java.util.Locale
import kotlin.math.abs

data class LatLon(val latitude: Double, val longitude: Double)

/**
 * Reads what anglers copy from a GPS, fish finder or chat: decimal ("5.35, 103.17"), degrees and
 * minutes ("5°21.0'N 103°10.2'E") or degrees, minutes and seconds. Hemisphere letters may be English
 * (N S E W) or Malay (U S T B). Returns null for anything it cannot read unambiguously.
 */
fun parseCoordinate(input: String): LatLon? {
    val text = input.trim().uppercase(Locale.ROOT)
    if (text.isEmpty()) return null
    val parts = splitPair(text) ?: return null
    val first = parsePart(parts.first) ?: return null
    val second = parsePart(parts.second) ?: return null

    // Longitude written first ("103°10'E 5°21'N") is swapped back.
    val (lat, lon) = when {
        first.axis == Axis.LON || second.axis == Axis.LAT -> second to first
        else -> first to second
    }
    if (lat.axis == Axis.LON || lon.axis == Axis.LAT) return null
    if (abs(lat.value) > 90 || abs(lon.value) > 180) return null
    return LatLon(lat.value, lon.value)
}

/** "5°21.000'N 103°10.200'E", the degrees-and-minutes form most marine GPS units show. */
fun formatDegreesMinutes(latitude: Double, longitude: Double): String =
    "${dm(latitude, 'N', 'S')} ${dm(longitude, 'E', 'W')}"

fun formatDecimal(latitude: Double, longitude: Double): String =
    String.format(Locale.ROOT, "%.5f, %.5f", latitude, longitude)

private fun dm(value: Double, positive: Char, negative: Char): String {
    val hemisphere = if (value < 0) negative else positive
    val totalMinutes = Math.round(abs(value) * 60_000) / 1000.0
    val degrees = (totalMinutes / 60).toInt()
    val minutes = totalMinutes - degrees * 60
    return String.format(Locale.ROOT, "%d°%06.3f'%c", degrees, minutes, hemisphere)
}

private enum class Axis { LAT, LON, UNKNOWN }

private data class Part(val value: Double, val axis: Axis)

private val HEMISPHERE = Regex("[NSEWUTB]")
private val NUMBER = Regex("""\d+(?:\.\d+)?""")

/** A comma separates the two halves; otherwise a hemisphere letter does; otherwise exactly two numbers. */
private fun splitPair(text: String): Pair<String, String>? {
    val commas = text.split(',', ';').map { it.trim() }.filter { it.isNotEmpty() }
    if (commas.size == 2) return commas[0] to commas[1]
    if (commas.size > 2) return null

    HEMISPHERE.find(text)?.let { letter ->
        // "N 5°21' E 103°10'" puts the letter first; "5°21'N 103°10'E" puts it last.
        val leading = text.substring(0, letter.range.first).isBlank()
        val next = if (leading) HEMISPHERE.find(text, letter.range.last + 1) else null
        val cut = if (leading) next?.range?.first ?: return null else letter.range.last + 1
        val first = text.substring(0, cut).trim()
        val second = text.substring(cut).trim()
        return if (first.isNotEmpty() && second.isNotEmpty()) first to second else null
    }

    val numbers = text.split(Regex("\\s+"))
    return if (numbers.size == 2) numbers[0] to numbers[1] else null
}

private fun parsePart(part: String): Part? {
    val letters = HEMISPHERE.findAll(part).map { it.value }.toList()
    if (letters.size > 1) return null
    val letter = letters.firstOrNull()
    // Anything left besides numbers, the letter, a sign and degree/minute/second marks is not a coordinate.
    if (part.replace(HEMISPHERE, "").replace(NUMBER, "").any { it !in " -+°º'′’\"″”" }) return null

    val numbers = NUMBER.findAll(part).map { it.value.toDouble() }.toList()
    if (numbers.isEmpty() || numbers.size > 3) return null
    val degrees = numbers[0]
    val minutes = numbers.getOrElse(1) { 0.0 }
    val seconds = numbers.getOrElse(2) { 0.0 }
    if (minutes >= 60 || seconds >= 60) return null
    // Only the last number may have decimals: "5.5°30'" is not a real coordinate.
    if (numbers.dropLast(1).any { it % 1.0 != 0.0 }) return null

    val magnitude = degrees + minutes / 60 + seconds / 3600
    val negative = part.trimStart().startsWith("-") || letter == "S" || letter == "W" || letter == "B"
    val axis = when (letter) {
        "N", "S", "U" -> Axis.LAT
        "E", "W", "T", "B" -> Axis.LON
        else -> Axis.UNKNOWN
    }
    return Part(if (negative) -magnitude else magnitude, axis)
}
