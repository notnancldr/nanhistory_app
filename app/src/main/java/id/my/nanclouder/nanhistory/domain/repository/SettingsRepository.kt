package id.my.nanclouder.nanhistory.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Domain port for reading, observing, and updating application settings and configuration.
 */
interface SettingsRepository {
    fun <T> observe(key: String, defaultValue: T): Flow<T>
    suspend fun <T> get(key: String, defaultValue: T): T
    suspend fun <T> set(key: String, value: T)
    fun <T> getSync(key: String, defaultValue: T): T
}
