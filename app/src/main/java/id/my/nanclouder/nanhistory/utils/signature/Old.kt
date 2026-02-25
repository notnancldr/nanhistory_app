package id.my.nanclouder.nanhistory.utils.signature

import android.util.Log
import com.google.gson.Gson
import id.my.nanclouder.nanhistory.utils.history.HistoryEventProperty
import id.my.nanclouder.nanhistory.utils.history.HistoryEventSignatureExcluded
import java.security.MessageDigest

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
    val result = messageDigest.digest(stringData.toByteArray())
        .joinToString("") { "%02x".format(it) }
    return result
}