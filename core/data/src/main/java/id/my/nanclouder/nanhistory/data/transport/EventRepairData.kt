package id.my.nanclouder.nanhistory.data.transport

import android.content.Context
import id.my.nanclouder.nanhistory.core.common.security.generateSignature
import id.my.nanclouder.nanhistory.core.common.security.validateSignature
import id.my.nanclouder.nanhistory.db.AppDatabase
import id.my.nanclouder.nanhistory.db.toEventEntity
import id.my.nanclouder.nanhistory.db.toLocationData
import id.my.nanclouder.nanhistory.domain.model.EventMetadata
import id.my.nanclouder.nanhistory.domain.model.EventRange
import id.my.nanclouder.nanhistory.domain.model.HistoryEvent
import id.my.nanclouder.nanhistory.domain.model.isBroken
import kotlinx.coroutines.flow.first

suspend fun HistoryEvent.repairEvent(context: Context) {
    val db = AppDatabase.getInstance(context)
    val dao = db.appDao()

    val isValid = validateSignature(context)
    val locationData = dao.getLocationsByEventId(this.id).first()
    val mappedLocations = locationData.map { it.toLocationData() }

    if (this is EventRange && this.isBroken(mappedLocations)) {
        val locationTimeStart = locationData.first().time
        val locationTimeEnd = locationData.last().time

        if (locationTimeStart < this.time) {
            this.time = locationTimeStart.minusSeconds(1)
        }
        if (locationTimeEnd > this.end) {
            this.end = locationTimeEnd.plusSeconds(1)
        }
    }

    if (isValid) {
        this.generateSignature(context, apply = true)
    }

    this.metadata.remove(EventMetadata.IGNORE_REPAIR_EVENT)
    dao.updateEvent(this.toEventEntity())
}
