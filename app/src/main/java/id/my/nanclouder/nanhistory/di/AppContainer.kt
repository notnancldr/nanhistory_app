package id.my.nanclouder.nanhistory.di

import android.content.Context
import id.my.nanclouder.nanhistory.data.repository.AndroidAttachmentStorageRepository
import id.my.nanclouder.nanhistory.data.repository.RoomEventRepository
import id.my.nanclouder.nanhistory.data.repository.RoomTagRepository
import id.my.nanclouder.nanhistory.db.AppDao
import id.my.nanclouder.nanhistory.db.AppDatabase
import id.my.nanclouder.nanhistory.domain.repository.AttachmentStorageRepository
import id.my.nanclouder.nanhistory.domain.repository.EventRepository
import id.my.nanclouder.nanhistory.domain.repository.TagRepository
import id.my.nanclouder.nanhistory.domain.usecase.PermanentlyDeleteEventsUseCase
import id.my.nanclouder.nanhistory.domain.usecase.RestoreEventsUseCase
import java.time.Clock

class AppContainer(private val context: Context) {
    val clock: Clock = Clock.systemDefaultZone()

    // Dynamic DAO provider guarantees active handle even after AppDatabase.clearInstance()
    val daoProvider: () -> AppDao = {
        AppDatabase.getInstance(context).appDao()
    }

    val eventRepository: EventRepository by lazy {
        RoomEventRepository(daoProvider = daoProvider)
    }

    val tagRepository: TagRepository by lazy {
        RoomTagRepository(daoProvider = daoProvider)
    }

    val attachmentStorageRepository: AttachmentStorageRepository by lazy {
        AndroidAttachmentStorageRepository(context = context)
    }

    val restoreEventsUseCase: RestoreEventsUseCase by lazy {
        RestoreEventsUseCase(eventRepository = eventRepository)
    }

    val permanentlyDeleteEventsUseCase: PermanentlyDeleteEventsUseCase by lazy {
        PermanentlyDeleteEventsUseCase(
            eventRepository = eventRepository,
            attachmentStorageRepository = attachmentStorageRepository
        )
    }
}
