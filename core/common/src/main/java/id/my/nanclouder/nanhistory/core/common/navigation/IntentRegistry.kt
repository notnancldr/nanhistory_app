package id.my.nanclouder.nanhistory.core.common.navigation

import android.content.Context
import android.content.Intent

interface IntentProvider {
    fun createEventDetailIntent(context: Context, eventId: String, path: String? = null): Intent
    fun createEditEventIntent(
        context: Context,
        eventId: String? = null,
        previousEventOf: String? = null,
        nextEventOf: String? = null
    ): Intent
    fun createEventLocationIntent(context: Context, eventId: String, cutMode: Boolean = false): Intent
    fun createShareMapIntent(context: Context, eventId: String): Intent
    fun createTagDetailIntent(context: Context, tagId: String): Intent
    fun createYearlyMapIntent(context: Context): Intent
    fun createTrashIntent(context: Context): Intent
    fun createSettingsIntent(context: Context): Intent
    fun createBackupIntent(context: Context): Intent
    fun createDebugIntent(context: Context): Intent?
}

object IntentRegistry {
    private var provider: IntentProvider? = null

    fun register(provider: IntentProvider) {
        this.provider = provider
    }

    private fun getProvider(): IntentProvider {
        return checkNotNull(provider) { "IntentProvider has not been registered in IntentRegistry." }
    }

    fun eventDetail(context: Context, eventId: String, path: String? = null): Intent =
        getProvider().createEventDetailIntent(context, eventId, path)

    fun editEvent(
        context: Context,
        eventId: String? = null,
        previousEventOf: String? = null,
        nextEventOf: String? = null
    ): Intent = getProvider().createEditEventIntent(context, eventId, previousEventOf, nextEventOf)

    fun eventLocation(context: Context, eventId: String, cutMode: Boolean = false): Intent =
        getProvider().createEventLocationIntent(context, eventId, cutMode)

    fun shareMap(context: Context, eventId: String): Intent =
        getProvider().createShareMapIntent(context, eventId)

    fun tagDetail(context: Context, tagId: String): Intent =
        getProvider().createTagDetailIntent(context, tagId)

    fun yearlyMap(context: Context): Intent =
        getProvider().createYearlyMapIntent(context)

    fun trash(context: Context): Intent =
        getProvider().createTrashIntent(context)

    fun settings(context: Context): Intent =
        getProvider().createSettingsIntent(context)

    fun backup(context: Context): Intent =
        getProvider().createBackupIntent(context)

    fun debug(context: Context): Intent? =
        provider?.createDebugIntent(context)
}
