package id.my.nanclouder.nanhistory.config

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import java.io.File

enum class LocationIterationLogic {
    Legacy, Default,
    LookBehindAhead_v1
}

object Config {
    private const val PATH = "config/config.json"

    private var _cache = MutableStateFlow(emptyMap<String, Any>())
    private var _currentCache = emptyMap<String, Any>()

    object Default {
        const val DEVELOPER_MODE_ENABLED = false
        const val DEVELOPER_1HOUR_AUTO_DELETE = false
        const val DEVELOPER_SERVICE_DEBUG = false
        const val DEVELOPER_SHOW_DETECTED_TRANSPORT = false

        const val SERVICE_MAX_DURATION = 900
        const val SERVICE_RESTART_ENABLED = true
        const val LOCATION_ACCURACY_THRESHOLD = 20
        const val LOCATION_MINIMUM_DISTANCE = 30
        const val LOCATION_UPDATE_INTERVAL = 5
        val LOCATION_ITERATION_LOGIC = LocationIterationLogic.Default

        const val RECORD_VIBRATE_ENABLED = false
        const val RECORD_VIBRATE_EVERY_UPDATE = false
        const val RECORD_VIBRATE_INTERVAL = 5 // s
        const val RECORD_VIBRATE_DURATION = 100 // ms

        const val RECORD_SHAKE_ENABLED = false
        const val RECORD_SHAKE_TO_START = false
        const val RECORD_SHAKE_SENSITIVITY = 2

        const val AUDIO_RECORD_MAX_DURATION = 10
        const val EXPERIMENTAL_AUDIO_RECORD = false
        const val INCLUDE_AUDIO_RECORD = false

        const val AUDIO_STEREO_CHANNEL = false
        const val AUDIO_ENCODING_BITRATE = 128
        const val AUDIO_SAMPLING_RATE = 44

        const val APPEARANCE_NEW_UI = false

        const val AUTO_DELETE_NOTIFY_USER = true
        const val AUTO_DELETE_ALWAYS_ASK = false
    }

    private fun readOrCreateNew(context: Context): String {
        val configFile = File(context.filesDir, PATH)
        configFile.parentFile?.mkdirs()
        return if (!configFile.exists()) {
            configFile.createNewFile()
            configFile.writeText("{}")
            "{}"
        } else {
            configFile.readText()
        }
    }

    private fun getConfig(context: Context): Map<String, Any> {
        return Gson().fromJson<Map<String, Any>>(
            readOrCreateNew(context),
            object : TypeToken<Map<String, Any>>() {}.type
        ).apply {
            _currentCache = this
            _cache.value = _currentCache
        }
    }

    private fun getConfigFlow(): StateFlow<Map<String, Any>> {
        return _cache
    }

    fun prepareCache(context: Context) {
        _currentCache = getConfig(context)
        _cache.value = _currentCache
    }

    private fun setConfig(context: Context, name: String, value: Any) {
        val config = getConfig(context).toMutableMap()
        config[name] = value
        val configFile = File(context.filesDir, PATH)
        configFile.writeText(Gson().toJson(config))

        // Update config cache
        _currentCache = config
        _cache.value = _currentCache
    }

    class IntValue internal constructor(val name: String, private val default: Int) {
        fun get(context: Context) =
            (getConfig(context)[name] as? Double)?.toInt() ?: default
        fun getCache() =
            (_currentCache[name] as? Double)?.toInt() ?: default
        fun set(context: Context, value: Int) =
            setConfig(context, name, value)
        fun getFlow() = getConfigFlow().map {
            (it[name] as? Double)?.toInt() ?: default
        }
        @Composable
        fun getState() = getFlow().collectAsState(get(LocalContext.current))
    }

    class BooleanValue internal constructor(val name: String, private val default: Boolean) {
        fun get(context: Context) =
            (getConfig(context)[name] as? Boolean) ?: default
        fun getCache() =
            (_currentCache[name] as? Boolean) ?: default
        fun set(context: Context, value: Boolean) =
            setConfig(context, name, value)
        fun getFlow() = getConfigFlow().map {
            (it[name] as? Boolean) ?: default
        }
        @Composable
        fun getState() = getFlow().collectAsState(get(LocalContext.current))
    }

    class EnumValue<E : Enum<E>> internal constructor(
        val name: String,
        private val default: E,
        private val enumClass: Class<E>
    ) {
        fun get(context: Context): E =
            (getConfig(context)[name] as? String)?.let { value ->
                try {
                    java.lang.Enum.valueOf(enumClass, value)
                } catch (e: IllegalArgumentException) {
                    default
                }
            } ?: default

        fun getCache(): E =
            (_currentCache[name] as? String)?.let { value ->
                try {
                    java.lang.Enum.valueOf(enumClass, value)
                } catch (e: IllegalArgumentException) {
                    default
                }
            } ?: default

        fun getFlow() = getConfigFlow().map {
            (it[name] as? String)?.let { value ->
                try {
                    java.lang.Enum.valueOf(enumClass, value)
                } catch (e: IllegalArgumentException) {
                    default
                }
            } ?: default
        }

        fun set(context: Context, value: E) =
            setConfig(context, name, value.name)

        @Composable
        fun getState() = getFlow().collectAsState(get(LocalContext.current))
    }

    val developerModeEnabled = BooleanValue(
        "developerModeEnabled", Default.DEVELOPER_MODE_ENABLED
    )
    val developer1hourAutoDelete = BooleanValue(
        "developer1hourAutoDelete", Default.DEVELOPER_1HOUR_AUTO_DELETE
    )
    val developerServiceDebug = BooleanValue(
        "developerServiceDebug", Default.DEVELOPER_SERVICE_DEBUG
    )
    val developerShowDetectedTransport = BooleanValue(
        "developerShowDetectedTransport", Default.DEVELOPER_SHOW_DETECTED_TRANSPORT
    )

    val serviceRestartEnabled = BooleanValue(
        "serviceRestartEnabled", Default.SERVICE_RESTART_ENABLED
    )
    val serviceMaxDuration = IntValue(
        "serviceMaxDuration", Default.SERVICE_MAX_DURATION
    )

    val locationAccuracyThreshold = IntValue(
        "locationAccuracyThreshold", Default.LOCATION_ACCURACY_THRESHOLD
    )
    val locationMinimumDistance = IntValue(
        "locationMinimumDistance", Default.LOCATION_MINIMUM_DISTANCE
    )
    val locationUpdateInterval = IntValue(
        "locationUpdateInterval", Default.LOCATION_UPDATE_INTERVAL
    )
    val locationIterationLogic = EnumValue(
        "locationIterationLogic", Default.LOCATION_ITERATION_LOGIC,
        LocationIterationLogic::class.java
    )

    val recordVibrateEnabled = BooleanValue(
        "recordVibrateEnabled", Default.RECORD_VIBRATE_ENABLED
    )
    val recordVibrateEveryUpdate = BooleanValue(
        "recordVibrateEveryUpdate", Default.RECORD_VIBRATE_EVERY_UPDATE
    )
    val recordVibrateInterval = IntValue(
        "recordVibrateInterval", Default.RECORD_VIBRATE_INTERVAL
    )
    val recordVibrateDuration = IntValue(
        "recordVibrateDuration", Default.RECORD_VIBRATE_DURATION
    )

    val recordShakeEnabled = BooleanValue(
        "recordShakeEnable", Default.RECORD_SHAKE_ENABLED
    )
    val recordShakeToStart = BooleanValue(
        "recordShakeToStart", Default.RECORD_SHAKE_TO_START
    )
    val recordShakeSensitivity = IntValue(
        "recordShakeSensitivity", Default.RECORD_SHAKE_SENSITIVITY
    )

    val audioRecordMaxDuration = IntValue(
        "audioRecordMaxDuration", Default.AUDIO_RECORD_MAX_DURATION
    )
    val experimentalAudioRecord = BooleanValue(
        "experimentalAudioRecord", Default.EXPERIMENTAL_AUDIO_RECORD
    )
    val includeAudioRecord = BooleanValue(
        "includeAudioRecord", Default.INCLUDE_AUDIO_RECORD
    )

    val appearanceNewUI = BooleanValue(
        "appearanceNewUI", Default.APPEARANCE_NEW_UI
    )

    val audioStereoChannel = BooleanValue(
        "audioStereoChannel", Default.AUDIO_STEREO_CHANNEL
    )
    val audioEncodingBitrate = IntValue(
        "audioEncodingBitrate", Default.AUDIO_ENCODING_BITRATE
    )
    val audioSamplingRate = IntValue(
        "audioSamplingRate", Default.AUDIO_SAMPLING_RATE
    )

    val autoDeleteNotifyUser = BooleanValue(
        "audioDeleteNotifyUser", Default.AUTO_DELETE_NOTIFY_USER
    )
    val autoDeleteAlwaysAsk = BooleanValue(
        "audioDeleteAlwaysAsk", Default.AUTO_DELETE_ALWAYS_ASK
    )
}
