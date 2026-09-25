package com.fishingcopilot.catchlog

import com.fishingcopilot.data.local.CatchLogEntity

/** How one bait has done: catches on it, their share of catches with a bait noted, and fish size. */
data class BaitStat(
    val bait: String,
    val catches: Int,
    val sharePercent: Int,
    val averageKg: Double?,
    val biggestKg: Double?
)

/**
 * Baits ranked by catches, then by average weight. Baits are free text, so "Udang hidup" and
 * "udang hidup " count as one, shown with the most recent spelling (the log is newest first).
 */
fun baitStats(logs: List<CatchLogEntity>): List<BaitStat> {
    val baited = logs.mapNotNull { log -> log.baitUsed?.trim()?.takeIf { it.isNotEmpty() }?.let { it to log } }
    if (baited.isEmpty()) return emptyList()
    return baited.groupBy { it.first.lowercase() }.values.map { group ->
        val weights = group.mapNotNull { it.second.weightKg }
        BaitStat(
            bait = group.first().first,
            catches = group.size,
            sharePercent = group.size * 100 / baited.size,
            averageKg = weights.takeIf { it.isNotEmpty() }?.average(),
            biggestKg = weights.maxOrNull()
        )
    }.sortedWith(compareByDescending<BaitStat> { it.catches }.thenByDescending { it.averageKg ?: 0.0 })
}

/** Species in the log, most caught first, for the filter chips. */
fun speciesCounts(logs: List<CatchLogEntity>): List<Pair<String, Int>> =
    logs.groupingBy { it.species }.eachCount().entries
        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
        .map { it.key to it.value }
