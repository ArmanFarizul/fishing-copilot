package com.fishingcopilot.catchlog

import com.fishingcopilot.astro.HijriDate
import com.fishingcopilot.astro.MoonPhaseName
import com.fishingcopilot.data.local.CatchLogEntity

/**
 * Conditions recorded automatically when the angler taps Strike, taken from what the home cards
 * already show. Anything not available yet stays null rather than being guessed.
 */
data class CatchConditions(
    val timestamp: Long,
    val spotId: Long?,
    val spotName: String?,
    /** Metres relative to the tide model's mean level. */
    val waterLevel: Double?,
    val rising: Boolean?,
    val moonPhase: MoonPhaseName?,
    val hijri: HijriDate?,
    val biteScore: Double?
) {
    companion object {
        /** The conditions a saved catch was logged with, for showing them again when it is edited. */
        fun of(log: CatchLogEntity, spotName: String?) = CatchConditions(
            timestamp = log.timestamp,
            spotId = log.spotId,
            spotName = spotName,
            waterLevel = log.waterLevel,
            rising = log.tideState?.let { it == "RISING" },
            moonPhase = MoonPhaseName.entries.firstOrNull { it.name == log.moonPhase },
            hijri = log.hijriDate?.split("-")?.mapNotNull { it.toIntOrNull() }?.takeIf { it.size == 3 }
                ?.let { (y, m, d) -> HijriDate(y, m, d) },
            biteScore = log.biteScore
        )
    }

    fun toEntity(
        species: String,
        bait: String?,
        weightKg: Double?,
        lengthCm: Double?,
        photoUri: String?,
        notes: String?
    ) = CatchLogEntity(
        spotId = spotId,
        species = species.trim(),
        weightKg = weightKg,
        lengthCm = lengthCm,
        baitUsed = bait?.trim()?.takeIf { it.isNotEmpty() },
        waterLevel = waterLevel,
        tideState = rising?.let { if (it) "RISING" else "FALLING" },
        moonPhase = moonPhase?.name,
        biteScore = biteScore,
        photoUri = photoUri,
        timestamp = timestamp,
        hijriDate = hijri?.let { "%04d-%02d-%02d".format(it.year, it.month, it.day) },
        notes = notes?.trim()?.takeIf { it.isNotEmpty() }
    )
}

/** Personal patterns from the log: the spec's "which bait works" analysis, for one angler. */
data class LogSummary(
    val count: Int,
    val topBait: String?,
    /** Share of catches with a recorded bait that used [topBait]. */
    val topBaitPercent: Int,
    /** True if most catches came on a rising tide, false if falling, null on a tie or no data. */
    val bestTideRising: Boolean?,
    val topSpecies: String?,
    val topSpeciesCount: Int
) {
    companion object {
        fun of(logs: List<CatchLogEntity>): LogSummary? {
            if (logs.isEmpty()) return null

            // Same ranking as the Bait Tracker card, so the two never name different baits.
            val topBait = baitStats(logs).firstOrNull()

            val rising = logs.count { it.tideState == "RISING" }
            val falling = logs.count { it.tideState == "FALLING" }

            val species = logs.groupingBy { it.species }.eachCount().maxByOrNull { it.value }

            return LogSummary(
                count = logs.size,
                topBait = topBait?.bait,
                topBaitPercent = topBait?.sharePercent ?: 0,
                bestTideRising = when {
                    rising > falling -> true
                    falling > rising -> false
                    else -> null
                },
                topSpecies = species?.key,
                topSpeciesCount = species?.value ?: 0
            )
        }
    }
}

/**
 * The same catch with the angler's own details changed. The conditions recorded at the strike stay
 * as they were, because they describe that moment.
 */
fun CatchLogEntity.edited(
    species: String,
    bait: String?,
    weightKg: Double?,
    lengthCm: Double?,
    photoUri: String?,
    notes: String?
) = copy(
    species = species.trim(),
    baitUsed = bait?.trim()?.takeIf { it.isNotEmpty() },
    weightKg = weightKg,
    lengthCm = lengthCm,
    photoUri = photoUri,
    notes = notes?.trim()?.takeIf { it.isNotEmpty() }
)
