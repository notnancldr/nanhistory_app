package id.my.nanclouder.nanhistory.di

import android.content.Context
import id.my.nanclouder.nanhistory.db.AppDao
import id.my.nanclouder.nanhistory.db.AppDatabase
import java.time.Clock

class AppContainer(private val context: Context) {
    val clock: Clock = Clock.systemDefaultZone()

    // Dynamic DAO provider guarantees active handle even after AppDatabase.clearInstance()
    val daoProvider: () -> AppDao = {
        AppDatabase.getInstance(context).appDao()
    }
}
