package id.my.nanclouder.nanhistory.data.transport

import android.content.Context
import id.my.nanclouder.nanhistory.db.AppDatabase
import id.my.nanclouder.nanhistory.db.toLocationData
import id.my.nanclouder.nanhistory.domain.model.HistoryEvent
import id.my.nanclouder.nanhistory.domain.model.LocationData
import id.my.nanclouder.nanhistory.utils.getLocationFile
import kotlinx.coroutines.flow.first

suspend fun HistoryEvent.getLocations(context: Context): List<LocationData> {
    if (locationPath == null) {
        val db = AppDatabase.getInstance(context)
        val dao = db.appDao()

        return dao.getLocationsByEventId(id).first().map {
            it.toLocationData()
        }
    }
    return (locationPath?.let {
        val locationFile = getLocationFile(context, it)
        val content = if (locationFile.exists()) locationFile.readText() else ""
        if (content.isBlank()) return emptyList()
        // parse or fallback
        emptyList<LocationData>()
    }) ?: emptyList()
}
