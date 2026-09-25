package com.fishingcopilot.catchlog

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** Which catches the log shows. Calendar years and dates are in the phone's time zone. */
sealed interface LogPeriod {
    data object ThisYear : LogPeriod
    data object LastYear : LogPeriod
    data object All : LogPeriod
    data class Year(val year: Int) : LogPeriod
    /** Both days included. */
    data class Range(val from: LocalDate, val to: LocalDate) : LogPeriod

    /** Epoch millis [start, end) for the query, or null for every catch. */
    fun bounds(today: LocalDate, zone: ZoneId): LongRange? {
        fun days(first: LocalDate, afterLast: LocalDate) =
            first.atStartOfDay(zone).toInstant().toEpochMilli() until afterLast.atStartOfDay(zone).toInstant().toEpochMilli()
        fun year(y: Int) = days(LocalDate.of(y, 1, 1), LocalDate.of(y + 1, 1, 1))
        return when (this) {
            ThisYear -> year(today.year)
            LastYear -> year(today.year - 1)
            All -> null
            is Year -> year(year)
            is Range -> days(minOf(from, to), maxOf(from, to).plusDays(1))
        }
    }
}

data class MonthGroup<T>(val month: YearMonth, val items: List<T>)

/** Consecutive runs of the same month, keeping the newest-first order the list already has. */
fun <T> groupByMonth(items: List<T>, zone: ZoneId, timestamp: (T) -> Long): List<MonthGroup<T>> {
    val groups = mutableListOf<MonthGroup<T>>()
    var current: YearMonth? = null
    var run = mutableListOf<T>()
    for (item in items) {
        val month = YearMonth.from(Instant.ofEpochMilli(timestamp(item)).atZone(zone))
        if (month != current && run.isNotEmpty()) {
            groups += MonthGroup(current!!, run)
            run = mutableListOf()
        }
        current = month
        run += item
    }
    if (run.isNotEmpty()) groups += MonthGroup(current!!, run)
    return groups
}
