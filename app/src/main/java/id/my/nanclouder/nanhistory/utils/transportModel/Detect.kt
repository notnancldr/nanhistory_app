// Detect.kt
package id.my.nanclouder.nanhistory.utils.transportModel

import id.my.nanclouder.nanhistory.utils.Coordinate
import id.my.nanclouder.nanhistory.utils.HistoryLocationData
import id.my.nanclouder.nanhistory.utils.history.TransportationType
import java.time.Duration
import kotlin.math.abs

enum class TransportMode {
    STILL, WALKING, BICYCLE, MOTORCYCLE, CAR, TRAIN, AIRPLANE, UNKNOWN
}

fun TransportMode.toTransportationType() = when(this) {
    TransportMode.STILL, TransportMode.UNKNOWN -> TransportationType.Unspecified
    TransportMode.WALKING -> TransportationType.Walk
    TransportMode.BICYCLE -> TransportationType.Bicycle
    TransportMode.MOTORCYCLE -> TransportationType.Motorcycle
    TransportMode.CAR -> TransportationType.Car
    TransportMode.TRAIN -> TransportationType.Train
    TransportMode.AIRPLANE -> TransportationType.Airplane
}

fun TransportationType.toTransportMode() = when(this) {
    TransportationType.Unspecified, TransportationType.Ferry -> TransportMode.UNKNOWN
    TransportationType.Walk -> TransportMode.WALKING
    TransportationType.Bicycle -> TransportMode.BICYCLE
    TransportationType.Motorcycle -> TransportMode.MOTORCYCLE
    TransportationType.Car -> TransportMode.CAR
    TransportationType.Train -> TransportMode.TRAIN
    TransportationType.Airplane -> TransportMode.AIRPLANE
}

// Scoring strategy enum
enum class ScoringStrategy {
    RANGE_BASED,      // Original: score 1.0 if in range, penalty if out
    IDEAL_BASED,      // New: score based on distance from ideal value(s)
    COMBINED           // Use both strategies with weights
}

fun getScore(
    history: List<HistoryLocationData>,
    mode: TransportMode,
    modelMap: Map<TransportMode, CalibrationModel>,
    strategy: ScoringStrategy = ScoringStrategy.RANGE_BASED
): Float {
    val model = modelMap[mode] ?: return 0f
    if (history.isEmpty()) return 0f

    // Extract primary samples
    val speeds = history.map { it.speed }
    val accels = history.map { it.acceleration }
    val distances = history.map { it.distance }

    val avgSpeed = speeds.average().toFloat()
    val topSpeed = (speeds.maxOrNull() ?: 0f)
    val avgAccel = accels.map { abs(it) }.average().toFloat()

    val totalDistance = distances.sum()

    val speedVar = speeds.let {
        if (it.isEmpty()) 0f else it.map { s -> (s - avgSpeed) * (s - avgSpeed) }.average().toFloat()
    }

    val accelVar = accels.let {
        if (it.isEmpty()) 0f else it.map { a -> (abs(a) - avgAccel) * (abs(a) - avgAccel) }.average().toFloat()
    }

    val stopDuration = history.sumOf {
        if (it.speed < 0.5f) {
            val dur = Duration.between(it.start, it.end).toSeconds()
            dur.toDouble()
        } else 0.0
    }.toFloat()

    val totalPoints = history.sumOf { it.points.size }
    val pathComplexity = if (totalDistance > 0f) {
        (totalPoints / totalDistance)
    } else 0f

    return when (strategy) {
        ScoringStrategy.RANGE_BASED -> scoreRangeBased(
            avgSpeed, topSpeed, avgAccel, stopDuration, totalDistance,
            speedVar, accelVar, pathComplexity, model
        )
        ScoringStrategy.IDEAL_BASED -> scoreIdealBased(
            avgSpeed, topSpeed, avgAccel, stopDuration, totalDistance,
            speedVar, accelVar, pathComplexity, model
        )
        ScoringStrategy.COMBINED -> {
            val rangeBased = scoreRangeBased(
                avgSpeed, topSpeed, avgAccel, stopDuration, totalDistance,
                speedVar, accelVar, pathComplexity, model
            )
            val idealBased = scoreIdealBased(
                avgSpeed, topSpeed, avgAccel, stopDuration, totalDistance,
                speedVar, accelVar, pathComplexity, model
            )
            (rangeBased * (model.rangeWeight ?: 0.5f) +
                    idealBased * (1f - (model.rangeWeight ?: 0.5f)))
        }
    }
}

private fun scoreRangeBased(
    avgSpeed: Float,
    topSpeed: Float,
    avgAccel: Float,
    stopDuration: Float,
    totalDistance: Float,
    speedVar: Float,
    accelVar: Float,
    pathComplexity: Float,
    model: CalibrationModel
): Float {
    fun scoreRange(value: Float, min: Float, max: Float): Float {
        if (max <= min) return 0f
        return when {
            value < min -> (value / min).coerceIn(0f, 1f)
            value > max -> ((max / value).coerceIn(0f, 1f))
            else -> 1f
        }
    }

    // Score each feature
    val scoreSpeed = scoreRange(avgSpeed, model.avgSpeedMin, model.avgSpeedMax)
    val scoreTopSpeed = scoreRange(topSpeed, model.topSpeedMin, model.topSpeedMax)
    val scoreAccel = scoreRange(avgAccel, model.avgAccelMin, model.avgAccelMax)
    val scoreStop = scoreRange(stopDuration, model.stopDurationMin, model.stopDurationMax)
    val scoreDistance = scoreRange(totalDistance, model.distanceMin, model.distanceMax)
    val scoreSpeedVar = scoreRange(speedVar, model.speedVarianceMin, model.speedVarianceMax)
    val scoreAccelVar = scoreRange(accelVar, model.accelVarianceMin, model.accelVarianceMax)
    val scorePath = scoreRange(pathComplexity, model.pathComplexityMin, model.pathComplexityMax)

    // Apply feature importance weights
    val weightedSum = (
            scoreSpeed * model.avgSpeedWeight +
                    scoreTopSpeed * model.topSpeedWeight +
                    scoreAccel * model.avgAccelWeight +
                    scoreStop * model.stopDurationWeight +
                    scoreDistance * model.distanceWeight +
                    scoreSpeedVar * model.speedVarianceWeight +
                    scoreAccelVar * model.accelVarianceWeight +
                    scorePath * model.pathComplexityWeight
            )

    val totalWeight = (
            model.avgSpeedWeight +
                    model.topSpeedWeight +
                    model.avgAccelWeight +
                    model.stopDurationWeight +
                    model.distanceWeight +
                    model.speedVarianceWeight +
                    model.accelVarianceWeight +
                    model.pathComplexityWeight
            )

    val combined = weightedSum / totalWeight

    // Apply per-mode bias and model weight
    return (combined + model.modeBias).coerceIn(0f, 1f) * model.weight
}

private fun scoreIdealBased(
    avgSpeed: Float,
    topSpeed: Float,
    avgAccel: Float,
    stopDuration: Float,
    totalDistance: Float,
    speedVar: Float,
    accelVar: Float,
    pathComplexity: Float,
    model: CalibrationModel
): Float {
    fun gaussianScore(value: Float, ideal: Float, stdDev: Float): Float {
        if (stdDev <= 0f) return 0f
        val diff = abs(value - ideal) / stdDev
        return (Math.exp(-(diff * diff / 2.0))).toFloat()
    }

    val scoreSpeed = gaussianScore(avgSpeed, model.idealAvgSpeed ?: model.avgSpeedMin,
        (model.avgSpeedMax - model.avgSpeedMin) / 4f)
    val scoreTopSpeed = gaussianScore(topSpeed, model.idealTopSpeed ?: model.topSpeedMin,
        (model.topSpeedMax - model.topSpeedMin) / 4f)
    val scoreAccel = gaussianScore(avgAccel, model.idealAvgAccel ?: model.avgAccelMin,
        (model.avgAccelMax - model.avgAccelMin) / 4f)
    val scoreStop = gaussianScore(stopDuration, model.idealStopDuration ?: model.stopDurationMin,
        (model.stopDurationMax - model.stopDurationMin) / 4f)
    val scoreDistance = gaussianScore(totalDistance, model.idealDistance ?: model.distanceMin,
        (model.distanceMax - model.distanceMin) / 4f)
    val scoreSpeedVar = gaussianScore(speedVar, model.idealSpeedVariance ?: model.speedVarianceMin,
        (model.speedVarianceMax - model.speedVarianceMin) / 4f)
    val scoreAccelVar = gaussianScore(accelVar, model.idealAccelVariance ?: model.accelVarianceMin,
        (model.accelVarianceMax - model.accelVarianceMin) / 4f)
    val scorePath = gaussianScore(pathComplexity, model.idealPathComplexity ?: model.pathComplexityMin,
        (model.pathComplexityMax - model.pathComplexityMin) / 4f)

    // Apply feature importance weights
    val weightedSum = (
            scoreSpeed * model.avgSpeedWeight +
                    scoreTopSpeed * model.topSpeedWeight +
                    scoreAccel * model.avgAccelWeight +
                    scoreStop * model.stopDurationWeight +
                    scoreDistance * model.distanceWeight +
                    scoreSpeedVar * model.speedVarianceWeight +
                    scoreAccelVar * model.accelVarianceWeight +
                    scorePath * model.pathComplexityWeight
            )

    val totalWeight = (
            model.avgSpeedWeight +
                    model.topSpeedWeight +
                    model.avgAccelWeight +
                    model.stopDurationWeight +
                    model.distanceWeight +
                    model.speedVarianceWeight +
                    model.accelVarianceWeight +
                    model.pathComplexityWeight
            )

    val combined = weightedSum / totalWeight

    // Apply per-mode bias and model weight
    return (combined + model.modeBias).coerceIn(0f, 1f) * model.weight
}

fun detectTransportMode(
    history: List<HistoryLocationData>,
    modelMap: Map<TransportMode, CalibrationModel>,
    strategy: ScoringStrategy = ScoringStrategy.RANGE_BASED
): TransportMode {
    if (history.isEmpty()) return TransportMode.UNKNOWN

    var bestMode = TransportMode.UNKNOWN
    var bestScore = -1f

    for ((mode, model) in modelMap) {
        val score = getScore(history, mode, modelMap, strategy)

        // Check confidence threshold - if below threshold, skip this mode
        if (score < model.confidenceThreshold) {
            continue
        }

        if (score > bestScore) {
            bestScore = score
            bestMode = mode
        }
    }

    // If no mode passed confidence threshold, return UNKNOWN
    if (bestMode == TransportMode.UNKNOWN && bestScore == -1f) {
        return TransportMode.UNKNOWN
    }

    return bestMode
}