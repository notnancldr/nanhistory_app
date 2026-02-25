package id.my.nanclouder.nanhistory.db

import androidx.compose.ui.graphics.Color
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import id.my.nanclouder.nanhistory.utils.Coordinate
import id.my.nanclouder.nanhistory.utils.PersistableZonedDateTime
import id.my.nanclouder.nanhistory.utils.history.EVENT_VERSION_NUMBER
import id.my.nanclouder.nanhistory.utils.history.EventPoint
import id.my.nanclouder.nanhistory.utils.history.EventRange
import id.my.nanclouder.nanhistory.utils.history.EventTypes
import id.my.nanclouder.nanhistory.utils.history.HistoryDay
import id.my.nanclouder.nanhistory.utils.history.HistoryEvent
import id.my.nanclouder.nanhistory.utils.history.HistoryTag
import id.my.nanclouder.nanhistory.utils.history.LocationData
import id.my.nanclouder.nanhistory.utils.history.TransportationType
import id.my.nanclouder.nanhistory.utils.toPersistable
import id.my.nanclouder.nanhistory.utils.toZonedDateTime
import java.math.RoundingMode
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.ZonedDateTime

@Entity(
    tableName = "events",
    foreignKeys = [
        ForeignKey(
            entity = DayEntity::class,
            parentColumns = ["date"],
            childColumns = ["date"],
            onDelete = ForeignKey.CASCADE,
            // onUpdate = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index(value = ["date"])
    ]
)
data class EventEntity(
    @PrimaryKey val id: String,

    val date: LocalDate,

    val title: String,
    val description: String,
    val time: ZonedDateTime,
    val timestamp: Long,
    val favorite: Boolean,
    val created: ZonedDateTime,
    val modified: ZonedDateTime,
    val signature: String?,
    val type: EventTypes,
    val metadata: Map<String, Any>,
    val audio: String?,
    val locationPath: String?,
    val transportationType: TransportationType = TransportationType.Unspecified,

    // EventRange
    val end: ZonedDateTime?,
    val endTimestamp: Long?,
    val locationDescriptions: Map<String, Any>?,

    // Soft-deletion tracking
    val deletePermanently: Long?,

    // version number
    @ColumnInfo(defaultValue = "0")
    val versionNumber: Int = EVENT_VERSION_NUMBER // Since v3
)

@Entity(tableName = "days")
data class DayEntity(
    @PrimaryKey val date: LocalDate,
    val description: String,
    val favorite: Boolean,
    val metadata: Map<String, Any> = mapOf()
)

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String = "",
    val created: ZonedDateTime,
    val tint: Color
)

/**
 * Added since `DB v4`
 */
@Entity(
    tableName = "locations",
    foreignKeys = [
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE,
        )
    ]
)
data class LocationEntity(
    // Time information
    @PrimaryKey val timestamp: Long,
    val eventId: String,
    val zoneId: String,

    // Location data
    val latitude: Double,
    val longitude: Double,

    val speed: Float? = null,
    val bearing: Float? = null,
    val altitude: Double? = null,

    // Accuracy fields
    val accuracy: Float? = null,
    val speedAccuracy: Float? = null,
    val bearingAccuracy: Float? = null,
    val verticalAccuracy: Float? = null,
) {
    val location
        get() = Coordinate(latitude, longitude)
    val time: ZonedDateTime
        get() = PersistableZonedDateTime(timestamp, zoneId).toZonedDateTime() ?: ZonedDateTime.now()

    override fun toString(): String {
        val formatter = DecimalFormat("#.####").apply {
            roundingMode = RoundingMode.HALF_UP
        }

        fun formatNumber(value: Number?): String {
            return value?.let { formatter.format(it.toDouble()) } ?: ""
        }

        return listOf(
            time.toOffsetDateTime().toString(),

            // Location
            location.toString(),
            formatNumber(accuracy),

            // Speed
            formatNumber(speed),
            formatNumber(speedAccuracy),

            // Bearing
            formatNumber(bearing),
            formatNumber(bearingAccuracy),

            // Altitude
            formatNumber(altitude),
            formatNumber(verticalAccuracy),
        ).joinToString("|")
    }
}

@Entity(primaryKeys = ["eventId", "tagId"], tableName = "event_tag_cross_refs")
data class EventTagCrossRef(
    val eventId: String,
    val tagId: String
)

@Entity(primaryKeys = ["date", "tagId"], tableName = "day_tag_cross_refs")
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

data class DayWithEventsAndTags(
    @Embedded val day: DayEntity,

    @Relation(
        parentColumn = "date",
        entityColumn = "date",
        associateBy = Junction(
            EventTagCrossRef::class,
            parentColumn = "eventId",
            entityColumn = "tagId"
        )
    )
    val events: List<EventWithTags>
)

data class DayWithTags(
    @Embedded val day: DayEntity,

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

data class EventWithTagsWithLocations(
    @Embedded val eventWithTags: EventWithTags,

    @Relation(
        parentColumn = "id",      // This refers to EventEntity.id (inside EventWithTags)
        entityColumn = "eventId"  // This refers to LocationEntity.eventId
    )
    val locations: List<LocationEntity>
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
            transportationType = event.transportationType,
            locationDescriptions = event.locationDescriptions?.map {
                val key = ZonedDateTime.parse(it.key)
                key to it.value.toString()
            }?.toMap()?.toMutableMap() ?: mutableMapOf()
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
    historyEvent.versionNumber = event.versionNumber

    return historyEvent
}

fun DayWithTags.toHistoryDay() = HistoryDay(
    date = day.date,
    description = day.description,
    favorite = day.favorite,
    tags = tags.map { it.toHistoryTag() },
    metadata = day.metadata.toMutableMap()
)

fun HistoryEvent.toEventEntity() = EventEntity(
    id = id,

    date = time.toLocalDate(),

    title = title,
    description = description,
    time = time,
    timestamp = time.toInstant().toEpochMilli(),
    favorite = favorite,
    created = created,
    modified = modified,
    metadata = metadata,
    locationPath = locationPath,
    audio = audio,

    end = (this as? EventRange)?.end,
    endTimestamp = (this as? EventRange)?.end?.toInstant()?.toEpochMilli(),
    locationDescriptions = (this as? EventRange)?.locationDescriptions?.map {
        it.key.toString() to it.value
    }?.toMap(),
    transportationType = (this as? EventRange)?.transportationType ?: TransportationType.Unspecified,

    signature = signature.ifBlank { null },

    type = if (this is EventPoint) EventTypes.Point else EventTypes.Range,
    deletePermanently = null,

    versionNumber = this.versionNumber
)

fun HistoryDay.toDayEntity() = DayEntity(
    date = date,
    description = description ?: "",
    favorite = favorite,
    metadata = metadata
)

fun DayEntity.toHistoryDay() = HistoryDay(
    date = date,
    description = description,
    favorite = favorite
)

fun LocationEntity.toLocationData() = LocationData(
    time = PersistableZonedDateTime(timestamp, zoneId).toZonedDateTime() ?: ZonedDateTime.now(),
    location = Coordinate(latitude, longitude),
    speed = speed,
    bearing = bearing,
    altitude = altitude,
    accuracy = accuracy,
    speedAccuracy = speedAccuracy,
    bearingAccuracy = bearingAccuracy,
    verticalAccuracy = verticalAccuracy,
)

fun LocationData.toLocationEntity(eventId: String) = LocationEntity(
    eventId = eventId,
    timestamp = time.toPersistable().utcDateTime,
    zoneId = time.toPersistable().zoneId,
    latitude = location.latitude,
    longitude = location.longitude,
    speed = speed,
    bearing = bearing,
    altitude = altitude,
    accuracy = accuracy,
    speedAccuracy = speedAccuracy,
    bearingAccuracy = bearingAccuracy,
    verticalAccuracy = verticalAccuracy,
)