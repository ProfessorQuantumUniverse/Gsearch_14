package com.olafsapp.gsearch14

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.olafsapp.gsearch14.core.UrlLauncher
import com.olafsapp.gsearch14.data.SearchEngine
import com.olafsapp.gsearch14.data.repo.OpenTarget
import com.olafsapp.gsearch14.data.repo.ThemeMode
import com.olafsapp.gsearch14.ui.GsearchViewModel
import com.olafsapp.gsearch14.ui.SearchRequest
import com.olafsapp.gsearch14.ui.home.HomeScreen
import com.olafsapp.gsearch14.ui.library.LibraryScreen
import com.olafsapp.gsearch14.ui.results.ResultsScreen
import com.olafsapp.gsearch14.ui.settings.SettingsScreen
import com.olafsapp.gsearch14.ui.theme.Gsearch14Theme
import kotlinx.serialization.Serializable

// --- Type-safe navigation routes ---

@Serializable
private object HomeRoute

@Serializable
private data class ResultsRoute(val url: String, val query: String, val engineId: String)

@Serializable
private object LibraryRoute

@Serializable
private object SettingsRoute

class MainActivity : ComponentActivity() {

    private val viewModel: GsearchViewModel by viewModels()

    /** Set when the activity is opened from the home screen widget. */
    private var launchedFromWidget by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        // Transparent system bars; the scrim handling is left to the platform, which picks
        // the right icon contrast from the theme.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.Transparent.toArgb(), Color.Transparent.toArgb()),
            navigationBarStyle = SystemBarStyle.auto(
                Color.Transparent.toArgb(),
                Color.Transparent.toArgb(),
            ),
        )
        super.onCreate(savedInstanceState)

        launchedFromWidget = intent.getBooleanExtra(EXTRA_FROM_WIDGET, false)

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (settings.themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            Gsearch14Theme(
                themeMode = settings.themeMode,
                accent = settings.accent,
                dynamicColor = settings.dynamicColor,
                pureBlackDark = settings.pureBlackDark,
                hapticsEnabled = settings.hapticsEnabled,
            ) {
                GsearchApp(
                    viewModel = viewModel,
                    darkTheme = darkTheme,
                    launchedFromWidget = launchedFromWidget,
                    onWidgetLaunchHandled = { launchedFromWidget = false },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // singleTask means a widget tap reuses this instance, so the flag has to be picked
        // up here as well as in onCreate.
        if (intent.getBooleanExtra(EXTRA_FROM_WIDGET, false)) {
            launchedFromWidget = true
        }
    }

    companion object {
        const val EXTRA_FROM_WIDGET = "from_widget"
    }
}

@Composable
private fun GsearchApp(
    viewModel: GsearchViewModel,
    darkTheme: Boolean,
    launchedFromWidget: Boolean,
    onWidgetLaunchHandled: () -> Unit,
) {
    val navController = rememberNavController()
    val context = LocalContext.current

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val library by viewModel.library.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()

    val toolbarColor = MaterialTheme.colorScheme.surfaceContainerLow.toArgb()

    /**
     * Routes a finished search to wherever the user wants results: the in-app reader, a
     * Custom Tab, or another browser entirely.
     */
    fun openResults(request: SearchRequest) {
        when (settings.openTarget) {
            OpenTarget.IN_APP -> navController.navigate(
                ResultsRoute(
                    url = request.url,
                    query = request.query,
                    engineId = request.engineId.name,
                ),
            )

            OpenTarget.CUSTOM_TAB -> UrlLauncher.openCustomTab(context, request.url, toolbarColor)
            OpenTarget.EXTERNAL_BROWSER -> {
                if (!UrlLauncher.openExternal(context, request.url)) {
                    // No browser installed: falling back keeps the search usable instead of
                    // silently doing nothing.
                    navController.navigate(
                        ResultsRoute(request.url, request.query, request.engineId.name),
                    )
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        enterTransition = { slideInHorizontally(tween(320)) { it / 8 } + fadeIn(tween(240)) },
        exitTransition = { slideOutHorizontally(tween(320)) { -it / 12 } + fadeOut(tween(180)) },
        popEnterTransition = { slideInHorizontally(tween(320)) { -it / 12 } + fadeIn(tween(240)) },
        popExitTransition = { slideOutHorizontally(tween(320)) { it / 8 } + fadeOut(tween(180)) },
    ) {
        composable<HomeRoute> {
            HomeScreen(
                settings = settings,
                query = viewModel.query,
                vertical = viewModel.vertical,
                suggestions = suggestions,
                history = library.history,
                focusInputOnStart = launchedFromWidget,
                onQueryChange = viewModel::onQueryChange,
                onVerticalChange = viewModel::selectVertical,
                onEngineChange = viewModel::selectEngine,
                onAiFreeChange = viewModel::setAiFree,
                onSubmit = viewModel::submit,
                onSearch = ::openResults,
                onReplay = { entry -> openResults(viewModel.replay(entry)) },
                onTogglePin = viewModel::togglePinned,
                onOpenLibrary = { navController.navigate(LibraryRoute) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
                onWidgetLaunchHandled = onWidgetLaunchHandled,
            )
        }

        composable<ResultsRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<ResultsRoute>()
            val engine = remember(route.engineId) {
                SearchEngine.byIdOrDefault(route.engineId)
            }
            val bookmarkedUrls = remember(library.bookmarks) {
                library.bookmarks.mapTo(HashSet()) { it.url }
            }

            ResultsScreen(
                url = route.url,
                query = route.query,
                engine = engine,
                bookmarkedUrls = bookmarkedUrls,
                blockThirdPartyCookies = settings.blockThirdPartyCookies,
                darkTheme = darkTheme,
                onBack = { navController.popBackStack() },
                onToggleBookmark = { title, url ->
                    if (library.bookmarks.any { it.url == url }) {
                        viewModel.removeBookmarkByUrl(url)
                    } else {
                        viewModel.addBookmark(title, url)
                    }
                },
            )
        }

        composable<LibraryRoute> {
            LibraryScreen(
                history = library.history,
                bookmarks = library.bookmarks,
                onBack = { navController.popBackStack() },
                onReplay = { entry -> openResults(viewModel.replay(entry)) },
                onTogglePin = viewModel::togglePinned,
                onDeleteHistoryEntry = viewModel::deleteHistoryEntry,
                onRestoreHistoryEntry = viewModel::restoreHistoryEntry,
                onClearHistory = viewModel::clearHistory,
                onOpenBookmark = { bookmark ->
                    openResults(
                        SearchRequest(
                            url = bookmark.url,
                            query = bookmark.title,
                            engineId = bookmark.engineId,
                        ),
                    )
                },
                onDeleteBookmark = viewModel::removeBookmark,
                onRestoreBookmark = viewModel::restoreBookmark,
                onClearBookmarks = viewModel::clearBookmarks,
            )
        }

        composable<SettingsRoute> {
            SettingsScreen(
                settings = settings,
                darkTheme = darkTheme,
                onBack = { navController.popBackStack() },
                onThemeModeChange = viewModel::setThemeMode,
                onDynamicColorChange = viewModel::setDynamicColor,
                onAccentChange = viewModel::setAccent,
                onPureBlackChange = viewModel::setPureBlackDark,
                onHapticsChange = viewModel::setHapticsEnabled,
                onEngineChange = viewModel::selectEngine,
                onSuggestionsChange = viewModel::setSuggestionsEnabled,
                onIncognitoChange = viewModel::setIncognito,
                onBlockCookiesChange = viewModel::setBlockThirdPartyCookies,
                onOpenTargetChange = viewModel::setOpenTarget,
            )
        }
    }
}
