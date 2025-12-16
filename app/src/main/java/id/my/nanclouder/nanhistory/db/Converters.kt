package id.my.nanclouder.nanhistory.db

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import id.my.nanclouder.nanhistory.utils.Coordinate
import id.my.nanclouder.nanhistory.utils.history.EventTypes
import id.my.nanclouder.nanhistory.utils.history.TransportationType
import java.time.LocalDate
import java.time.ZonedDateTime

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromEventType(value: EventTypes): String = value.name

    @TypeConverter
    fun toEventType(value: String): EventTypes = EventTypes.valueOf(value)

    @TypeConverter
    fun fromTransportationType(value: TransportationType): String = value.name

    @TypeConverter
    fun toTransportationType(value: String): TransportationType = TransportationType.valueOf(value)

    @TypeConverter
    fun fromZonedDateTime(value: ZonedDateTime): String = value.toOffsetDateTime().toString()

    @TypeConverter
    fun toZonedDateTime(value: String): ZonedDateTime = ZonedDateTime.parse(value)

    @TypeConverter
    fun fromStringList(value: List<String>): String = gson.toJson(value)

    @TypeConverter
    fun toStringList(value: String): List<String> =
        gson.fromJson(
            value,
            object : TypeToken<List<String>>() {}.type
        )

    @TypeConverter
    fun fromMap(value: Map<String, Any>): String = gson.toJson(value)

    @TypeConverter
    fun toMap(value: String): Map<String, Any> =
        gson.fromJson(
            value,
            object : TypeToken<Map<String, Any>>() {}.type
        )

    @TypeConverter
    fun fromLocationDescription(value: Map<ZonedDateTime, Coordinate>): String =
        gson.toJson(value)

    @TypeConverter
    fun toLocationDescription(value: String): Map<ZonedDateTime, String> =
        gson.fromJson<Map<String, String>>(
            value,
            object : TypeToken<Map<String, String>>() {}.type
        ).map {
            ZonedDateTime.parse(it.key) to it.value
        }.toMap()

    @TypeConverter
    fun fromLocalDate(value: LocalDate): String =
        value.toString()

    @TypeConverter
    fun toLocalDate(value: String): LocalDate =
        LocalDate.parse(value)

    @TypeConverter
    fun fromColor(value: Color): Int =
        value.toArgb()

    @TypeConverter
    fun toColor(value: Int): Color =
        Color(value)
}