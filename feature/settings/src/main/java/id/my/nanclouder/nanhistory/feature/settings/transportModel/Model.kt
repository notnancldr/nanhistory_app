// Model.kt
package id.my.nanclouder.nanhistory.feature.settings.transportModel
import id.my.nanclouder.nanhistory.domain.logic.transport.*

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import id.my.nanclouder.nanhistory.db.AppDatabase
import id.my.nanclouder.nanhistory.db.toHistoryEvent
import id.my.nanclouder.nanhistory.domain.model.Coordinate
import id.my.nanclouder.nanhistory.domain.model.HistoryLocationData
import id.my.nanclouder.nanhistory.core.common.location.getLocationData
import id.my.nanclouder.nanhistory.domain.model.LocationData
import id.my.nanclouder.nanhistory.data.transport.getLocations
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.ZonedDateTime

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

    val type = object : TypeToken<Map<String, CalibrationModel>>() {}.type
    val jsonMap: Map<String, CalibrationModel> = Gson().fromJson(file.readText(), type)

    return jsonMap.mapKeys { TransportMode.valueOf(it.key) }
}

fun resetModels(context: Context) {
    val file = File(context.filesDir, "transport_calibration_all.json")
    if (file.exists()) file.delete()
}

fun clearModels(context: Context) {
    val file = File(context.filesDir, "transport_calibration_all.json")
    file.writeText("{}")
}

// CalibrationModel moved to domain/logic/transport/CalibrationModel.kt

/**
 * Calculates the longest duration (in seconds) where acceleration
 * remains below the given threshold
 */
// calculateMaxCoastingDuration moved to domain/logic/transport/TransportMetrics.kt

fun recalibrateModels(
    history: List<HistoryLocationData>,
    existing: Map<TransportMode, CalibrationModel>,
    expected: TransportMode
): Map<TransportMode, CalibrationModel> {

    val newModel = computeBatchMetrics(history)

    val previous = existing[expected]

    val merged = if (previous != null) {
        mergeModels(previous, newModel)
    } else newModel

    return existing.toMutableMap().apply {
        this[expected] = merged
    }
}

@Composable
fun CalibrationParamsPreview(
    original: CalibrationModel?,
    current: CalibrationModel
) {
    data class ParamDiff(val label: String, val old: Float?, val new: Float)

    val diffs = listOf(
        ParamDiff("Avg Speed Min", original?.avgSpeedMin, current.avgSpeedMin),
        ParamDiff("Avg Speed Max", original?.avgSpeedMax, current.avgSpeedMax),
        ParamDiff("Top Speed Min", original?.topSpeedMin, current.topSpeedMin),
        ParamDiff("Top Speed Max", original?.topSpeedMax, current.topSpeedMax),
        ParamDiff("Avg Accel Min", original?.avgAccelMin, current.avgAccelMin),
        ParamDiff("Avg Accel Max", original?.avgAccelMax, current.avgAccelMax)
    )

    Column {
        diffs.forEach { diff ->
            val changed = diff.old != null && abs(diff.new - diff.old) > 0.0001f

            Row {
                Text(
                    text = diff.label + ": ",
                    style = MaterialTheme.typography.labelMedium
                )

                if (changed) {
                    val color = if (diff.new > diff.old) Color(0xFF00AA33) else Color(0xFFCC0033)
                    Text(
                        text = "${diff.old} → ${diff.new}",
                        color = color,
                        style = MaterialTheme.typography.labelMedium
                    )
                } else {
                    Text(
                        text = diff.new.toString(),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

suspend fun getTrainingData(context: Context): List<Pair<TransportMode, List<HistoryLocationData>>> {
    val db = AppDatabase.getInstance(context)
    val dao = db.appDao()

    val cacheFile = File(context.cacheDir, "transport_detection_training_data.json")

    val eventWithTransportMode = dao.getAllEvents()
        .first()
        .filter {
            it.event.transportationType.toTransportMode() != TransportMode.UNKNOWN
        }

    return withContext(Dispatchers.IO) {
        eventWithTransportMode.map {
            it.event.transportationType.toTransportMode() to it.toHistoryEvent().getLocations(context).getLocationData()
        }.let { trainingData ->
            // try {
            //     val cacheData = trainingData.map { (mode, locations) ->
            //         TrainingDataCache(
            //             mode = mode.name,
            //             locations = locations.map { loc ->
            //                 CachedLocationData(
            //                     speed = loc.speed,
            //                     acceleration = loc.acceleration,
            //                     distance = loc.distance,
            //                     start = loc.start.toString(),
            //                     end = loc.end.toString(),
            //                     points = loc.points.map { CachedCoordinate(it.latitude, it.longitude) }
            //                 )
            //             }
            //         )
            //     }
            //     cacheFile.writeText(Gson().toJson(cacheData))
            // } catch (e: Exception) {
            //     e.printStackTrace()
            // }
            TrainingDataCache.clearCache()
            TrainingDataCache.setCache(trainingData)
            trainingData
        }
    }
}

suspend fun getTrainingDataFromCache(context: Context): List<Pair<TransportMode, List<HistoryLocationData>>>? {
    val cacheFile = File(context.cacheDir, "transport_detection_training_data.json")

    if (!cacheFile.exists()) {
        return null
    }

    return TrainingDataCache.cache ?: withContext(Dispatchers.IO) {
        try {
            val type = object : TypeToken<List<TrainingDataCache>>() {}.type
            val cachedData: List<TrainingDataCache> = Gson().fromJson(cacheFile.readText(), type)

            cachedData.map { cache ->
                TransportMode.valueOf(cache.mode) to cache.locations.map { loc ->
                    val start = ZonedDateTime.parse(loc.start)
                    val end = ZonedDateTime.parse(loc.end)

                    val points = loc.points.map { Coordinate(it.latitude, it.longitude) }
                    HistoryLocationData(
                        speed = loc.speed,
                        acceleration = loc.acceleration,
                        distance = loc.distance,
                        start = start,
                        end = end,
                        points = points,
                        locationData = listOf(
                            LocationData(
                                time = start,
                                location = points.first()
                            ),
                            LocationData(
                                time = end,
                                location = points.last()
                            )
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

fun clearTrainingDataCache(context: Context) {
    TrainingDataCache.clearCache()
    val cacheFile = File(context.cacheDir, "transport_detection_training_data.json")
    if (cacheFile.exists()) {
        cacheFile.delete()
    }
}

// Cache data classes
data class TrainingDataCache(
    val mode: String,
    val locations: List<CachedLocationData>
) {
    companion object {
        @Volatile private var _cache: List<Pair<TransportMode, List<HistoryLocationData>>>? = null

        val cache
            get() = _cache

        fun setCache(data: List<Pair<TransportMode, List<HistoryLocationData>>>) {
            _cache = data
        }

        fun clearCache() {
            _cache = null
        }
    }
}

data class CachedLocationData(
    val speed: Float,
    val acceleration: Float,
    val distance: Float,
    val start: String,
    val end: String,
    val points: List<CachedCoordinate>
)

data class CachedCoordinate(
    val latitude: Double,
    val longitude: Double
)

// computeCoastingMetrics moved to domain/logic/transport/TransportMetrics.kt
// fun computeCoastingMetrics delegated via domain import

// computeBatchMetrics moved to domain/logic/transport/TransportMetrics.kt

// mergeModels moved to domain/logic/transport/TransportMetrics.kt

data class TrainingResult(
    val accuracy: Float,
    val totalSamples: Int,
    val correctSamples: Int,
    val models: Map<TransportMode, CalibrationModel>
)

suspend fun evaluate(
    models: Map<TransportMode, CalibrationModel>,
    data: List<Pair<TransportMode, List<HistoryLocationData>>>,
    strategy: ScoringStrategy = ScoringStrategy.RANGE_BASED
): TrainingResult {
    var total = 0
    var correct = 0

    data.forEach { (expected, history) ->
        if (history.isEmpty()) return@forEach
        total++

        val detected = detectTransportMode(history, models, strategy)
        if (detected == expected) correct++
    }

    val accuracy = if (total == 0) 0f else correct.toFloat() / total.toFloat()
    return TrainingResult(accuracy, total, correct, models)
}

suspend fun train(
    context: Context,
    existing: Map<TransportMode, CalibrationModel>,
    strategy: ScoringStrategy = ScoringStrategy.RANGE_BASED
): TrainingResult = withContext(Dispatchers.Default) {

    val training = getTrainingData(context)
    if (training.isEmpty()) return@withContext TrainingResult(
        accuracy = 0f,
        totalSamples = 0,
        correctSamples = 0,
        models = existing
    )

    val updated = existing.toMutableMap()

    for ((mode, historyList) in training) {
        if (historyList.isEmpty()) continue

        val batchModel = computeBatchMetrics(historyList)

        val prev = updated[mode]
        val merged = if (prev != null) mergeModels(prev, batchModel) else batchModel

        updated[mode] = merged
    }

    saveCalibrationModels(context, updated)

    val evaluationResult = evaluate(updated, training, strategy)

    return@withContext evaluationResult
}

private fun smooth(old: Float, new: Float, lr: Float): Float {
    return old + (new - old) * lr
}