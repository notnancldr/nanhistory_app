package id.my.nanclouder.nanhistory.ui.map.maplibre

/**
 * Configuration and endpoints for MapLibre vector map styles and offline fallbacks.
 *
 * Primary provider: OpenFreeMap (vector tiles derived from OpenStreetMap data, no API key required).
 * Styles:
 * - Positron: Muted light style providing high contrast for colored trip corridors.
 * - Dark: Muted dark style tailored for dark theme contrast.
 *
 * Fallback: Self-contained background-only style JSONs bundled in assets so the map never crashes
 * or goes completely blank when offline or when remote vector styles fail to load.
 */
object MapStyleConfig {
    /** Primary vector style URL for light theme (OpenFreeMap Positron). */
    const val STYLE_POSITRON_URI: String = "https://tiles.openfreemap.org/styles/positron"

    /** Primary vector style URL for dark theme (OpenFreeMap Dark). */
    const val STYLE_DARK_URI: String = "https://tiles.openfreemap.org/styles/dark"

    /** Asset path to bundled fallback light style JSON. */
    const val ASSET_FALLBACK_STYLE_LIGHT: String = "styles/fallback_style.json"

    /** Asset path to bundled fallback dark style JSON. */
    const val ASSET_FALLBACK_STYLE_DARK: String = "styles/fallback_style_dark.json"

    /** Legal attribution text required by OpenStreetMap and OpenFreeMap. */
    const val ATTRIBUTION_TEXT: String = "© OpenStreetMap contributors, OpenFreeMap"

    /**
     * Bundled fallback JSON for light theme with a neutral light background (#E5E5E5).
     * Has zero network dependencies and zero symbol layers.
     */
    val FALLBACK_STYLE_LIGHT_JSON: String = """
        {
          "version": 8,
          "name": "NanHistory Fallback Offline Style",
          "metadata": {},
          "sources": {},
          "layers": [
            {
              "id": "background",
              "type": "background",
              "paint": {
                "background-color": "#E5E5E5"
              }
            }
          ]
        }
    """.trimIndent()

    /**
     * Bundled fallback JSON for dark theme with a neutral dark background (#1A1A1A).
     * Has zero network dependencies and zero symbol layers.
     */
    val FALLBACK_STYLE_DARK_JSON: String = """
        {
          "version": 8,
          "name": "NanHistory Fallback Offline Style Dark",
          "metadata": {},
          "sources": {},
          "layers": [
            {
              "id": "background",
              "type": "background",
              "paint": {
                "background-color": "#1A1A1A"
              }
            }
          ]
        }
    """.trimIndent()

    /**
     * Resolves the primary vector style URI based on system theme.
     */
    fun getStyleUri(isDark: Boolean): String = if (isDark) STYLE_DARK_URI else STYLE_POSITRON_URI

    /**
     * Resolves the fallback style JSON based on system theme.
     */
    fun getFallbackJson(isDark: Boolean): String =
        if (isDark) FALLBACK_STYLE_DARK_JSON else FALLBACK_STYLE_LIGHT_JSON
}
