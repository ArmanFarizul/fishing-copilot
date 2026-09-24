package com.fishingcopilot.data.hijri

import com.fishingcopilot.astro.HijriDate
import com.fishingcopilot.data.local.HijriDao
import com.fishingcopilot.data.local.HijriDayEntity
import com.fishingcopilot.data.remote.JakimTakwimClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HijriRepositoryTest {
    private class FakeDao : HijriDao {
        val rows = MutableStateFlow<Map<String, HijriDayEntity>>(emptyMap())
        override fun range(from: String, to: String): Flow<List<HijriDayEntity>> =
            rows.map { all -> all.values.filter { it.date in from..to }.sortedBy { it.date } }
        override suspend fun countInYear(yearPrefix: String): Int = rows.value.keys.count { it.startsWith(yearPrefix) }
        override suspend fun upsertAll(days: List<HijriDayEntity>) { rows.value += days.associateBy { it.date } }
    }

    private val today = LocalDate.of(2026, 12, 20)
    private var requests = 0
    // JAKIM only publishes the current year: 2026-12-20 .. 2026-12-31 here, 1448-07-10 onwards.
    private val client = JakimTakwimClient {
        requests++
        val rows = (0..11).joinToString(",") { i ->
            val d = today.plusDays(i.toLong())
            """{"hijri":"1448-07-${"%02d".format(10 + i)}","date":"${d.dayOfMonth}-Dis-2026"}"""
        }
        """{"prayerTime":[$rows],"status":"OK!"}"""
    }
    private val estimator = HijriEstimator { date -> HijriDate(1448, 7, 10 + (date.toEpochDay() - today.toEpochDay()).toInt() + 1) }

    @Test
    fun `official dates win and later days fall back to a marked estimate`() = runTest {
        val repo = HijriRepository(FakeDao(), client, estimator)
        repo.refreshIfNeeded(today)

        val days = repo.calendar(today, 15).first()

        assertEquals(15, days.size)
        assertEquals(HijriEntry(today, HijriDate(1448, 7, 10), official = true), days[0])
        assertTrue(days.take(12).all { it.official })
        assertEquals(HijriEntry(LocalDate.of(2027, 1, 1), HijriDate(1448, 7, 23), official = false), days[12])
        assertFalse(days.drop(12).any { it.official })
    }

    @Test
    fun `refresh downloads only when this year is missing`() = runTest {
        val dao = FakeDao()
        val repo = HijriRepository(dao, client, estimator)
        assertTrue(repo.refreshIfNeeded(today))
        // The fake serves 12 days; a real year has 365. A partial year counts as missing.
        assertTrue(repo.refreshIfNeeded(today))
        dao.upsertAll((1..365).map { HijriDayEntity(LocalDate.of(2026, 1, 1).plusDays(it - 1L).toString(), 1447, 1, 1) })
        assertFalse(repo.refreshIfNeeded(today))
        assertEquals(2, requests)
    }
}
