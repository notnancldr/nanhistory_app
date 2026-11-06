package id.my.nanclouder.nanhistory.domain.logic.transport

import id.my.nanclouder.nanhistory.domain.model.HistoryLocationData
import java.time.Duration
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

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


