package com.fishingcopilot.data.remote

import com.fishingcopilot.warnings.MetWarning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.DateTimeException
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * MetMalaysia warnings through data.gov.my (docs/sumber-data.md section 5). No key needed.
 * Field list: https://developer.data.gov.my/realtime-api/weather
 */
class MetWarningClient(
    private val get: suspend (url: String) -> String = ::warningGet
) {
    /** Returns the raw body only after it parses, so a broken download never replaces a good cache. */
    suspend fun download(): String = get(URL_WARNINGS).also { body ->
        try {
            parse(body)
        } catch (e: IllegalArgumentException) {
            throw IOException("Unreadable warning list", e)
        } catch (e: DateTimeException) {
            throw IOException("Unreadable warning list", e)
        }
    }

    companion object {
        // Without the trailing slash the server answers with a 301.
        const val URL_WARNINGS = "https://api.data.gov.my/weather/warning/"
        private val MALAYSIA = ZoneId.of("Asia/Kuala_Lumpur")
        private val json = Json { ignoreUnknownKeys = true }

        fun parse(body: String): List<MetWarning> = json.decodeFromString<List<WarningJson>>(body).map { w ->
            MetWarning(
                issued = w.issue.issued.toInstant(),
                validFrom = w.validFrom?.toInstant(),
                validTo = w.validTo?.toInstant(),
                titleEn = w.issue.titleEn,
                titleBm = w.issue.titleBm,
                headingEn = w.headingEn.orEmpty(),
                headingBm = w.headingBm.orEmpty(),
                textEn = w.textEn.orEmpty(),
                textBm = w.textBm.orEmpty(),
                instructionEn = w.instructionEn,
                instructionBm = w.instructionBm
            )
        }

        // Timestamps carry no offset; they are Malaysian local time.
        private fun String.toInstant() = LocalDateTime.parse(this).atZone(MALAYSIA).toInstant()
    }
}

@Serializable
private data class WarningJson(
    @SerialName("warning_issue") val issue: IssueJson,
    @SerialName("valid_from") val validFrom: String? = null,
    @SerialName("valid_to") val validTo: String? = null,
    @SerialName("heading_en") val headingEn: String? = null,
    @SerialName("heading_bm") val headingBm: String? = null,
    @SerialName("text_en") val textEn: String? = null,
    @SerialName("text_bm") val textBm: String? = null,
    @SerialName("instruction_en") val instructionEn: String? = null,
    @SerialName("instruction_bm") val instructionBm: String? = null
)

@Serializable
private data class IssueJson(
    val issued: String,
    @SerialName("title_en") val titleEn: String,
    @SerialName("title_bm") val titleBm: String
)

private suspend fun warningGet(url: String): String = withContext(Dispatchers.IO) {
    val connection = URL(url).openConnection() as HttpURLConnection
    try {
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        if (connection.responseCode >= 400) throw IOException("HTTP ${connection.responseCode} from data.gov.my")
        connection.inputStream.bufferedReader().use { it.readText() }
    } finally {
        connection.disconnect()
    }
}
