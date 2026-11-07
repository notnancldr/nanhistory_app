package id.my.nanclouder.nanhistory.utils.signature

import android.content.Context
import android.util.Log
import id.my.nanclouder.nanhistory.utils.StreamDigest
import id.my.nanclouder.nanhistory.utils.history.HistoryEvent

typealias EventDataDigest = id.my.nanclouder.nanhistory.core.common.security.EventDataDigest
typealias EventLocationDigest = id.my.nanclouder.nanhistory.core.common.security.EventLocationDigest

suspend fun HistoryEvent.generateSignature(
    context: Context,
    apply: Boolean = false
): String {
    val digested = if (versionNumber >= 2) {
      generateSignatureV2(context)
    } else if (versionNumber >= 1) {
        generateSignatureV1(context)
    } else {
        generateSignatureV0(context)
    }

    val result = digested.joinToString("") { "%02x".format(it) }
    if (apply) this.signature = result

    return result
}

suspend fun HistoryEvent.validateSignature(context: Context): Boolean =
    this.signature == this.generateSignature(context = context)