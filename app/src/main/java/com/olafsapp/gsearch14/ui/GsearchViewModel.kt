package com.olafsapp.gsearch14.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.olafsapp.gsearch14.appContainer
import com.olafsapp.gsearch14.data.AiFreeSupport
import com.olafsapp.gsearch14.data.SearchEngine
import com.olafsapp.gsearch14.data.SearchEngineId
import com.olafsapp.gsearch14.data.SearchVertical
import com.olafsapp.gsearch14.data.model.Bookmark
import com.olafsapp.gsearch14.data.model.HistoryEntry
import com.olafsapp.gsearch14.data.model.LibraryData
import com.olafsapp.gsearch14.data.repo.AccentPalette
import com.olafsapp.gsearch14.data.repo.AppSettings
import com.olafsapp.gsearch14.data.repo.OpenTarget
import com.olafsapp.gsearch14.data.repo.ThemeMode
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One autocomplete row. */
data class Suggestion(val text: String, val fromHistory: Boolean)

/** Everything needed to open a result page. */
data class SearchRequest(
    val url: String,
    val query: String,
    val engineId: SearchEngineId,
)

@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class GsearchViewModel(application: Application) : AndroidViewModel(application) {

    private val container = application.appContainer
    private val settingsRepo = container.settings
    private val libraryRepo = container.library

    val settings: StateFlow<AppSettings> = settingsRepo.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    val library: StateFlow<LibraryData> = libraryRepo.data
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryData())

    /** The current input. Held as Compose state so the field stays in sync on rotation. */
    var query by mutableStateOf("")
        private set

    /** The result type. Session state on purpose — it resets to Web on a fresh start. */
    var vertical by mutableStateOf(SearchVertical.WEB)
        private set

    /**
     * Autocomplete for the current input.
     *
     * Local history matches appear first and instantly; remote completions are debounced so
     * a fast typist triggers one request rather than one per keystroke.
     */
    val suggestions: StateFlow<List<Suggestion>> =
        combine(
            snapshotFlow { query }.debounce(180).distinctUntilChanged(),
            settings,
            library,
        ) { text, appSettings, libraryData ->
            Triple(text.trim(), appSettings, libraryData)
        }.flatMapLatest { (text, appSettings, libraryData) ->
            flow {
                if (text.length < 2) {
                    emit(emptyList())
                    return@flow
                }

                val fromHistory = libraryData.history
                    .map { it.query }
                    .distinct()
                    .filter { it.startsWith(text, ignoreCase = true) && !it.equals(text, true) }
                    .take(3)
                    .map { Suggestion(it, fromHistory = true) }

                // Show what we already know before the network answers.
                emit(fromHistory)

                if (!appSettings.suggestionsEnabled) return@flow

                val remote = container.suggestions
                    .suggestions(appSettings.engine, text)
                    .filterNot { candidate ->
                        fromHistory.any { it.text.equals(candidate, ignoreCase = true) }
                    }
                    .map { Suggestion(it, fromHistory = false) }

                emit(fromHistory + remote)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // --- Input -------------------------------------------------------------------

    fun onQueryChange(value: String) {
        query = value
    }

    fun selectVertical(value: SearchVertical) {
        vertical = value
    }

    /**
     * Builds the request for the current input and records it in history.
     *
     * Returns null when there is nothing to search for, so the caller can nudge the user
     * instead of opening an empty results page.
     */
    fun submit(text: String = query): SearchRequest? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null

        val current = settings.value
        val engine = current.engine
        val effectiveVertical = if (engine.supports(vertical)) vertical else SearchVertical.WEB
        // ALWAYS_AI_FREE engines have nothing to switch off, so the URL is the same either way.
        val aiFree = current.aiFree || engine.aiFreeSupport == AiFreeSupport.ALWAYS_AI_FREE

        if (!current.incognito) {
            viewModelScope.launch {
                libraryRepo.recordSearch(trimmed, engine.id, effectiveVertical, current.aiFree)
            }
        }

        return SearchRequest(
            url = engine.resultUrl(trimmed, effectiveVertical, aiFree),
            query = trimmed,
            engineId = engine.id,
        )
    }

    /** Replays a history entry exactly as it was run, engine and vertical included. */
    fun replay(entry: HistoryEntry): SearchRequest {
        val engine = SearchEngine.byId(entry.engineId)
        query = entry.query
        vertical = entry.vertical
        viewModelScope.launch {
            settingsRepo.setEngine(entry.engineId)
            if (engine.aiFreeSupport == AiFreeSupport.VIA_URL) {
                settingsRepo.setAiFree(entry.aiFree)
            }
            if (!settings.value.incognito) {
                libraryRepo.recordSearch(entry.query, entry.engineId, entry.vertical, entry.aiFree)
            }
        }
        val aiFree = entry.aiFree || engine.aiFreeSupport == AiFreeSupport.ALWAYS_AI_FREE
        return SearchRequest(
            url = engine.resultUrl(entry.query, entry.vertical, aiFree),
            query = entry.query,
            engineId = entry.engineId,
        )
    }

    // --- Settings ----------------------------------------------------------------

    fun selectEngine(engine: SearchEngine) = viewModelScope.launch {
        settingsRepo.setEngine(engine.id)
        // Keep the vertical valid for the new engine rather than silently searching the web
        // when the user had, say, Videos selected.
        if (!engine.supports(vertical)) vertical = SearchVertical.WEB
    }

    fun setAiFree(value: Boolean) = viewModelScope.launch { settingsRepo.setAiFree(value) }
    fun setThemeMode(value: ThemeMode) = viewModelScope.launch { settingsRepo.setThemeMode(value) }
    fun setDynamicColor(value: Boolean) =
        viewModelScope.launch { settingsRepo.setDynamicColor(value) }

    fun setAccent(value: AccentPalette) = viewModelScope.launch { settingsRepo.setAccent(value) }
    fun setPureBlackDark(value: Boolean) =
        viewModelScope.launch { settingsRepo.setPureBlackDark(value) }

    fun setOpenTarget(value: OpenTarget) =
        viewModelScope.launch { settingsRepo.setOpenTarget(value) }

    fun setSuggestionsEnabled(value: Boolean) =
        viewModelScope.launch { settingsRepo.setSuggestionsEnabled(value) }

    fun setIncognito(value: Boolean) = viewModelScope.launch { settingsRepo.setIncognito(value) }
    fun setHapticsEnabled(value: Boolean) =
        viewModelScope.launch { settingsRepo.setHapticsEnabled(value) }

    fun setBlockThirdPartyCookies(value: Boolean) =
        viewModelScope.launch { settingsRepo.setBlockThirdPartyCookies(value) }

    // --- Library -----------------------------------------------------------------

    fun deleteHistoryEntry(entry: HistoryEntry) = viewModelScope.launch {
        libraryRepo.deleteHistoryEntry(entry.id)
    }

    fun restoreHistoryEntry(entry: HistoryEntry) = viewModelScope.launch {
        libraryRepo.restoreHistoryEntry(entry)
    }

    fun togglePinned(entry: HistoryEntry) = viewModelScope.launch {
        libraryRepo.togglePinned(entry.id)
    }

    fun clearHistory() = viewModelScope.launch { libraryRepo.clearHistory(keepPinned = true) }

    fun addBookmark(title: String, url: String) = viewModelScope.launch {
        libraryRepo.addBookmark(title, url, settings.value.engine.id)
    }

    fun removeBookmarkByUrl(url: String) = viewModelScope.launch {
        libraryRepo.removeBookmarkByUrl(url)
    }

    fun removeBookmark(bookmark: Bookmark) = viewModelScope.launch {
        libraryRepo.removeBookmark(bookmark.id)
    }

    fun restoreBookmark(bookmark: Bookmark) = viewModelScope.launch {
        libraryRepo.restoreBookmark(bookmark)
    }

    fun clearBookmarks() = viewModelScope.launch { libraryRepo.clearBookmarks() }

    fun isBookmarked(url: String): Boolean = library.value.bookmarks.any { it.url == url }
}
