package com.fishingcopilot.data.remote

import com.fishingcopilot.astro.HijriDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

/**
 * Official Malaysian Hijri dates from JAKIM's e-solat takwim. The Hijri date is the same for every
 * zone (checked across JHR01, SBH07 and PLS01 for 2026), so one zone is enough. JAKIM publishes the
 * current Gregorian year only.
 */
class JakimTakwimClient(
    private val get: suspend (url: String) -> String = ::httpGet
) {
    suspend fun currentYear(): Map<LocalDate, HijriDate> {
        val response = json.decodeFromString<TakwimResponse>(get("$BASE_URL&period=year&zone=$ZONE"))
        if (!response.status.startsWith("OK")) throw IOException("JAKIM e-solat: ${response.status}")
        return response.prayerTime.associate { day -> parseDate(day.date) to parseHijri(day.hijri) }
    }

    private companion object {
        const val BASE_URL = "https://www.e-solat.gov.my/index.php?r=esolatApi/takwimsolat"
        const val ZONE = "WLY01"
        val json = Json { ignoreUnknownKeys = true }

        // e-solat writes Gregorian months with Malay abbreviations, e.g. "21-Mac-2026", "14-Ogos-2026".
        val MONTHS = listOf("jan", "feb", "mac", "apr", "mei", "jun", "jul", "ogos", "sep", "okt", "nov", "dis")

        fun parseDate(text: String): LocalDate {
            val (day, month, year) = text.split("-")
            val index = MONTHS.indexOf(month.lowercase())
            if (index < 0) throw IOException("Unknown month in e-solat date: $text")
            return LocalDate.of(year.toInt(), index + 1, day.toInt())
        }

        fun parseHijri(text: String): HijriDate {
            val (year, month, day) = text.split("-").map { it.toInt() }
            return HijriDate(year, month, day)
        }

        suspend fun httpGet(url: String): String = withContext(Dispatchers.IO) {
            val connection = URL(url).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 15_000
                connection.readTimeout = 60_000
                if (connection.responseCode >= 400) throw IOException("HTTP ${connection.responseCode} from e-solat")
                connection.inputStream.bufferedReader().use { it.readText() }
            } finally {
                connection.disconnect()
            }
        }
    }
}

@Serializable
private data class TakwimResponse(val status: String = "", val prayerTime: List<TakwimDay> = emptyList())

@Serializable
private data class TakwimDay(val hijri: String, val date: String)
