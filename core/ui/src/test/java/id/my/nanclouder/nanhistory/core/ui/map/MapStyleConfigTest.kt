package id.my.nanclouder.nanhistory.ui.map

import id.my.nanclouder.nanhistory.ui.map.maplibre.BaseMapStyle
import id.my.nanclouder.nanhistory.ui.map.maplibre.MapStyleConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.maplibre.compose.style.BaseStyle
import java.io.File

/**
 * Unit tests verifying vector base style selection, offline fallbacks, and JSON validity.
 */
class MapStyleConfigTest {

    @Test
    fun getStyleUri_returnsCorrectUriForTheme() {
        assertEquals(
            MapStyleConfig.STYLE_POSITRON_URI,
            MapStyleConfig.getStyleUri(isDark = false)
        )
        assertEquals(
            MapStyleConfig.STYLE_DARK_URI,
            MapStyleConfig.getStyleUri(isDark = true)
        )
    }

    @Test
    fun getBaseStyle_selectsUriOrFallbackJson() {
        val lightStyle = BaseMapStyle.getBaseStyle(isDark = false, isFallback = false)
        assertTrue(lightStyle is BaseStyle.Uri)
        assertEquals(MapStyleConfig.STYLE_POSITRON_URI, (lightStyle as BaseStyle.Uri).uri)

        val darkStyle = BaseMapStyle.getBaseStyle(isDark = true, isFallback = false)
        assertTrue(darkStyle is BaseStyle.Uri)
        assertEquals(MapStyleConfig.STYLE_DARK_URI, (darkStyle as BaseStyle.Uri).uri)

        val lightFallback = BaseMapStyle.getBaseStyle(isDark = false, isFallback = true)
        assertTrue(lightFallback is BaseStyle.Json)
        assertEquals(MapStyleConfig.FALLBACK_STYLE_LIGHT_JSON, (lightFallback as BaseStyle.Json).json)

        val darkFallback = BaseMapStyle.getBaseStyle(isDark = true, isFallback = true)
        assertTrue(darkFallback is BaseStyle.Json)
        assertEquals(MapStyleConfig.FALLBACK_STYLE_DARK_JSON, (darkFallback as BaseStyle.Json).json)
    }

    @Test
    fun fallbackStyles_areValidMapboxV8Json() {
        for (jsonString in listOf(MapStyleConfig.FALLBACK_STYLE_LIGHT_JSON, MapStyleConfig.FALLBACK_STYLE_DARK_JSON)) {
            val json = Json.parseToJsonElement(jsonString).jsonObject
            assertEquals("8", json["version"]?.jsonPrimitive?.content)
            assertNotNull(json["sources"])
            val layers = json["layers"]?.jsonArray
            assertNotNull(layers)
            assertTrue(layers!!.size > 0)
            val bgLayer = layers[0].jsonObject
            assertEquals("background", bgLayer["type"]?.jsonPrimitive?.content)
            val paint = bgLayer["paint"]?.jsonObject
            assertNotNull(paint)
            assertNotNull(paint!!["background-color"]?.jsonPrimitive?.content)
        }
    }

    @Test
    fun assetFallbackFiles_existAndParseValidly() {
        val lightFile = File("src/main/assets/${MapStyleConfig.ASSET_FALLBACK_STYLE_LIGHT}")
        val darkFile = File("src/main/assets/${MapStyleConfig.ASSET_FALLBACK_STYLE_DARK}")

        assertTrue("Light fallback asset should exist", lightFile.exists())
        assertTrue("Dark fallback asset should exist", darkFile.exists())

        val lightJson = Json.parseToJsonElement(lightFile.readText()).jsonObject
        assertEquals("8", lightJson["version"]?.jsonPrimitive?.content)

        val darkJson = Json.parseToJsonElement(darkFile.readText()).jsonObject
        assertEquals("8", darkJson["version"]?.jsonPrimitive?.content)
    }
}
