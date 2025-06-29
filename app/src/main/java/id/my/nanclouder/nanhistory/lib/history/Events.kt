package id.my.nanclouder.nanhistory.lib.history

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import id.my.nanclouder.nanhistory.db.AppDatabase
import id.my.nanclouder.nanhistory.db.toDayEntity
import id.my.nanclouder.nanhistory.db.toEventEntity
import id.my.nanclouder.nanhistory.lib.Coordinate
import id.my.nanclouder.nanhistory.lib.FILE_VERSION
import id.my.nanclouder.nanhistory.lib.matchOrNull
import id.my.nanclouder.nanhistory.lib.toCoordinateOrNull
import id.my.nanclouder.nanhistory.lib.toZonedDateTimeOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

enum class EventTypes {
    Point, Range
}

enum class TransportationType {
    Unspecified, Walk, Bicycle, Motorcycle, Car, Train,
    Airplane, Ferry
}

object HistoryFileDataProperty {
    const val FILE_VERSION = "fileVersion"
    const val DATE = "date"
    const val DESCRIPTION = "description"
    const val FAVORITE = "favorite"
    const val TAGS = "tags"
    const val EVENTS = "events"
    const val METADATA = "metadata"
}

object HistoryEventProperty {
    const val ID = "id"
    const val TITLE = "title"
    const val DESCRIPTION = "description"
    const val FAVORITE = "favorite"
    const val SIGNATURE = "signature"
    const val TAGS = "tags"
    const val METADATA = "metadata"
    const val TIME = "time"
    const val CREATED = "created"
    const val MODIFIED = "modified"
    const val TYPE = "type"
    const val TYPE_POINT = "point"
    const val TYPE_RANGE = "range"
    const val LOCATION = "location"
    const val END = "end"
    const val LOCATIONS = "locations"
    const val LOCATION_DESCRIPTIONS = "locationDescriptions"
    const val LOCATION_PATH = "locationPath"
    const val AUDIO = "audio"
    const val TRANSPORTATION_TYPE = "transportationType"
}

val EventPointProperties = listOf(
    HistoryEventProperty.ID,
    HistoryEventProperty.TITLE,
    HistoryEventProperty.DESCRIPTION,
    HistoryEventProperty.FAVORITE,
    HistoryEventProperty.SIGNATURE,
    HistoryEventProperty.TAGS,
    HistoryEventProperty.METADATA,
    HistoryEventProperty.TIME,
    HistoryEventProperty.CREATED,
    HistoryEventProperty.MODIFIED,
    HistoryEventProperty.TYPE,
    HistoryEventProperty.LOCATION,
    HistoryEventProperty.LOCATION_PATH,
    HistoryEventProperty.AUDIO
)

val EventRangeProperties = listOf(
    HistoryEventProperty.ID,
    HistoryEventProperty.TITLE,
    HistoryEventProperty.DESCRIPTION,
    HistoryEventProperty.FAVORITE,
    HistoryEventProperty.SIGNATURE,
    HistoryEventProperty.TAGS,
    HistoryEventProperty.METADATA,
    HistoryEventProperty.TIME,
    HistoryEventProperty.CREATED,
    HistoryEventProperty.MODIFIED,
    HistoryEventProperty.TYPE,
    HistoryEventProperty.END,
    HistoryEventProperty.LOCATIONS,
    HistoryEventProperty.LOCATION_DESCRIPTIONS,
    HistoryEventProperty.LOCATION_PATH,
    HistoryEventProperty.AUDIO,
    HistoryEventProperty.TRANSPORTATION_TYPE
)

val HistoryEventSignatureExcluded = listOf(
    HistoryEventProperty.TITLE,
    HistoryEventProperty.DESCRIPTION,
    HistoryEventProperty.TAGS,
    HistoryEventProperty.MODIFIED,
    HistoryEventProperty.FAVORITE,
    HistoryEventProperty.SIGNATURE,
    HistoryEventProperty.METADATA,
    HistoryEventProperty.TRANSPORTATION_TYPE
)

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
    var tags: List<HistoryTag> = listOf(),
    var metadata: MutableMap<String, Any> = mutableMapOf(),
)

abstract class HistoryEvent (
    open var id: String = generateEventId(),
    open var title: String,
    open var description: String,
    open var time: ZonedDateTime,
    open var favorite: Boolean = false,
    open var tags: List<HistoryTag> = listOf(),
    open var created: ZonedDateTime = ZonedDateTime.now(),
    open var modified: ZonedDateTime = ZonedDateTime.now(),
    open var broken: Boolean = false,
    open var signature: String = "",
    open var type: String,
    open var metadata: MutableMap<String, Any> = mutableMapOf(),
    open var unknownProperties: MutableMap<String, Any> = mutableMapOf(),
    open var audio: String? = null,
    open var locationPath: String? = null
) {
    private var locationsData: Map<ZonedDateTime, Coordinate>? = null

    fun getLocations(context: Context): Map<ZonedDateTime, Coordinate> {
        return (locationPath?.let {
            val locationFile = getLocationFile(it, context)
            locationsData = locationFile.locations.ifEmpty {
                File(it).delete()
                locationPath = null
                null
            } ?: mapOf()
            locationsData
        })?: mapOf()
    }
}

data class EventPoint(
    override var id: String = generateEventId(),
    override var title: String,
    override var description: String,
    override var time: ZonedDateTime,
    override var favorite: Boolean = false,
    override var tags: List<HistoryTag> = listOf(),
    override var created: ZonedDateTime = ZonedDateTime.now(),
    override var modified: ZonedDateTime = ZonedDateTime.now(),
    override var metadata: MutableMap<String, Any> = mutableMapOf(),
    override var unknownProperties: MutableMap<String, Any> = mutableMapOf(),
    override var audio: String? = null,
    override var locationPath: String? = null,
//    val location: Coordinate? = null
) : HistoryEvent(id, title, description, time, favorite,
    created = created,
    modified = modified,
    type = "point",
    locationPath = locationPath
)

data class EventRange(
    override var id: String = generateEventId(),
    override var title: String,
    override var description: String,
    override var time: ZonedDateTime,
    override var favorite: Boolean = false,
    override var tags: List<HistoryTag> = listOf(),
    override var created: ZonedDateTime = ZonedDateTime.now(),
    override var modified: ZonedDateTime = ZonedDateTime.now(),
    override var metadata: MutableMap<String, Any> = mutableMapOf(),
    override var unknownProperties: MutableMap<String, Any> = mutableMapOf(),
    override var locationPath: String? = null,
    override var audio: String? = null,
    var end: ZonedDateTime,
    var transportationType: TransportationType = TransportationType.Unspecified,
    var locationDescriptions: MutableMap<ZonedDateTime, String> = mutableMapOf()
    // var locations: MutableMap<ZonedDateTime, Coordinate> = mutableMapOf(),
) : HistoryEvent(id, title, description, time, favorite,
    created = created,
    modified = modified,
    type = "range",
    locationPath = locationPath
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
//            try {
                Gson().fromJson<Map<String, Any?>>(
                    json,
                    object : TypeToken<Map<String, Any?>>() {}.type
                ).toHistoryFileData(context)
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
        // tags = tags
    )
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
//    suspend fun forEachAsync(block: (HistoryFileData) -> Unit) {
//        forEachAsync { block(it) }
//    }
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

class LocationFile(
    val locations: MutableMap<ZonedDateTime, Coordinate>,
    val file: File
)

fun generateEventId(instant: Instant = Instant.now()): String =
    "${instant.toEpochMilli().toString(16)}-${Random.nextLong().toString(16)}"

fun makeSureLocationsDir(context: Context): File {
    val locationsDir = File(context.filesDir, "locations")
    if (!locationsDir.exists()) locationsDir.mkdir()
    return locationsDir
}

fun createLocationSubdir(context: Context, time: ZonedDateTime = ZonedDateTime.now()): File {
    val locationsDir = makeSureLocationsDir(context)
    return File(locationsDir, time.format(DateTimeFormatter.ofPattern("yyyy-MM"))).apply {
        mkdir()
    }
}

fun createLocationFile(context: Context, time: ZonedDateTime = ZonedDateTime.now()): File {
    val locationSubdir = createLocationSubdir(context, time)
    val formatter = DateTimeFormatter.ofPattern("dd-HHmmss")
    return File(locationSubdir, time.format(formatter) + "-" + Random.nextInt().toString(16))
}

fun Map<ZonedDateTime, Coordinate>.writeToLocationFile(file: File) {
    if (!file.exists()) file.createNewFile()
    val gson = Gson()
    file.writeText(
        gson.toJson(
            this.map {
                it.key.toOffsetDateTime().toString() to it.value.toString()
            }.toMap()
        )
    )
}

fun getLocationFile(path: String, context: Context): LocationFile {
    val locationsDir = makeSureLocationsDir(context)
    val file = File(locationsDir, path)
    val gson = Gson()

//    if (!file.exists()) {
//        file.createNewFile()
//        file.writeText(gson.toJson(mapOf<ZonedDateTime, Coordinate>()))
//        return LocationFile(mutableMapOf(), file)
//    }
    val mapType = object : TypeToken<Map<String, String>>() {}.type
    val data = try {
        gson.fromJson<Map<String, String>>(file.readText(), mapType)
    } catch (_: Exception) {
        mapOf()
    }
    return LocationFile(
        data.map {
            (it.key.toZonedDateTimeOrNull() ?: ZonedDateTime.now()) to (it.value.toCoordinateOrNull() ?: Coordinate(0.0, 0.0))
        }.toMap().toMutableMap(),
        file
    )
}

class MigrationState(
    val progress: Float,
    val finish: Boolean
)

suspend fun migrateData(context: Context, onUpdate: ((MigrationState, String) -> Unit)) {
    migrateLocationData(context) { onUpdate(it, "locationData") }
    migrateToDatabase(context) { onUpdate(it, "toDatabase") }
}

suspend fun migrateToDatabase(context: Context, onUpdate: ((MigrationState) -> Unit)) {
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

        onUpdate(MigrationState(1f, true))
    }
}

suspend fun migrateLocationData(context: Context, onUpdate: ((MigrationState) -> Unit)) {
    val historyDir = File(context.filesDir, "history")
    val gson = Gson()
    val fileList = historyDir.walkTopDown()

    val totalData = fileList.toList().size
    var progress = 0

    withContext(Dispatchers.IO) {
        fileList.forEach iterator@{ file ->
            progress++
            if (file.isDirectory) return@iterator

            val fileData = gson.fromJson<Map<String, Any?>>(
                file.readText(),
                object : TypeToken<Map<String, Any?>>() {}.type
            ).toMutableMap()

            if ((matchOrNull<Double>(fileData[HistoryFileDataProperty.FILE_VERSION])
                    ?: 0.0) >= 6.0
            ) return@iterator

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
    onUpdate(MigrationState(1f, true))
}

fun generateOldSignature(eventData: Map<String, Any?>): String {
    val data = eventData.toMutableMap()
    val messageDigest = MessageDigest.getInstance("SHA-512")
    val stringData = data.let {
        for (excluded in HistoryEventSignatureExcluded) {
            data.remove(excluded)
        }
        if (data[HistoryEventProperty.AUDIO] == null)
            data.remove(HistoryEventProperty.AUDIO)
        if (data[HistoryEventProperty.LOCATION] == "")
            data[HistoryEventProperty.LOCATION] = null

        Gson().toJson(data)
    }
    Log.d("NanHistoryDebug", stringData)
    val result = messageDigest.digest(stringData.toByteArray()).let {
        BigInteger(1, it).toString(16)
    }
    return result
}

fun HistoryEvent.generateSignature(apply: Boolean = false, context: Context): String {
//    Log.d("NanHistoryDebug" , this.toMap().toString())
    val messageDigest = MessageDigest.getInstance("SHA-512")
    val stringData = this.let {
        val data = this.toMap().toMutableMap()
        for (excluded in HistoryEventSignatureExcluded) {
            data.remove(excluded)
        }
        if (this.audio == null)
            data.remove(HistoryEventProperty.AUDIO)

        if (data[HistoryEventProperty.LOCATION_PATH] is String)
            data[HistoryEventProperty.LOCATIONS] =
                getLocations(context).map { it.key.toOffsetDateTime().toString() to it.value.toString() }.toMap()
        Gson().toJson(data)
    }
//    Log.d("NanHistoryDebug", stringData)
    val result = messageDigest.digest(stringData.toByteArray()).let {
        BigInteger(1, it).toString(16)
    }
    if (apply) this.signature = result
//    Log.d("NanHistoryDebug", "Generated: $result")
    return result
}

fun HistoryEvent.validateSignature(context: Context): Boolean =
    this.signature == this.generateSignature(context = context)

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
//    val tags =
//        matchOrNull<List<String>>(this[HistoryEventProperty.TAGS])
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
        favorite == null || tags == null || eventSignatureOrNull == null
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
//            var location = matchOrNull<String>(this[HistoryEventProperty.LOCATION]).let {
//                it?.toCoordinateOrNull()
//            }
            val locationPath = matchOrNull<String>(this[HistoryEventProperty.LOCATION_PATH])
//            if (locationPath == null && location != null) {
//                val locationFile = createLocationFile(context, time)
//                mapOf(time to location).writeToLocationFile(locationFile)
//                locationPath = locationFile.absolutePath.removePrefix(File(context.filesDir, "locations").absolutePath + "/")
//            }
//            else if (locationPath != null) {
//                val locationFile = getLocationFile(locationPath, context)
//                location = locationFile.locations[locationFile.locations.keys.first()]
//            }

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
//                location = location,
                locationPath = locationPath,
                unknownProperties = this.filterNot {
                    it.key in EventPointProperties
                }.toMutableMap()
            )
        }
        HistoryEventProperty.TYPE_RANGE -> {
            val end = matchOrNull<String>(this[HistoryEventProperty.END])?.toZonedDateTimeOrNull() ?: noTime()
//            var locations = matchOrNull<Map<String, String>>(this[HistoryEventProperty.LOCATIONS])
//                ?.map {
//                    (it.key.toZonedDateTimeOrNull() ?: noTime()) to (it.value.toCoordinateOrNull() ?: Coordinate(0.0, 0.0))
//                }?.toMap()
            val locationDescriptions = matchOrNull<Map<String, String>>(this[HistoryEventProperty.LOCATION_DESCRIPTIONS])
                ?.map {
                    (it.key.toZonedDateTimeOrNull() ?: noTime()) to it.value
                }?.toMap() ?: run {
                    brokenEvent = true
                    mapOf()
                }
            val locationPath = matchOrNull<String>(this[HistoryEventProperty.LOCATION_PATH])
//            if (locationPath == null && !locations.isNullOrEmpty()) {
//                val locationFile = createLocationFile(context)
//                locations.writeToLocationFile(locationFile)
//                locationPath = locationFile.absolutePath.removePrefix(File(context.filesDir, "locations").absolutePath + "/")
//            }
//            else if (locationPath != null) {
//                val locationFile = getLocationFile(locationPath, context)
//                locations = locationFile.locations
//            }
//            if (locations == null) {
//                brokenEvent = true
//                locations = mapOf()
//            }

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
//                locations = locations.toMutableMap(),
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

fun List<Map<String, Any>>.toHistoryEvent(context: Context): List<HistoryEvent> =
    this.map { it.toHistoryEvent(context) }

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

fun Map<String, Any?>.toHistoryFileData(context: Context): HistoryFileData {
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

fun HistoryFileData.toMap(): Map<String, Any?> = mapOf(
    "fileVersion" to this.fileVersion,
    "date" to this.date.format(DateTimeFormatter.ISO_DATE),
    "description" to this.description,
    "favorite" to this.favorite,
    "tags" to this.tags,
    "events" to this.events.toMap(),
    "metadata" to this.metadata
)

fun HistoryFileData.toJson(): String =
    Gson().toJson(this.toMap())

fun HistoryEvent.updateModifiedTime() {
    this.modified = ZonedDateTime.now()
}

fun HistoryFileData.Companion.get(context: Context, path: String): HistoryFileData? {
    val file = File(context.filesDir, path)

    return if (file.exists() && file.isFile) fromJson(file.readText(), context)
    else null
}

fun HistoryFileData.Companion.get(context: Context, date: LocalDate): HistoryFileData? =
    HistoryFileData.get(context, getFilePathFromDate(date))

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

            if (fileTime < from || fileTime > until) null
            else it.absolutePath
        }.toList(),
        context
    )
}

fun HistoryFileData.Companion.getList(
    context: Context,
    from: Instant = Instant.ofEpochMilli(0),
    until: Instant = Instant.MAX,
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
            fromJson(data, context)
        }
    }.toList()
}

//fun HistoryEvent.save(context: Context, ignoreOldData: Boolean = true) {
//    val date = this.time.toLocalDate()
//    val fileData = HistoryFileData.get(context, date)
//        ?: HistoryFileData(
//            date = date,
//            description = null,
//            events = mutableListOf()
//        )
//
//    // Cancel saving data if the newer one with the same ID is found
//    if (fileData.events.find { it.modified > this.modified && it.id == this.id } != null
//        && ignoreOldData) return
//    fileData.events.removeAll { it.id == this.id }
//    fileData.events.add(this)
//    fileData.save(context)
//}

//fun HistoryEvent.delete(context: Context, deleteAttachments: Boolean = true) {
//    val fileData = HistoryFileData.get(context, this.time.toLocalDate())
//
//    fileData?.events?.removeAll {
////        Log.d("NanHistoryDebug", "DELETING: ${it.title}, ID: ${it.id}")
//        if (it.id == this.id) {
//            if (it.audio != null && deleteAttachments) {
//                val audioFile = File(context.applicationContext.filesDir, "audio/${it.audio}")
////                Log.d("NanHistoryDebug", "Audio file: ${audioFile.absolutePath}")
//                if (audioFile.exists()) Log.d("NanHistoryDebug", "Audio file exists")
////                else Log.d("NanHistoryDebug", "Audio file doesn't exist")
//                audioFile.delete()
//            }
//            if (it.locationPath != null && deleteAttachments) {
//                val locationFile = File(context.filesDir, "locations/${it.locationPath}")
//                Log.d("NanHistoryDebug", "Location file: ${locationFile.absolutePath}")
//                if (locationFile.exists()) Log.d("NanHistoryDebug", "Location file exists")
//                else Log.d("NanHistoryDebug", "Location file doesn't exist")
//                if (locationFile.exists()) locationFile.delete()
//            }
//            true
//        }
//        else false
//    }
//    fileData?.save(context)
//}

suspend fun HistoryEvent.safeDelete(context: Context, deleteAttachments: Boolean = true) {
    val db = AppDatabase.getInstance(context)
    val dao = db.appDao()
    if (audio != null && deleteAttachments) {
        val audioFile = File(context.applicationContext.filesDir, "audio/$audio")
//                Log.d("NanHistoryDebug", "Audio file: ${audioFile.absolutePath}")
        if (audioFile.exists()) Log.d("NanHistoryDebug", "Audio file exists")
//                else Log.d("NanHistoryDebug", "Audio file doesn't exist")
        audioFile.delete()
    }
    if (locationPath != null && deleteAttachments) {
        val locationFile = File(context.filesDir, "locations/$locationPath")
        Log.d("NanHistoryDebug", "Location file: ${locationFile.absolutePath}")
        if (locationFile.exists()) Log.d("NanHistoryDebug", "Location file exists")
        else Log.d("NanHistoryDebug", "Location file doesn't exist")
        if (locationFile.exists()) locationFile.delete()
    }

    dao.deleteEvent(toEventEntity())
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

fun HistoryFileData.delete(context: Context) {
    val file = File(context.filesDir, getFilePathFromDate(this.date))
    if (file.exists()) file.delete()
}