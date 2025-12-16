// ModelManager.kt
package id.my.nanclouder.nanhistory.utils.transportModel

import android.content.Context
import android.content.ClipboardManager
import android.content.ClipData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class SavedModel(
    val name: String,
    val timestamp: String,
    val accuracy: Float,
    val totalSamples: Int,
    val correctSamples: Int,
    val strategy: ScoringStrategy,
    val models: Map<TransportMode, CalibrationModel>
)

object ModelManager {
    private const val MODELS_DIR = "saved_models"
    private const val METADATA_FILE = "model_metadata.json"

    private fun getModelsDirectory(context: Context): File {
        val dir = File(context.filesDir, MODELS_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun getMetadataFile(context: Context): File {
        return File(getModelsDirectory(context), METADATA_FILE)
    }

    /**
     * Save current models with a name and accuracy snapshot
     */
    fun saveModel(
        context: Context,
        name: String,
        accuracy: Float,
        totalSamples: Int,
        correctSamples: Int,
        strategy: ScoringStrategy,
        models: Map<TransportMode, CalibrationModel>
    ): Boolean {
        return try {
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

            val savedModel = SavedModel(
                name = name,
                timestamp = timestamp,
                accuracy = accuracy,
                totalSamples = totalSamples,
                correctSamples = correctSamples,
                strategy = strategy,
                models = models
            )

            // Save model data
            val modelFile = File(getModelsDirectory(context), "${name}_model.json")
            val jsonMap = savedModel.models.mapKeys { it.key.name }
            modelFile.writeText(Gson().toJson(jsonMap))

            // Update metadata
            val existingMetadata = loadAllModelsMetadata(context).toMutableMap()
            existingMetadata[name] = SavedModelMetadata(
                name = name,
                timestamp = timestamp,
                accuracy = accuracy,
                totalSamples = totalSamples,
                correctSamples = correctSamples,
                strategy = strategy
            )

            val metadataFile = getMetadataFile(context)
            metadataFile.writeText(Gson().toJson(existingMetadata))

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Load a saved model by name
     */
    fun loadModel(context: Context, name: String): SavedModel? {
        return try {
            val modelFile = File(getModelsDirectory(context), "${name}_model.json")
            if (!modelFile.exists()) return null

            val metadata = loadAllModelsMetadata(context)[name] ?: return null

            val type = object : TypeToken<Map<String, CalibrationModel>>() {}.type
            val jsonMap: Map<String, CalibrationModel> = Gson().fromJson(modelFile.readText(), type)
            val models = jsonMap.mapKeys { TransportMode.valueOf(it.key) }

            SavedModel(
                name = metadata.name,
                timestamp = metadata.timestamp,
                accuracy = metadata.accuracy,
                totalSamples = metadata.totalSamples,
                correctSamples = metadata.correctSamples,
                strategy = metadata.strategy,
                models = models
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Delete a saved model
     */
    fun deleteModel(context: Context, name: String): Boolean {
        return try {
            val modelFile = File(getModelsDirectory(context), "${name}_model.json")
            val deleted = modelFile.delete()

            if (deleted) {
                val existingMetadata = loadAllModelsMetadata(context).toMutableMap()
                existingMetadata.remove(name)
                getMetadataFile(context).writeText(Gson().toJson(existingMetadata))
            }

            deleted
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Get all saved models metadata
     */
    fun loadAllModelsMetadata(context: Context): Map<String, SavedModelMetadata> {
        return try {
            val metadataFile = getMetadataFile(context)
            if (!metadataFile.exists()) return emptyMap()

            val type = object : TypeToken<Map<String, SavedModelMetadata>>() {}.type
            Gson().fromJson(metadataFile.readText(), type) ?: emptyMap()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyMap()
        }
    }

    /**
     * Get list of all saved model names sorted by accuracy
     */
    fun getSavedModelsSortedByAccuracy(context: Context): List<SavedModelMetadata> {
        return loadAllModelsMetadata(context)
            .values
            .sortedByDescending { it.accuracy }
    }

    /**
     * Copy model metadata to clipboard
     */
    fun copyModelMetadataToClipboard(context: Context, name: String): Boolean {
        return try {
            val metadataFile = getMetadataFile(context)
            if (!metadataFile.exists()) return false

            val metadata = loadAllModelsMetadata(context)[name] ?: return false
            val jsonString = Gson().toJson(metadata)

            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Model Metadata", jsonString)
            clipboard.setPrimaryClip(clip)

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Copy entire model (including data) to clipboard
     */
    fun copyFullModelToClipboard(context: Context, name: String): Boolean {
        return try {
            val modelFile = File(getModelsDirectory(context), "${name}_model.json")
            if (!modelFile.exists()) return false

            val metadata = loadAllModelsMetadata(context)[name] ?: return false
            val modelJson = modelFile.readText()

            val fullModelData = mapOf(
                "metadata" to metadata,
                "models" to Gson().fromJson<Map<String, Any>>(modelJson, Map::class.java)
            )
            val jsonString = Gson().toJson(fullModelData)

            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Full Model Data", jsonString)
            clipboard.setPrimaryClip(clip)

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

data class SavedModelMetadata(
    val name: String,
    val timestamp: String,
    val accuracy: Float,
    val totalSamples: Int,
    val correctSamples: Int,
    val strategy: ScoringStrategy
)