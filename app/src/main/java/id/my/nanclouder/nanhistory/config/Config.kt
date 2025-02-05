package id.my.nanclouder.nanhistory.config

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import id.my.nanclouder.nanhistory.lib.matchOrNull
import java.io.File

object Config {
    private const val PATH = "config/config.json"

    object Default {
        const val LOCATION_ACCURACY_THRESHOLD = 20
        const val LOCATION_MINIMUM_DISTANCE = 30
        const val LOCATION_UPDATE_INTERVAL = 5
        const val RECORD_EVENT_RANGE_MINIMUM_DURATION = 10
        const val EXPERIMENTAL_STREAM_LIST = false
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

    val locationAccuracyThreshold = IntValue(
        "locationAccuracyThreshold", Default.LOCATION_ACCURACY_THRESHOLD
    )
    val locationMinimumDistance = IntValue(
        "locationMinimumDistance", Default.LOCATION_MINIMUM_DISTANCE
    )
    val locationUpdateInterval = IntValue(
        "locationUpdateInterval", Default.LOCATION_UPDATE_INTERVAL
    )
    val recordEventRangeMinimumDuration = IntValue(
        "recordEventRangeMinimumDuration", Default.RECORD_EVENT_RANGE_MINIMUM_DURATION
    )
    val experimentalStreamList = BooleanValue(
        "experimentalStreamList", Default.EXPERIMENTAL_STREAM_LIST
    )
}
