package id.my.nanclouder.nanhistory.config

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import id.my.nanclouder.nanhistory.lib.matchOrNull
import java.io.File

object Config {
    private const val PATH = "config/config.json"

    object default {
        const val LOCATION_ACCURACY_THRESHOLD = 20
        const val LOCATION_MINIMUM_DISTANCE = 30
        const val LOCATION_UPDATE_INTERVAL = 5
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

    val locationAccuracyThreshold = IntValue(
        "locationAccuracyThreshold", default.LOCATION_ACCURACY_THRESHOLD
    )
    val locationMinimumDistance = IntValue(
        "locationMinimumDistance", default.LOCATION_MINIMUM_DISTANCE
    )
    val locationUpdateInterval = IntValue(
        "locationUpdateInterval", default.LOCATION_UPDATE_INTERVAL
    )
}
