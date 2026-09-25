package com.fishingcopilot.satellite

import com.fishingcopilot.data.spots.haversineKm
import java.time.LocalDate
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/** The pipeline's coarse grid (pipeline/README.md): row-major, south to north, west to east. */
data class SatelliteGrid(val lat0: Double, val lon0: Double, val step: Double, val nlat: Int, val nlon: Int) {
    fun centerLat(row: Int) = lat0 + (row + 0.5) * step
    fun centerLon(col: Int) = lon0 + (col + 0.5) * step
}

data class SatelliteLayer(val date: LocalDate, val values: List<Double?>)

data class SatelliteData(
    val grid: SatelliteGrid,
    val seaTemp: SatelliteLayer?,
    val front: SatelliteLayer?,
    val chlorophyll: SatelliteLayer?,
    val clarity: SatelliteLayer?
)

/** Spec: 0.2 to 1.5 mg/m³ is healthy plankton that draws baitfish. */
enum class PlanktonLevel {
    LOW, IDEAL, HIGH;

    companion object {
        fun of(mgPerM3: Double) = when {
            mgPerM3 < 0.2 -> LOW
            mgPerM3 <= 1.5 -> IDEAL
            else -> HIGH
        }
    }
}

/** Spec: 28 to 30 °C is the tropical optimum. */
enum class WaterTempLevel {
    COOL, IDEAL, WARM;

    companion object {
        fun of(celsius: Double) = when {
            celsius < 28.0 -> COOL
            celsius <= 30.0 -> IDEAL
            else -> WARM
        }
    }
}

/** Secchi depth bands; not in the spec, see docs/satelit.md. */
enum class ClarityLevel {
    MURKY, MODERATE, CLEAR;

    companion object {
        fun of(meters: Double) = when {
            meters < 5.0 -> MURKY
            meters < 15.0 -> MODERATE
            else -> CLEAR
        }
    }
}

/** Spec: a change of 1.0 °C or more is a front. The weaker band is ours, see docs/satelit.md. */
enum class FrontStrength {
    WEAK, STRONG;

    companion object {
        const val WEAK_MIN = 0.5
        const val STRONG_MIN = 1.0

        fun of(celsiusPer10Km: Double): FrontStrength? = when {
            celsiusPer10Km >= STRONG_MIN -> STRONG
            celsiusPer10Km >= WEAK_MIN -> WEAK
            else -> null
        }
    }
}

data class NearbyFront(val strength: FrontStrength, val celsiusPer10Km: Double, val distanceKm: Double, val bearingDeg: Double)

data class SatelliteSummary(
    val seaTempC: Double?,
    val chlorophyll: Double?,
    val clarityM: Double?,
    val front: NearbyFront?,
    /** The oldest layer date shown, so the card never claims fresher data than it has. */
    val dataDate: LocalDate
)

/** Nearest cell with data: a coastal spot's own cell is often mostly land. */
const val NEAREST_CELL_KM = 30.0
const val FRONT_SEARCH_KM = 50.0

fun summarizeSatellite(data: SatelliteData, latitude: Double, longitude: Double): SatelliteSummary? {
    val grid = data.grid
    val cells = cellsWithin(grid, latitude, longitude, maxOf(NEAREST_CELL_KM, FRONT_SEARCH_KM))

    fun nearestValue(layer: SatelliteLayer?): Double? = layer?.let {
        cells.asSequence().filter { c -> c.distanceKm <= NEAREST_CELL_KM }.firstNotNullOfOrNull { c -> it.values.getOrNull(c.index) }
    }

    val seaTemp = nearestValue(data.seaTemp)
    val chlorophyll = nearestValue(data.chlorophyll)
    val clarity = nearestValue(data.clarity)
    val front = data.front?.let { layer ->
        cells.asSequence()
            .filter { it.distanceKm <= FRONT_SEARCH_KM }
            .mapNotNull { c ->
                val value = layer.values.getOrNull(c.index) ?: return@mapNotNull null
                val strength = FrontStrength.of(value) ?: return@mapNotNull null
                NearbyFront(strength, value, c.distanceKm, c.bearingDeg)
            }
            .firstOrNull()
    }

    val shown = listOfNotNull(
        data.seaTemp?.takeIf { seaTemp != null },
        data.chlorophyll?.takeIf { chlorophyll != null },
        data.clarity?.takeIf { clarity != null }
    )
    if (shown.isEmpty()) return null
    return SatelliteSummary(seaTemp, chlorophyll, clarity, front, shown.minOf { it.date })
}

private data class Cell(val index: Int, val distanceKm: Double, val bearingDeg: Double)

/** Cells whose centre lies within [radiusKm], nearest first; the spot's own cell counts as distance 0. */
private fun cellsWithin(grid: SatelliteGrid, latitude: Double, longitude: Double, radiusKm: Double): List<Cell> {
    val ownRow = floor((latitude - grid.lat0) / grid.step).toInt()
    val ownCol = floor((longitude - grid.lon0) / grid.step).toInt()
    val reach = (radiusKm / (grid.step * 111.0)).toInt() + 2
    val cells = mutableListOf<Cell>()
    for (row in (ownRow - reach)..(ownRow + reach)) {
        if (row !in 0 until grid.nlat) continue
        for (col in (ownCol - reach)..(ownCol + reach)) {
            if (col !in 0 until grid.nlon) continue
            val own = row == ownRow && col == ownCol
            val lat = grid.centerLat(row)
            val lon = grid.centerLon(col)
            val distance = if (own) 0.0 else haversineKm(latitude, longitude, lat, lon)
            if (distance <= radiusKm) cells += Cell(row * grid.nlon + col, distance, bearing(latitude, longitude, lat, lon))
        }
    }
    return cells.sortedBy { it.distanceKm }
}

private fun bearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val phi1 = Math.toRadians(lat1)
    val phi2 = Math.toRadians(lat2)
    val dLon = Math.toRadians(lon2 - lon1)
    val y = sin(dLon) * cos(phi2)
    val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(dLon)
    return (Math.toDegrees(atan2(y, x)) + 360) % 360
}
