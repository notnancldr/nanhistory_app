package id.my.nanclouder.nanhistory.data.transport

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import id.my.nanclouder.nanhistory.domain.logic.transport.CalibrationModel
import id.my.nanclouder.nanhistory.domain.logic.transport.DEFAULT_TRANSPORT_MODE_DETECTION_MODEL
import id.my.nanclouder.nanhistory.domain.logic.transport.TransportMode
import java.io.File

object CalibrationModelStorage {
    fun saveCalibrationModels(
        context: Context,
        models: Map<TransportMode, CalibrationModel>
    ) {
        val file = File(context.filesDir, "transport_calibration_all.json")
        val jsonMap = models.mapKeys { it.key.name }
        file.writeText(Gson().toJson(jsonMap))
    }

    fun loadCalibrationModels(context: Context): Map<TransportMode, CalibrationModel> {
        val file = File(context.filesDir, "transport_calibration_all.json")
        if (!file.exists()) return DEFAULT_TRANSPORT_MODE_DETECTION_MODEL

        return try {
            val type = object : TypeToken<Map<String, CalibrationModel>>() {}.type
            val jsonMap: Map<String, CalibrationModel> = Gson().fromJson(file.readText(), type)
            jsonMap.mapKeys { TransportMode.valueOf(it.key) }
        } catch (_: Exception) {
            DEFAULT_TRANSPORT_MODE_DETECTION_MODEL
        }
    }
}

fun loadCalibrationModels(context: Context): Map<TransportMode, CalibrationModel> =
    CalibrationModelStorage.loadCalibrationModels(context)

fun saveCalibrationModels(context: Context, models: Map<TransportMode, CalibrationModel>) =
    CalibrationModelStorage.saveCalibrationModels(context, models)

fun resetModels(context: Context) {
    val file = File(context.filesDir, "transport_calibration_all.json")
    if (file.exists()) file.delete()
}
