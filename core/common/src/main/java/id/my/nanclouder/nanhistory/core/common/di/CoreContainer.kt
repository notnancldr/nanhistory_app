package id.my.nanclouder.nanhistory.core.common.di

/**
 * Marker and base interface for the central dependency injection container.
 * Concrete implementations and feature sub-containers are implemented in `:app` and `:core:data`.
 */
interface CoreContainer

/**
 * Interface implemented by Application or Context wrappers to provide access to [CoreContainer].
 */
interface CoreContainerProvider {
    val coreContainer: CoreContainer
}
