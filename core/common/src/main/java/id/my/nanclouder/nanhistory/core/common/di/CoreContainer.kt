package id.my.nanclouder.nanhistory.core.common.di

import id.my.nanclouder.nanhistory.domain.repository.CorridorRepository
import id.my.nanclouder.nanhistory.domain.repository.EventRepository
import id.my.nanclouder.nanhistory.domain.repository.LocationRepository
import id.my.nanclouder.nanhistory.domain.repository.TagRepository
import id.my.nanclouder.nanhistory.domain.usecase.SaveEventUseCase
import id.my.nanclouder.nanhistory.domain.usecase.PermanentlyDeleteEventsUseCase
import id.my.nanclouder.nanhistory.domain.usecase.RestoreEventsUseCase

/**
 * Common repository access for screens and components that need general Event and Tag querying.
 */
interface CommonRepositoryContainer {
    val eventRepository: EventRepository
    val tagRepository: TagRepository
}

/**
 * Container contract for the Trash feature module.
 */
interface TrashContainer {
    val restoreEventsUseCase: RestoreEventsUseCase
    val permanentlyDeleteEventsUseCase: PermanentlyDeleteEventsUseCase
}

/**
 * Container contract for the Tag feature module.
 */
interface TagDetailContainer {
    val tagRepository: TagRepository
    val eventRepository: EventRepository
}

/**
 * Container contract for the EventLocation feature module.
 */
interface EventLocationContainer {
    val eventRepository: EventRepository
    val locationRepository: LocationRepository
}

/**
 * Container contract for the YearlyMap feature module.
 */
interface YearlyMapContainer {
    val eventRepository: EventRepository
    val locationRepository: LocationRepository
    val corridorRepository: CorridorRepository?
}

/**
 * Container contract for the ShareMap feature module.
 */
interface ShareMapContainer {
    val eventRepository: EventRepository
    val locationRepository: LocationRepository
}

/**
 * Container contract for the Record feature module.
 */
interface RecordContainer {
    val eventRepository: EventRepository
    val locationRepository: LocationRepository
}

/**
 * Container contract for the EventDetail feature module.
 */
interface EventDetailContainer {
    val eventRepository: EventRepository
    val locationRepository: LocationRepository
    val tagRepository: TagRepository
    val saveEventUseCase: SaveEventUseCase
}

/**
 * Marker and base interface for the central dependency injection container.
 * Concrete implementations and feature sub-containers are implemented in `:app`.
 */
interface CoreContainer :
    CommonRepositoryContainer,
    TrashContainer,
    TagDetailContainer,
    EventLocationContainer,
    YearlyMapContainer,
    ShareMapContainer,
    RecordContainer,
    EventDetailContainer

/**
 * Interface implemented by Application or Context wrappers to provide access to [CoreContainer].
 */
interface CoreContainerProvider {
    val coreContainer: CoreContainer
}
