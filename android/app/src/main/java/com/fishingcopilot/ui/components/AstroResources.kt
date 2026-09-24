package com.fishingcopilot.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.fishingcopilot.R
import com.fishingcopilot.astro.HijriDate
import com.fishingcopilot.astro.MoonPhaseName

@get:StringRes
val MoonPhaseName.label: Int
    get() = when (this) {
        MoonPhaseName.NEW_MOON -> R.string.moon_new
        MoonPhaseName.WAXING_CRESCENT -> R.string.moon_waxing_crescent
        MoonPhaseName.FIRST_QUARTER -> R.string.moon_first_quarter
        MoonPhaseName.WAXING_GIBBOUS -> R.string.moon_waxing_gibbous
        MoonPhaseName.FULL_MOON -> R.string.moon_full
        MoonPhaseName.WANING_GIBBOUS -> R.string.moon_waning_gibbous
        MoonPhaseName.LAST_QUARTER -> R.string.moon_last_quarter
        MoonPhaseName.WANING_CRESCENT -> R.string.moon_waning_crescent
    }


private val HIJRI_MONTHS = listOf(
    R.string.hijri_month_1, R.string.hijri_month_2, R.string.hijri_month_3, R.string.hijri_month_4,
    R.string.hijri_month_5, R.string.hijri_month_6, R.string.hijri_month_7, R.string.hijri_month_8,
    R.string.hijri_month_9, R.string.hijri_month_10, R.string.hijri_month_11, R.string.hijri_month_12
)

/** "13 Rabiulakhir 1448H" in the user's language, with JAKIM's month spellings. */
@Composable
fun hijriDateText(date: HijriDate): String =
    stringResource(R.string.hijri_date, date.day, stringResource(HIJRI_MONTHS[date.month - 1]), date.year)
