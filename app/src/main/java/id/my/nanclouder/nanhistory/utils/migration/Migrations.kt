package id.my.nanclouder.nanhistory.utils.migration

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import id.my.nanclouder.nanhistory.db.AppDatabase
import id.my.nanclouder.nanhistory.db.toDayEntity
import id.my.nanclouder.nanhistory.db.toEventEntity
import id.my.nanclouder.nanhistory.db.toHistoryEvent
import id.my.nanclouder.nanhistory.db.toLocationEntity
import id.my.nanclouder.nanhistory.utils.getLocationFile
import id.my.nanclouder.nanhistory.utils.history.EVENT_VERSION_NUMBER
import id.my.nanclouder.nanhistory.utils.history.HistoryEventProperty
import id.my.nanclouder.nanhistory.utils.history.HistoryFileData
import id.my.nanclouder.nanhistory.utils.history.HistoryFileDataProperty
import id.my.nanclouder.nanhistory.utils.history.createLocationFile
import id.my.nanclouder.nanhistory.utils.history.delete
import id.my.nanclouder.nanhistory.utils.signature.generateOldSignature
import id.my.nanclouder.nanhistory.utils.signature.generateSignature
import id.my.nanclouder.nanhistory.utils.history.getFileListStream
import id.my.nanclouder.nanhistory.utils.history.toHistoryEvent
import id.my.nanclouder.nanhistory.utils.signature.validateSignature
import id.my.nanclouder.nanhistory.utils.matchOrNull
import id.my.nanclouder.nanhistory.utils.toZonedDateTimeOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.time.ZonedDateTime

suspend fun migrateToDatabase(context: Context, onUpdate: ((MigrationState) -> Unit)) {
    onUpdate(MigrationState(0f, false))
    withContext(Dispatchers.IO) {
        val db = AppDatabase.getInstance(context)
        val dao = db.appDao()

        val fileData = HistoryFileData.getFileListStream(context)

        val totalData = fileData.size
        var progress = 0

        if (fileData.size > 0) {
            dao.deleteAllEvents()
            dao.deleteAllDays()
            dao.deleteAllTags()
        }

        fileData.forEachAsync { data ->
            dao.insertDay(data.historyDay.toDayEntity())
            data.events.forEach {
                dao.insertEvent(it.toEventEntity())
            }

            Log.d("NanHistoryDebug", "Migrated: ${data.date}")

            progress++
            onUpdate(MigrationState(progress / totalData.toFloat(), false))
            data.delete(context)
        }

        onUpdate(MigrationState(1f, false))
    }
}

suspend fun migrateLocationData(context: Context, onUpdate: ((MigrationState) -> Unit)) {
    val historyDir = File(context.filesDir, "history")
    val gson = Gson()
    val fileList = historyDir.walkTopDown()

    val totalData = fileList.toList().size
    var progress = 0

    onUpdate(MigrationState(0f, false))

    withContext(Dispatchers.IO) {
        for (file in fileList) {
            progress++
            if (file.isDirectory) continue

            val fileData = gson.fromJson<Map<String, Any?>>(
                file.readText(),
                object : TypeToken<Map<String, Any?>>() {}.type
            ).toMutableMap()

            if ((matchOrNull<Double>(fileData[HistoryFileDataProperty.FILE_VERSION])
                    ?: 0.0) >= 6.0
            ) continue

            val events =
                matchOrNull<List<Map<String, Any?>>>(fileData[HistoryFileDataProperty.EVENTS])
            val newEvents = events?.map {
                val checkSignature = generateOldSignature(it)
                val oldSignature = it[HistoryEventProperty.SIGNATURE]
                val signatureValid = checkSignature == oldSignature
                val regenerateSignature =
                    (matchOrNull<String>(it[HistoryEventProperty.SIGNATURE]) ?: "").isNotBlank()

                Log.d(
                    "NanHistoryDebug",
                    "MIGRATION SIGNATURE CHECK: $checkSignature, OLD: $oldSignature, REGENERATE: $regenerateSignature"
                )

                val data = it.toMutableMap()
                var locationData = mutableMapOf<String, String>()
                val timeRaw = matchOrNull<String>(data[HistoryEventProperty.TIME]) ?: "0"
                val time = timeRaw
                    .toZonedDateTimeOrNull()
                    ?: ZonedDateTime.now()

                if (data[HistoryEventProperty.LOCATION] is String) {
                    if ((data[HistoryEventProperty.LOCATION] as String).isNotBlank()) {
                        locationData[timeRaw] =
                            matchOrNull<String>(data[HistoryEventProperty.LOCATION]) ?: "0,0"
                    }
                    data.remove(HistoryEventProperty.LOCATION)
                } else if (data[HistoryEventProperty.LOCATIONS] != null) {
                    if ((data[HistoryEventProperty.LOCATIONS] as Map<String, String>).isNotEmpty()) {
                        locationData =
                            (matchOrNull<Map<String, String>>(data[HistoryEventProperty.LOCATIONS])
                                ?: mapOf()).toMutableMap()
                    }
                    data.remove(HistoryEventProperty.LOCATIONS)
                }
                val locationFile =
                    if (locationData.isNotEmpty()) createLocationFile(context, time) else null
                val locationPath = locationFile?.absolutePath?.removePrefix(
                    File(
                        context.filesDir,
                        "locations"
                    ).absolutePath + "/"
                )
                locationFile?.writeText(gson.toJson(locationData))
                data[HistoryEventProperty.LOCATION_PATH] = locationPath

                if (signatureValid && regenerateSignature) {
                    data[HistoryEventProperty.SIGNATURE] =
                        (data as Map<String, Any>).toHistoryEvent(context)
                            .apply {
                                versionNumber = 0
                            }
                            .generateSignature(context = context)
                }

                data
            }
            fileData[HistoryFileDataProperty.EVENTS] = newEvents
            fileData[HistoryFileDataProperty.FILE_VERSION] = 6

            file.writeText(gson.toJson(fileData))
            onUpdate(MigrationState((progress / totalData).toFloat(), false))
        }
    }
    onUpdate(MigrationState(1f, false))
}

suspend fun migrateLocationToDatabase(context: Context, onUpdate: (MigrationState) -> Unit) {
    onUpdate(MigrationState(0f, false))
    withContext(Dispatchers.IO) {
        val db = AppDatabase.getInstance(context)
        val dao = db.appDao()

        val notDeletedEvents = dao.getAllEvents().first()
        val deletedEvents = dao.getDeletedEvents()

        val allEvents = (notDeletedEvents + deletedEvents).filter {
            it.event.locationPath != null
        }

        for ((index, data) in allEvents.withIndex()) {
            val event = data.toHistoryEvent()

            val locations = event.getLocations(context)

            dao.insertLocations(locations.map { it.toLocationEntity(event.id) })
            val isValid = event.validateSignature(context)

            // Remove old file
            val locationFile = getLocationFile(context, event.locationPath!!)
            locationFile.delete()
            dao.updateEvent(
                event.apply {
                    locationPath = null
                    versionNumber = EVENT_VERSION_NUMBER

                    // Update the signature if event was valid before migration
                    if (isValid) {
                        signature = generateSignature(context)
                    }
                }.toEventEntity()
            )

            onUpdate(MigrationState(index / allEvents.size.toFloat(), true))
        }
    }
}