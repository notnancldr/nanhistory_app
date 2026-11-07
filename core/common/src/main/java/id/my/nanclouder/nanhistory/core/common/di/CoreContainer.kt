package id.my.nanclouder.nanhistory.core.common.di

import id.my.nanclouder.nanhistory.domain.repository.EventRepository
import id.my.nanclouder.nanhistory.domain.repository.TagRepository
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
 * Marker and base interface for the central dependency injection container.
 * Concrete implementations and feature sub-containers are implemented in `:app`.
 */
interface CoreContainer : CommonRepositoryContainer

/**
 * Interface implemented by Application or Context wrappers to provide access to [CoreContainer].
 */
interface CoreContainerProvider {
    val coreContainer: CoreContainer
}
