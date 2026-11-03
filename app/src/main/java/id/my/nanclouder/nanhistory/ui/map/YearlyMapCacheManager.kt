package id.my.nanclouder.nanhistory.ui.map

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

/**
 * Manages cold-start disk caching for [CorridorRender] instances and legacy [YearlyMapPersistentState].
 */
object YearlyMapCacheManager {
    const val ALGO_VERSION: Int = 2
    private const val LEGACY_CACHE_DIR: String = "yearly_maps"
    private const val RENDER_CACHE_DIR: String = "corridor_cache"

    private val gson: Gson by lazy { GsonBuilder().create() }

    private data class SerializedCorridorRender(
        val version: Long = 0L,
        val geoJson: String = "",
        val eventIdPool: List<String> = emptyList(),
        val edgeEventIndices: List<List<Int>> = emptyList()
    )

    private fun getRenderCacheDir(context: Context): File {
        val dir = File(context.cacheDir, RENDER_CACHE_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun getRenderCacheFile(context: Context, key: String): File {
        val sanitized = key.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return File(getRenderCacheDir(context), "corridor_${sanitized}_v${ALGO_VERSION}.json")
    }

    /**
     * Persists [render] to cacheDir under [key].
     */
    fun saveRender(context: Context, key: String, render: CorridorRender) {
        if (render.geoJson == CorridorRender.EMPTY_FEATURE_COLLECTION) return
        try {
            val file = getRenderCacheFile(context, key)
            val dto = SerializedCorridorRender(
                version = render.version,
                geoJson = render.geoJson,
                eventIdPool = render.eventIdPool.toList(),
                edgeEventIndices = render.edgeEventIndices.map { it.toList() }
            )
            file.writeText(gson.toJson(dto))
        } catch (e: Throwable) {
            Log.w("YearlyMapCache", "Failed to save corridor render cache", e)
        }
    }

    /**
     * Loads cached [CorridorRender] for [key] if present, or null.
     */
    fun loadRender(context: Context, key: String): CorridorRender? {
        val file = getRenderCacheFile(context, key)
        if (!file.exists()) return null
        return try {
            val text = file.readText()
            val dto = gson.fromJson(text, SerializedCorridorRender::class.java) ?: return null
            CorridorRender(
                version = dto.version,
                geoJson = dto.geoJson,
                eventIdPool = dto.eventIdPool.toTypedArray(),
                edgeEventIndices = dto.edgeEventIndices.map { it.toIntArray() }.toTypedArray()
            )
        } catch (e: Throwable) {
            Log.w("YearlyMapCache", "Failed to load corridor render cache", e)
            null
        }
    }

    private fun getNetworkCacheFile(context: Context, key: String): File {
        val sanitized = key.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return File(getRenderCacheDir(context), "corridor_net_${sanitized}_v${ALGO_VERSION}.bin")
    }

    /**
     * Persists [network] to cacheDir under [key] using high-performance binary serialization.
     */
    fun saveNetworkBinary(context: Context, key: String, network: CorridorNetwork) {
        if (network.edges.isEmpty()) return
        try {
            val file = getNetworkCacheFile(context, key)
            file.outputStream().buffered().use { output ->
                network.writeTo(output)
            }
        } catch (e: Throwable) {
            Log.w("YearlyMapCache", "Failed to save corridor network binary cache", e)
        }
    }

    /**
     * Loads cached [CorridorNetwork] for [key] if present, or null.
     */
    fun loadNetworkBinary(context: Context, key: String): CorridorNetwork? {
        val file = getNetworkCacheFile(context, key)
        if (!file.exists()) return null
        return try {
            file.inputStream().buffered().use { input ->
                CorridorNetwork.readFrom(input)
            }
        } catch (e: Throwable) {
            Log.w("YearlyMapCache", "Failed to load corridor network binary cache", e)
            null
        }
    }

    /**
     * Deletes binary and JSON render cache files for a specific time range key.
     */
    fun clearRangeCache(context: Context, key: String) {
        try {
            getNetworkCacheFile(context, key).delete()
            getRenderCacheFile(context, key).delete()
        } catch (e: Throwable) {
            Log.w("YearlyMapCache", "Failed to clear range cache for key $key", e)
        }
    }

    /**
     * Recursively deletes all cached corridor network files and legacy OSMDroid caches.
     */
    fun clearAllCorridorCaches(context: Context) {
        clearCache(context)
    }

    /**
     * Calculates total storage bytes consumed by corridor caches.
     */
    fun getCorridorCacheSize(context: Context): Long {
        val renderDir = getRenderCacheDir(context)
        val legacyDir = getLegacyCacheDir(context)
        val renderSize = if (renderDir.exists()) {
            renderDir.walk().filter { it.isFile }.sumOf { it.length() }
        } else 0L
        val legacySize = if (legacyDir.exists()) {
            legacyDir.walk().filter { it.isFile }.sumOf { it.length() }
        } else 0L
        return renderSize + legacySize
    }

    /* Legacy OSMDroid caching methods below */

    private fun getLegacyCacheDir(context: Context): File {
        val dir = File(context.cacheDir, LEGACY_CACHE_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun getLegacyCacheFile(context: Context, year: Int): File {
        return File(getLegacyCacheDir(context), "year_$year.json")
    }

    fun saveCache(context: Context, year: Int, state: YearlyMapPersistentState) {
        try {
            val file = getLegacyCacheFile(context, year)
            val json = gson.toJson(state)
            file.writeText(json)
            Log.d("YearlyMapCache", "Saved cache for year $year")
        } catch (e: Throwable) {
            Log.e("YearlyMapCache", "Failed to save cache for year $year", e)
        }
    }

    fun loadCache(context: Context, year: Int): YearlyMapPersistentState? {
        val file = getLegacyCacheFile(context, year)
        if (!file.exists()) return null
        return try {
            val json = file.readText()
            gson.fromJson(json, YearlyMapPersistentState::class.java)
        } catch (e: Throwable) {
            Log.e("YearlyMapCache", "Failed to load cache for year $year", e)
            null
        }
    }

    fun clearCache(context: Context) {
        try {
            getRenderCacheDir(context).deleteRecursively()
            getLegacyCacheDir(context).deleteRecursively()
            Log.d("YearlyMapCache", "Cleared yearly map cache")
        } catch (e: Throwable) {
            Log.e("YearlyMapCache", "Failed to clear cache", e)
        }
    }
}
