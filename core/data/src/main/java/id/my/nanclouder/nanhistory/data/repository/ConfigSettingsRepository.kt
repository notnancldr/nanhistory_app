package id.my.nanclouder.nanhistory.data.repository

import android.content.Context
import id.my.nanclouder.nanhistory.config.Config
import id.my.nanclouder.nanhistory.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Data implementation of [SettingsRepository] adapting NanHistory's file-backed [Config] system.
 */
class ConfigSettingsRepository(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : SettingsRepository {

    override fun <T> observe(key: String, defaultValue: T): Flow<T> {
        return Config.getConfigFlow().map { cache ->
            @Suppress("UNCHECKED_CAST")
            (cache[key] as? T) ?: defaultValue
        }
    }

    override suspend fun <T> get(key: String, defaultValue: T): T = withContext(ioDispatcher) {
        getSync(key, defaultValue)
    }

    override fun <T> getSync(key: String, defaultValue: T): T {
        val cache = Config.getConfigMap(context)
        @Suppress("UNCHECKED_CAST")
        return (cache[key] as? T) ?: defaultValue
    }

    override suspend fun <T> set(key: String, value: T): Unit = withContext(ioDispatcher) {
        if (value != null) {
            Config.setConfigValue(context, key, value as Any)
        }
    }
}
