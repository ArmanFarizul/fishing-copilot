package com.fishingcopilot.data.spots

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

enum class MalaysianState {
    PERLIS, KEDAH, PULAU_PINANG, PERAK, SELANGOR, NEGERI_SEMBILAN, MELAKA, JOHOR,
    PAHANG, TERENGGANU, KELANTAN, SARAWAK, LABUAN, SABAH
}

/**
 * Preset coastal areas for picking a spot without a map. Each point sits in the sea just off the
 * named area and was checked on 2026-09-24 to return hourly sea level from Open-Meteo Marine
 * (inland points return none). Tanjung Piai is left out: it shares Kukup's Open-Meteo grid cell.
 */
/**
 * [prayerZone] is the JAKIM e-Solat zone of the town the area is named after (looked up 2026-09-25), used
 * when the angler is offshore where no zone covers the water.
 */
enum class CoastalArea(val latitude: Double, val longitude: Double, val state: MalaysianState, val prayerZone: String) {
    KUALA_PERLIS(6.39, 100.10, MalaysianState.PERLIS, "PLS01"),
    KUALA_KEDAH(6.10, 100.26, MalaysianState.KEDAH, "KDH01"),
    PULAU_PINANG(5.42, 100.36, MalaysianState.PULAU_PINANG, "PNG01"),
    LUMUT_PANGKOR(4.22, 100.58, MalaysianState.PERAK, "PRK05"),
    KUALA_SELANGOR(3.34, 101.22, MalaysianState.SELANGOR, "SGR02"),
    PELABUHAN_KLANG(2.98, 101.30, MalaysianState.SELANGOR, "SGR03"),
    PORT_DICKSON(2.50, 101.78, MalaysianState.NEGERI_SEMBILAN, "NGS03"),
    MELAKA(2.17, 102.22, MalaysianState.MELAKA, "MLK01"),
    MUAR(2.03, 102.52, MalaysianState.JOHOR, "JHR04"),
    KUKUP(1.325, 103.44, MalaysianState.JOHOR, "JHR03"),
    DESARU(1.55, 104.28, MalaysianState.JOHOR, "JHR02"),
    MERSING(2.45, 103.88, MalaysianState.JOHOR, "JHR02"),
    PULAU_TIOMAN(2.80, 104.10, MalaysianState.PAHANG, "PHG01"),
    KUANTAN(3.80, 103.38, MalaysianState.PAHANG, "PHG02"),
    KEMAMAN(4.24, 103.47, MalaysianState.TERENGGANU, "TRG04"),
    KUALA_TERENGGANU(5.35, 103.17, MalaysianState.TERENGGANU, "TRG01"),
    TOK_BALI(5.90, 102.50, MalaysianState.KELANTAN, "KTN01"),
    SANTUBONG(1.73, 110.33, MalaysianState.SARAWAK, "SWK08"),
    MIRI(4.42, 113.95, MalaysianState.SARAWAK, "SWK02"),
    LABUAN(5.27, 115.22, MalaysianState.LABUAN, "WLY02"),
    KOTA_KINABALU(5.99, 116.03, MalaysianState.SABAH, "SBH07"),
    KUDAT(6.90, 116.85, MalaysianState.SABAH, "SBH05"),
    SANDAKAN(5.83, 118.13, MalaysianState.SABAH, "SBH01"),
    SEMPORNA(4.47, 118.63, MalaysianState.SABAH, "SBH03");

    companion object {
        fun nearest(latitude: Double, longitude: Double): Pair<CoastalArea, Double> =
            entries
                .map { it to haversineKm(latitude, longitude, it.latitude, it.longitude) }
                .minBy { it.second }
    }
}

/** Great-circle distance on a spherical Earth (mean radius 6371.0088 km). */
fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    return 2 * 6371.0088 * asin(sqrt(a))
}

/** Initial compass bearing from the first point to the second, in degrees clockwise from north. */
fun bearingDeg(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val phi1 = Math.toRadians(lat1)
    val phi2 = Math.toRadians(lat2)
    val dLon = Math.toRadians(lon2 - lon1)
    val y = sin(dLon) * cos(phi2)
    val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(dLon)
    return (Math.toDegrees(atan2(y, x)) + 360) % 360
}
