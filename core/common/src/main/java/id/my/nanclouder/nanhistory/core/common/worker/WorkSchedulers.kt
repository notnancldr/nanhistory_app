package id.my.nanclouder.nanhistory.core.common.worker

import android.content.Context

interface WorkScheduler {
    fun schedulePeriodicWork(context: Context)
}

object WorkSchedulers {
    private var scheduler: WorkScheduler? = null

    fun register(scheduler: WorkScheduler) {
        this.scheduler = scheduler
    }

    fun schedule(context: Context) {
        scheduler?.schedulePeriodicWork(context)
    }
}
