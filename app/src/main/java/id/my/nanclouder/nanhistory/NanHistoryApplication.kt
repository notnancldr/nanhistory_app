package id.my.nanclouder.nanhistory

import android.app.Application
import id.my.nanclouder.nanhistory.di.AppContainer

class NanHistoryApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
