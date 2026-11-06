package id.my.nanclouder.nanhistory.utils.history

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import id.my.nanclouder.nanhistory.db.AppDatabase
import id.my.nanclouder.nanhistory.db.toEventEntity
import id.my.nanclouder.nanhistory.db.toLocationData
import id.my.nanclouder.nanhistory.db.toLocationEntity
import id.my.nanclouder.nanhistory.domain.model.Coordinate
import id.my.nanclouder.nanhistory.domain.model.EVENT_VERSION_NUMBER
import id.my.nanclouder.nanhistory.utils.getAudioFile
import id.my.nanclouder.nanhistory.utils.getLocationFile
import kotlinx.coroutines.flow.first
import java.io.File
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

typealias HistoryDay = id.my.nanclouder.nanhistory.domain.model.HistoryDay
typealias HistoryEvent = id.my.nanclouder.nanhistory.domain.model.HistoryEvent
typealias EventPoint = id.my.nanclouder.nanhistory.domain.model.EventPoint
typealias EventRange = id.my.nanclouder.nanhistory.domain.model.EventRange
typealias LocationData = id.my.nanclouder.nanhistory.domain.model.LocationData

fun generateEventId(instant: Instant = Instant.now()): String =
    id.my.nanclouder.nanhistory.domain.model.generateEventId(instant)

// Backward compatibility extension for HistoryEvent to get locations
@Suppress("DEPRECATION")
suspend fun HistoryEvent.getLocations(context: Context): List<LocationData> {
    if (locationPath == null) {
        val db = AppDatabase.getInstance(context)
        val dao = db.appDao()

        return dao.getLocationsByEventId(id).first().map {
            it.toLocationData()
        }
    }
    return (locationPath?.let {
        val locationFile = getLocationFile(it, context)
        locationFile.locations.ifEmpty { null }
    }) ?: listOf()
}

fun File.toLocationPath(context: Context) =
    this.absolutePath.removePrefix(File(context.filesDir, "locations").absolutePath + "/")

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
 */
fun Map<ZonedDateTime, Coordinate>.toLocationData() =
    this.map { LocationData(
        time = it.key,
        location = it.value
    ) }

/**
 * Append list of [LocationData] into an existing location file
 */
@Deprecated(
    "Use List<LocationData>.insertToDatabase instead",
    ReplaceWith("fun List<LocationData>.insertToDatabase(context: Context, eventId: String): String")
)
fun List<LocationData>.appendToLocationFile(file: File): String {
    var data = ""
    if (!file.exists()) {
        file.createNewFile()
        data += "$LOCATION_FORMAT_VERSION\n\n"
    }

    data += this.joinToString("") { locationData ->
        locationData.toString() + "\n"
    }
    file.appendText(data)
    return data
}

/**
 * Append list of [LocationData] into database
 */
suspend fun List<LocationData>.insertToDatabase(context: Context, eventId: String) {
    val db = AppDatabase.getInstance(context)
    val dao = db.appDao()

    val converted = this.map { it.toLocationEntity(eventId) }
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

    if (currentLocationPath != null && deleteAttachments) {
        val locationFile = getLocationFile(context, currentLocationPath)
        if (locationFile.exists()) Log.d("NanHistoryDebug", "Location file exists")
        if (locationFile.exists()) locationFile.delete()
    }

    dao.deleteEvent(toEventEntity())
}
