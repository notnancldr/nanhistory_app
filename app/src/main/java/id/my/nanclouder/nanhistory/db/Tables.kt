package id.my.nanclouder.nanhistory.db

import androidx.compose.ui.graphics.Color
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import id.my.nanclouder.nanhistory.lib.history.EventPoint
import id.my.nanclouder.nanhistory.lib.history.EventRange
import id.my.nanclouder.nanhistory.lib.history.EventTypes
import id.my.nanclouder.nanhistory.lib.history.HistoryEvent
import id.my.nanclouder.nanhistory.lib.history.HistoryTag
import java.time.LocalDate
import java.time.ZonedDateTime

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val time: ZonedDateTime,
    val favorite: Boolean,
    val created: ZonedDateTime,
    val modified: ZonedDateTime,
    val signature: String?,
    val type: EventTypes,
    val metadata: Map<String, Any>,
    val audio: String?,
    val locationPath: String?,

    // EventRange
    val end: ZonedDateTime?,
    val locationDescriptions: Map<ZonedDateTime, String>?,

    // Soft-deletion tracking
    val deletePermanently: Long?
)

@Entity(tableName = "days")
data class DayEntity(
    @PrimaryKey val date: LocalDate,
    val description: String,
    val favorite: Boolean,
    val tags: List<String>
)

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String = "",
    val created: ZonedDateTime,
    val tint: Color
)

@Entity(primaryKeys = ["eventId", "tagId"])
data class EventTagCrossRef(
    val eventId: String,
    val tagId: String
)

@Entity(primaryKeys = ["date", "tagId"])
data class DayTagCrossRef(
    val date: LocalDate,
    val tagId: String
)

data class EventWithTags(
    @Embedded val event: EventEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            EventTagCrossRef::class,
            parentColumn = "eventId",
            entityColumn = "tagId"
        )
    )
    val tags: List<TagEntity>
)

data class DayWithTags(
    @Embedded val event: DayEntity,

    @Relation(
        parentColumn = "date",
        entityColumn = "id",
        associateBy = Junction(
            DayTagCrossRef::class,
            parentColumn = "date",
            entityColumn = "tagId"
        )
    )
    val tags: List<TagEntity>
)

fun HistoryTag.toTagEntity() = TagEntity(
    id = id,
    name = name,
    description = description,
    created = created,
    tint = tint
)

fun TagEntity.toHistoryTag() = HistoryTag(
    id = id,
    name = name,
    description = description,
    created = created,
    tint = tint
)

fun EventWithTags.toHistoryEvent(): HistoryEvent {
    val historyEvent = when (event.type) {
        EventTypes.Point -> EventPoint(
            id = event.id,
            title = event.title,
            description = event.description,
            time = event.time,
        )
        EventTypes.Range -> EventRange(
            id = event.id,
            title = event.title,
            description = event.description,
            time = event.time,
            end = event.end ?: ZonedDateTime.now(),
        )
    }
    historyEvent.signature = event.signature ?: ""
    historyEvent.favorite = event.favorite
    historyEvent.tags = tags.map { it.toHistoryTag() }
    historyEvent.created = event.created
    historyEvent.modified = event.modified
    historyEvent.metadata = event.metadata.toMutableMap()
    historyEvent.audio = event.audio
    historyEvent.locationPath = event.locationPath

    return historyEvent
}

fun HistoryEvent.toEventEntity() = EventEntity(
    id = id,
    title = title,
    description = description,
    time = time,
    favorite = favorite,
    created = created,
    modified = modified,
    metadata = metadata,
    locationPath = locationPath,
    audio = audio,
    end = if (this is EventRange) end else null,
    locationDescriptions = if (this is EventRange) locationDescriptions else null,
    signature = signature.ifBlank { null },

    type = if (this is EventPoint) EventTypes.Point else EventTypes.Range,
    deletePermanently = null
)