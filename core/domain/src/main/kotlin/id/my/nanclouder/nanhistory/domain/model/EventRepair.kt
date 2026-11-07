package id.my.nanclouder.nanhistory.domain.model

object EventMetadata {
    const val IGNORE_REPAIR_EVENT = "ignore_repair_event"
}

fun HistoryEvent.isBroken(locationData: List<LocationData>): Boolean {
    val (locationTimeStart, locationTimeEnd) = locationData.minByOrNull { it.time }?.time to locationData.maxByOrNull { it.time }?.time
    if (locationTimeStart == null || locationTimeEnd == null) return false
    return (this is EventRange && (locationTimeStart < this.time || locationTimeEnd > this.end))
}

fun HistoryEvent.getBrokenReason(locationData: List<LocationData>): String {
    val (locationTimeStart, locationTimeEnd) = locationData.minByOrNull { it.time }?.time to locationData.maxByOrNull { it.time }?.time
    var reason = ""
    if (locationTimeStart != null && locationTimeEnd != null && this is EventRange && (locationTimeStart < this.time || locationTimeEnd > this.end)) {
        reason += "Time to location mismatch\n"
        reason += "|-- Location:\n"
        reason += "|   |-- Start: $locationTimeStart\n"
        reason += "|   `-- End:   $locationTimeEnd\n"
        reason += "`-- Event:\n"
        reason += "    |-- Start: ${this.time}\n"
        reason += "    `-- End:   ${this.end}\n"
    }
    return reason.removeSuffix("\n")
}
