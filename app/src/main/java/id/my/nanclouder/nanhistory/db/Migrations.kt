package id.my.nanclouder.nanhistory.db

import android.content.Context
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import id.my.nanclouder.nanhistory.utils.LogData

const val QUERY_1_2 =
    "CREATE VIEW `events_time_with_tag` " +
    "AS SELECT events.id, events.time, tags.id as tag_id " +
    "FROM events " +
    "INNER JOIN event_tag_cross_refs ON events.id = event_tag_cross_refs.eventId " +
    "INNER JOIN tags ON event_tag_cross_refs.tagId = tags.id"

class Migrations(context: Context) {
    val migrations1to2 = object : Migration(1, 2) {
        val logData = LogData.inCommonPath("DB-MIGRATION-1to2")

        override fun migrate(db: SupportSQLiteDatabase) {
            logData.appendWithTimestamp("Starting migration v1 to v2")

            try {
                db.execSQL(QUERY_1_2)
            } catch (e: Throwable) {
                logData.appendWithTimestamp("==============================================")
                logData.append("Error while migrating from v1 to v2: ${e.message ?: "unknown error"}")
                logData.append(e.stackTraceToString())
            } finally {
                logData.save(context)
            }
        }
    }
}