package com.fishingcopilot.maps

import com.fishingcopilot.data.local.SpotEntity

data class Bounds(val south: Double, val west: Double, val north: Double, val east: Double)

/** One offline region to keep: [key] ties it to the overview or to a saved spot. */
data class PlannedRegion(val key: String, val bounds: Bounds, val minZoom: Double, val maxZoom: Double)

const val OVERVIEW_KEY = "overview"

// The satellite pipeline's area, Straits of Melaka to eastern Sabah, at coastline detail.
private val OVERVIEW = Bounds(south = 0.5, west = 99.0, north = 7.5, east = 119.5)
private const val OVERVIEW_MAX_ZOOM = 10.0

// About 40 km each way around a spot, down to jetty and river-mouth detail.
private const val SPOT_HALF_SIZE_DEG = 0.35
private const val SPOT_MIN_ZOOM = 11.0
private const val SPOT_MAX_ZOOM = 14.0

fun spotRegionKey(spotId: Long) = "spot-$spotId"

/** The whole coverage at low zoom, plus a close-up around every saved spot. */
fun offlinePlan(spots: List<SpotEntity>): List<PlannedRegion> =
    listOf(PlannedRegion(OVERVIEW_KEY, OVERVIEW, 0.0, OVERVIEW_MAX_ZOOM)) + spots.map { spot ->
        PlannedRegion(
            key = spotRegionKey(spot.id),
            bounds = Bounds(
                south = spot.latitude - SPOT_HALF_SIZE_DEG,
                west = spot.longitude - SPOT_HALF_SIZE_DEG,
                north = spot.latitude + SPOT_HALF_SIZE_DEG,
                east = spot.longitude + SPOT_HALF_SIZE_DEG
            ),
            minZoom = SPOT_MIN_ZOOM,
            maxZoom = SPOT_MAX_ZOOM
        )
    }
