package id.my.nanclouder.nanhistory.state

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import id.my.nanclouder.nanhistory.domain.repository.CorridorRepository
import id.my.nanclouder.nanhistory.domain.repository.EventRepository
import id.my.nanclouder.nanhistory.domain.repository.LocationRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Factory for creating [YearlyMapViewModel] instances.
 */
class YearlyMapViewModelFactory(
    private val eventRepository: EventRepository,
    private val locationRepository: LocationRepository,
    private val corridorRepository: CorridorRepository? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val context: Context? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return YearlyMapViewModel(
            eventRepository = eventRepository,
            locationRepository = locationRepository,
            corridorRepository = corridorRepository,
            ioDispatcher = ioDispatcher,
            context = context
        ) as T
    }
}
