package com.fishingcopilot.maps

import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.rasterOpacity
import org.maplibre.android.style.layers.PropertyFactory.visibility
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.RasterSource
import org.maplibre.android.style.sources.TileSet

private const val DEPTH_SOURCE = "gebco-depth"
private const val DEPTH_LAYER = "gebco-depth-layer"
private const val SEAMARK_SOURCE = "openseamap"
private const val SEAMARK_LAYER = "openseamap-layer"

// GEBCO WMS in web mercator; MapLibre fills {bbox-epsg-3857} per tile. Colour-coded depth, not for navigation.
private const val DEPTH_URL = "https://wms.gebco.net/mapserv?SERVICE=WMS&VERSION=1.3.0&REQUEST=GetMap" +
    "&LAYERS=gebco_latest_2&STYLES=&CRS=EPSG:3857&BBOX={bbox-epsg-3857}&WIDTH=256&HEIGHT=256&FORMAT=image/png"
private const val SEAMARK_URL = "https://tiles.openseamap.org/seamark/{z}/{x}/{y}.png"

/**
 * Depth shading (GEBCO) and seamarks (OpenSeaMap: buoys, lights, beacons) over the base map, under its
 * labels and under the app's own pins and squares. Both are online tiles; MapLibre keeps what it has
 * shown in its cache.
 */
fun Style.setNautical(on: Boolean) {
    if (on && getSource(DEPTH_SOURCE) == null) {
        addSource(RasterSource(DEPTH_SOURCE, TileSet("tiles", DEPTH_URL).apply { minZoom = 0f; maxZoom = 12f }, 256))
        addSource(RasterSource(SEAMARK_SOURCE, TileSet("tiles", SEAMARK_URL).apply { minZoom = 9f; maxZoom = 18f }, 256))
        val firstLabel = layers.firstOrNull { it is SymbolLayer }?.id
        val depth = RasterLayer(DEPTH_LAYER, DEPTH_SOURCE).withProperties(rasterOpacity(0.55f))
        val seamarks = RasterLayer(SEAMARK_LAYER, SEAMARK_SOURCE)
        if (firstLabel != null) {
            addLayerBelow(depth, firstLabel)
            addLayerBelow(seamarks, firstLabel)
        } else {
            addLayer(depth)
            addLayer(seamarks)
        }
    }
    val shown = if (on) Property.VISIBLE else Property.NONE
    getLayer(DEPTH_LAYER)?.setProperties(visibility(shown))
    getLayer(SEAMARK_LAYER)?.setProperties(visibility(shown))
}
