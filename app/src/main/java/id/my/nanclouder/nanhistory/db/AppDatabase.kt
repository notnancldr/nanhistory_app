package id.my.nanclouder.nanhistory.db

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import id.my.nanclouder.nanhistory.config.Config
import id.my.nanclouder.nanhistory.utils.history.HistoryDay
import id.my.nanclouder.nanhistory.utils.history.HistoryEvent
import id.my.nanclouder.nanhistory.ui.main.EventSelectMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate

@Database(
    entities = [
        EventEntity::class,
        DayEntity::class,
        TagEntity::class,
        EventTagCrossRef::class,
        DayTagCrossRef::class
    ],
    views = [
        EventsTimeWithTag::class
    ],
    version = 2
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        const val DATABASE_NAME = "nanhistory_db"

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val migrations = Migrations(context)

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(migrations.migrations1to2)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun clearInstance() {
            INSTANCE?.close()
            INSTANCE = null
        }

        fun getEventsInRange(
            dao: AppDao, from: LocalDate, until: LocalDate = LocalDate.now(),
            mode: EventSelectMode = EventSelectMode.Default
        ): Flow<List<HistoryEvent>> {
            return when (mode) {
                EventSelectMode.FavoriteEvent -> dao.getFavoriteEvents()
                EventSelectMode.Deleted -> dao.getDeletedEventsFlow()
                else -> dao.getEventsInRange(from.toString(), until.toString())
            }.map { events ->
                events.map {
                    it.toHistoryEvent()
                }
            }
        }

        fun getDaysInRange(
            dao: AppDao, from: LocalDate, until: LocalDate = LocalDate.now(),
            favorite: Boolean = false
        ): Flow<List<HistoryDay>> {
            return if (!favorite) dao.getDaysInRange(from, until).map { days ->
                days.map {
                    it.toHistoryDay()
                }
            } else dao.getFavoriteDays().map { days ->
                days.map {
                    it.toHistoryDay()
                }
            }
        }

        fun getEventsAll(dao: AppDao) =
            getEventsInRange(dao, LocalDate.MIN, LocalDate.MAX)

        fun getDaysAll(dao: AppDao) =
            getDaysInRange(dao, LocalDate.MIN, LocalDate.MAX)

        fun search(dao: AppDao, query: String): Flow<List<HistoryEvent>> {
            return dao.searchEvents(query).map { events ->
                events.map { it.toHistoryEvent() }
            }
        }

        fun searchMatchWholeWord(dao: AppDao, query: String): Flow<List<HistoryEvent>> {
            return dao.searchEventsMatchWholeWord(query).map { events ->
                events.map { it.toHistoryEvent() }
            }
        }

        fun searchWithTagIds(dao: AppDao, query: String, tags: List<String>): Flow<List<HistoryEvent>> {
            return dao.searchEventsWithTagIds(query, tags).map { events ->
                events.map { it.toHistoryEvent() }
            }
        }

        suspend fun moveToTrash(dao: AppDao, context: Context, ids: List<String>) {
            val delete1hour = Config.developer1hourAutoDelete.get(context) && Config.developerModeEnabled.get(context)
            if (delete1hour)
                dao.softDeleteEvents(ids, Instant.now().plusSeconds(3600).toEpochMilli())
            else
                dao.softDeleteEvents(ids)
        }

        suspend fun ensureDayExists(dao: AppDao, date: LocalDate) {
            if (dao.getDayByDate(date) == null) {
                dao.insertDay(
                    day = DayEntity(
                        date = date,
                        description = "",
                        favorite = false,
                        metadata = mapOf()
                    )
                )
            }
        }
    }

    suspend fun forceCheckpoint() {
        withContext(Dispatchers.IO) {
            val db = INSTANCE?.openHelper
                ?.writableDatabase
            db?.query("pragma wal_checkpoint(restart)")?.use {
                // Optionally inspect results
                if (it.moveToFirst()) {
                    val result = it.getInt(0)
                    Log.d("Checkpoint", "Result code: $result")
                }
            }
        }
    }
}