// Model.kt
package id.my.nanclouder.nanhistory.utils.transportModel

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
import id.my.nanclouder.nanhistory.utils.Coordinate
import id.my.nanclouder.nanhistory.utils.HistoryLocationData
import id.my.nanclouder.nanhistory.utils.getLocationData
import id.my.nanclouder.nanhistory.utils.history.LocationData
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

data class CalibrationModel(
    // --- Range-based scoring (original) ---
    val avgSpeedMin: Float,
    val avgSpeedMax: Float,

    val topSpeedMin: Float,
    val topSpeedMax: Float,

    val avgAccelMin: Float,
    val avgAccelMax: Float,

    val stopDurationMin: Float,
    val stopDurationMax: Float,

    val distanceMin: Float,
    val distanceMax: Float,

    val speedVarianceMin: Float,
    val speedVarianceMax: Float,

    val accelVarianceMin: Float,
    val accelVarianceMax: Float,

    val pathComplexityMin: Float,
    val pathComplexityMax: Float,

    // --- Coasting duration parameters (new) ---
    // Maximum coasting duration (no acceleration) at different thresholds
    val maxCoastingDuration05Min: Float,      // Threshold: 0.5 m/s²
    val maxCoastingDuration05Max: Float,

    val maxCoastingDuration10Min: Float,      // Threshold: 1.0 m/s²
    val maxCoastingDuration10Max: Float,

    val maxCoastingDuration20Min: Float,      // Threshold: 2.0 m/s²
    val maxCoastingDuration20Max: Float,

    val maxCoastingDuration40Min: Float,      // Threshold: 4.0 m/s²
    val maxCoastingDuration40Max: Float,

    // --- Ideal values ---
    val idealAvgSpeed: Float? = null,
    val idealTopSpeed: Float? = null,
    val idealAvgAccel: Float? = null,
    val idealStopDuration: Float? = null,
    val idealDistance: Float? = null,
    val idealSpeedVariance: Float? = null,
    val idealAccelVariance: Float? = null,
    val idealPathComplexity: Float? = null,
    val idealMaxCoastingDuration05: Float? = null,
    val idealMaxCoastingDuration10: Float? = null,
    val idealMaxCoastingDuration20: Float? = null,
    val idealMaxCoastingDuration40: Float? = null,

    // --- Strategy configuration ---
    val rangeWeight: Float? = 0.5f,

    // --- Feature Importance Weights (learned during training) ---
    val avgSpeedWeight: Float = 1.0f,
    val topSpeedWeight: Float = 0.8f,
    val avgAccelWeight: Float = 0.7f,
    val stopDurationWeight: Float = 0.6f,
    val distanceWeight: Float = 0.5f,
    val speedVarianceWeight: Float = 0.8f,
    val accelVarianceWeight: Float = 0.7f,
    val pathComplexityWeight: Float = 0.4f,
    val maxCoastingDuration05Weight: Float = 0.6f,
    val maxCoastingDuration10Weight: Float = 0.6f,
    val maxCoastingDuration20Weight: Float = 0.6f,
    val maxCoastingDuration40Weight: Float = 0.6f,

    // --- Per-Mode Bias ---
    val modeBias: Float = 0.0f,

    // --- Confidence Threshold ---
    val confidenceThreshold: Float = 0.3f,

    // Model weight—higher means more confidence
    val weight: Float = 1f
) { companion object }

/**
 * Calculates the longest duration (in seconds) where acceleration
 * remains below the given threshold
 */
private fun calculateMaxCoastingDuration(
    history: List<HistoryLocationData>,
    accelThreshold: Float
): Float {
    if (history.isEmpty()) return 0f

    var maxDuration = 0.0
    var currentDuration = 0.0

    for (sample in history) {
        val accel = abs(sample.acceleration)

        if (accel < accelThreshold) {
            // Continue coasting
            val dur = Duration.between(sample.start, sample.end).toSeconds()
            currentDuration += dur
        } else {
            // Acceleration exceeded threshold, reset counter
            maxDuration = max(maxDuration, currentDuration)
            currentDuration = 0.0
        }
    }

    // Don't forget the final segment
    maxDuration = max(maxDuration, currentDuration)

    return maxDuration.toFloat()
}

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

fun computeCoastingMetrics(history: List<HistoryLocationData>): Map<String, Float> {
    return mapOf(
        "maxCoastingDuration05" to calculateMaxCoastingDuration(history, 500f),
        "maxCoastingDuration10" to calculateMaxCoastingDuration(history, 1000f),
        "maxCoastingDuration20" to calculateMaxCoastingDuration(history, 2000f),
        "maxCoastingDuration40" to calculateMaxCoastingDuration(history, 4000f)
    )
}

fun computeBatchMetrics(history: List<HistoryLocationData>): CalibrationModel {
    val speeds = history.map { it.speed }
    val accels = history.map { it.acceleration }
    val distances = history.map { it.distance }
    val totalDistance = distances.sum()

    val avgSpeed = speeds.average().toFloat()
    val topSpeed = speeds.maxOrNull() ?: 0f
    val avgAccel = accels.map { abs(it) }.average().toFloat()

    val speedVar = if (speeds.isEmpty()) 0f else
        speeds.map { (it - avgSpeed) * (it - avgSpeed) }.average().toFloat()

    val accelVar = if (accels.isEmpty()) 0f else
        accels.map { (abs(it) - avgAccel) * (abs(it) - avgAccel) }.average().toFloat()

    val stopDuration = history.sumOf {
        if (it.speed < 0.5f) {
            val dur = Duration.between(it.start, it.end).toSeconds()
            dur.toDouble()
        } else 0.0
    }.toFloat()

    val totalPoints = history.sumOf { it.points.size }
    val pathComplexity = if (totalDistance > 0f)
        (totalPoints / totalDistance) else 0f

    // Calculate coasting durations
    val coastingMetrics = computeCoastingMetrics(history)
    val maxCoastingDuration05 = coastingMetrics["maxCoastingDuration05"] ?: 0f
    val maxCoastingDuration10 = coastingMetrics["maxCoastingDuration10"] ?: 0f
    val maxCoastingDuration20 = coastingMetrics["maxCoastingDuration20"] ?: 0f
    val maxCoastingDuration40 = coastingMetrics["maxCoastingDuration40"] ?: 0f

    fun expand(value: Float, factor: Float = 1.3f): Pair<Float, Float> {
        val minV = max(0f, value / factor)
        val maxV = value * factor
        return minV to maxV
    }

    val (avgSpeedMin, avgSpeedMax) = expand(avgSpeed)
    val (topSpeedMin, topSpeedMax) = expand(topSpeed)
    val (avgAccelMin, avgAccelMax) = expand(avgAccel)
    val (stopMin, stopMax) = expand(stopDuration)
    val (distMin, distMax) = expand(totalDistance)
    val (speedVarMin, speedVarMax) = expand(speedVar)
    val (accelVarMin, accelVarMax) = expand(accelVar)
    val (pathMin, pathMax) = expand(pathComplexity)
    val (coast05Min, coast05Max) = expand(maxCoastingDuration05)
    val (coast10Min, coast10Max) = expand(maxCoastingDuration10)
    val (coast20Min, coast20Max) = expand(maxCoastingDuration20)
    val (coast40Min, coast40Max) = expand(maxCoastingDuration40)

    return CalibrationModel(
        avgSpeedMin,
        avgSpeedMax,
        topSpeedMin,
        topSpeedMax,
        avgAccelMin,
        avgAccelMax,
        stopMin,
        stopMax,
        distMin,
        distMax,
        speedVarMin,
        speedVarMax,
        accelVarMin,
        accelVarMax,
        pathMin,
        pathMax,

        // Coasting duration ranges
        coast05Min,
        coast05Max,
        coast10Min,
        coast10Max,
        coast20Min,
        coast20Max,
        coast40Min,
        coast40Max,

        // Set ideal values to computed means
        idealAvgSpeed = avgSpeed,
        idealTopSpeed = topSpeed,
        idealAvgAccel = avgAccel,
        idealStopDuration = stopDuration,
        idealDistance = totalDistance,
        idealSpeedVariance = speedVar,
        idealAccelVariance = accelVar,
        idealPathComplexity = pathComplexity,
        idealMaxCoastingDuration05 = maxCoastingDuration05,
        idealMaxCoastingDuration10 = maxCoastingDuration10,
        idealMaxCoastingDuration20 = maxCoastingDuration20,
        idealMaxCoastingDuration40 = maxCoastingDuration40,

        rangeWeight = 0.5f,

        avgSpeedWeight = 1.0f,
        topSpeedWeight = 1.0f,
        avgAccelWeight = 1.0f,
        stopDurationWeight = 1.0f,
        distanceWeight = 1.0f,
        speedVarianceWeight = 1.0f,
        accelVarianceWeight = 1.0f,
        pathComplexityWeight = 1.0f,
        maxCoastingDuration05Weight = 1.0f,
        maxCoastingDuration10Weight = 1.0f,
        maxCoastingDuration20Weight = 1.0f,
        maxCoastingDuration40Weight = 1.0f,

        modeBias = 0.0f,
        confidenceThreshold = 0.3f,
        weight = 1f
    )
}

fun mergeModels(old: CalibrationModel, new: CalibrationModel, weightCap: Float? = null): CalibrationModel {
    val totalW = old.weight + new.weight

    fun wavg(a: Float?, b: Float?): Float? {
        return if (a != null && b != null) {
            (a * old.weight + b * new.weight) / totalW
        } else a ?: b
    }

    fun wavgNonNull(a: Float, b: Float): Float {
        return (a * old.weight + b * new.weight) / totalW
    }

    return CalibrationModel(
        avgSpeedMin = min(wavgNonNull(old.avgSpeedMin, new.avgSpeedMin), old.avgSpeedMin),
        avgSpeedMax = max(wavgNonNull(old.avgSpeedMax, new.avgSpeedMax), old.avgSpeedMax),

        topSpeedMin = min(wavgNonNull(old.topSpeedMin, new.topSpeedMin), old.topSpeedMin),
        topSpeedMax = max(wavgNonNull(old.topSpeedMax, new.topSpeedMax), old.topSpeedMax),

        avgAccelMin = min(wavgNonNull(old.avgAccelMin, new.avgAccelMin), old.avgAccelMin),
        avgAccelMax = max(wavgNonNull(old.avgAccelMax, new.avgAccelMax), old.avgAccelMax),

        stopDurationMin = min(wavgNonNull(old.stopDurationMin, new.stopDurationMin), old.stopDurationMin),
        stopDurationMax = max(wavgNonNull(old.stopDurationMax, new.stopDurationMax), old.stopDurationMax),

        distanceMin = min(wavgNonNull(old.distanceMin, new.distanceMin), old.distanceMin),
        distanceMax = max(wavgNonNull(old.distanceMax, new.distanceMax), old.distanceMax),

        speedVarianceMin = min(wavgNonNull(old.speedVarianceMin, new.speedVarianceMin), old.speedVarianceMin),
        speedVarianceMax = max(wavgNonNull(old.speedVarianceMax, new.speedVarianceMax), old.speedVarianceMax),

        accelVarianceMin = min(wavgNonNull(old.accelVarianceMin, new.accelVarianceMin), old.accelVarianceMin),
        accelVarianceMax = max(wavgNonNull(old.accelVarianceMax, new.accelVarianceMax), old.accelVarianceMax),

        pathComplexityMin = min(wavgNonNull(old.pathComplexityMin, new.pathComplexityMin), old.pathComplexityMin),
        pathComplexityMax = max(wavgNonNull(old.pathComplexityMax, new.pathComplexityMax), old.pathComplexityMax),

        maxCoastingDuration05Min = min(wavgNonNull(old.maxCoastingDuration05Min, new.maxCoastingDuration05Min), old.maxCoastingDuration05Min),
        maxCoastingDuration05Max = max(wavgNonNull(old.maxCoastingDuration05Max, new.maxCoastingDuration05Max), old.maxCoastingDuration05Max),

        maxCoastingDuration10Min = min(wavgNonNull(old.maxCoastingDuration10Min, new.maxCoastingDuration10Min), old.maxCoastingDuration10Min),
        maxCoastingDuration10Max = max(wavgNonNull(old.maxCoastingDuration10Max, new.maxCoastingDuration10Max), old.maxCoastingDuration10Max),

        maxCoastingDuration20Min = min(wavgNonNull(old.maxCoastingDuration20Min, new.maxCoastingDuration20Min), old.maxCoastingDuration20Min),
        maxCoastingDuration20Max = max(wavgNonNull(old.maxCoastingDuration20Max, new.maxCoastingDuration20Max), old.maxCoastingDuration20Max),

        maxCoastingDuration40Min = min(wavgNonNull(old.maxCoastingDuration40Min, new.maxCoastingDuration40Min), old.maxCoastingDuration40Min),
        maxCoastingDuration40Max = max(wavgNonNull(old.maxCoastingDuration40Max, new.maxCoastingDuration40Max), old.maxCoastingDuration40Max),

        idealAvgSpeed = wavg(old.idealAvgSpeed, new.idealAvgSpeed),
        idealTopSpeed = wavg(old.idealTopSpeed, new.idealTopSpeed),
        idealAvgAccel = wavg(old.idealAvgAccel, new.idealAvgAccel),
        idealStopDuration = wavg(old.idealStopDuration, new.idealStopDuration),
        idealDistance = wavg(old.idealDistance, new.idealDistance),
        idealSpeedVariance = wavg(old.idealSpeedVariance, new.idealSpeedVariance),
        idealAccelVariance = wavg(old.idealAccelVariance, new.idealAccelVariance),
        idealPathComplexity = wavg(old.idealPathComplexity, new.idealPathComplexity),
        idealMaxCoastingDuration05 = wavg(old.idealMaxCoastingDuration05, new.idealMaxCoastingDuration05),
        idealMaxCoastingDuration10 = wavg(old.idealMaxCoastingDuration10, new.idealMaxCoastingDuration10),
        idealMaxCoastingDuration20 = wavg(old.idealMaxCoastingDuration20, new.idealMaxCoastingDuration20),
        idealMaxCoastingDuration40 = wavg(old.idealMaxCoastingDuration40, new.idealMaxCoastingDuration40),

        rangeWeight = ((old.rangeWeight ?: 0.5f) + (new.rangeWeight ?: 0.5f)) / 2f,

        avgSpeedWeight = wavgNonNull(old.avgSpeedWeight, new.avgSpeedWeight),
        topSpeedWeight = wavgNonNull(old.topSpeedWeight, new.topSpeedWeight),
        avgAccelWeight = wavgNonNull(old.avgAccelWeight, new.avgAccelWeight),
        stopDurationWeight = wavgNonNull(old.stopDurationWeight, new.stopDurationWeight),
        distanceWeight = wavgNonNull(old.distanceWeight, new.distanceWeight),
        speedVarianceWeight = wavgNonNull(old.speedVarianceWeight, new.speedVarianceWeight),
        accelVarianceWeight = wavgNonNull(old.accelVarianceWeight, new.accelVarianceWeight),
        pathComplexityWeight = wavgNonNull(old.pathComplexityWeight, new.pathComplexityWeight),
        maxCoastingDuration05Weight = wavgNonNull(old.maxCoastingDuration05Weight, new.maxCoastingDuration05Weight),
        maxCoastingDuration10Weight = wavgNonNull(old.maxCoastingDuration10Weight, new.maxCoastingDuration10Weight),
        maxCoastingDuration20Weight = wavgNonNull(old.maxCoastingDuration20Weight, new.maxCoastingDuration20Weight),
        maxCoastingDuration40Weight = wavgNonNull(old.maxCoastingDuration40Weight, new.maxCoastingDuration40Weight),

        modeBias = wavgNonNull(old.modeBias, new.modeBias),
        confidenceThreshold = wavgNonNull(old.confidenceThreshold, new.confidenceThreshold),

        // Use weightCap if set
        weight = if (weightCap != null) min(totalW, weightCap) else totalW
    )
}

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