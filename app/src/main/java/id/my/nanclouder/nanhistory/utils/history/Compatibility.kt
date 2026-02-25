package id.my.nanclouder.nanhistory.utils.history

/**
 * Compatibility Definitions
 * These definitions are meant for compatibility for migration from old format to new format
 */

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import id.my.nanclouder.nanhistory.utils.Coordinate
import id.my.nanclouder.nanhistory.utils.FILE_VERSION
import id.my.nanclouder.nanhistory.utils.LogData
import id.my.nanclouder.nanhistory.utils.matchOrNull
import id.my.nanclouder.nanhistory.utils.toCoordinateOrNull
import id.my.nanclouder.nanhistory.utils.toZonedDateTimeOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.collections.toMap

class LocationFile(
    val locations: List<LocationData>,
    val file: File,
    val formatVersion: Int,
)

data class HistoryFileData(
    val fileVersion: Int = FILE_VERSION,
    var date: LocalDate,
    var description: String?,
    var favorite: Boolean = false,
    var tags: MutableList<String> = mutableListOf(),
    val events: MutableList<HistoryEvent>,
    var metadata: MutableMap<String, Any> = mutableMapOf(),
    var broken: Boolean = false
) {
    companion object {
        fun fromJson(json: String, context: Context): HistoryFileData =
            Gson().fromJson<Map<String, Any?>>(
                json,
                object : TypeToken<Map<String, Any?>>() {}.type
            ).toHistoryFileData(context)
    }
    val historyDay get(): HistoryDay = HistoryDay(
        date = date,
        description = description,
        favorite = favorite,
        // tags = tags
    )
}

private fun getLocationFileDataJson(fileData: String): Pair<List<LocationData>, Int> {
    val gson = Gson()

    // if (!file.exists()) {
    //     file.createNewFile()
    //     file.writeText(gson.toJson(mapOf<ZonedDateTime, Coordinate>()))
    //     return LocationFile(mutableMapOf(), file)
    // }
    val mapType = object : TypeToken<Map<String, String?>>() {}.type
    val data = try {
        gson.fromJson<Map<String, String?>>(fileData, mapType)
    } catch (_: Exception) {
        mapOf()
    }
    return Pair(
        data.filter { it.value != null }
            .map { (key, value) ->
                LocationData(
                    time = (key.toZonedDateTimeOrNull() ?: ZonedDateTime.now()),
                    location = (value!!.toCoordinateOrNull() ?: Coordinate(0.0, 0.0))
                )
            },
        0
    )
}

private fun getLocationFileDataNew(fileData: String): Pair<List<LocationData>, Int> {
    val lines = fileData.split("\n")

    val formatVersion = lines[0].toIntOrNull() ?: 1

    // lines[1] is reserved
    val locationData = lines.drop(2).mapNotNull { line ->
        // Parse per line using fromString()
        try {
            LocationData.fromString(line)
        } catch (_: Throwable) {
            null
        }
    }

    return Pair(locationData, formatVersion)
}

fun getLocationFilePath(path: String, context: Context): String {
    val locationsDir = makeSureLocationsDir(context)
    return File(locationsDir, path).absolutePath
}

fun getLocationFile(path: String, context: Context): LocationFile {
    val file = File(getLocationFilePath(path, context))

    val rawData = try {
        file.readText().ifBlank { "{}" }.trim()
    } catch (_: java.io.FileNotFoundException) {
        "{}"
    }

    val locationData = try {
        // If started with curly brace (recognized as JSON, legacy support)
        if (rawData.getOrNull(0) == '{') {
            getLocationFileDataJson(rawData)
        }
        // New format import
        else {
            getLocationFileDataNew(rawData)
        }
    } catch (e: Throwable) {
        // Handle invalid format or TODO: repair invalid format (if possible)
        val logData = LogData.inCommonPath("getLocationFile()")

        logData.appendWithTimestamp("Unable to parse location file: ${e.message ?: "Unknown error"}")
        logData.appendWithTimestamp(e.stackTraceToString())
        logData.save(context)

        // Throw caught error
        throw e
    }

    return LocationFile(
        locations = locationData.first,
        file = file,
        formatVersion = locationData.second
    )
}

fun getFilePathFromDate(date: LocalDate): String =
    "history/${date.year}/${date.monthValue}/${date.dayOfMonth}.json"

fun getDateFromFilePath(path: String): LocalDate? {
    val regex = Regex("history/(\\d+)/(\\d+)/(\\d+).json$")
    val groups = regex.find(path)?.groups ?: run {
        Log.e("NanHistoryDebug", "ERROR: Unable to find pattern")
        return null
    }
    if (groups.size != 4) return null
    return try { LocalDate.of(
        groups[1]!!.value.toInt(),
        groups[2]!!.value.toInt(),
        groups[3]!!.value.toInt()
    )} catch (e: Exception) {
        Log.e("NanHistoryDebug", "ERROR: $e")
        return null
    }
}

class HistoryFileDataStream(private var list: List<String>, val context: Context) {
    private var index = 0
    private var currPath: String? = null

    init {
        currPath = if (list.isNotEmpty()) list[0] else null
        Log.d("NanHistoryDebug", "list: $list")
        Log.d("NanHistoryDebug", "index: $index")
        Log.d("NanHistoryDebug", "currPath: $currPath")
    }

    val size: Int
        get() = list.size

    val fileData: HistoryFileData?
        get() = currPath?.let { HistoryFileData.get(context, it.removePrefix(context.filesDir.absolutePath)) }

    fun <T : Comparable<T>> sortedBy(selector: (File) -> T?) {
        list = list.sortedBy { selector(File(it)) }
    }
    fun <T : Comparable<T>> sortedByDescending(selector: (File) -> T?) {
        list = list.sortedByDescending { selector(File(it)) }
    }
    fun <T : Comparable<T>> filter(predicate: (File) -> Boolean) {
        list = list.filter { predicate(File(it)) }
    }

    fun reset() {
        index = 0
        currPath = list[index]
    }

    fun next(): Boolean {
        index++
        if (index < list.size) {
            currPath = list[index]
            return true
        }
        return false
    }

    suspend fun forEachAsync(block: suspend (HistoryFileData) -> Unit) {
        withContext(Dispatchers.IO) {
            list.forEach {
                currPath = it
                fileData?.let { data ->
                    block(data)
                }
            }
        }
    }
}

fun HistoryFileData.Companion.get(context: Context, path: String): HistoryFileData? {
    val file = File(context.filesDir, path)

    return if (file.exists() && file.isFile) fromJson(file.readText(), context)
    else null
}

fun HistoryFileData.Companion.getFileListStream(
    context: Context,
    from: LocalDate = LocalDate.MIN,
    until: LocalDate = LocalDate.MAX,
): HistoryFileDataStream {
    val fileList = File(context.filesDir, "history").walkTopDown().filter { it.isFile }
    return HistoryFileDataStream(
        fileList.mapNotNull {
            val fileTime = try {
                getDateFromFilePath(it.absolutePath)
            }
            catch (e: Exception) {
                Log.e("NanHistoryDebug", "ERROR: $e")
                null
            } ?: LocalDate.MIN

            if (fileTime !in from..until) null
            else it.absolutePath
        }.toList(),
        context
    )
}

fun Map<String, Any>.toHistoryEvent(context: Context): HistoryEvent {
    var brokenEvent: Boolean
    val noTime: () -> ZonedDateTime = {
        brokenEvent = true
        ZonedDateTime.parse("1970-01-01T00:00:00Z")
    }

    val id =
        matchOrNull<String>(this[HistoryEventProperty.ID])
    val title =
        matchOrNull<String>(this[HistoryEventProperty.TITLE])
    val description =
        matchOrNull<String>(this[HistoryEventProperty.DESCRIPTION])
    val favorite =
        matchOrNull<Boolean>(this[HistoryEventProperty.FAVORITE])
    val eventSignatureOrNull =
        matchOrNull<String>(this[HistoryEventProperty.SIGNATURE])
    val tags = listOf<HistoryTag>()

    val metadata =
        matchOrNull<Map<String, Any>>(this[HistoryEventProperty.METADATA]) ?: mapOf()

    val eventAudio =
        matchOrNull<String>(this[HistoryEventProperty.AUDIO]).let {
            if (it?.isEmpty() != false) null
            else it
        }
    val timeStr =
        matchOrNull<String>(this[HistoryEventProperty.TIME])
    val createdStr =
        matchOrNull<String>(this[HistoryEventProperty.CREATED])
    val modifiedStr =
        matchOrNull<String>(this[HistoryEventProperty.MODIFIED])

    brokenEvent = (
            id == null || title == null || description == null || timeStr == null ||
                    favorite == null || eventSignatureOrNull == null
            )

    val eventSignature = eventSignatureOrNull ?: ""
    val time = timeStr?.toZonedDateTimeOrNull() ?: noTime()
    val created = createdStr?.toZonedDateTimeOrNull() ?: noTime()
    val modified = modifiedStr?.toZonedDateTimeOrNull() ?: noTime()

    val type = matchOrNull<String>(this["type"]).let {
        if (it == HistoryEventProperty.TYPE_POINT || it == HistoryEventProperty.TYPE_RANGE) it else {
            brokenEvent = true
            HistoryEventProperty.TYPE_POINT
        }
    }

    val event = when (type) {
        HistoryEventProperty.TYPE_POINT -> {
            val locationPath = matchOrNull<String>(this[HistoryEventProperty.LOCATION_PATH])

            EventPoint(
                id = id ?: generateEventId(Instant.now()),
                title = title ?: "",
                description = description ?: "",
                time = time,
                favorite = favorite ?: false,
                tags = tags,
                created = created,
                modified = modified,
                metadata = metadata.toMutableMap(),
                locationPath = locationPath,
                unknownProperties = this.filterNot {
                    it.key in EventPointProperties
                }.toMutableMap()
            )
        }
        HistoryEventProperty.TYPE_RANGE -> {
            val end = matchOrNull<String>(this[HistoryEventProperty.END])?.toZonedDateTimeOrNull() ?: noTime()
            val locationDescriptions = matchOrNull<Map<String, String>>(this[HistoryEventProperty.LOCATION_DESCRIPTIONS])
                ?.map {
                    (it.key.toZonedDateTimeOrNull() ?: noTime()) to it.value
                }?.toMap() ?: run {
                brokenEvent = true
                mapOf()
            }
            val locationPath = matchOrNull<String>(this[HistoryEventProperty.LOCATION_PATH])

            val transportationType = (this[HistoryEventProperty.TRANSPORTATION_TYPE] as? String)
                ?.let {
                    try { TransportationType.valueOf(it) }
                    catch (_: Exception) { null }
                } ?: TransportationType.Unspecified

            EventRange(
                id = id ?: generateEventId(Instant.now()),
                title = title ?: "",
                description = description ?: "",
                time = time,
                favorite = favorite ?: false,
                tags = tags,
                created = created,
                modified = modified,
                end = end,
                metadata = metadata.toMutableMap(),
                locationDescriptions = locationDescriptions.toMutableMap(),
                locationPath = locationPath,
                unknownProperties = this.filterNot {
                    it.key in EventRangeProperties
                }.toMutableMap(),
                transportationType = transportationType
            )
        }
        else -> null
    }?.apply {
        broken = brokenEvent
        signature = eventSignature
        audio = eventAudio
    }
    return event!!
}

fun Map<String, Any?>.toHistoryFileData(context: Context): HistoryFileData {
    var brokenData = false
    fun <T> noValue(defaultValue: T): T {
        brokenData = true
        Log.e("NanHistoryDebug", "Broken data! Return: $defaultValue")
        return defaultValue
    }
    val fileVersion = (matchOrNull<Double>(this["fileVersion"]) ?: noValue(FILE_VERSION)).toInt()
    val date = matchOrNull<String>(this["date"]).let {
        try { LocalDate.parse(it ?: "", DateTimeFormatter.ISO_DATE) } catch (e: Exception) {
            brokenData = true
            LocalDate.of(0, 0, 0)
        }
    }
    val description = matchOrNull<String?>(this["description"])
    val favorite = matchOrNull<Boolean>(this["favorite"]) ?: noValue(false)
    val tags = matchOrNull<List<String>>(this["tags"]) ?: noValue(listOf())
    val metadata = matchOrNull<Map<String, Any>>(this["metadata"]) ?: mapOf()
    val events = (matchOrNull<List<Map<String, Any>>>(this["events"]) ?: noValue(listOf())).toHistoryEvent(context)
    return HistoryFileData(
        fileVersion = fileVersion,
        date = date,
        description = description,
        favorite = favorite,
        tags = tags.toMutableList(),
        events = events.toMutableList(),
        metadata = metadata.toMutableMap(),
        broken = brokenData
    )
}

fun HistoryEvent.toMap(): Map<String, Any> {
    val map = mutableMapOf<String, Any>()
    map[HistoryEventProperty.ID] = this.id
    map[HistoryEventProperty.TITLE] = this.title
    map[HistoryEventProperty.DESCRIPTION] = this.description
    map[HistoryEventProperty.TIME] = this.time.toOffsetDateTime().toString()
    map[HistoryEventProperty.FAVORITE] = this.favorite
//    map[HistoryEventProperty.TAGS] = this.tags
    map[HistoryEventProperty.TAGS] = listOf<String>()
    map[HistoryEventProperty.CREATED] = this.created.toOffsetDateTime().toString()
    map[HistoryEventProperty.MODIFIED] = this.modified.toOffsetDateTime().toString()
    map[HistoryEventProperty.SIGNATURE] = this.signature
    map[HistoryEventProperty.METADATA] = this.metadata
    map[HistoryEventProperty.AUDIO] = this.audio ?: ""
    map[HistoryEventProperty.TYPE] = this.type
    when (this) {
        is EventPoint -> {
//            map[HistoryEventProperty.LOCATION] = this.location?.toString() ?: ""
            map[HistoryEventProperty.LOCATION_PATH] = this.locationPath ?: false
        }
        is EventRange -> {
            map[HistoryEventProperty.END] = this.end.toOffsetDateTime().toString()
//            map[HistoryEventProperty.LOCATIONS] = this.locations.map {
//                it.key.toOffsetDateTime().toString() to it.value.toString()
//            }.toMap()
            map[HistoryEventProperty.LOCATION_PATH] = this.locationPath ?: false
            map[HistoryEventProperty.LOCATION_DESCRIPTIONS] = this.locationDescriptions.map {
                it.key.toOffsetDateTime().toString() to it.value
            }.toMap()
        }
    }
    return map + this.unknownProperties
}

fun List<HistoryEvent>.toMap(): List<Map<String, Any>> =
    this.map {
        it.toMap()
    }

fun List<Map<String, Any>>.toHistoryEvent(context: Context): List<HistoryEvent> =
    this.map { it.toHistoryEvent(context) }

fun HistoryFileData.delete(context: Context) {
    val file = File(context.filesDir, getFilePathFromDate(this.date))
    if (file.exists()) file.delete()
}