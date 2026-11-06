package id.my.nanclouder.nanhistory.data.repository

import android.content.Context
import id.my.nanclouder.nanhistory.domain.model.HistoryEvent
import id.my.nanclouder.nanhistory.domain.repository.AttachmentStorageRepository
import id.my.nanclouder.nanhistory.utils.getAudioFile
import id.my.nanclouder.nanhistory.utils.getLocationFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android storage backed implementation of [AttachmentStorageRepository].
 * Handles on-disk cleanup of audio and location recordings when events are permanently deleted.
 */
class AndroidAttachmentStorageRepository(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AttachmentStorageRepository {

    override suspend fun deleteAttachments(event: HistoryEvent) = withContext(ioDispatcher) {
        val currentAudioPath = event.audio
        val currentLocationPath = event.locationPath

        if (currentAudioPath != null) {
            val audioFile = getAudioFile(context, currentAudioPath)
            if (audioFile.exists()) {
                audioFile.delete()
            }
        }

        if (currentLocationPath != null) {
            val locationFile = getLocationFile(context, currentLocationPath)
            if (locationFile.exists()) {
                locationFile.delete()
            }
        }
    }
}
