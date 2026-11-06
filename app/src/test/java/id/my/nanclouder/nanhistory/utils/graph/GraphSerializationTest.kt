package id.my.nanclouder.nanhistory.utils.graph

import id.my.nanclouder.nanhistory.domain.logic.graph.SeriesDataSource
import id.my.nanclouder.nanhistory.domain.logic.graph.StandardProperty

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GraphSerializationTest {

    private val gson = GsonBuilder()
        .registerTypeAdapter(SeriesDataSource::class.java, GraphPreferencesRepository.SeriesDataSourceAdapter())
        .create()

    @Test
    fun testStandardAndCustomSeriesSerialization() {
        val s1 = SeriesConfig(
            id = "s1",
            name = "Speed",
            dataSource = SeriesDataSource.Standard(StandardProperty.SPEED),
            colorPresetId = "preset_speed"
        )
        val s2 = SeriesConfig(
            id = "s2",
            name = "Custom Diff",
            dataSource = SeriesDataSource.Custom("speed - speed[-1]", "km/h"),
            colorPresetId = "preset_acceleration"
        )
        val graph = GraphConfig(
            id = "g1",
            title = "Test Graph",
            series = listOf(s1, s2),
            scaleMode = GraphScaleMode.DUAL_AXIS
        )

        val json = gson.toJson(listOf(graph))
        val type = object : TypeToken<List<GraphConfig>>() {}.type
        val restoredList: List<GraphConfig> = gson.fromJson(json, type)

        assertEquals(1, restoredList.size)
        val restored = restoredList[0]
        assertEquals("g1", restored.id)
        assertEquals("Test Graph", restored.title)
        assertEquals(GraphScaleMode.DUAL_AXIS, restored.scaleMode)
        assertEquals(2, restored.series.size)

        val restoredS1 = restored.series[0]
        assertEquals("Speed", restoredS1.name)
        assertTrue(restoredS1.dataSource is SeriesDataSource.Standard)
        assertEquals(StandardProperty.SPEED, (restoredS1.dataSource as SeriesDataSource.Standard).property)

        val restoredS2 = restored.series[1]
        assertEquals("Custom Diff", restoredS2.name)
        assertTrue(restoredS2.dataSource is SeriesDataSource.Custom)
        assertEquals("speed - speed[-1]", (restoredS2.dataSource as SeriesDataSource.Custom).expression)
        assertEquals("km/h", (restoredS2.dataSource as SeriesDataSource.Custom).unit)
    }

    @Test
    fun testColorPresetSerialization() {
        val preset = ColorPreset(
            id = "cp1",
            name = "My Palette",
            category = ColorPresetCategory.CUSTOM,
            stops = mapOf(0f to 0xFF00FF00, 50f to 0xFFFF0000)
        )
        val json = gson.toJson(listOf(preset))
        val type = object : TypeToken<List<ColorPreset>>() {}.type
        val restoredList: List<ColorPreset> = gson.fromJson(json, type)

        assertEquals(1, restoredList.size)
        val restored = restoredList[0]
        assertEquals("cp1", restored.id)
        assertEquals("My Palette", restored.name)
        assertEquals(ColorPresetCategory.CUSTOM, restored.category)
        assertEquals(2, restored.stops.size)
    }
}
