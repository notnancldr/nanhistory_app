package id.my.nanclouder.nanhistory.utils.transportModel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import android.content.Context
import android.util.Log
import android.widget.Toast
import id.my.nanclouder.nanhistory.utils.HistoryLocationData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Instant
import kotlin.math.roundToInt

data class TrainingState(
    val isRunning: Boolean = false,
    val currentAccuracy: Float = 0f,
    val totalSamples: Int = 0,
    val correctSamples: Int = 0,
    val iteration: Int = 0,
    val accuracyHistory: List<Float> = emptyList(),
    val totalSamplesPerMode: Map<TransportMode, Int> = emptyMap(),
    val models: Map<TransportMode, CalibrationModel> = emptyMap(),
    val lastUpdateTime: Long = System.currentTimeMillis(),
    val strategy: ScoringStrategy = ScoringStrategy.RANGE_BASED,
    val trainingDuration: Long = 0L  // in milliseconds
)

class TransportModelTrainer(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var trainingJob: Job? = null
    private var trainingStartTime: Long = 0L
    private var allTrainingData: List<Pair<TransportMode, List<HistoryLocationData>>> = emptyList()
    private var bufferedTrainingData: MutableList<Pair<TransportMode, List<HistoryLocationData>>> = mutableListOf()

    private var stateListener: ((TrainingState) -> Unit)? = null

    fun setStateListener(listener: (TrainingState) -> Unit) {
        stateListener = listener
    }

    fun startTraining(
        strategy: ScoringStrategy = ScoringStrategy.RANGE_BASED,
        existingModels: Map<TransportMode, CalibrationModel> = emptyMap(),
        weightCap: Float? = null,
        targetAccuracy: Int? = null,
        learningRate: Float = 0.05f,
        bufferSize: Int = 8,
        cleanCache: Boolean = true,
    ) {
        if (trainingJob?.isActive == true) return

        var currentModels = existingModels.ifEmpty { loadCalibrationModels(context) }
        var iteration = 0
        var accuracyHistory = mutableListOf<Float>()

        trainingStartTime = System.currentTimeMillis()

        stateListener?.invoke(
            TrainingState(
                isRunning = true,
                currentAccuracy = 0f,
                totalSamples = 0,
                correctSamples = 0,
                iteration = iteration,
                accuracyHistory = accuracyHistory.toList(),
                models = currentModels,
                lastUpdateTime = System.currentTimeMillis(),
                strategy = strategy,
                trainingDuration = 0,
            )
        )

        trainingJob = scope.launch(Dispatchers.Default) {
            // Get all training data once, reuse it in one training session

            val t0Load = Instant.now()
            if (cleanCache) {
                clearTrainingDataCache(context)
                allTrainingData = getTrainingData(context)
            }
            else {
                allTrainingData = allTrainingData.ifEmpty {
                    TrainingDataCache.cache ?: getTrainingDataFromCache(context) ?: getTrainingData(context)
                }
            }
            val t1Load = Instant.now()
            val loadTime = t1Load.toEpochMilli() - t0Load.toEpochMilli()

            scope.launch(Dispatchers.Main) {
                Toast.makeText(context, "Loaded training data in ${loadTime}ms", Toast.LENGTH_SHORT)
                    .show()
            }

            val samplesPerMode = allTrainingData.groupBy { it.first }.map { it.key to it.value.size }.toMap()
            val bufferedModels = mutableMapOf<TransportMode,MutableList<CalibrationModel>>()

            while (isActive) {
                try {
                    // Load fresh training data on each iteration
                    if (allTrainingData.isEmpty()) {
                        return@launch
                    }

                    // Refill buffer when it all has been used for training
                    // (Or should I say continue to next pass)
                    if (bufferedTrainingData.isEmpty()) {
                        bufferedTrainingData.addAll(allTrainingData)
                    }

                    // List all found modes
                    val availableModes = bufferedTrainingData.map { it.first }.toSet().toList()

                    // Pick next transport mode
                    val randomMode = availableModes[iteration % availableModes.size]

                    // Get all data for this transport mode
                    val modeDataList = bufferedTrainingData.filter { it.first == randomMode }
                    if (modeDataList.isEmpty()) {
                        iteration++
                        delay(50)
                        throw Throwable()
                    }

                    // Pick random sample from this transport mode
                    val (mode, history) = modeDataList.random()

                    if (history.isNotEmpty()) {
                        // Train on this single sample
                        val batchModel = computeBatchMetrics(history)
                        val prev = currentModels[mode]

                        // Initiate new list for current model if doesn't exist yet
                        if (bufferedModels.getOrDefault(mode, null) == null) {
                            bufferedModels[mode] = mutableListOf()
                        }

                        // Add new model to the buffer
                        bufferedModels[mode]!!.add(batchModel)

                        if (bufferedModels[mode]!!.size >= bufferSize || (iteration % bufferSize == 0 && iteration > 0)) {
                            // Average last [bufferSize] models
                            val avgModel = bufferedModels[mode]!!.reduce { acc, model ->
                                mergeModels(acc, model, weightCap)
                            }

                            // merge average model with previous model
                            val merged = if (prev != null) {
                                mergeModels(prev, avgModel, weightCap)
                            } else {
                                avgModel
                            }

                            // Update current model
                            currentModels =
                                currentModels.toMutableMap().apply { this[mode] = merged }
                            bufferedModels[mode]!!.clear()
                        }
                    }

                    // Remove data that has been used for training from the buffer
                    bufferedTrainingData.removeAll(modeDataList)

                    // Evaluate every 8 iterations to avoid performance hit
                    if (iteration % 8 == 0 && iteration > 0) {
                        val evalResult = evaluate(currentModels, allTrainingData, strategy)
                        accuracyHistory.add(evalResult.accuracy)

                        val accPercentage = evalResult.accuracy * 100

                        // Stop when the target reached
                        Log.d("TransportModeDetection", "Evaluate training | acc: ${accPercentage.roundToInt()}, target: $targetAccuracy")
                        if (targetAccuracy != null && accPercentage.roundToInt() >= targetAccuracy) {
                            val trainingDuration = System.currentTimeMillis() - trainingStartTime
                            // saveCalibrationModels(context, currentModels)
                            stateListener?.invoke(
                                TrainingState(
                                    isRunning = false,
                                    currentAccuracy = evalResult.accuracy,
                                    totalSamples = evalResult.totalSamples,
                                    correctSamples = evalResult.correctSamples,
                                    iteration = iteration,
                                    accuracyHistory = accuracyHistory.toList(),
                                    models = currentModels,
                                    lastUpdateTime = System.currentTimeMillis(),
                                    strategy = strategy,
                                    trainingDuration = trainingDuration,
                                    totalSamplesPerMode = samplesPerMode
                                )
                            )
                            Log.d("TransportModeDetection", "Stopping training | acc >= target")
                            trainingJob?.cancel()
                            trainingJob = null
                            return@launch
                        }

                        // Keep only last 100 accuracy values for the graph
                        if (accuracyHistory.size > 100) {
                            accuracyHistory = accuracyHistory.takeLast(100).toMutableList()
                        }

                        // Perform adaptive learning every [bufferSize * 2] iterations
                        if (iteration % (bufferSize * 8) == 0) {
                            currentModels = performAdaptiveLearning(
                                currentModels,
                                allTrainingData,
                                strategy,
                                learningRate = learningRate
                            )
                        }

                        val trainingDuration = System.currentTimeMillis() - trainingStartTime

                        if (!isActive) return@launch
                        stateListener?.invoke(
                            TrainingState(
                                isRunning = true,
                                currentAccuracy = evalResult.accuracy,
                                totalSamples = evalResult.totalSamples,
                                correctSamples = evalResult.correctSamples,
                                iteration = iteration,
                                accuracyHistory = accuracyHistory.toList(),
                                models = currentModels,
                                lastUpdateTime = System.currentTimeMillis(),
                                strategy = strategy,
                                trainingDuration = trainingDuration,
                                totalSamplesPerMode = samplesPerMode
                            )
                        )

                        // saveCalibrationModels(context, currentModels)
                    }

                    iteration++
                    // Small delay to prevent CPU overload
                    delay(50)

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Final save when stopped
            // saveCalibrationModels(context, currentModels)
        }
    }

    fun stopTraining() {
        trainingJob?.cancel()
        trainingJob = null
    }

    fun isTraining(): Boolean = trainingJob?.isActive == true
}