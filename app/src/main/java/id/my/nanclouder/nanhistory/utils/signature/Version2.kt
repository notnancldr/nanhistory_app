package id.my.nanclouder.nanhistory.utils.signature

import android.content.Context
import android.util.Log
import id.my.nanclouder.nanhistory.db.AppDatabase
import id.my.nanclouder.nanhistory.db.toLocationData
import id.my.nanclouder.nanhistory.utils.history.HistoryEvent
import kotlinx.coroutines.flow.first
import java.security.MessageDigest

suspend fun HistoryEvent.locationDigestV2(context: Context): ByteArray {
    val db = AppDatabase.getInstance(context)
    val dao = db.appDao()

    val locationDigest = EventLocationDigest()
    dao.getLocationsByEventId(id).first().forEachIndexed { index, data ->
        if (index > 0) locationDigest.update("\n".encodeToByteArray())
        locationDigest.update(data.toString().encodeToByteArray())
        Log.d("Signature", "Digesting: --$data--")
    }

    return locationDigest.finalizeDigest()
}

suspend fun HistoryEvent.generateSignatureV2(
    context: Context,
    locationDigest: EventLocationDigest? = null,
    eventDigest: EventDataDigest? = null,
): ByteArray {
    // Get current digest state from location digest or digest current location
    val locationDigested = locationDigest?.getDigestState() ?: locationDigestV2(context)

    // Get current digest state from data digest or digest current event data
    val eventDataDigested = eventDigest?.getDigestState() ?: dataDigestV1()

    val messageDigest = MessageDigest.getInstance("SHA-512")

    return messageDigest.digest(eventDataDigested + locationDigested)
}