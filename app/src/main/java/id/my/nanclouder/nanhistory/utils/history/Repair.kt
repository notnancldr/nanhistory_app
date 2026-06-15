package id.my.nanclouder.nanhistory.utils.history

import android.content.Context
import id.my.nanclouder.nanhistory.db.AppDatabase
import id.my.nanclouder.nanhistory.db.toEventEntity
import id.my.nanclouder.nanhistory.db.toLocationData
import id.my.nanclouder.nanhistory.utils.signature.generateSignature
import id.my.nanclouder.nanhistory.utils.signature.validateSignature
import kotlinx.coroutines.flow.first

private fun getLocationTimeRange(locationData: List<LocationData>) =
    locationData.minByOrNull { it.time }?.time to locationData.maxByOrNull { it.time }?.time

/**
 * @return `true` if time and location mismatch
 */
private fun HistoryEvent.checkTimeToLocationMismatch(locationData: List<LocationData>): Boolean {
    val (locationTimeStart, locationTimeEnd) = getLocationTimeRange(locationData)
    if (locationTimeStart == null || locationTimeEnd == null) return false

    return (this is EventRange && (locationTimeStart < this.time || locationTimeEnd > this.end))
}

/**
 * @return `true` if event has [EventRange] type where it should have [EventPoint] type
 */
private fun HistoryEvent.checkShouldBeEventPoint(): Boolean {
    return this is EventRange && this.time.plusSeconds(1) >= this.end
}

/**
 * Check if an event is broken. When an event is broken.
 * * Repair can be done using [HistoryEvent.repairEvent].
 * * Reason can be retrieved from [HistoryEvent.getBrokenReason].
 * @return `true` if the event met at least one 'broken' condition.
 */
fun HistoryEvent.isBroken(locationData: List<LocationData>): Boolean {
    return checkTimeToLocationMismatch(locationData) /*|| checkShouldBeEventPoint() */
}

/**
 * Retrieve formatted reason of a broken event.
 */
fun HistoryEvent.getBrokenReason(locationData: List<LocationData>): String {
    var reason = ""
    if (this.checkTimeToLocationMismatch(locationData) && this is EventRange) {
        val locationTimeRange = getLocationTimeRange(locationData).toList()
        val eventTimeRange = listOf(this.time, this.end)
        reason += "Time to location mismatch\n"
        reason += "|-- Location:\n"
        reason += "|   |-- Start: ${locationTimeRange.first()}\n"
        reason += "|   `-- End:   ${locationTimeRange.last()}\n"
        reason += "`-- Event:\n"
        reason += "    |-- Start: ${eventTimeRange.first()}\n"
        reason += "    `-- End:   ${eventTimeRange.last()}\n"
    }
    if (this.checkShouldBeEventPoint()) {
        reason += "This event should have 'point' type\n"
    }

    return reason.removeSuffix("\n")
}

/**
 * Repair a broken event.
 */
suspend fun HistoryEvent.repairEvent(context: Context) {
    val db = AppDatabase.getInstance(context)
    val dao = db.appDao()

    val isValid = validateSignature(context)
    val locationData = dao.getLocationsByEventId(this.id).first()

    if (this is EventRange && checkTimeToLocationMismatch(locationData.map { it.toLocationData() })) {
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

    // if (this is EventRange && checkShouldBeEventPoint()) {
    //     dao.forceEventPoint(this.id)
    // }
}