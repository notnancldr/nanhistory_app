package id.my.nanclouder.nanhistory.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.core.content.FileProvider
import org.osmdroid.util.GeoPoint
import java.io.File
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.round

// The numbers before 5 is reserved for Flutter application
const val FILE_VERSION = 6

val DateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy")
val TimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
val TimeFormatterWithSecond: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

object ServiceBroadcast {
    const val ACTION_SERVICE_STATUS = "id.my.nanclouder.nanhistory.ACTION_SERVICE_STATUS"
    const val EXTRA_STATUS = "EXTRA_IS_RUNNING"
    const val EXTRA_EVENT_ID = "EXTRA_EVENT_ID"
    const val EXTRA_EVENT_PATH = "EXTRA_EVENT_PATH"
    const val EXTRA_EVENT_POINT = "EXTRA_EVENT_POINT"

    const val ACTION_UPDATE_DATA = "id.my.nanclouder.nanhistory.ACTION_UPDATE_DATA"
}

object RecordStatus {
    const val RESTARTING = -2
    const val BUSY = -1
    const val READY = 0
    const val IDLE = 1
    const val RUNNING = 2
}

data class Coordinate(val latitude: Double, val longitude: Double) {
    override fun toString(): String = "$latitude,$longitude"
}

fun Coordinate.toGeoPoint(): GeoPoint {
    return GeoPoint(this.latitude, this.longitude)
}

inline fun <reified T> matchOrNull(value: Any?): T? =
    if (value is T) value else null

fun String.toCoordinateOrNull(): Coordinate? {
    val parts = this.split(",", limit = 2)
        .map { it.trim().toDoubleOrNull() }

    return if (parts.size == 2 && parts[0] != null && parts[1] != null) {
        Coordinate(parts[0]!!, parts[1]!!)
    } else {
        null
    }
}

fun String.toCoordinate(): Coordinate = this.toCoordinateOrNull() ?: throw NumberFormatException()

fun String.toZonedDateTimeOrNull(): ZonedDateTime? =
    try { ZonedDateTime.parse(this) } catch (e: Exception) { null }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerModal(
    state: DatePickerState? = null,
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = state ?: rememberDatePickerState()

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onDateSelected(datePickerState.selectedDateMillis)
                onDismiss()
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    state: TimePickerState? = null,
    onDismiss: () -> Unit,
    onTimeSelected: (Int, Int) -> Unit,
) {
    val timePickerState = state ?: rememberTimePickerState()
    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text("Cancel")
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onTimeSelected(timePickerState.hour, timePickerState.minute)
                onDismiss()
            }) {
                Text("OK")
            }
        },
        text = {
            TimePicker(
                state = timePickerState,
            )
        }
    )
}

///// SOURCE: stackoverflow

@Composable
fun <T: Any> rememberMutableStateListOf(vararg elements: T): SnapshotStateList<T> {
    return rememberSaveable(saver = snapshotStateListSaver()) {
        elements.toList().toMutableStateList()
    }
}

private fun <T : Any> snapshotStateListSaver() = listSaver<SnapshotStateList<T>, T>(
    save = { stateList -> stateList.toList() },
    restore = { it.toMutableStateList() },
)

fun getPackageInfo(context: Context): PackageInfo? {
    return try {
        context.packageManager.getPackageInfo(context.packageName, 0)
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }
}

fun readableSize(size: Double): String {
    return if (size > 1_000_000_000) "${round(size / 100_000_000) / 10} GB"
        else if (size > 1_000_000) "${round(size / 100_000) / 10} MB"
        else if (size > 1_000) "${round(size / 100) / 10} KB"
        else "${size.toInt()} Bytes"
}

fun readableSize(size: Float) = readableSize(size.toDouble())
fun readableSize(size: Int) = readableSize(size.toDouble())
fun readableSize(size: Long) = readableSize(size.toDouble())

fun readableTime(duration: Duration): String {
    val days = duration.toDays()
    val hours = duration.toHoursPart()
    val minutes = duration.toMinutesPart()
    val seconds = duration.toSecondsPart()

    var text = ""
    if (days > 0) text += "${days}d "
    if (hours > 0) text += "${hours}h "
    if (minutes > 0) text += "${minutes}m "
    if (seconds > 0) text += "${seconds}s"
    return text
}

fun readableTimeHours(duration: Duration): String {
    val days = duration.toDays()
    val hours = duration.toHoursPart()
    val minutes = duration.toMinutesPart()
    val seconds = duration.toSecondsPart()

    var text = ""
    if (days > 0) text += "${days.toString().padStart(2, '0')}:"
    if (hours > 0) text += "${hours.toString().padStart(2, '0')}:"
    text += "${minutes.toString().padStart(2, '0')}:"
    text += seconds.toString().padStart(2, '0')
    return text
}

fun getAudioFile(context: Context, path: String) =
    File(context.filesDir, "audio/$path")

fun getLocationFile(context: Context, path: String) =
    File(context.filesDir, "locations/$path")

fun getAudioFiles(context: Context) =
    File(context.filesDir, "audio")
        .walkTopDown().filter { it.isFile }.toList()

fun getLocationFiles(context: Context) =
    File(context.filesDir, "locations")
        .walkTopDown().filter { it.isFile }.toList()

fun shareFile(context: Context, fileName: String) {
    val file = File(context.filesDir, fileName)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "*/*"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(Intent.createChooser(intent, "Share file via"))
}

/**
 * Shares a file using an ACTION_SEND intent.
 * This function handles creating a FileProvider URI for the file.
 *
 * @param context The context from which to start the activity.
 * @param filePath The absolute path to the file to be shared.
 */
fun shareFileAbsolute(context: Context, filePath: String) {
    val file = File(filePath)
    if (!file.exists()) {
        Toast.makeText(context, "File not found for sharing.", Toast.LENGTH_SHORT).show()
        return
    }

    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider", // Make sure this matches the authority in your manifest
            file
        )

        val shareIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            type = context.contentResolver.getType(uri) // Dynamically get MIME type
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Image"))
    } catch (e: Exception) {
        Log.e("ShareMap", "Error sharing file: $e\nFile path: $filePath")
        Toast.makeText(context, "Failed to prepare image for sharing: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}