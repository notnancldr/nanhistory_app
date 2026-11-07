package id.my.nanclouder.nanhistory.core.common.security

import android.content.Context
import id.my.nanclouder.nanhistory.domain.model.HistoryEvent

/**
 * Interface contract for event cryptographic signature validation and generation.
 */
interface EventSignatureVerifier {
    suspend fun generateSignature(context: Context, event: HistoryEvent, apply: Boolean = false): String
    suspend fun validateSignature(context: Context, event: HistoryEvent): Boolean
}

/**
 * Global registry for signature verification, decoupled from direct DB dependencies.
 */
object EventSignatures {
    private var verifier: EventSignatureVerifier? = null

    fun register(verifier: EventSignatureVerifier) {
        this.verifier = verifier
    }

    suspend fun generateSignature(context: Context, event: HistoryEvent, apply: Boolean = false): String {
        return verifier?.generateSignature(context, event, apply) ?: ""
    }

    suspend fun validateSignature(context: Context, event: HistoryEvent): Boolean {
        return verifier?.validateSignature(context, event) ?: true
    }
}
