package com.fishingcopilot.warnings

import com.fishingcopilot.data.spots.CoastalArea
import com.fishingcopilot.data.spots.MalaysianState
import java.time.Instant

/** One record from the data.gov.my warning feed, as published. */
data class MetWarning(
    val issued: Instant,
    val validFrom: Instant?,
    val validTo: Instant?,
    val titleEn: String,
    val titleBm: String,
    val headingEn: String,
    val headingBm: String,
    val textEn: String,
    val textBm: String,
    val instructionEn: String?,
    val instructionBm: String?
)

/** A single warning to show, with MetMalaysia's own wording in both languages. */
data class ShownWarning(
    val headingEn: String,
    val headingBm: String,
    val textEn: String,
    val textBm: String,
    val instructionEn: String?,
    val instructionBm: String?,
    val issued: Instant,
    val validTo: Instant?
)

data class WarningSummary(val forSpot: List<ShownWarning>, val elsewhere: List<ShownWarning>)

/**
 * Splits the feed into single warnings and sorts them by whether they name the spot's waters or state.
 * A warning whose areas cannot be read counts as the spot's, so nothing uncertain gets tucked away.
 */
fun summarizeWarnings(warnings: List<MetWarning>, latitude: Double, longitude: Double, now: Instant): WarningSummary {
    val names = areaNamesFor(latitude, longitude)
    val shipping = shippingAreasFor(latitude, longitude)
    val shown = warnings
        .filter { !it.titleEn.equals(NO_ADVISORY, ignoreCase = true) }
        // The same bulletin is listed once per validity window; any live window keeps it.
        .groupBy { it.textEn }
        .values
        .filter { copies -> copies.any { it.validTo == null || it.validTo.isAfter(now) } }
        .map { copies -> copies.maxBy { it.issued }.copy(validTo = copies.mapNotNull { it.validTo }.maxOrNull()) }
        .sortedByDescending { it.issued }
        .flatMap(::splitWarning)
        .distinctBy { it.first.textEn }

    // Shipping areas come as "Southern part of Condore" or "Northern Straits Of Melaka", so match by containment.
    val (forSpot, elsewhere) = shown.partition { (_, areas) ->
        areas.isEmpty() || areas.any { area -> area in names || shipping.any { it in area } }
    }
    return WarningSummary(forSpot.map { it.first }, elsewhere.map { it.first })
}

private const val NO_ADVISORY = "No Advisory"

/** Area names MetMalaysia uses for the waters and state around a spot, lower case. */
internal fun areaNamesFor(latitude: Double, longitude: Double): Set<String> =
    when (CoastalArea.nearest(latitude, longitude).first.state) {
        MalaysianState.PERLIS -> setOf("perlis")
        MalaysianState.KEDAH -> setOf("kedah")
        MalaysianState.PULAU_PINANG -> setOf("pulau pinang", "penang")
        MalaysianState.PERAK -> setOf("perak")
        MalaysianState.SELANGOR -> setOf("selangor")
        MalaysianState.NEGERI_SEMBILAN -> setOf("n. sembilan", "negeri sembilan")
        MalaysianState.MELAKA -> setOf("melaka")
        // Johor's waters are split at the Straits: Kukup (103.44 E) is west, Desaru (104.28 E) east.
        MalaysianState.JOHOR -> setOf("johor", if (longitude < JOHOR_SPLIT_LON) "west johor" else "east johor")
        MalaysianState.PAHANG -> setOf("pahang")
        MalaysianState.TERENGGANU -> setOf("terengganu")
        MalaysianState.KELANTAN -> setOf("kelantan")
        MalaysianState.SARAWAK -> setOf("sarawak")
        // The Malay bulletin reads "Sabah Barat dan Labuan" where the English says "Western Sabah".
        MalaysianState.LABUAN -> setOf("labuan", "western sabah")
        MalaysianState.SABAH -> setOf("sabah", if (longitude < SABAH_SPLIT_LON) "western sabah" else "eastern sabah")
    }

/**
 * MetMalaysia shipping forecast areas whose waters reach a spot. MetMalaysia publishes no boundaries for
 * them (checked 2026-09-25), so this leans towards showing a warning when an area may be near.
 * See docs/sumber-data.md section 5.
 */
internal fun shippingAreasFor(latitude: Double, longitude: Double): Set<String> =
    when (CoastalArea.nearest(latitude, longitude).first.state) {
        MalaysianState.PERLIS, MalaysianState.KEDAH, MalaysianState.PULAU_PINANG -> setOf("phuket", NORTH_STRAITS)
        MalaysianState.PERAK -> setOf(NORTH_STRAITS)
        // Where the two Straits areas meet is not published, so both apply here.
        MalaysianState.SELANGOR, MalaysianState.NEGERI_SEMBILAN -> setOf(NORTH_STRAITS, SOUTH_STRAITS)
        MalaysianState.MELAKA -> setOf(SOUTH_STRAITS)
        MalaysianState.JOHOR -> if (longitude < JOHOR_SPLIT_LON) setOf(SOUTH_STRAITS) else setOf("tioman")
        MalaysianState.PAHANG -> setOf("tioman")
        MalaysianState.TERENGGANU, MalaysianState.KELANTAN -> setOf("samui")
        MalaysianState.SARAWAK -> if (longitude < SARAWAK_SPLIT_LON) setOf("bunguran") else setOf("labuan", "reef south")
        MalaysianState.LABUAN -> setOf("labuan")
        MalaysianState.SABAH -> when {
            longitude < SABAH_SPLIT_LON -> setOf("labuan")
            latitude < SEMPORNA_SPLIT_LAT -> setOf("sulu", "sulawesi")
            else -> setOf("sulu")
        }
    }

private const val NORTH_STRAITS = "northern straits of melaka"
private const val SOUTH_STRAITS = "southern straits of melaka"
private const val SARAWAK_SPLIT_LON = 112.0
private const val SEMPORNA_SPLIT_LAT = 5.0

private const val JOHOR_SPLIT_LON = 103.6
private const val SABAH_SPLIT_LON = 117.2

private val SECTION = Regex("""(?:SECTION|SEKSYEN) [A-Z]:""")
private val ITEM = Regex("""(?:^|\s)\d\)\s""")
private val HEADING = Regex("""^([A-Z][A-Z0-9 ()\-/]*?)\s(?=[A-Z][a-z])""")
// A full stop after a capital letter is an abbreviation ("N. Sembilan"), not the end of the sentence.
private val AREA_LIST = Regex("""(?:waters|states) of (.+?)(?= until | within | from |(?<![A-Z])\.(?:\s|$)|$)""")

/** A bulletin such as "SECTION A ... 1) THUNDERSTORMS WARNING ..." becomes one entry per numbered warning. */
private fun splitWarning(warning: MetWarning): List<Pair<ShownWarning, List<String>>> {
    val en = items(warning.textEn)
    val bm = items(warning.textBm).takeIf { it.size == en.size }
    val single = en.size == 1
    return en.mapIndexed { i, itemEn ->
        val itemBm = bm?.get(i) ?: itemEn
        val headingEn = if (single) null else HEADING.find(itemEn)?.groupValues?.get(1)
        val headingBm = if (single) null else HEADING.find(itemBm)?.groupValues?.get(1)
        ShownWarning(
            headingEn = headingEn ?: warning.headingEn,
            headingBm = headingBm ?: warning.headingBm.ifEmpty { warning.headingEn },
            textEn = headingEn?.let { itemEn.removePrefix(it).trim() } ?: itemEn,
            textBm = headingBm?.let { itemBm.removePrefix(it).trim() } ?: itemBm,
            // A bulletin-wide instruction may not fit every warning inside it.
            instructionEn = warning.instructionEn.takeIf { single },
            instructionBm = warning.instructionBm.takeIf { single },
            issued = warning.issued,
            validTo = warning.validTo
        ) to areas(itemEn)
    }
}

private fun items(text: String): List<String> {
    val sections = SECTION.findAll(text).toList()
    val bodies = if (sections.isEmpty()) listOf(text) else sections.mapIndexed { i, match ->
        text.substring(match.range.last + 1, sections.getOrNull(i + 1)?.range?.first ?: text.length)
    }
    return bodies.flatMap { body ->
        val parts = body.split(ITEM)
        // The text before "1)" is the section title, such as "FOR SHIPPING".
        if (parts.size == 1) listOf(body.trim()) else parts.drop(1).map { it.trim() }
    }.filter { it.isNotEmpty() }
}

private fun areas(itemEn: String): List<String> =
    AREA_LIST.findAll(itemEn).flatMap { it.groupValues[1].split("•") }
        .map { it.substringBefore("(").substringBefore(":").trim().lowercase() }
        .filter { it.isNotEmpty() }
        .toList()
