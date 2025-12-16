package id.my.nanclouder.nanhistory.utils.history

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import id.my.nanclouder.nanhistory.utils.FILE_VERSION
import id.my.nanclouder.nanhistory.utils.matchOrNull
import id.my.nanclouder.nanhistory.utils.toZonedDateTimeOrNull
import java.io.File
import java.time.Instant
import java.time.ZonedDateTime
import kotlin.random.Random

private const val FILE_PATH = "tags.json"

fun generateTagId(instant: Instant = Instant.now()): String =
    "TAG-${instant.toEpochMilli().toString(16)}-${Random.nextLong().toString(16)}"

data class HistoryTag(
    var id: String = generateTagId(),
    var name: String,
    var description: String = "",
    var created: ZonedDateTime = ZonedDateTime.now(),
    var tint: Color
) { companion object }

data class HistoryTagsData(
    var fileVersion: Int = FILE_VERSION,
    var tags: MutableList<HistoryTag>
) { companion object }

fun HistoryTagsData.toMap(): Map<String, Any> = mapOf(
    "fileVersion" to this.fileVersion,
    "tags" to this.tags.map { it.toMap() }
)

fun Map<String, Any>.toHistoryTagsData(): HistoryTagsData {
    val fileVersion = matchOrNull<Int>(this["fileVersion"]) ?: 5
    val tags = matchOrNull<List<Map<String, Any>>>(this["tags"]) ?: listOf()
    return HistoryTagsData(
        fileVersion = fileVersion,
        tags = tags.map { it.toHistoryTag() }.toMutableList()
    )
}

fun HistoryTagsData.toJson(): String =
    Gson().toJson(this.toMap())

fun HistoryTagsData.Companion.fromJson(json: String): HistoryTagsData =
    Gson().fromJson<Map<String, Any>>(
        json, object : TypeToken<Map<String, Any>>() {}.type
    ).toHistoryTagsData()

fun Map<String, Any>.toHistoryTag(): HistoryTag {
    val id = matchOrNull<String>(this["id"]) ?: generateTagId()
    val name = matchOrNull<String>(this["name"]) ?: "Unknown"
    val description = matchOrNull<String>(this["description"]) ?: ""
    val created = matchOrNull<String>(this["created"])?.toZonedDateTimeOrNull() ?:
    ZonedDateTime.now()
    val tint = matchOrNull<String>(this["tint"]).let {
        val colorData =
            if (it != null) try { it.toLong(16) } catch(_: Exception) { null }
            else null
        Color(colorData ?: 0xFF808080)
    }
    return HistoryTag(
        id = id,
        name = name,
        description = description,
        created = created,
        tint = tint
    )
}

fun HistoryTag.toMap(): Map<String, Any> = mapOf(
    "id" to this.id,
    "name" to this.name,
    "description" to this.description,
    "created" to this.created.toOffsetDateTime().toString(),
    "tint" to this.tint.value.toString(16)
)

fun HistoryTagsData.Companion.get(context: Context): HistoryTagsData? {
    val file = File(context.filesDir, FILE_PATH)

    return if (file.exists()) HistoryTagsData.fromJson(file.readText())
        else null
}

fun HistoryTagsData.Companion.getOrCreateNew(context: Context): HistoryTagsData {
    return HistoryTagsData.get(context) ?: HistoryTagsData(
        fileVersion = FILE_VERSION,
        tags = mutableListOf()
    )
}

fun HistoryTag.Companion.get(context: Context, id: String): HistoryTag? {
    return HistoryTagsData.get(context)?.tags?.first { it.id == id }
}

fun HistoryTagsData.save(context: Context) {
    val appFile = context.filesDir
    val file = File(appFile, FILE_PATH)
    if (!file.exists()) {
        file.parentFile?.mkdirs()
        file.createNewFile()
    }
    file.writeText(this.toJson())
}

fun HistoryTag.save(context: Context) {
    val fileData = HistoryTagsData.getOrCreateNew(context)

    fileData.tags.removeAll { it.id == this.id }
    fileData.tags.add(this)
    fileData.save(context)
}

fun HistoryTag.Companion.remove(context: Context, id: String) {
    val fileData = HistoryTagsData.getOrCreateNew(context)

    fileData.tags.removeAll { it.id == id }
    fileData.save(context)
}