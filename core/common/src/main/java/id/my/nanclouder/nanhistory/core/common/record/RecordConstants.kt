package id.my.nanclouder.nanhistory.core.common.record

object ServiceBroadcast {
    const val ACTION_SERVICE_STATUS = "id.my.nanclouder.nanhistory.ACTION_SERVICE_STATUS"
    const val EXTRA_STATUS = "EXTRA_IS_RUNNING"
    const val EXTRA_EVENT_ID = "EXTRA_EVENT_ID"
    const val EXTRA_EVENT_PATH = "EXTRA_EVENT_PATH"
    const val EXTRA_EVENT_POINT = "EXTRA_EVENT_POINT"

    const val ACTION_UPDATE_DATA = "id.my.nanclouder.nanhistory.ACTION_UPDATE_DATA"
}

object RecordStatus {
    const val RESTARTING = -2
    const val BUSY = -1
    const val READY = 0
    const val IDLE = 1
    const val RUNNING = 2
}
