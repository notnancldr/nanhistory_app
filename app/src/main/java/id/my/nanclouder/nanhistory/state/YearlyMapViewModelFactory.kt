package id.my.nanclouder.nanhistory.state

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import id.my.nanclouder.nanhistory.db.AppDao
import id.my.nanclouder.nanhistory.db.CorridorNetworkDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Factory for creating [YearlyMapViewModel] instances.
 */
class YearlyMapViewModelFactory(
    private val dao: AppDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val context: Context? = null,
    private val corridorNetworkDao: CorridorNetworkDao? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return YearlyMapViewModel(
            dao = dao,
            ioDispatcher = ioDispatcher,
            context = context,
            corridorNetworkDao = corridorNetworkDao
        ) as T
    }
}
