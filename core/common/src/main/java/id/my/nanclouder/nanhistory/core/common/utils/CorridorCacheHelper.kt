package id.my.nanclouder.nanhistory.core.common.utils

import android.content.Context
import java.io.File

object CorridorCacheHelper {
    private const val LEGACY_CACHE_DIR: String = "yearly_maps"
    private const val RENDER_CACHE_DIR: String = "corridor_cache"

    fun getCorridorCacheSize(context: Context): Long {
        val renderDir = File(context.cacheDir, RENDER_CACHE_DIR)
        val legacyDir = File(context.cacheDir, LEGACY_CACHE_DIR)
        val renderSize = if (renderDir.exists()) {
            renderDir.walk().filter { it.isFile }.sumOf { it.length() }
        } else 0L
        val legacySize = if (legacyDir.exists()) {
            legacyDir.walk().filter { it.isFile }.sumOf { it.length() }
        } else 0L
        return renderSize + legacySize
    }

    fun clearAllCorridorCaches(context: Context) {
        try {
            File(context.cacheDir, RENDER_CACHE_DIR).deleteRecursively()
            File(context.cacheDir, LEGACY_CACHE_DIR).deleteRecursively()
        } catch (_: Throwable) {
        }
    }
}
