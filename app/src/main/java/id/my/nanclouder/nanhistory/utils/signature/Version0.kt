package id.my.nanclouder.nanhistory.utils.signature

import android.content.Context
import com.google.gson.Gson
import id.my.nanclouder.nanhistory.utils.history.HistoryEvent
import id.my.nanclouder.nanhistory.utils.history.HistoryEventProperty
import id.my.nanclouder.nanhistory.utils.history.HistoryEventSignatureExcluded
import id.my.nanclouder.nanhistory.utils.history.toMap
import java.security.MessageDigest

suspend fun HistoryEvent.generateSignatureV0(
    context: Context
): ByteArray {
    // Log.d("NanHistoryDebug" , this.toMap().toString())
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
                getLocations(context).associate {
                    it.time.toOffsetDateTime().toString() to it.location.toString()
                }
        Gson().toJson(data)
    }
    // Log.d("NanHistoryDebug", stringData)
    val result = messageDigest.digest(stringData.toByteArray())

    // Log.d("NanHistoryDebug", "Generated: $result")
    return result
}