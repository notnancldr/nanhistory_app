package id.my.nanclouder.nanhistory.utils.signature

import android.content.Context
import com.google.gson.Gson
import id.my.nanclouder.nanhistory.utils.history.HistoryEvent
import id.my.nanclouder.nanhistory.utils.history.HistoryEventProperty
import id.my.nanclouder.nanhistory.utils.history.HistoryEventSignatureExcluded
import id.my.nanclouder.nanhistory.utils.history.getLocationFilePath
import id.my.nanclouder.nanhistory.utils.history.toMap
import java.io.File
import java.security.MessageDigest

fun HistoryEvent.dataDigestV1(): ByteArray {
    val messageDigest = MessageDigest.getInstance("SHA-512")
    val stringData = this.let {
        val data = this.toMap().toMutableMap()
        for (excluded in HistoryEventSignatureExcluded) {
            data.remove(excluded)
        }
        if (this.audio == null)
            data.remove(HistoryEventProperty.AUDIO)

        Gson().toJson(data)
    }
    // Log.d("NanHistoryDebug", stringData)
    val result = messageDigest.digest(stringData.toByteArray())

    //     .joinToString("") { "%02x".format(it) }
    // Log.d("NanHistoryDebug", "Generated: $result")
    return result
}

@Suppress("DEPRECATION")
fun HistoryEvent.locationDigestV1(context: Context): ByteArray? {
    val relativePath = locationPath
    val absolutePath = if (relativePath != null) getLocationFilePath(relativePath, context) else return null
    val file = File(absolutePath)

    val locationDigest = EventLocationDigest()

    if (!file.exists()) return null

    locationDigest.update(file.readText().toByteArray())

    return locationDigest.finalizeDigest()
}

fun HistoryEvent.generateSignatureV1(
    context: Context,
    locationDigest: EventLocationDigest? = null,
    eventDigest: EventDataDigest? = null,
): ByteArray {
    // Get current digest state from location digest or digest current location
    val locationDigested = locationDigest?.getDigestState() ?: locationDigestV1(context) ?: ByteArray(1)

    // Get current digest state from data digest or digest current event data
    val eventDataDigested = eventDigest?.getDigestState() ?: dataDigestV1()

    val messageDigest = MessageDigest.getInstance("SHA-512")

    return messageDigest.digest(eventDataDigested + locationDigested)
}