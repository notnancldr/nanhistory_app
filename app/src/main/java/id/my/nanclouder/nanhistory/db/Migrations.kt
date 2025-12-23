package id.my.nanclouder.nanhistory.db

import android.content.Context
import android.util.Log
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import id.my.nanclouder.nanhistory.utils.LogData

const val QUERY_1_2 =
    "CREATE VIEW `events_time_with_tag` " +
    "AS SELECT events.id, events.time, tags.id as tag_id " +
    "FROM events " +
    "INNER JOIN event_tag_cross_refs ON events.id = event_tag_cross_refs.eventId " +
    "INNER JOIN tags ON event_tag_cross_refs.tagId = tags.id"

const val QUERY_2_3 = "ALTER TABLE events ADD COLUMN versionNumber INTEGER NOT NULL DEFAULT 0"


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

                Log.e("DBMigration", "Error while migrating from v1 to v2", e)
            } finally {
                logData.save(context)
            }
        }
    }

    val migrations2to3 = object : Migration(2, 3) {
        val logData = LogData.inCommonPath("DB-MIGRATION-2to3")

        override fun migrate(db: SupportSQLiteDatabase) {
            logData.appendWithTimestamp("Starting migration v2 to v3")

            try {
                db.execSQL(QUERY_2_3)
            } catch (e: Throwable) {
                logData.appendWithTimestamp("==============================================")
                logData.append("Error while migrating from v2 to v3: ${e.message ?: "unknown error"}")
                logData.append(e.stackTraceToString())

                Log.e("DBMigration", "Error while migrating from v2 to v3", e)
            } finally {
                logData.save(context)
            }
        }
    }
}