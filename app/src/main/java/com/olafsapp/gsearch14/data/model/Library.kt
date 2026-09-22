package com.olafsapp.gsearch14.data.model

import com.olafsapp.gsearch14.data.SearchEngineId
import com.olafsapp.gsearch14.data.SearchVertical
import kotlinx.serialization.Serializable

/** One recorded search. [id] is stable so list animations can key on it. */
@Serializable
data class HistoryEntry(
    val id: String,
    val query: String,
    val engineId: SearchEngineId = SearchEngineId.GOOGLE,
    val vertical: SearchVertical = SearchVertical.WEB,
    val aiFree: Boolean = true,
    val timestamp: Long = 0L,
    /** Pinned entries sort to the top and survive the history size cap. */
    val pinned: Boolean = false,
)

/** A saved result page. */
@Serializable
data class Bookmark(
    val id: String,
    val title: String,
    val url: String,
    val engineId: SearchEngineId = SearchEngineId.GOOGLE,
    val timestamp: Long = 0L,
)

/** Everything the library screen shows, persisted as one atomic document. */
@Serializable
data class LibraryData(
    val history: List<HistoryEntry> = emptyList(),
    val bookmarks: List<Bookmark> = emptyList(),
    /** Guards the one-time import of the pre-4.0 SharedPreferences history. */
    val migratedLegacyHistory: Boolean = false,
)
