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

            // Baits are free text, so "Udang hidup" and "udang hidup " count as one; show the most recent spelling.
            val baited = logs.mapNotNull { log -> log.baitUsed?.trim()?.takeIf { it.isNotEmpty() }?.let { it.lowercase() to it } }
            val topBaitGroup = baited.groupBy({ it.first }, { it.second }).maxByOrNull { it.value.size }

            val rising = logs.count { it.tideState == "RISING" }
            val falling = logs.count { it.tideState == "FALLING" }

            val species = logs.groupingBy { it.species }.eachCount().maxByOrNull { it.value }

            return LogSummary(
                count = logs.size,
                topBait = topBaitGroup?.value?.first(),
                topBaitPercent = topBaitGroup?.let { it.value.size * 100 / baited.size } ?: 0,
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
