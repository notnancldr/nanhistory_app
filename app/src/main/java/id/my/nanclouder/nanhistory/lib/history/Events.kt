package id.my.nanclouder.nanhistory.lib.history

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import id.my.nanclouder.nanhistory.lib.Coordinate
import id.my.nanclouder.nanhistory.lib.FILE_VERSION
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random
import id.my.nanclouder.nanhistory.lib.matchOrNull
import id.my.nanclouder.nanhistory.lib.toCoordinateOrNull
import id.my.nanclouder.nanhistory.lib.toZonedDateTimeOrNull
import java.time.ZoneId

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

data class HistoryDay(
    var date: LocalDate,
    var description: String?,
    var favorite: Boolean = false,
    var tags: List<String> = listOf()
)

abstract class HistoryEvent (
    open var id: String = generateEventId(),
    open var title: String,
    open var description: String,
    open var time: ZonedDateTime,
    open var favorite: Boolean = false,
    open var tags: List<String> = listOf(),
    open var created: ZonedDateTime = ZonedDateTime.now(),
    open var modified: ZonedDateTime = ZonedDateTime.now(),
    open var broken: Boolean = false,
    open var signature: String = "",
    open var type: String
)

data class EventPoint(
    override var id: String = generateEventId(),
    override var title: String,
    override var description: String,
    override var time: ZonedDateTime,
    override var favorite: Boolean = false,
    override var tags: List<String> = listOf(),
    override var created: ZonedDateTime = ZonedDateTime.now(),
    override var modified: ZonedDateTime = ZonedDateTime.now(),
    var location: Coordinate? = null
) : HistoryEvent(id, title, description, time, favorite,
    created = created,
    modified = modified,
    type = "point"
)

data class EventRange(
    override var id: String = generateEventId(),
    override var title: String,
    override var description: String,
    override var time: ZonedDateTime,
    override var favorite: Boolean = false,
    override var tags: List<String> = listOf(),
    override var created: ZonedDateTime = ZonedDateTime.now(),
    override var modified: ZonedDateTime = ZonedDateTime.now(),
    var end: ZonedDateTime,
    var locations: MutableMap<ZonedDateTime, Coordinate> = mutableMapOf(),
    var locationDescriptions: MutableMap<ZonedDateTime, String> = mutableMapOf()
) : HistoryEvent(id, title, description, time, favorite,
    created = created,
    modified = modified,
    type = "range"
)

data class HistoryFileData(
    val fileVersion: Int = FILE_VERSION,
    var date: LocalDate,
    var description: String?,
    var favorite: Boolean = false,
    var tags: MutableList<String> = mutableListOf(),
    val events: MutableList<HistoryEvent>,
    var broken: Boolean = false
) {
    companion object {
        fun fromJson(json: String): HistoryFileData =
//            try {
                Gson().fromJson<Map<String, Any?>>(
                    json,
                    object : TypeToken<Map<String, Any?>>() {}.type
                ).toHistoryFileData()
//            }
//            catch(_: Exception) {
//                HistoryFileData(
//                    date =
//                )
//            }
    }
    val historyDay get(): HistoryDay = HistoryDay(
        date = date,
        description = description,
        favorite = favorite,
        tags = tags
    )
}

/*
  int id = 0;
  String name = "";
  String description = "";
  DateTime time = DateTime(0);
  bool isFavorite = false;
  String? geotag;

  * * * * * * * * * * * * *
  Map<String, dynamic> toMap() {
    Map<String, dynamic> mapped = <String, dynamic>{
      'id': id,
      'name': name,
      'description': description,
      'time': time.millisecondsSinceEpoch,
      // "until": event.until.millisecondsSinceEpoch,
      'type': type.name,
      'geotag': geotag,
      'signature': signature,
    };

    return mapped;
  }

* */

fun generateEventId(instant: Instant = Instant.now()): String =
    "${instant.toEpochMilli().toString(16)}-${Random.nextLong().toString(16)}"

fun HistoryEvent.generateSignature(apply: Boolean = false): String {
    val messageDigest = MessageDigest.getInstance("SHA-512")
    val stringData = this.let {
        val data = this.toMap().toMutableMap()
        data.remove("title")
        data.remove("description")
        data.remove("tags")
        data.remove("modified")
        data.remove("favorite")
        data.remove("signature")
        Gson().toJson(data)
    }
    val result = messageDigest.digest(stringData.toByteArray()).let {
        BigInteger(1, it).toString(16)
    }
    if (apply) this.signature = result
    return result
}

fun HistoryEvent.validateSignature(): Boolean =
    this.signature == this.generateSignature()

fun Map<String, Any>.toHistoryEvent(): HistoryEvent {
    var brokenEvent: Boolean
    val noTime: () -> ZonedDateTime = {
        brokenEvent = true
        ZonedDateTime.parse("1970-01-01T00:00:00Z")
    }

    val id = matchOrNull<String>(this["id"])
    val title = matchOrNull<String>(this["title"])
    val description = matchOrNull<String>(this["description"])
    val favorite = matchOrNull<Boolean>(this["favorite"])
    val eventSignatureOrNull = matchOrNull<String>(this["signature"])
    val tags = matchOrNull<List<String>>(this["tags"])

    val timeStr = matchOrNull<String>(this["time"])
    val createdStr = matchOrNull<String>(this["created"])
    val modifiedStr = matchOrNull<String>(this["modified"])

    brokenEvent = (
        id == null || title == null || description == null || timeStr == null ||
        favorite == null || tags == null || eventSignatureOrNull == null
    )

    val eventSignature = eventSignatureOrNull ?: ""
    val time = timeStr?.toZonedDateTimeOrNull() ?: noTime()
    val created = createdStr?.toZonedDateTimeOrNull() ?: noTime()
    val modified = modifiedStr?.toZonedDateTimeOrNull() ?: noTime()

    val type = matchOrNull<String>(this["type"]).let {
       if (it == "point" || it == "range") it else {
           brokenEvent = true
           "point"
       }
    }

    val event = when (type) {
        "point" -> {
            val location = matchOrNull<String>(this["location"]).let {
                if (it == null) brokenEvent = true
                it?.toCoordinateOrNull()
            }
            EventPoint(
                id = id ?: generateEventId(Instant.now()),
                title = title ?: "",
                description = description ?: "",
                time = time,
                favorite = favorite ?: false,
                tags = tags ?: listOf(),
                created = created,
                modified = modified,
                location = location,
            )
        }
        "range" -> {
            val end = matchOrNull<String>(this["end"])?.toZonedDateTimeOrNull() ?: noTime()
            val locations = matchOrNull<Map<String, String>>(this["locations"])
                ?.map {
                    (it.key.toZonedDateTimeOrNull() ?: noTime()) to (it.value.toCoordinateOrNull() ?: Coordinate(0.0, 0.0))
                }?.toMap() ?: run {
                    brokenEvent = true
                    mapOf()
                }
            val locationDescriptions = matchOrNull<Map<String, String>>(this["locationDescriptions"])
                ?.map {
                    (it.key.toZonedDateTimeOrNull() ?: noTime()) to it.value
                }?.toMap() ?: run {
                    brokenEvent = true
                    mapOf()
                }
            EventRange(
                id = id ?: generateEventId(Instant.now()),
                title = title ?: "",
                description = description ?: "",
                time = time,
                favorite = favorite ?: false,
                tags = tags ?: listOf(),
                created = created,
                modified = modified,
                end = end,
                locations = locations.toMutableMap(),
                locationDescriptions = locationDescriptions.toMutableMap()
            )
        }
        else -> null
    }?.apply {
        broken = brokenEvent
        signature = eventSignature
    }
    return event!!
}

fun List<Map<String, Any>>.toHistoryEvent(): List<HistoryEvent> =
    this.map { it.toHistoryEvent() }

fun HistoryEvent.toMap(): Map<String, Any> {
    val map = mutableMapOf<String, Any>()
    map["id"] = this.id
    map["title"] = this.title
    map["description"] = this.description
    map["time"] = this.time.toOffsetDateTime().toString()
    map["favorite"] = this.favorite
    map["tags"] = this.tags
    map["created"] = this.created.toOffsetDateTime().toString()
    map["modified"] = this.modified.toOffsetDateTime().toString()
    map["signature"] = this.signature
    map["type"] = this.type
    when (this) {
        is EventPoint -> {
            map["location"] = this.location?.toString() ?: ""
        }
        is EventRange -> {
            map["end"] = this.end.toOffsetDateTime().toString()
            map["locations"] = this.locations.map {
                it.key.toOffsetDateTime().toString() to it.value.toString()
            }.toMap()
            map["locationDescriptions"] = this.locationDescriptions.map {
                it.key.toOffsetDateTime().toString() to it.value
            }.toMap()
        }
    }
    return map
}

fun List<HistoryEvent>.toMap(): List<Map<String, Any>> =
    this.map {
        it.toMap()
    }

fun Map<String, Any?>.toHistoryFileData(): HistoryFileData {
    var brokenData = false
    fun <T> noValue(defaultValue: T): T {
        brokenData = true
        Log.e("NanHistoryDebug", "Broken data! Return: $defaultValue")
        return defaultValue
    }
//    Log.d("NanHistoryDebug", "fileVersion: " + this["fileVersion"].toString())
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
    val events = (matchOrNull<List<Map<String, Any>>>(this["events"]) ?: noValue(listOf())).toHistoryEvent()
    return HistoryFileData(
        fileVersion = fileVersion,
        date = date,
        description = description,
        favorite = favorite,
        tags = tags.toMutableList(),
        events = events.toMutableList(),
        broken = brokenData
    )
}

fun HistoryFileData.toMap(): Map<String, Any?> = mapOf(
    "fileVersion" to this.fileVersion,
    "date" to this.date.format(DateTimeFormatter.ISO_DATE),
    "description" to this.description,
    "favorite" to this.favorite,
    "tags" to this.tags,
    "events" to this.events.toMap()
)

fun HistoryFileData.toJson(): String =
    Gson().toJson(this.toMap())

fun HistoryEvent.updateModifiedTime() {
    this.modified = ZonedDateTime.now()
}

fun HistoryFileData.Companion.get(context: Context, path: String): HistoryFileData? {
    val file = File(context.filesDir, path)

    return if (file.exists() && file.isFile) fromJson(file.readText())
    else null
}

fun HistoryFileData.Companion.get(context: Context, date: LocalDate): HistoryFileData? =
    HistoryFileData.get(context, getFilePathFromDate(date))

fun HistoryFileData.Companion.getList(
    context: Context,
    from: Instant = Instant.ofEpochMilli(0),
    until: Instant = Instant.now(),
): List<HistoryFileData> {
    val fileList = File(context.filesDir, "history").walkTopDown().filter { it.isFile }
    return fileList.mapNotNull {
        val fileTime = try {
            getDateFromFilePath(it.absolutePath)?.atStartOfDay(ZoneId.systemDefault())
                ?.toInstant()
        }
        catch (e: Exception) {
            Log.e("NanHistoryDebug", "ERROR: $e")
            null
        } ?: Instant.ofEpochMilli(0)

        if (fileTime < from || fileTime > until) null
        else {
            val data = it.readText()
            fromJson(data)
        }
    }.toList()
}

fun HistoryEvent.save(context: Context) {
    val date = this.time.toLocalDate()
    val fileData = HistoryFileData.get(context, date)
        ?: HistoryFileData(
            date = date,
            description = null,
            events = mutableListOf()
        )

    fileData.events.removeAll { it.id == this.id }
    fileData.events.add(this)
    fileData.save(context)
}

fun HistoryEvent.delete(context: Context) {
    val fileData = HistoryFileData.get(context, this.time.toLocalDate())

    fileData?.events?.removeAll { it.id == this.id }
    fileData?.save(context)
}

fun HistoryFileData.save(context: Context) {
    Log.d("NanHistoryDebug", "Saving file data...")
    val appFile = context.filesDir
    val file = File(appFile, getFilePathFromDate(this.date))
    if (!file.exists()) {
        Log.d("NanHistoryDebug", "File doesn't exist")
        file.parentFile?.mkdirs()
        file.createNewFile()
    }
    file.writeText(this.toJson())
}