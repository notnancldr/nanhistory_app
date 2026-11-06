package id.my.nanclouder.nanhistory.domain.model

import java.time.Instant
import java.time.ZonedDateTime
import kotlin.random.Random
import kotlin.random.nextULong

fun generateEventId(instant: Instant = Instant.now()): String =
    "${instant.toEpochMilli().toString(16)}-${Random.nextULong().toString(16)}"

abstract class HistoryEvent(
    open var id: String = generateEventId(),
    open var title: String,
    open var description: String,
    open var time: ZonedDateTime,
    open var favorite: Boolean = false,
    open var tags: List<HistoryTag> = listOf(),
    open var created: ZonedDateTime = ZonedDateTime.now(),
    open var modified: ZonedDateTime = ZonedDateTime.now(),
    open var broken: Boolean = false,
    open var signature: String = "",
    open var type: String,
    open var metadata: MutableMap<String, Any> = mutableMapOf(),
    open var unknownProperties: MutableMap<String, Any> = mutableMapOf(),
    open var audio: String? = null,
    @Deprecated("Use database system instead")
    open var locationPath: String? = null,
    open var versionNumber: Int = EVENT_VERSION_NUMBER,
)

data class EventPoint(
    override var id: String = generateEventId(),
    override var title: String,
    override var description: String,
    override var time: ZonedDateTime,
    override var favorite: Boolean = false,
    override var tags: List<HistoryTag> = listOf(),
    override var created: ZonedDateTime = ZonedDateTime.now(),
    override var modified: ZonedDateTime = ZonedDateTime.now(),
    override var metadata: MutableMap<String, Any> = mutableMapOf(),
    override var unknownProperties: MutableMap<String, Any> = mutableMapOf(),
    override var audio: String? = null,
    @Deprecated("Use database system instead")
    override var locationPath: String? = null,
    override var versionNumber: Int = EVENT_VERSION_NUMBER,
) : HistoryEvent(
    id = id,
    title = title,
    description = description,
    time = time,
    favorite = favorite,
    tags = tags,
    created = created,
    modified = modified,
    type = "point",
    metadata = metadata,
    unknownProperties = unknownProperties,
    audio = audio,
    locationPath = locationPath,
    versionNumber = versionNumber
)

data class EventRange(
    override var id: String = generateEventId(),
    override var title: String,
    override var description: String,
    override var time: ZonedDateTime,
    override var favorite: Boolean = false,
    override var tags: List<HistoryTag> = listOf(),
    override var created: ZonedDateTime = ZonedDateTime.now(),
    override var modified: ZonedDateTime = ZonedDateTime.now(),
    override var metadata: MutableMap<String, Any> = mutableMapOf(),
    override var unknownProperties: MutableMap<String, Any> = mutableMapOf(),
    @Deprecated("Use database system instead")
    override var locationPath: String? = null,
    override var audio: String? = null,
    var end: ZonedDateTime,
    var transportationType: TransportationType = TransportationType.Unspecified,
    var locationDescriptions: MutableMap<ZonedDateTime, String> = mutableMapOf(),
    override var versionNumber: Int = EVENT_VERSION_NUMBER,
) : HistoryEvent(
    id = id,
    title = title,
    description = description,
    time = time,
    favorite = favorite,
    tags = tags,
    created = created,
    modified = modified,
    type = "range",
    metadata = metadata,
    unknownProperties = unknownProperties,
    audio = audio,
    locationPath = locationPath,
    versionNumber = versionNumber
)
