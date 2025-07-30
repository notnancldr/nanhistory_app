package id.my.nanclouder.nanhistory.lib

import android.content.Context
import id.my.nanclouder.nanhistory.db.AppDatabase
import id.my.nanclouder.nanhistory.db.EventEntity
import kotlinx.coroutines.flow.first
import java.io.File

fun getAttachmentPath(path: String) = getAttachmentPath(File(path))

fun getAttachmentPath(file: File) = "${file.parentFile?.name}/${file.name}"



suspend fun searchEventsByAudioPath(context: Context, audioPath: String): List<EventEntity> {
    val db = AppDatabase.getInstance(context)
    val dao = db.appDao()
    val events = dao.getEventsByAudio(audioPath).first()

    return events
}

suspend fun searchEventsByLocationPath(context: Context, locationPath: String): List<EventEntity> {
    val db = AppDatabase.getInstance(context)
    val dao = db.appDao()
    val events = dao.getEventsByLocation(locationPath).first()

    return events
}