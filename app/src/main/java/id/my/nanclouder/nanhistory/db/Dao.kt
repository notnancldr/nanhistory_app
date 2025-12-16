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
import java.time.LocalDate

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

    @Delete
    suspend fun deleteEvents(events: List<EventEntity>)

    @Query("UPDATE events SET favorite = true WHERE id = :id")
    suspend fun setFavoriteEvent(id: String)

    @Query("UPDATE events SET favorite = false WHERE id = :id")
    suspend fun unsetFavoriteEvent(id: String)

    @Query("UPDATE events SET favorite = not favorite WHERE id = :id")
    suspend fun toggleFavoriteEvent(id: String)

    @Query("UPDATE events SET deletePermanently = :deleteTime WHERE id IN (:ids)")
    suspend fun softDeleteEvents(
        ids: List<String>,
        deleteTime: Long = Instant.now().plusSeconds(2_592_000).toEpochMilli()
    )

    @Query("SELECT * FROM events WHERE audio = :audio")
    fun getEventsByAudio(audio: String): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE locationPath = :location")
    fun getEventsByLocation(location: String): Flow<List<EventEntity>>

    @Query("UPDATE events SET deletePermanently = NULL WHERE id IN (:ids)")
    suspend fun restoreEvents(
        ids: List<String>
    )

    @Transaction
    @Query("SELECT * FROM events WHERE deletePermanently IS NULL ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<EventWithTags>>

    @Transaction
    @Query("SELECT * FROM events WHERE deletePermanently IS NULL AND favorite = true ORDER BY timestamp DESC")
    fun getFavoriteEvents(): Flow<List<EventWithTags>>

    @Transaction
    @Query("SELECT * FROM events WHERE deletePermanently IS NULL AND date = :date ORDER BY timestamp DESC")
    fun getEventsByDay(date: LocalDate): Flow<List<EventWithTags>>

    @Transaction
    @Query("SELECT * FROM events WHERE deletePermanently IS NOT NULL ORDER BY timestamp DESC")
    fun getDeletedEventsFlow(): Flow<List<EventWithTags>>

    @Transaction
    @Query("SELECT * FROM events WHERE deletePermanently IS NOT NULL ORDER BY timestamp DESC")
    suspend fun getDeletedEvents(): List<EventWithTags>

    @Transaction
    @Query(
        "SELECT * FROM events WHERE (LOWER(title) LIKE LOWER('%' || :query || '%')" +
        "OR LOWER(description) LIKE LOWER('%' || :query || '%')" +
        "OR LOWER(time) LIKE LOWER('%' || :query || '%')) AND deletePermanently " +
        "IS NULL ORDER BY timestamp DESC"
    )
    fun searchEvents(query: String): Flow<List<EventWithTags>>

    @Transaction
    @Query(
        "SELECT * FROM events WHERE (" +
                // ----- TITLE FIELD -----
                "(' ' || LOWER(" +
                "REPLACE(" +
                "REPLACE(" +
                "REPLACE(" +
                "REPLACE(" +
                "REPLACE(" +
                "REPLACE(" +
                "REPLACE(" +
                "REPLACE(" +
                "REPLACE(" +
                "REPLACE(" +
                "REPLACE(" +
                "REPLACE(" +
                "REPLACE(title, CHAR(10), ' ')," +      // \n
                "CHAR(13), ' ')," +                     // \r
                "CHAR(9), ' ')," +                      // \t
                "'.', ' ')," +               // .
                "',' , ' ')," +              // ,
                "':', ' ')," +               // :
                "';', ' ')," +               // ;
                "'\"', ' ')," +              // "
                "'''', ' ')," +              // '
                "'!', ' ')," +               // !
                "'?', ' ')," +               // ?
                "'(', ' ')," +               // (
                "')', ' ')" +                // )
                ") || ' ') LIKE '% ' || LOWER(:query) || ' %' " +

                "OR " +

                // ----- DESCRIPTION FIELD -----
                "(' ' || LOWER(" +
                "REPLACE(" +
                "REPLACE(" +
                "REPLACE(" +
                "REPLACE(" +
                "REPLACE(" +
                "REPLACE(" +
                "REPLACE(" +
                "REPLACE(" +
                "REPLACE(" +
                "REPLACE(" +
                "REPLACE(" +
                "REPLACE(" +
                "REPLACE(description, CHAR(10), ' ')," +
                "CHAR(13), ' ')," +
                "CHAR(9), ' ')," +
                "'.', ' ')," +
                "',' , ' ')," +
                "':', ' ')," +
                "';', ' ')," +
                "'\"', ' ')," +
                "'''', ' ')," +
                "'!', ' ')," +
                "'?', ' ')," +
                "'(', ' ')," +
                "')', ' ')" +
                ") || ' ') LIKE '% ' || LOWER(:query) || ' %' " +

                ") AND deletePermanently IS NULL " +
                "ORDER BY timestamp DESC"
    )
    fun searchEventsMatchWholeWord(query: String): Flow<List<EventWithTags>>

    @Transaction
    @Query(
        "SELECT DISTINCT evt.* FROM events evt " +
                "JOIN event_tag_cross_refs crsrf ON evt.id = crsrf.eventId " +
                "WHERE crsrf.tagId IN (:tagIds) AND evt.deletePermanently IS NULL " +
                "AND (LOWER(evt.title) LIKE LOWER('%' || :query || '%') " +
                "OR LOWER(evt.description) LIKE LOWER('%' || :query || '%')) " +
                "GROUP BY evt.id " +
                "HAVING COUNT(DISTINCT crsrf.tagId) = :tagCount " +
                "ORDER BY evt.timestamp DESC"
    )
    fun searchEventsWithTagIds(
        query: String,
        tagIds: List<String>,
        tagCount: Int = tagIds.size
    ): Flow<List<EventWithTags>>

    @Transaction
    @Query("SELECT * FROM events WHERE id IN (:eventId)")
    suspend fun getEventById(eventId: String): EventWithTags?

    @Transaction
    @Query("SELECT * FROM events WHERE id = :eventIds")
    suspend fun getEventsByIds(eventIds: List<String>): List<EventWithTags>

    @Transaction
    @Query("SELECT * FROM events WHERE id = :eventId")
    fun getEventFlowById(eventId: String): Flow<EventWithTags?>

    @Transaction
    @Query("SELECT * FROM events evt JOIN event_tag_cross_refs crsrf ON evt.id = crsrf.eventId " +
            "WHERE crsrf.tagId IN (:tagIds) AND evt.deletePermanently IS NULL " +
            "ORDER BY timestamp DESC")
    fun getEventsByTagIds(tagIds: List<String>): Flow<List<EventWithTags>>

    @Transaction
    @Query("SELECT * FROM events WHERE deletePermanently IS NULL AND date BETWEEN :from AND :to ORDER BY timestamp DESC")
    fun getEventsInRange(from: String, to: String = LocalDate.MAX.toString()): Flow<List<EventWithTags>>


    /* Day Dao */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDay(day: DayEntity)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateDay(day: DayEntity)

    @Delete
    suspend fun deleteDay(day: DayEntity)

    @Query("UPDATE days SET favorite = not favorite WHERE date = :date")
    suspend fun toggleFavoriteDay(date: LocalDate)

    @Transaction
    @Query("SELECT * FROM days WHERE date = :date LIMIT 1")
    suspend fun getDayByDate(date: LocalDate): DayWithTags?

    @Transaction
    @Query("SELECT * FROM days WHERE date = :date LIMIT 1")
    fun getDayFlowByDate(date: LocalDate): Flow<DayWithTags?>

    @Transaction
    @Query("SELECT * FROM days WHERE date IN (:dates)")
    fun getDayFlowByDates(dates: List<LocalDate>): Flow<List<DayWithTags>>

    @Transaction
    @Query("SELECT * FROM days")
    fun getAllDays(): Flow<List<DayWithTags>>

    @Transaction
    @Query("SELECT * FROM days WHERE favorite = true")
    fun getFavoriteDays(): Flow<List<DayWithTags>>

    @Transaction
    @Query("SELECT * FROM days WHERE date BETWEEN :from AND :to")
    fun getDaysInRange(from: LocalDate, to: LocalDate = LocalDate.now()): Flow<List<DayWithTags>>


    /* Tag Dao */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TagEntity)

    @Update
    suspend fun updateTag(tag: TagEntity)

    @Delete
    suspend fun deleteTag(tag: TagEntity)

    @Delete
    suspend fun deleteTags(tags: List<TagEntity>)

    @Query("SELECT * FROM tags")
    fun getAllTags(): Flow<List<TagEntity>>

    @Query(
        "SELECT * FROM tags JOIN day_tag_cross_refs ON tags.id = day_tag_cross_refs.tagId " +
        "WHERE day_tag_cross_refs.date = :date"
    )
    fun getTagsForDay(date: LocalDate): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE id = :tagId")
    fun getTagById(tagId: String): Flow<TagEntity?>

    /* Tag Cross References */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEventTagCrossRef(crossRef: EventTagCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEventTagCrossRefs(crossRefs: List<EventTagCrossRef>)

    @Query("SELECT * FROM tags WHERE LOWER(name) LIKE LOWER('%' || :query || '%')")
    fun searchTagsByName(query: String): Flow<List<TagEntity>>

    @Delete
    suspend fun deleteEventTagCrossRef(crossRef: EventTagCrossRef)

    @Delete
    suspend fun deleteEventTagCrossRefs(crossRefs: List<EventTagCrossRef>)

    @Query(
        "SELECT tagId FROM event_tag_cross_refs " +
        "WHERE eventId IN (:eventIds) GROUP BY tagId " +
        "HAVING COUNT(DISTINCT eventId) = :eventCount"
    )
    fun getTagIdsMatchingAllEventIds(
        eventIds: List<String>,
        eventCount: Int = eventIds.size
    ): Flow<List<String>>

    @Query(
        "SELECT tagId FROM day_tag_cross_refs " +
        "WHERE date IN (:dates) GROUP BY tagId " +
        "HAVING COUNT(DISTINCT date) = :dayCount"
    )
    fun getTagIdsMatchingAllDayIds(
        dates: List<LocalDate>,
        dayCount: Int = dates.size
    ): Flow<List<String>>

    @Query(
        "SELECT COUNT(*) FROM event_tag_cross_refs " +
        "WHERE tagId = :tagId"
    )
    fun getEventCountForTag(tagId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDayTagCrossRef(crossRef: DayTagCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDayTagCrossRefs(crossRefs: List<DayTagCrossRef>)

    @Delete
    suspend fun deleteDayTagCrossRef(crossRef: DayTagCrossRef)

    @Delete
    suspend fun deleteDayTagCrossRefs(crossRefs: List<DayTagCrossRef>)

    /* Delete permanently all deleted events that are older than 1 month */
    @Query("DELETE FROM events WHERE deletePermanently IS NOT NULL AND deletePermanently < :currentTime")
    suspend fun deleteOldEvents(currentTime: Long = Instant.now().toEpochMilli())

    /* Views */
    @Query("SELECT * FROM events_time_with_tag WHERE tag_id = :tagId ORDER BY time ASC")
    fun getEventsTimeByTag(tagId: String): Flow<List<EventsTimeWithTag>>

    //!!!! DELETE ALL DATA !!!!//
    @Query("DELETE FROM events")
    suspend fun deleteAllEvents()

    @Query("DELETE FROM days")
    suspend fun deleteAllDays()

    @Query("DELETE FROM tags")
    suspend fun deleteAllTags()
}