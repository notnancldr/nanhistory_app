// Learn.kt
package id.my.nanclouder.nanhistory.utils.transportModel

import id.my.nanclouder.nanhistory.utils.HistoryLocationData
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Adaptive Learning System
 * Automatically adjusts feature weights, biases, and confidence thresholds
 * based on prediction performance
 */

data class PerformanceMetrics(
    val mode: TransportMode,
    val totalPredictions: Int = 0,
    val correctPredictions: Int = 0,
    val falsePositives: Int = 0,
    val falseNegatives: Int = 0,
    val precision: Float = 0f,  // TP / (TP + FP)
    val recall: Float = 0f,     // TP / (TP + FN)
    val f1Score: Float = 0f
)

/**
 * Analyzes prediction performance and returns metrics per transport mode
 */
fun analyzePerformance(
    expectedModes: List<TransportMode>,
    detectedModes: List<TransportMode>
): Map<TransportMode, PerformanceMetrics> {
    val metrics = mutableMapOf<TransportMode, PerformanceMetrics>()

    for (mode in TransportMode.entries.filter { it != TransportMode.UNKNOWN }) {
        var tp = 0  // True Positives
        var fp = 0  // False Positives
        var fn = 0  // False Negatives
        var total = 0

        for (i in expectedModes.indices) {
            if (expectedModes[i] == mode) {
                total++
                if (detectedModes[i] == mode) {
                    tp++
                } else {
                    fn++
                }
            } else if (detectedModes[i] == mode) {
                fp++
            }
        }

        val precision = if ((tp + fp) > 0) tp.toFloat() / (tp + fp) else 0f
        val recall = if ((tp + fn) > 0) tp.toFloat() / (tp + fn) else 0f
        val f1 = if ((precision + recall) > 0) {
            2 * (precision * recall) / (precision + recall)
        } else 0f

        metrics[mode] = PerformanceMetrics(
            mode = mode,
            totalPredictions = total,
            correctPredictions = tp,
            falsePositives = fp,
            falseNegatives = fn,
            precision = precision,
            recall = recall,
            f1Score = f1
        )
    }

    return metrics
}

/**
 * Calculates feature importance based on variance analysis
 * Features with high variance contribute more to distinguishing the mode
 */
fun calculateFeatureImportance(
    correctHistories: List<List<HistoryLocationData>>,
    incorrectHistories: List<List<HistoryLocationData>>
): Map<String, Float> {
    fun extractFeatures(histories: List<List<HistoryLocationData>>): Map<String, List<Float>> {
        val speeds = mutableListOf<Float>()
        val topSpeeds = mutableListOf<Float>()
        val accels = mutableListOf<Float>()
        val stopDurations = mutableListOf<Float>()
        val distances = mutableListOf<Float>()
        val speedVars = mutableListOf<Float>()
        val accelVars = mutableListOf<Float>()
        val pathComplexities = mutableListOf<Float>()
        val coasting05s = mutableListOf<Float>()
        val coasting10s = mutableListOf<Float>()
        val coasting20s = mutableListOf<Float>()
        val coasting40s = mutableListOf<Float>()

        histories.forEach { history ->
            if (history.isNotEmpty()) {
                val batchModel = computeBatchMetrics(history)
                speeds.add((batchModel.avgSpeedMin + batchModel.avgSpeedMax) / 2)
                topSpeeds.add((batchModel.topSpeedMin + batchModel.topSpeedMax) / 2)
                accels.add((batchModel.avgAccelMin + batchModel.avgAccelMax) / 2)
                stopDurations.add((batchModel.stopDurationMin + batchModel.stopDurationMax) / 2)
                distances.add((batchModel.distanceMin + batchModel.distanceMax) / 2)
                speedVars.add((batchModel.speedVarianceMin + batchModel.speedVarianceMax) / 2)
                accelVars.add((batchModel.accelVarianceMin + batchModel.accelVarianceMax) / 2)
                pathComplexities.add((batchModel.pathComplexityMin + batchModel.pathComplexityMax) / 2)
                coasting05s.add((batchModel.maxCoastingDuration05Min + batchModel.maxCoastingDuration05Max) / 2)
                coasting10s.add((batchModel.maxCoastingDuration10Min + batchModel.maxCoastingDuration10Max) / 2)
                coasting20s.add((batchModel.maxCoastingDuration20Min + batchModel.maxCoastingDuration20Max) / 2)
                coasting40s.add((batchModel.maxCoastingDuration40Min + batchModel.maxCoastingDuration40Max) / 2)
            }
        }

        return mapOf(
            "avgSpeed" to speeds,
            "topSpeed" to topSpeeds,
            "avgAccel" to accels,
            "stopDuration" to stopDurations,
            "distance" to distances,
            "speedVariance" to speedVars,
            "accelVariance" to accelVars,
            "pathComplexity" to pathComplexities,
            "maxCoastingDuration05" to coasting05s,
            "maxCoastingDuration10" to coasting10s,
            "maxCoastingDuration20" to coasting20s,
            "maxCoastingDuration40" to coasting40s
        )
    }

    fun variance(values: List<Float>): Float {
        if (values.isEmpty()) return 0f
        val mean = values.average()
        return sqrt(values.map { (it - mean) * (it - mean) }.average()).toFloat()
    }

    fun divergence(correct: List<Float>, incorrect: List<Float>): Float {
        if (correct.isEmpty() || incorrect.isEmpty()) return 0f
        val meanCorrect = correct.average()
        val meanIncorrect = incorrect.average()
        return (abs(meanCorrect - meanIncorrect) / (variance(correct) + variance(incorrect) + 0.001f)).toFloat()
    }

    val correctFeatures = extractFeatures(correctHistories)
    val incorrectFeatures = extractFeatures(incorrectHistories)

    return mapOf(
        "avgSpeed" to divergence(correctFeatures["avgSpeed"] ?: emptyList(), incorrectFeatures["avgSpeed"] ?: emptyList()),
        "topSpeed" to divergence(correctFeatures["topSpeed"] ?: emptyList(), incorrectFeatures["topSpeed"] ?: emptyList()),
        "avgAccel" to divergence(correctFeatures["avgAccel"] ?: emptyList(), incorrectFeatures["avgAccel"] ?: emptyList()),
        "stopDuration" to divergence(correctFeatures["stopDuration"] ?: emptyList(), incorrectFeatures["stopDuration"] ?: emptyList()),
        "distance" to divergence(correctFeatures["distance"] ?: emptyList(), incorrectFeatures["distance"] ?: emptyList()),
        "speedVariance" to divergence(correctFeatures["speedVariance"] ?: emptyList(), incorrectFeatures["speedVariance"] ?: emptyList()),
        "accelVariance" to divergence(correctFeatures["accelVariance"] ?: emptyList(), incorrectFeatures["accelVariance"] ?: emptyList()),
        "pathComplexity" to divergence(correctFeatures["pathComplexity"] ?: emptyList(), incorrectFeatures["pathComplexity"] ?: emptyList()),
        "maxCoastingDuration05" to divergence(correctFeatures["maxCoastingDuration05"] ?: emptyList(), incorrectFeatures["maxCoastingDuration05"] ?: emptyList()),
        "maxCoastingDuration10" to divergence(correctFeatures["maxCoastingDuration10"] ?: emptyList(), incorrectFeatures["maxCoastingDuration10"] ?: emptyList()),
        "maxCoastingDuration20" to divergence(correctFeatures["maxCoastingDuration20"] ?: emptyList(), incorrectFeatures["maxCoastingDuration20"] ?: emptyList()),
        "maxCoastingDuration40" to divergence(correctFeatures["maxCoastingDuration40"] ?: emptyList(), incorrectFeatures["maxCoastingDuration40"] ?: emptyList())
    )
}

/**
 * Adaptively adjusts model parameters based on performance
 */
fun adaptModelParameters(
    model: CalibrationModel,
    metrics: PerformanceMetrics,
    featureImportance: Map<String, Float>,
    learningRate: Float = 0.1f
): CalibrationModel {
    // Adjust feature weights based on importance
    val normalizedImportance = featureImportance.values.maxOrNull()?.let { max ->
        if (max > 0) featureImportance.mapValues { (it.value / max).coerceIn(0.3f, 2.0f) } else featureImportance
    } ?: featureImportance

    val newAvgSpeedWeight = model.avgSpeedWeight + ((normalizedImportance["avgSpeed"] ?: 1f) - 1f) * learningRate
    val newTopSpeedWeight = model.topSpeedWeight + ((normalizedImportance["topSpeed"] ?: 1f) - 1f) * learningRate
    val newAvgAccelWeight = model.avgAccelWeight + ((normalizedImportance["avgAccel"] ?: 1f) - 1f) * learningRate
    val newStopDurationWeight = model.stopDurationWeight + ((normalizedImportance["stopDuration"] ?: 1f) - 1f) * learningRate
    val newDistanceWeight = model.distanceWeight + ((normalizedImportance["distance"] ?: 1f) - 1f) * learningRate
    val newSpeedVarianceWeight = model.speedVarianceWeight + ((normalizedImportance["speedVariance"] ?: 1f) - 1f) * learningRate
    val newAccelVarianceWeight = model.accelVarianceWeight + ((normalizedImportance["accelVariance"] ?: 1f) - 1f) * learningRate
    val newPathComplexityWeight = model.pathComplexityWeight + ((normalizedImportance["pathComplexity"] ?: 1f) - 1f) * learningRate
    val newMaxCoastingDuration05Weight = model.maxCoastingDuration05Weight + ((normalizedImportance["maxCoastingDuration05"] ?: 1f) - 1f) * learningRate
    val newMaxCoastingDuration10Weight = model.maxCoastingDuration10Weight + ((normalizedImportance["maxCoastingDuration10"] ?: 1f) - 1f) * learningRate
    val newMaxCoastingDuration20Weight = model.maxCoastingDuration20Weight + ((normalizedImportance["maxCoastingDuration20"] ?: 1f) - 1f) * learningRate
    val newMaxCoastingDuration40Weight = model.maxCoastingDuration40Weight + ((normalizedImportance["maxCoastingDuration40"] ?: 1f) - 1f) * learningRate

    // Adjust mode bias based on false positive/negative rate
    val fpratio = if (metrics.totalPredictions > 0) {
        metrics.falsePositives.toFloat() / metrics.totalPredictions
    } else 0f

    val fnratio = if (metrics.totalPredictions > 0) {
        metrics.falseNegatives.toFloat() / metrics.totalPredictions
    } else 0f

    val biasDelta = (fpratio - fnratio) * learningRate * 0.5f
    val newModeBias = (model.modeBias - biasDelta).coerceIn(-0.5f, 0.5f)

    // Adjust confidence threshold based on precision-recall tradeoff
    // Increase threshold if precision is low (too many false positives)
    // Decrease threshold if recall is low (too many false negatives)
    val thresholdDelta = if (metrics.precision < 0.5f) {
        0.05f  // Increase threshold, be more conservative
    } else if (metrics.recall < 0.5f) {
        -0.05f // Decrease threshold, be more inclusive
    } else {
        0f
    }

    val newConfidenceThreshold = (model.confidenceThreshold + thresholdDelta).coerceIn(0.1f, 0.9f)

    return model.copy(
        avgSpeedWeight = newAvgSpeedWeight.coerceIn(0.1f, 2.0f),
        topSpeedWeight = newTopSpeedWeight.coerceIn(0.1f, 2.0f),
        avgAccelWeight = newAvgAccelWeight.coerceIn(0.1f, 2.0f),
        stopDurationWeight = newStopDurationWeight.coerceIn(0.1f, 2.0f),
        distanceWeight = newDistanceWeight.coerceIn(0.1f, 2.0f),
        speedVarianceWeight = newSpeedVarianceWeight.coerceIn(0.1f, 2.0f),
        accelVarianceWeight = newAccelVarianceWeight.coerceIn(0.1f, 2.0f),
        pathComplexityWeight = newPathComplexityWeight.coerceIn(0.1f, 2.0f),
        maxCoastingDuration05Weight = newMaxCoastingDuration05Weight.coerceIn(0.1f, 2.0f),
        maxCoastingDuration10Weight = newMaxCoastingDuration10Weight.coerceIn(0.1f, 2.0f),
        maxCoastingDuration20Weight = newMaxCoastingDuration20Weight.coerceIn(0.1f, 2.0f),
        maxCoastingDuration40Weight = newMaxCoastingDuration40Weight.coerceIn(0.1f, 2.0f),
        modeBias = newModeBias,
        confidenceThreshold = newConfidenceThreshold
    )
}

/**
 * Full adaptive learning process
 * Should be called periodically (e.g., every 50 iterations)
 */
fun performAdaptiveLearning(
    models: Map<TransportMode, CalibrationModel>,
    trainingData: List<Pair<TransportMode, List<HistoryLocationData>>>,
    strategy: ScoringStrategy,
    learningRate: Float = 0.05f
): Map<TransportMode, CalibrationModel> {
    if (trainingData.isEmpty()) return models

    // Get expected and detected modes
    val expectedModes = mutableListOf<TransportMode>()
    val detectedModes = mutableListOf<TransportMode>()

    trainingData.forEach { (expected, history) ->
        if (history.isNotEmpty()) {
            expectedModes.add(expected)
            detectedModes.add(detectTransportMode(history, models, strategy))
        }
    }

    // Analyze performance per mode
    val performanceMetrics = analyzePerformance(expectedModes, detectedModes)

    // Adapt each mode's parameters
    val adaptedModels = mutableMapOf<TransportMode, CalibrationModel>()

    for ((mode, metrics) in performanceMetrics) {
        val model = models[mode] ?: continue

        // Collect correct and incorrect histories for this mode
        val correctHistories = mutableListOf<List<HistoryLocationData>>()
        val incorrectHistories = mutableListOf<List<HistoryLocationData>>()

        trainingData.forEach { (expected, history) ->
            if (expected == mode && history.isNotEmpty()) {
                val detected = detectTransportMode(history, models, strategy)
                if (detected == expected) {
                    correctHistories.add(history)
                } else {
                    incorrectHistories.add(history)
                }
            }
        }

        // Calculate feature importance
        val featureImportance = if (correctHistories.isNotEmpty() && incorrectHistories.isNotEmpty()) {
            calculateFeatureImportance(correctHistories, incorrectHistories)
        } else {
            emptyMap()
        }

        // Adapt model based on performance and feature importance
        adaptedModels[mode] = adaptModelParameters(model, metrics, featureImportance, learningRate)
    }

    // Return adapted models, keeping any models not in adapted list
    return models.toMutableMap().apply {
        putAll(adaptedModels)
    }
}