package id.my.nanclouder.nanhistory.config

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import id.my.nanclouder.nanhistory.lib.matchOrNull
import java.io.File

object Config {
    private const val PATH = "config/config.json"

    object Default {
        const val SERVICE_MAX_DURATION = 900
        const val LOCATION_ACCURACY_THRESHOLD = 20
        const val LOCATION_MINIMUM_DISTANCE = 30
        const val LOCATION_UPDATE_INTERVAL = 5

        const val AUDIO_RECORD_MAX_DURATION = 10
        const val EXPERIMENTAL_AUDIO_RECORD = false
        const val INCLUDE_AUDIO_RECORD = false

        const val AUDIO_STEREO_CHANNEL = false
        const val AUDIO_ENCODING_BITRATE = 128
        const val AUDIO_SAMPLING_RATE = 44
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
        return Gson().fromJson(
            readOrCreateNew(context),
            object : TypeToken<Map<String, Any>>() {}.type
        )
    }

    private fun setConfig(context: Context, name: String, value: Any) {
        val config = getConfig(context).toMutableMap()
        config[name] = value
        val configFile = File(context.filesDir, PATH)
        configFile.writeText(Gson().toJson(config))
    }

    class IntValue internal constructor(val name: String, private val default: Int) {
        fun get(context: Context) =
            matchOrNull<Double>(getConfig(context)[name])?.toInt() ?: default
        fun set(context: Context, value: Int) =
            setConfig(context, name, value)
    }

    class BooleanValue internal constructor(val name: String, private val default: Boolean) {
        fun get(context: Context) =
            matchOrNull<Boolean>(getConfig(context)[name]) ?: default
        fun set(context: Context, value: Boolean) =
            setConfig(context, name, value)
    }

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
    val audioRecordMaxDuration = IntValue(
        "audioRecordMaxDuration", Default.AUDIO_RECORD_MAX_DURATION
    )
    val experimentalAudioRecord = BooleanValue(
        "experimentalAudioRecord", Default.EXPERIMENTAL_AUDIO_RECORD
    )
    val includeAudioRecord = BooleanValue(
        "includeAudioRecord", Default.INCLUDE_AUDIO_RECORD
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
}
