package id.my.nanclouder.nanhistory.utils.history

object HistoryFileDataProperty {
    const val FILE_VERSION = "fileVersion"
    const val DATE = "date"
    const val DESCRIPTION = "description"
    const val FAVORITE = "favorite"
    const val TAGS = "tags"
    const val EVENTS = "events"
    const val METADATA = "metadata"
}

object HistoryEventProperty {
    const val ID = "id"
    const val TITLE = "title"
    const val DESCRIPTION = "description"
    const val FAVORITE = "favorite"
    const val SIGNATURE = "signature"
    const val TAGS = "tags"
    const val METADATA = "metadata"
    const val TIME = "time"
    const val CREATED = "created"
    const val MODIFIED = "modified"
    const val TYPE = "type"
    const val TYPE_POINT = "point"
    const val TYPE_RANGE = "range"
    const val LOCATION = "location"
    const val END = "end"
    const val LOCATIONS = "locations"
    const val LOCATION_DESCRIPTIONS = "locationDescriptions"
    const val LOCATION_PATH = "locationPath"
    const val AUDIO = "audio"
    const val TRANSPORTATION_TYPE = "transportationType"
}

val EventPointProperties = listOf(
    HistoryEventProperty.ID,
    HistoryEventProperty.TITLE,
    HistoryEventProperty.DESCRIPTION,
    HistoryEventProperty.FAVORITE,
    HistoryEventProperty.SIGNATURE,
    HistoryEventProperty.TAGS,
    HistoryEventProperty.METADATA,
    HistoryEventProperty.TIME,
    HistoryEventProperty.CREATED,
    HistoryEventProperty.MODIFIED,
    HistoryEventProperty.TYPE,
    HistoryEventProperty.LOCATION,
    HistoryEventProperty.LOCATION_PATH,
    HistoryEventProperty.AUDIO
)

val EventRangeProperties = listOf(
    HistoryEventProperty.ID,
    HistoryEventProperty.TITLE,
    HistoryEventProperty.DESCRIPTION,
    HistoryEventProperty.FAVORITE,
    HistoryEventProperty.SIGNATURE,
    HistoryEventProperty.TAGS,
    HistoryEventProperty.METADATA,
    HistoryEventProperty.TIME,
    HistoryEventProperty.CREATED,
    HistoryEventProperty.MODIFIED,
    HistoryEventProperty.TYPE,
    HistoryEventProperty.END,
    HistoryEventProperty.LOCATIONS,
    HistoryEventProperty.LOCATION_DESCRIPTIONS,
    HistoryEventProperty.LOCATION_PATH,
    HistoryEventProperty.AUDIO,
    HistoryEventProperty.TRANSPORTATION_TYPE
)

val HistoryEventSignatureExcluded = listOf(
    HistoryEventProperty.TITLE,
    HistoryEventProperty.DESCRIPTION,
    HistoryEventProperty.TAGS,
    HistoryEventProperty.MODIFIED,
    HistoryEventProperty.FAVORITE,
    HistoryEventProperty.SIGNATURE,
    HistoryEventProperty.METADATA,
    HistoryEventProperty.TRANSPORTATION_TYPE
)