package com.fishingcopilot.data.hijri

import com.fishingcopilot.astro.HijriDate
import com.fishingcopilot.astro.MabimsHijri
import com.fishingcopilot.data.local.HijriDao
import com.fishingcopilot.data.local.HijriDayEntity
import com.fishingcopilot.data.remote.JakimTakwimClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

data class HijriEntry(val date: LocalDate, val hijri: HijriDate, val official: Boolean)

/** Computes a Hijri date when JAKIM's takwim does not cover the day yet. Can still differ from the official one. */
fun interface HijriEstimator {
    fun estimate(date: LocalDate): HijriDate
}

class HijriRepository(
    private val dao: HijriDao,
    private val client: JakimTakwimClient,
    // MABIMS matched all of JAKIM's 2026 month starts; Umm al-Qura differed on 219 of 365 days.
    private val estimator: HijriEstimator = HijriEstimator(MabimsHijri::date)
) {
    fun calendar(from: LocalDate, days: Int): Flow<List<HijriEntry>> {
        val to = from.plusDays(days - 1L)
        return dao.range(from.toString(), to.toString()).map { rows ->
            val official = rows.associateBy { LocalDate.parse(it.date) }
            (0 until days).map { i ->
                val date = from.plusDays(i.toLong())
                official[date]?.let { HijriEntry(date, HijriDate(it.hijriYear, it.hijriMonth, it.hijriDay), official = true) }
                    ?: HijriEntry(date, estimator.estimate(date), official = false)
            }
        }
    }

    /** Downloads JAKIM's current-year takwim unless it is already stored in full. Returns whether it downloaded. */
    suspend fun refreshIfNeeded(today: LocalDate): Boolean {
        if (dao.countInYear("${today.year}-") >= today.lengthOfYear()) return false
        val days = client.currentYear()
        dao.upsertAll(days.map { (date, h) -> HijriDayEntity(date.toString(), h.year, h.month, h.day) })
        return true
    }
}
