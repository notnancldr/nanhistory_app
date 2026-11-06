package id.my.nanclouder.nanhistory.di

import android.content.Context
import id.my.nanclouder.nanhistory.data.repository.AndroidAttachmentStorageRepository
import id.my.nanclouder.nanhistory.data.repository.RoomCorridorRepository
import id.my.nanclouder.nanhistory.data.repository.RoomEventRepository
import id.my.nanclouder.nanhistory.data.repository.RoomLocationRepository
import id.my.nanclouder.nanhistory.data.repository.RoomTagRepository
import id.my.nanclouder.nanhistory.db.AppDao
import id.my.nanclouder.nanhistory.db.AppDatabase
import id.my.nanclouder.nanhistory.db.CorridorNetworkDao
import id.my.nanclouder.nanhistory.domain.repository.AttachmentStorageRepository
import id.my.nanclouder.nanhistory.domain.repository.CorridorRepository
import id.my.nanclouder.nanhistory.domain.repository.EventRepository
import id.my.nanclouder.nanhistory.domain.repository.LocationRepository
import id.my.nanclouder.nanhistory.domain.repository.TagRepository
import id.my.nanclouder.nanhistory.domain.usecase.PermanentlyDeleteEventsUseCase
import id.my.nanclouder.nanhistory.domain.usecase.RestoreEventsUseCase
import id.my.nanclouder.nanhistory.domain.usecase.SaveEventUseCase
import id.my.nanclouder.nanhistory.data.repository.ConfigSettingsRepository
import id.my.nanclouder.nanhistory.domain.repository.SettingsRepository
import id.my.nanclouder.nanhistory.domain.usecase.PurgeExpiredTrashUseCase
import java.time.Clock

class AppContainer(private val context: Context) {
    val clock: Clock = Clock.systemDefaultZone()

    // Dynamic DAO provider guarantees active handle even after AppDatabase.clearInstance()
    val daoProvider: () -> AppDao = {
        AppDatabase.getInstance(context).appDao()
    }

    val corridorNetworkDaoProvider: () -> CorridorNetworkDao = {
        AppDatabase.getInstance(context).corridorNetworkDao()
    }

    val eventRepository: EventRepository by lazy {
        RoomEventRepository(daoProvider = daoProvider, context = context)
    }

    val tagRepository: TagRepository by lazy {
        RoomTagRepository(daoProvider = daoProvider)
    }

    val locationRepository: LocationRepository by lazy {
        RoomLocationRepository(daoProvider = daoProvider)
    }

    val corridorRepository: CorridorRepository by lazy {
        RoomCorridorRepository(daoProvider = corridorNetworkDaoProvider)
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

    val saveEventUseCase: SaveEventUseCase by lazy {
        SaveEventUseCase(eventRepository = eventRepository)
    }

    val settingsRepository: SettingsRepository by lazy {
        ConfigSettingsRepository(context = context)
    }

    val purgeExpiredTrashUseCase: PurgeExpiredTrashUseCase by lazy {
        PurgeExpiredTrashUseCase(
            eventRepository = eventRepository,
            attachmentStorageRepository = attachmentStorageRepository
        )
    }
}
