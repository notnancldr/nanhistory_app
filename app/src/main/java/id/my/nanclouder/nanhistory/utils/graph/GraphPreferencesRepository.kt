package id.my.nanclouder.nanhistory.utils.graph

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import id.my.nanclouder.nanhistory.config.Config
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.lang.reflect.Type

/**
 * Repository for managing persisted graph configurations and custom color presets,
 * persisting directly to Config.graphSaved and Config.graphColors in config/config.json.
 */
class GraphPreferencesRepository(
    private val context: Context,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    val gson: Gson = GsonBuilder()
        .registerTypeAdapter(SeriesDataSource::class.java, SeriesDataSourceAdapter())
        .create()

    private val _graphs = MutableStateFlow<List<GraphConfig>>(loadInitialGraphs())
    val graphs: StateFlow<List<GraphConfig>> = _graphs.asStateFlow()

    private val _customColors = MutableStateFlow<List<ColorPreset>>(loadInitialColors())
    val customColors: StateFlow<List<ColorPreset>> = _customColors.asStateFlow()

    init {
        coroutineScope.launch {
            Config.graphSaved.getFlow().collect { json ->
                val parsed = parseGraphsJson(json)
                if (parsed != _graphs.value) {
                    _graphs.value = parsed
                }
            }
        }
        coroutineScope.launch {
            Config.graphColors.getFlow().collect { json ->
                val parsed = parseColorsJson(json)
                if (parsed != _customColors.value) {
                    _customColors.value = parsed
                }
            }
        }
    }

    private fun loadInitialGraphs(): List<GraphConfig> {
        val raw = Config.graphSaved.get(context)
        return parseGraphsJson(raw)
    }

    private fun loadInitialColors(): List<ColorPreset> {
        val raw = Config.graphColors.get(context)
        return parseColorsJson(raw)
    }

    private fun parseGraphsJson(json: String?): List<GraphConfig> {
        if (json.isNullOrBlank() || json == "[]") return emptyList()
        return try {
            val type = object : TypeToken<List<GraphConfig>>() {}.type
            gson.fromJson<List<GraphConfig>>(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseColorsJson(json: String?): List<ColorPreset> {
        if (json.isNullOrBlank() || json == "[]") return emptyList()
        return try {
            val type = object : TypeToken<List<ColorPreset>>() {}.type
            gson.fromJson<List<ColorPreset>>(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveGraphs(newGraphs: List<GraphConfig>) {
        _graphs.value = newGraphs
        val json = gson.toJson(newGraphs)
        Config.graphSaved.set(context, json)
    }

    fun addGraph(graph: GraphConfig) {
        val updated = _graphs.value + graph
        saveGraphs(updated)
    }

    fun updateGraph(graph: GraphConfig) {
        val updated = _graphs.value.map { if (it.id == graph.id) graph else it }
        saveGraphs(updated)
    }

    fun deleteGraph(graphId: String) {
        val updated = _graphs.value.filterNot { it.id == graphId }
        saveGraphs(updated)
    }

    fun saveCustomColor(preset: ColorPreset) {
        val existing = _customColors.value.filterNot { it.id == preset.id }
        val updated = existing + preset
        _customColors.value = updated
        val json = gson.toJson(updated)
        Config.graphColors.set(context, json)
    }

    fun deleteCustomColor(presetId: String) {
        val updated = _customColors.value.filterNot { it.id == presetId }
        _customColors.value = updated
        val json = gson.toJson(updated)
        Config.graphColors.set(context, json)
    }

    fun getAllColorPresets(): List<ColorPreset> {
        return DefaultGraphPresets.DEFAULT_COLOR_PRESETS + _customColors.value
    }

    fun getColorPreset(id: String): ColorPreset? {
        return getAllColorPresets().firstOrNull { it.id == id }
    }

    internal class SeriesDataSourceAdapter : JsonSerializer<SeriesDataSource>, JsonDeserializer<SeriesDataSource> {
        override fun serialize(
            src: SeriesDataSource,
            typeOfSrc: Type,
            context: JsonSerializationContext
        ): JsonElement {
            val obj = JsonObject()
            when (src) {
                is SeriesDataSource.Standard -> {
                    obj.addProperty("type", "standard")
                    obj.addProperty("property", src.property.name)
                }
                is SeriesDataSource.Custom -> {
                    obj.addProperty("type", "custom")
                    obj.addProperty("expression", src.expression)
                    obj.addProperty("unit", src.unit)
                }
            }
            return obj
        }

        override fun deserialize(
            json: JsonElement,
            typeOfT: Type,
            context: JsonDeserializationContext
        ): SeriesDataSource {
            val obj = json.asJsonObject
            val type = obj.get("type")?.asString ?: "standard"
            return if (type.equals("custom", ignoreCase = true)) {
                val expr = obj.get("expression")?.asString ?: ""
                val unit = obj.get("unit")?.asString ?: ""
                SeriesDataSource.Custom(expr, unit)
            } else {
                val propName = obj.get("property")?.asString ?: StandardProperty.SPEED.name
                val prop = try {
                    StandardProperty.valueOf(propName)
                } catch (_: Exception) {
                    StandardProperty.SPEED
                }
                SeriesDataSource.Standard(prop)
            }
        }
    }
}
