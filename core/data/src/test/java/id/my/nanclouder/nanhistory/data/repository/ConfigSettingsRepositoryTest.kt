package id.my.nanclouder.nanhistory.data.repository

import id.my.nanclouder.nanhistory.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ConfigSettingsRepositoryTest {

    private class FakeSettingsRepository : SettingsRepository {
        private val state = MutableStateFlow<Map<String, Any>>(emptyMap())

        override fun <T> observe(key: String, defaultValue: T): Flow<T> {
            return state.map {
                @Suppress("UNCHECKED_CAST")
                (it[key] as? T) ?: defaultValue
            }
        }

        override suspend fun <T> get(key: String, defaultValue: T): T {
            return getSync(key, defaultValue)
        }

        override fun <T> getSync(key: String, defaultValue: T): T {
            @Suppress("UNCHECKED_CAST")
            return (state.value[key] as? T) ?: defaultValue
        }

        override suspend fun <T> set(key: String, value: T) {
            val updated = state.value.toMutableMap()
            if (value != null) {
                updated[key] = value as Any
            } else {
                updated.remove(key)
            }
            state.value = updated
        }
    }

    @Test
    fun `get returns default when key is unset`() = runBlocking {
        val repo = FakeSettingsRepository()
        val result = repo.get("sample_key", 42)
        assertEquals(42, result)
    }

    @Test
    fun `set updates value and observe emits updated value`() = runBlocking {
        val repo = FakeSettingsRepository()
        repo.set("theme_dark", true)
        val result = repo.get("theme_dark", false)
        assertEquals(true, result)

        val observed = repo.observe("theme_dark", false).first()
        assertEquals(true, observed)
    }
}
