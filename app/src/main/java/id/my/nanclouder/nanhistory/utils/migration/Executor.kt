package id.my.nanclouder.nanhistory.utils.migration

import android.content.Context
import android.util.Log

/**
 * @param context Context of the application
 * @param onUpdate Callback to update the migration progress
 *
 * ### [onUpdate] params
 * 1. [MigrationState]: Progress of the migration
 * 2. [String]: Name of the migration step
 */
suspend fun migrateData(context: Context, onSafeToUse: () -> Unit, onUpdate: ((MigrationState, String) -> Unit)) {
    Log.d("NanHistoryDebug", "MIGRATING...")

    // Initialize migration state
    onUpdate(MigrationState(0f, true), "")

    // Tell the service that the application finally safe to use while the migration
    // while the migration can be continued on background
    onSafeToUse()
    Log.d("NanHistoryDebug", "MIGRATING... [SAFE TO USE]")

    // Migrations here
    migrateLocationData(context) { onUpdate(it, "locationData") }
    migrateToDatabase(context) { onUpdate(it, "toDatabase") }

    // Safe to use while migrations
    migrateLocationToDatabase(context) { onUpdate(it, "locationToDatabase") }

    // Mark the progress as complete (1f of 1f)
    onUpdate(MigrationState(1f, true), "toDatabase")
}