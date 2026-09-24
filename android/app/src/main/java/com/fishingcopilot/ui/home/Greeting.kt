package com.fishingcopilot.ui.home

import androidx.annotation.StringRes
import com.fishingcopilot.R

// Follows Malaysian usage: "tengah hari" and "petang" are distinct periods, unlike English afternoon.
enum class DayPeriod(@StringRes val greeting: Int) {
    MORNING(R.string.greeting_morning),
    MIDDAY(R.string.greeting_midday),
    AFTERNOON(R.string.greeting_afternoon),
    NIGHT(R.string.greeting_night);

    companion object {
        fun fromHour(hour: Int): DayPeriod = when (hour) {
            in 5..11 -> MORNING
            in 12..13 -> MIDDAY
            in 14..18 -> AFTERNOON
            else -> NIGHT
        }
    }
}
