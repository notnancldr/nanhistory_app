package id.my.nanclouder.nanhistory.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import id.my.nanclouder.nanhistory.BuildConfig
import id.my.nanclouder.nanhistory.db.AppDatabase
import id.my.nanclouder.nanhistory.db.EventEntity
import id.my.nanclouder.nanhistory.db.toDayEntity
import id.my.nanclouder.nanhistory.db.toEventEntity
import id.my.nanclouder.nanhistory.db.toHistoryDay
import id.my.nanclouder.nanhistory.db.toHistoryEvent
import id.my.nanclouder.nanhistory.utils.history.EventPoint
import id.my.nanclouder.nanhistory.utils.history.EventRange
import id.my.nanclouder.nanhistory.utils.history.HistoryDay
import id.my.nanclouder.nanhistory.utils.history.createLocationFile
import id.my.nanclouder.nanhistory.utils.history.generateSignature
import id.my.nanclouder.nanhistory.utils.history.safeDelete
import id.my.nanclouder.nanhistory.utils.history.toLocationPath
import id.my.nanclouder.nanhistory.utils.history.writeToLocationFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object LegacyImport {
    private const val LEGACY_ENCRYPTION_KEY = BuildConfig.LEGACY_ENCRYPTION_KEY
    private var logData: LogData? = null

    private val logTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

    object State {
        /**
         * @return `true` if the import is running or `false` otherwise.
         */
        var isRunning = MutableStateFlow(false)

        /**
         * @return `float` progress in range 0..1
         */
        var progress = MutableStateFlow(0f)
    }

    private fun logProcess(context: Context, message: String) {
        val formattedTime = LocalDateTime.now().format(logTimeFormatter)
        logData?.append("$formattedTime    $message")
        logData?.save(context)
    }

    private fun logError(context: Context, throwable: Throwable) {
        val formattedTime = LocalDateTime.now().format(logTimeFormatter)
        logData?.append("\n$formattedTime    =========================================================")
        logData?.append(throwable.message ?: "Unknown error")
        logData?.append(throwable.stackTraceToString())
        logData?.save(context)
    }

    private fun logWarning(context: Context, throwable: Throwable) {
        val formattedTime = LocalDateTime.now().format(logTimeFormatter)
        logData?.append("\n$formattedTime    WARNING!!!")
        logData?.append(throwable.message ?: "Unknown error")
        logData?.append(throwable.stackTraceToString())
        logData?.save(context)
    }

    private fun getLegacySignature(legacyMapped: Map<String, Any?>): String {
        val type = legacyMapped["type"]
        val dataList = when (type) {
            "point" -> listOf(
                (legacyMapped["id"] as? Double)?.toLong(),
                (legacyMapped["time"] as? Double)?.toLong(),
                0,
                legacyMapped["geotag"]
            )
            "range" -> listOf(
                (legacyMapped["id"] as? Double)?.toLong(),
                1,
                (legacyMapped["time"] as? Double)?.toLong(),
                (legacyMapped["until"] as? Double)?.toLong(),
                legacyMapped["geotag"],
                legacyMapped["geotagEnd"],
                legacyMapped["geotagPoints"]
            )
            "location_name" -> listOf(
                (legacyMapped["id"] as? Double)?.toLong(),
                (legacyMapped["time"] as? Double)?.toLong(),
                2,
                legacyMapped["geotag"],
                legacyMapped["locationName"]
            )
            else -> emptyList()
        }

        val signatureData = mutableListOf<Any>();
        for (entry in dataList) {
            if (entry == null) continue;
            signatureData.add(entry);
        }

        val data = try {
            Gson().toJson(signatureData)
        } catch (_: Throwable) { "" }

        // Log.d("LegacyImport", "Event JSON for signature check: $data")

        val md = MessageDigest.getInstance("SHA-256")
        val bytes = data.toByteArray(Charsets.UTF_8)
        val hash = md.digest(bytes).joinToString("") { "%02x".format(it) }
        return hash
    }

    private fun verifyLegacySignature(legacyMapped: Map<String, Any?>, signature: String): Boolean =
        getLegacySignature(legacyMapped) == signature

    private fun importEvent(context: Context, legacyMapped: Map<String, Any?>): EventEntity? {
        val zoneId = ZoneId.systemDefault()
        val now = ZonedDateTime.now()

        // Log.d("LegacyImport", "Importing event: $legacyMapped")
        logProcess(context, "Importing event: ${legacyMapped["name"]}")

        val event = when (val legacyType = legacyMapped["type"] as? String) {
            "point", "location_name" -> {
                val legacyId = (legacyMapped["id"] as? Double)?.toLong() ?: Instant.now().toEpochMilli()
                val newId = legacyId.toString()

                val legacyLocationName = legacyMapped["locationName"]

                EventPoint(
                    id = newId,
                    title = legacyMapped["name"] as? String ?: "",
                    description = legacyMapped["description"] as? String ?: "",
                    time = (legacyMapped["time"] as? Double)?.let {
                        try {
                            ZonedDateTime.ofInstant(Instant.ofEpochMilli(it.toLong()), zoneId)
                        } catch (e: Throwable) {
                            logWarning(context, e)
                            null
                        }
                    } ?: ZonedDateTime.ofInstant(Instant.ofEpochMilli(0), zoneId),
                    created = ZonedDateTime.ofInstant(Instant.ofEpochMilli(legacyId), zoneId),
                    modified = now,
                    metadata = mutableMapOf<String, Any>(
                        "legacy_type" to legacyType,
                        "legacy_id" to legacyId.toString(),
                        "converted_from_legacy" to now.toString()
                    ).apply {
                        if (legacyLocationName is String) {
                            put("legacy_locationName", legacyLocationName)
                        }
                    }
                ).apply {
                    if (legacyLocationName is String) {
                        description = "_${legacyLocationName}_\n"
                    }

                    val legacyGeotag = legacyMapped["geotag"]

                    if (legacyGeotag is String) {
                        val locationData = mapOf(
                            time to legacyGeotag.toCoordinateOrNull()
                        )
                        if (locationData.filter { it.value != null }.isNotEmpty()) {
                            val locationFile = createLocationFile(context)
                            (locationData as? Map<ZonedDateTime, Coordinate>)?.writeToLocationFile(locationFile)

                            locationPath = locationFile.toLocationPath(context)
                        }
                    }

                    val legacySignature = legacyMapped["signature"]
                    if (legacySignature is String) {
                        metadata["legacy_signature"] = legacySignature

                        // Log.d("LegacyImport", "Found signature: $legacySignature")
                        val check = getLegacySignature(legacyMapped)
                        if (check == legacySignature) {
                            // Log.d("LegacyImport", "Valid old signature, generating new signature")
                            generateSignature(context, true)
                        }
                        else {
                            Log.w("LegacyImport", "Invalid signature (old: $legacySignature, check: $check), continuing to use old signature")
                            signature = legacySignature
                        }
                    }
                }
            }
            "range" -> {
                val legacyId = (legacyMapped["id"] as? Double)?.toLong() ?: Instant.now().toEpochMilli()
                val newId = legacyId.toString()

                EventRange(
                    id = newId,
                    title = legacyMapped["name"] as? String ?: "",
                    description = legacyMapped["description"] as? String ?: "",

                    // Time
                    time = (legacyMapped["time"] as? Double)?.let {
                        try {
                            ZonedDateTime.ofInstant(Instant.ofEpochMilli(it.toLong()), zoneId)
                        } catch (e: Throwable) {
                            logWarning(context, e)
                            null
                        }
                    } ?: ZonedDateTime.ofInstant(Instant.ofEpochMilli(0), zoneId),
                    end = (legacyMapped["until"] as? Double)?.let {
                        try {
                            ZonedDateTime.ofInstant(Instant.ofEpochMilli(it.toLong()), zoneId)
                        } catch (e: Throwable) {
                            logWarning(context, e)
                            null
                        }
                    } ?: ZonedDateTime.ofInstant(Instant.ofEpochMilli(0), zoneId),

                    created = ZonedDateTime.ofInstant(Instant.ofEpochMilli(legacyId), zoneId),
                    modified = now,
                    metadata = mutableMapOf(
                        "legacy_type" to legacyType,
                        "legacy_id" to legacyId.toString(),
                        "converted_from_legacy" to now.toString()
                    )
                ).apply {
                    val legacyGeotag = legacyMapped["geotag"]
                    val legacyGeotagEnd = legacyMapped["geotagEnd"]
                    val legacyGeotagPoints = legacyMapped["geotagPoints"]

                    val locationData = mutableMapOf<ZonedDateTime, Coordinate>()

                    val startPoint = (legacyGeotag as? String)?.toCoordinateOrNull()
                    val endPoint = (legacyGeotagEnd as? String)?.toCoordinateOrNull()

                    val middlePoints = (legacyGeotagPoints as? Map<String, String>)?.mapNotNull {
                        try {
                            ZonedDateTime.ofInstant(
                                Instant.ofEpochMilli(it.key.toLong()),
                                zoneId
                            ) to it.value.toCoordinate()
                        } catch (e: Throwable) {
                            logWarning(context, e)
                            null
                        }
                    }?.toMap() ?: emptyMap()

                    if (startPoint != null) {
                        locationData[time] = startPoint
                    }

                    locationData.putAll(middlePoints)

                    if (endPoint != null) {
                        locationData[end] = endPoint
                    }

                    if (legacyGeotag is String) {
                        if (locationData.isNotEmpty()) {
                            val locationFile = createLocationFile(context)
                            locationData.writeToLocationFile(locationFile)

                            locationPath = locationFile.toLocationPath(context)
                        }
                    }

                    val legacySignature = legacyMapped["signature"]
                    if (legacySignature is String) {
                        metadata["legacy_signature"] = legacySignature

                        // Log.d("LegacyImport", "Found signature: $legacySignature")
                        val check = getLegacySignature(legacyMapped)
                        if (check == legacySignature) {
                            // Log.d("LegacyImport", "Valid old signature, generating new signature")
                            generateSignature(context, true)
                        }
                        else {
                            Log.i("LegacyImport", "Invalid signature (old: $legacySignature, check: $check), continuing to use old signature")
                            signature = legacySignature
                        }
                    }
                }
            }
            else -> null
        }

        val db = AppDatabase.getInstance(context)
        val dao = db.appDao()

        return event?.toEventEntity()
    }

    private suspend fun importLegacyData(context: Context, legacyMapped: Any, filePath: String) {
        var fileVersion = 0

        logProcess(context, "Importing data")

        val now = ZonedDateTime.now()

        val db = AppDatabase.getInstance(context)
        val dao = db.appDao()

        var data = emptyList<Any?>()
        var dayData = emptyMap<String, Any?>()

        if (legacyMapped is List<Any?>) {
            data = legacyMapped
            fileVersion = 1
        }
        else if (legacyMapped is Map<*,*>) {
            data = legacyMapped["data"] as? List<Any?> ?: emptyList()
            fileVersion = (legacyMapped["fileVersion"] as? Double)?.toInt() ?: 3 // current is 3

            val dayDataMapped = try {
                legacyMapped["dayData"] as Map<String, Any?>
            } catch (e: Throwable) {
                logWarning(context, e)
                null
            }

            if (fileVersion > 1 && dayDataMapped != null) {
                dayData = dayDataMapped
            }
        }

        // Log.d("LegacyImport", "DAY DATA: $dayData")
        // Log.d("LegacyImport", "DATA: $data")

        val dateFromPath = getDateFromPath(filePath)

        // Log.d("LegacyImport", "File path: $filePath")
        // Log.d("LegacyImport", "Date from path: $dateFromPath")

        // Import day data first
        val dayOfMonth = (dayData["day"] as? Double)?.toInt() ?: dateFromPath?.dayOfMonth ?: 1
        val month = (dayData["month"] as? Double)?.toInt() ?: dateFromPath?.monthValue ?: 1
        val year = (dayData["year"] as? Double)?.toInt() ?: dateFromPath?.year ?: 1970
        val isFavoriteDay = (dayData["isFavorite"] as? Boolean) ?: false

        val convertedDay = HistoryDay(
            date = LocalDate.of(
                year, month, dayOfMonth
            ),
            description = null,
            favorite = isFavoriteDay,
            metadata = mutableMapOf(
                "converted_from_legacy" to now.toString()
            )
        )

        val existingDay = dao.getDayByDate(convertedDay.date)?.toHistoryDay()

        logProcess(context, "Importing day data")
        if (existingDay == null)
            dao.insertDay(convertedDay.toDayEntity())
        else
            dao.updateDay(convertedDay.toDayEntity())

        val convertedEvents = mutableListOf<EventEntity>()

        // Then import events
        for (legacyEvent in data) {
            if (legacyEvent is Map<*,*>) {
                importEvent(context, legacyEvent as Map<String, Any?>)?.let {
                    convertedEvents.add(it)
                }
            }
        }

        // Make sure the data replaced properly if it already exists
        // Get existing events with the same ID
        val replacedEvents = dao.getEventsByIds(convertedEvents.map { it.id })
        replacedEvents
            .forEach { it.toHistoryEvent().safeDelete(context) }

        // Insert converted legacy events
        dao.insertEvents(convertedEvents)
    }

    private fun listFilesRecursively(dir: DocumentFile): List<DocumentFile> {
        val result = mutableListOf<DocumentFile>()
        dir.listFiles().forEach { file ->
            result.add(file)
            if (file.isDirectory) {
                result.addAll(listFilesRecursively(file))
            }
        }
        return result
    }

    private fun DocumentFile.readBytes(context: Context): ByteArray {
        if (!exists()) throw Exception("Unable to read file '$uri'. File does not exist")
        if (!isFile) throw Exception("Unable to read file '$uri'. Expected file, got directory or else")

        return try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        } ?: throw IOException("Unable to access the file: $uri")
    }

    private fun Uri.getRelativePathFromDocumentsUri(): String? {
        try {
            val docId = DocumentsContract.getDocumentId(this) // "primary:Documents/DigitalHistory/2024-10/14.dhenc"
            val decoded = URLDecoder.decode(docId, StandardCharsets.UTF_8.name())
            return decoded.substringAfter("primary:") // remove "primary:" prefix
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun getDateFromPath(path: String): LocalDate? {
        val regex = Regex("""(\\|/|^)(\d+)-(\d+)[/\\](\d+)\.(.\w+)$""")
        val match = regex.find(path)

        return try {
            if (match != null) {
                val year = match.groupValues[2].toIntOrNull() ?: 0
                val month = match.groupValues[3].toIntOrNull() ?: 0
                val day = match.groupValues[4].toIntOrNull() ?: 0

                // Return null if any value is invalid
                if (year > 0 && month in 1..12 && day in 1..31) {
                    LocalDate.of(year, month, day)
                } else null
            } else null
        } catch (e: DateTimeParseException) {
            e.printStackTrace() // or Log.d("getDateFromPath", "Invalid date")
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun handleImport(context: Context, uri: Uri) {
        Log.d("LegacyImport", "Path: ${uri.path}")
        logProcess(context, "Selected path: ${uri.path}")

        val dir = DocumentFile.fromTreeUri(context, uri)
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION

        context.contentResolver.takePersistableUriPermission(uri, flags)

        if (dir?.isDirectory != true) {
            throw Exception("Selected item is not a directory or doesn't exist")
        }

        // Filter to only .dhenc files
        val files = listFilesRecursively(dir).filter { it.isFile }

        val keyBytes = LEGACY_ENCRYPTION_KEY.toByteArray(Charsets.UTF_8)
        val md = MessageDigest.getInstance("SHA-512")
        val digest = md.digest(keyBytes)

        for ((index, file) in files.withIndex()) {
            Log.d("LegacyImport", "Importing file: ${file.uri}")
            logProcess(context, "")
            logProcess(context, "Importing file: ${file.uri.path}")
            val fileData = file.readBytes(context)
            val decryptedBytes = ByteArray(fileData.size)

            logProcess(context, "Decrypting")
            for ((index, item) in fileData.withIndex()) {
                decryptedBytes[index] = (-item + 256).toByte()
                decryptedBytes[index] = (decryptedBytes[index].toInt() + (digest[(index + 4) % 64] * (index + 4 % 20)) % 256).toByte()
                decryptedBytes[index] = (-decryptedBytes[index] + 256).toByte()
            }

            val decrypted = decryptedBytes.toString(Charsets.UTF_8)

            Log.d("LegacyImportDecrypted", "DECRYPTED FROM ${file.uri}")
            Log.d("LegacyImportDecrypted", decrypted)

            val mapped = try {
                Gson().fromJson<Map<String, Any?>>(
                    decrypted,
                    object : TypeToken<Map<String, Any?>>() {}.type
                )
            } catch (_: Throwable) {
                Gson().fromJson<List<Any?>>(
                    decrypted,
                    object : TypeToken<List<Any?>>() {}.type
                )
            }

            importLegacyData(context, mapped, file.uri.getRelativePathFromDocumentsUri() ?: "")

            State.progress.value = index.toFloat() / files.size
        }
    }

    // suspend fun pickFolder(context: Context, launcher: ActivityResultLauncher<Intent>) = suspendCancellableCoroutine { cont ->
    //     val activity = context.getActivity()
    //     // val filePicker = activity?.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
    //     //     result.data?.data?.let { uri ->
    //     //         cont.resume(uri)
    //     //     } ?: throw Exception("No selected file")
    //     // }
    //     val initialUri = Uri.parse(
    //         "content://com.android.externalstorage.documents/tree/primary:Documents/DigitalHistory"
    //     )
    //      val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
    //          putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri)
    //      }
    //
    //     launcher.launch(intent)
    // }

    suspend fun import(context: Context, uri: Uri, onError: ((String) -> Unit)? = null): Boolean {
        // State update
        State.isRunning.value = true
        State.progress.value = 0f

        Log.i("LegacyImport", "Starting import process")

        // Logging init
        val now = ZonedDateTime.now()
        logData = LogData.inCommonPath("IMPORT-LEGACY")

        // Wait user to pick folder (usually named "DigitalHistory")
        try {
            withContext(Dispatchers.IO) {
                handleImport(context, uri)
            }
        } catch (e: Throwable) {
            Log.e("LegacyImport", e.message ?: "Unknown error")
            e.printStackTrace()
            onError?.invoke(e.message ?: "Unknown error")
            logError(context, e)
            return false
        } finally {
            State.isRunning.value = false
            State.progress.value = 0f
            logData = null
        }

        // Return true which means the import completed successfully
        return true
    }
}