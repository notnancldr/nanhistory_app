package id.my.nanclouder.nanhistory.utils.history

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import id.my.nanclouder.nanhistory.db.AppDatabase
import id.my.nanclouder.nanhistory.db.toEventEntity
import id.my.nanclouder.nanhistory.db.toLocationData
import id.my.nanclouder.nanhistory.db.toLocationEntity
import id.my.nanclouder.nanhistory.utils.Coordinate
import id.my.nanclouder.nanhistory.utils.getAudioFile
import id.my.nanclouder.nanhistory.utils.getLocationFile
import id.my.nanclouder.nanhistory.utils.signature.validateSignature
import id.my.nanclouder.nanhistory.utils.toCoordinate
import kotlinx.coroutines.flow.first
import java.io.File
import java.math.RoundingMode
import java.text.DecimalFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.collections.joinToString
import kotlin.collections.toMap
import kotlin.random.Random
import kotlin.random.nextULong

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
    @Deprecated("Use database system instead")
    open var locationPath: String? = null,
    open var versionNumber: Int = EVENT_VERSION_NUMBER,
) {
    private var locationsData: List<LocationData>? = null

    // Ignore deprecation because we are using deprecated functions as backward compatibility
    @Suppress("DEPRECATION")
    suspend fun getLocations(context: Context): List<LocationData> {
        if (locationPath == null) {
            val db = AppDatabase.getInstance(context)
            val dao = db.appDao()

            locationsData = dao.getLocationsByEventId(id).first().map {
                // Log.d("LocationDBTest", "$it")
                it.toLocationData()
            }
            return locationsData ?: listOf()
        }
        return (locationPath?.let {
            val locationFile = getLocationFile(it, context)
            locationsData = locationFile.locations.ifEmpty {
                // This part probably breaks signature system (deleting without regenerate signature):
                // File(it).delete()
                // locationPath = null
                null
            } ?: listOf()
            locationsData
        })?: listOf()
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
    @Deprecated("Use database system instead")
    override var locationPath: String? = null,
    override var versionNumber: Int = EVENT_VERSION_NUMBER,
    // val location: Coordinate? = null
) : HistoryEvent(id, title, description, time, favorite,
    created = created,
    modified = modified,
    type = "point",
    locationPath = locationPath,
    versionNumber = versionNumber
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
    @Deprecated("Use database system instead")
    override var locationPath: String? = null,
    override var audio: String? = null,
    var end: ZonedDateTime,
    var transportationType: TransportationType = TransportationType.Unspecified,
    var locationDescriptions: MutableMap<ZonedDateTime, String> = mutableMapOf(),
    override var versionNumber: Int = EVENT_VERSION_NUMBER,
    // var locations: MutableMap<ZonedDateTime, Coordinate> = mutableMapOf(),
) : HistoryEvent(id, title, description, time, favorite,
    created = created,
    modified = modified,
    type = "range",
    locationPath = locationPath,
    versionNumber = versionNumber
)

data class LocationData(
    val time: ZonedDateTime,
    val location: Coordinate,

    // New fields
    val speed: Float? = null,
    val bearing: Float? = null,
    val altitude: Double? = null,

    // Accuracy fields
    val accuracy: Float? = null,
    val speedAccuracy: Float? = null,
    val bearingAccuracy: Float? = null,
    val verticalAccuracy: Float? = null,
) {
    override fun toString(): String {
        val formatter = DecimalFormat("#.####").apply {
            roundingMode = RoundingMode.HALF_UP
        }

        fun formatNumber(value: Number?): String {
            return value?.let { formatter.format(it.toDouble()) } ?: ""
        }

        return listOf(
            time.toOffsetDateTime().toString(),

            // Location
            location.toString(),
            formatNumber(accuracy),

            // Speed
            formatNumber(speed),
            formatNumber(speedAccuracy),

            // Bearing
            formatNumber(bearing),
            formatNumber(bearingAccuracy),

            // Altitude
            formatNumber(altitude),
            formatNumber(verticalAccuracy),
        ).joinToString("|")
    }

    companion object {
        fun fromString(data: String): LocationData {
            val split = data.split("|")

            return LocationData(
                time = ZonedDateTime.parse(split[0]),

                // Location
                location = split[1].toCoordinate(),
                accuracy = split.getOrNull(2)?.toFloatOrNull(),

                // Speed
                speed = split.getOrNull(3)?.toFloatOrNull(),
                speedAccuracy = split.getOrNull(4)?.toFloatOrNull(),

                // Bearing
                bearing = split.getOrNull(5)?.toFloatOrNull(),
                bearingAccuracy = split.getOrNull(6)?.toFloatOrNull(),

                // Altitude
                altitude = split.getOrNull(7)?.toDoubleOrNull(),
                verticalAccuracy = split.getOrNull(8)?.toFloatOrNull(),
            )
        }
    }
}

fun File.toLocationPath(context: Context) =
    this.absolutePath.removePrefix(File(context.filesDir, "locations").absolutePath + "/")

fun generateEventId(instant: Instant = Instant.now()): String =
    "${instant.toEpochMilli().toString(16)}-${Random.nextULong().toString(16)}"

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

/**
 * Write time-coordinate mapping into location file. Write into new file or overwrite existing file.
 *
 * @param file A location file, can be existing file or a new one.
 */
@Deprecated(
    "Use fun List<LocationData>.appendToLocationFile(file: File) instead",
    ReplaceWith("fun List<LocationData>.appendToLocationFile(file: File)")
)
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

/**
 * Convert time-coordinate mapping into list of [LocationData]
 *
 * @return [List] of [LocationData]
 */
fun Map<ZonedDateTime, Coordinate>.toLocationData() =
    this.map { LocationData(
        time = it.key,
        location = it.value
    ) }

/**
 * Append list of [LocationData] into an existing location file
 *
 * @param file Existing location file
 * @return Appended data in [String]
 */
@Deprecated(
    "Use List<LocationData>.insertToDatabase instead",
    ReplaceWith("fun List<LocationData>.insertToDatabase(context: Context, eventId: String): String")
)
fun List<LocationData>.appendToLocationFile(file: File): String {
    var data = ""
    if (!file.exists()) {
        file.createNewFile()

        // Format version stored at line index 0
        // Double line break to reserve line index 1
        data += "$LOCATION_FORMAT_VERSION\n\n"
    }

    // Location data starts at line index 2
    data += this.joinToString("") { locationData ->
        locationData.toString() + "\n"
    }
    file.appendText(
        data
    )

    return data // return written data
}

/**
 * Append list of [LocationData] into database
 *
 * @param context Context
 * @param eventId Event ID
 * @return Appended data in [String]
 */
suspend fun List<LocationData>.insertToDatabase(context: Context, eventId: String) {
    val db = AppDatabase.getInstance(context)
    val dao = db.appDao()

    val converted = this.map { it.toLocationEntity(eventId) }

//    dao.insertLocations(converted)
    converted.forEach {
        dao.insertLocation(it)
    }
    Log.d("LocationDB", "Inserted ${converted.size} locations to $eventId")
}

fun HistoryEvent.updateModifiedTime() {
    this.modified = ZonedDateTime.now()
}

@Suppress("DEPRECATION")
suspend fun HistoryEvent.safeDelete(context: Context, deleteAttachments: Boolean = true) {
    val db = AppDatabase.getInstance(context)
    val dao = db.appDao()

    val currentAudioPath = audio
    val currentLocationPath = locationPath

    if (currentAudioPath != null && deleteAttachments) {
        val audioFile = getAudioFile(context, currentAudioPath)
        if (audioFile.exists()) Log.d("NanHistoryDebug", "Audio file exists")
        audioFile.delete()
    }

    // TODO: location file cannot exist after migration
    // Backward compatibility
    if (currentLocationPath != null && deleteAttachments) {
        val locationFile = getLocationFile(context, currentLocationPath)
        if (locationFile.exists()) Log.d("NanHistoryDebug", "Location file exists")
        if (locationFile.exists()) locationFile.delete()
    }

    dao.deleteEvent(toEventEntity())
}