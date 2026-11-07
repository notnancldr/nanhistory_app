package id.my.nanclouder.nanhistory.ui.map

/**
 * Utility functions for zoom conversion across map rendering engines.
 *
 * MapLibre uses a standard 512px tile coordinate system at integer zoom levels,
 * whereas OSMDroid uses 256px raster tiles. Consequently, MapLibre zoom level [z]
 * visually corresponds to OSMDroid zoom level [z + 1.0].
 */
object MapZoomUtils {

    /**
     * Converts a MapLibre zoom level to its equivalent OSMDroid (256px) zoom level.
     */
    fun osmEquivalentZoom(mapLibreZoom: Double): Double = mapLibreZoom + 1.0

    /**
     * Converts an OSMDroid zoom level to its equivalent MapLibre zoom level.
     */
    fun mapLibreEquivalentZoom(osmZoom: Double): Double = osmZoom - 1.0
}
