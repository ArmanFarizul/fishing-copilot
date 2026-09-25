package com.fishingcopilot.prayer

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

enum class Prayer { FAJR, SYURUK, DHUHR, ASR, MAGHRIB, ISHA }

data class PrayerDay(val date: LocalDate, val times: Map<Prayer, LocalTime>)

data class NextPrayer(val prayer: Prayer, val at: LocalDateTime)

/** Syuruk is sunrise, not a prayer, so it is shown but never counted as the next prayer. */
fun nextPrayer(days: List<PrayerDay>, now: LocalDateTime): NextPrayer? =
    days.sortedBy { it.date }.asSequence()
        .flatMap { day -> day.times.filterKeys { it != Prayer.SYURUK }.map { (p, t) -> NextPrayer(p, day.date.atTime(t)) }.sortedBy { it.at } }
        .firstOrNull { it.at.isAfter(now) }

/** Prayer times are Malaysian time whatever the phone's zone. */
val MALAYSIA: ZoneId = ZoneId.of("Asia/Kuala_Lumpur")
