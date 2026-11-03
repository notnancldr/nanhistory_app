package id.my.nanclouder.nanhistory.ui.map.maplibre

import org.maplibre.compose.style.BaseStyle

/**
 * Provides [BaseStyle] instances for MapLibre Compose based on system theme and network availability.
 *
 * Vector base styles are powered by OpenFreeMap without hardcoded API keys.
 * If offline or style fails to load, a bundled zero-network fallback style is provided.
 */
object BaseMapStyle {

    /**
     * Obtains the appropriate [BaseStyle] given the current theme and offline fallback flag.
     *
     * @param isDark Whether dark theme is active.
     * @param isFallback When true, returns the bundled offline background style.
     */
    fun getBaseStyle(isDark: Boolean, isFallback: Boolean = false): BaseStyle {
        return if (isFallback) {
            BaseStyle.Json(MapStyleConfig.getFallbackJson(isDark))
        } else {
            BaseStyle.Uri(MapStyleConfig.getStyleUri(isDark))
        }
    }
}
