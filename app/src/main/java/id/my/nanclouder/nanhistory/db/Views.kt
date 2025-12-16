package id.my.nanclouder.nanhistory.db

import androidx.room.ColumnInfo
import androidx.room.DatabaseView
import java.time.ZonedDateTime

/*

@DatabaseView(
    "SELECT COUNT(eventId) AS eventCount, name " +
            "description" +
            "created " +
            "tint"
)
data class TagWithEventCount {}

 */

@DatabaseView(
    viewName = "events_time_with_tag",
    value =
        "SELECT events.id, events.time, tags.id as tag_id " +
        "FROM events " +
        "INNER JOIN event_tag_cross_refs ON events.id = event_tag_cross_refs.eventId " +
        "INNER JOIN tags ON event_tag_cross_refs.tagId = tags.id"
)
data class EventsTimeWithTag(
    @ColumnInfo(name = "id") val eventId: String,
    @ColumnInfo(name = "time") val eventTime: ZonedDateTime,
    @ColumnInfo(name = "tag_id") val tagId: String
)