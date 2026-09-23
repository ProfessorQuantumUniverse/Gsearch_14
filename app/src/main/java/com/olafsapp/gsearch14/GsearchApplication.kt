package com.olafsapp.gsearch14

import android.app.Application
import android.content.Context
import android.util.Log
import com.olafsapp.gsearch14.data.repo.LibraryRepository
import com.olafsapp.gsearch14.data.repo.SettingsRepository
import com.olafsapp.gsearch14.data.suggest.SuggestionClient
import com.olafsapp.gsearch14.widget.SearchWidgetPreviews
import kotlinx.coroutines.CoroutineExceptionHandler
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
    // Background work here is best effort. A failed migration or a full disk must be
    // logged, not take the whole process down with an uncaught exception.
    val applicationScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, error ->
            Log.w(TAG, "Background task failed", error)
        },
    )

    val settings = SettingsRepository(context)
    val library = LibraryRepository(context, applicationScope)
    val suggestions = SuggestionClient()

    /** Pulls forward anything stored by versions up to 3.0. Safe to call more than once. */
    fun migrateLegacyData() {
        // Two launches so a failure in one migration does not skip the other.
        applicationScope.launch { settings.migrateLegacySettingsIfNeeded() }
        applicationScope.launch { library.migrateLegacyHistoryIfNeeded() }
    }

    private companion object {
        const val TAG = "Gsearch"
    }
}

class GsearchApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.migrateLegacyData()
        SearchWidgetPreviews.publishIfNeeded(this, container.applicationScope)
    }
}

/** Reaches the container from anywhere that has a [Context]. */
val Context.appContainer: AppContainer
    get() = (applicationContext as GsearchApplication).container
