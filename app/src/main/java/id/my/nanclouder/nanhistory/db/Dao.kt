package id.my.nanclouder.nanhistory.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface AppDao {
    /* Event Dao */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(event: List<EventEntity>)

    @Update
    suspend fun updateEvent(event: EventEntity)

    @Delete
    suspend fun deleteEvent(event: EventEntity)

    @Query("UPDATE events SET favorite = true WHERE id = :id")
    suspend fun setFavorite(id: String)

    @Query("UPDATE events SET favorite = false WHERE id = :id")
    suspend fun unsetFavorite(id: String)

    @Query("UPDATE events SET deletePermanently = :deleteTime WHERE id = :id")
    suspend fun softDeleteEvent(id: String, deleteTime: Long = Instant.now().plusSeconds(1_296_000).toEpochMilli())

    @Query("SELECT * FROM events WHERE deletePermanently = NULL AND id = :id LIMIT 1")
    suspend fun getEventById(id: String): EventEntity?

    @Query("SELECT * FROM events WHERE deletePermanently = NULL")
    fun getAllEvents(): Flow<List<EventEntity>>

    @Query(
        "SELECT * FROM events WHERE LOWER(title) LIKE LOWER('%' || :query || '%') OR LOWER(description) LIKE LOWER('%' || :query || '%')" +
        "OR LOWER(time) LIKE LOWER('%' || :query || '%')"
    )
    fun searchEvents(query: String): Flow<List<EventEntity>>

    @Transaction
    @Query("SELECT * FROM events WHERE id = :eventId")
    suspend fun getEventWithTags(eventId: String): EventWithTags?


    /* Day Dao */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDay(event: DayEntity)

    @Update
    suspend fun updateDay(event: DayEntity)

    @Delete
    suspend fun deleteDay(event: DayEntity)

    @Query("SELECT * FROM days")
    fun getAllDays(): Flow<List<DayEntity>>


    /* Tag Dao */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(event: TagEntity)

    @Update
    suspend fun updateTag(event: TagEntity)

    @Delete
    suspend fun deleteTag(event: TagEntity)

    @Query("SELECT * FROM tags")
    fun getAllTags(): Flow<List<TagEntity>>


    /* Tag Cross References */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEventTagCrossRef(crossRef: EventTagCrossRef)

    @Delete
    suspend fun deleteEventTagCrossRef(crossRef: EventTagCrossRef)
}