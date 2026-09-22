package com.olafsapp.gsearch14

import android.app.Application
import android.content.Context
import com.olafsapp.gsearch14.data.repo.LibraryRepository
import com.olafsapp.gsearch14.data.repo.SettingsRepository
import com.olafsapp.gsearch14.data.suggest.SuggestionClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Manual dependency container.
 *
 * The app has three collaborators and no build-time code generation, so a hand-written
 * container is both smaller and faster to build than a DI framework would be here.
 */
class AppContainer(context: Context) {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val settings = SettingsRepository(context)
    val library = LibraryRepository(context, applicationScope)
    val suggestions = SuggestionClient()

    /** Pulls forward anything stored by versions up to 3.0. Safe to call more than once. */
    fun migrateLegacyData() {
        applicationScope.launch {
            settings.migrateLegacySettingsIfNeeded()
            library.migrateLegacyHistoryIfNeeded()
        }
    }
}

class GsearchApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.migrateLegacyData()
    }
}

/** Reaches the container from anywhere that has a [Context]. */
val Context.appContainer: AppContainer
    get() = (applicationContext as GsearchApplication).container
