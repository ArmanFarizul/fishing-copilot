package com.fishingcopilot.satellite

import java.time.LocalDate

enum class SatelliteMapLayer { PLANKTON, SEA_TEMP, FRONTS, CLARITY }

/** One grid square to colour, with the same level the satellite card would give it. */
data class MapCell(
    val south: Double,
    val west: Double,
    val north: Double,
    val east: Double,
    val value: Double,
    val level: Enum<*>
)

data class MapLayerCells(val date: LocalDate, val cells: List<MapCell>)

/**
 * Squares to draw for [layer]. Land and cloud have no value and are left out. For fronts only squares
 * that reach a front level are drawn, so the gentle sea in between stays clear.
 */
fun mapCells(data: SatelliteData, layer: SatelliteMapLayer): MapLayerCells? {
    val source = when (layer) {
        SatelliteMapLayer.PLANKTON -> data.chlorophyll
        SatelliteMapLayer.SEA_TEMP -> data.seaTemp
        SatelliteMapLayer.FRONTS -> data.front
        SatelliteMapLayer.CLARITY -> data.clarity
    } ?: return null
    val grid = data.grid
    val cells = source.values.mapIndexedNotNull { index, value ->
        value ?: return@mapIndexedNotNull null
        val level: Enum<*> = when (layer) {
            SatelliteMapLayer.PLANKTON -> PlanktonLevel.of(value)
            SatelliteMapLayer.SEA_TEMP -> WaterTempLevel.of(value)
            SatelliteMapLayer.FRONTS -> FrontStrength.of(value) ?: return@mapIndexedNotNull null
            SatelliteMapLayer.CLARITY -> ClarityLevel.of(value)
        }
        val row = index / grid.nlon
        val col = index % grid.nlon
        val south = grid.lat0 + row * grid.step
        val west = grid.lon0 + col * grid.step
        MapCell(south, west, south + grid.step, west + grid.step, value, level)
    }
    return MapLayerCells(source.date, cells)
}
