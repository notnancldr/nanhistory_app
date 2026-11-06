package id.my.nanclouder.nanhistory

import android.app.Application
import id.my.nanclouder.nanhistory.core.common.di.CoreContainer
import id.my.nanclouder.nanhistory.core.common.di.CoreContainerProvider
import id.my.nanclouder.nanhistory.core.common.navigation.IntentRegistry
import id.my.nanclouder.nanhistory.di.AppContainer
import id.my.nanclouder.nanhistory.navigation.AppIntentProvider

class NanHistoryApplication : Application(), CoreContainerProvider {
    lateinit var container: AppContainer
        private set

    override val coreContainer: CoreContainer
        get() = container

    override fun onCreate() {
        super.onCreate()
        IntentRegistry.register(AppIntentProvider())
        container = AppContainer(this)
    }
}

