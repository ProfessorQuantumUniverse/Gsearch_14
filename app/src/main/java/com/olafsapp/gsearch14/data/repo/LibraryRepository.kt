package com.olafsapp.gsearch14.data.repo

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.dataStoreFile
import com.olafsapp.gsearch14.data.SearchEngineId
import com.olafsapp.gsearch14.data.SearchVertical
import com.olafsapp.gsearch14.data.model.Bookmark
import com.olafsapp.gsearch14.data.model.HistoryEntry
import com.olafsapp.gsearch14.data.model.LibraryData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

private const val MAX_HISTORY = 300
private const val MAX_BOOKMARKS = 500

/**
 * Reader/writer for the stored library.
 *
 * `coerceInputValues` matters for upgrades: a history entry written by an older version can
 * name a search engine that has since been removed from the catalogue. Without coercion the
 * enum fails to decode, the whole document counts as corrupt, and the corruption handler
 * replaces the user's entire history and bookmarks with an empty library. With it, the
 * unknown value falls back to the property's default instead.
 *
 * Internal rather than private so the upgrade path can be covered by a unit test.
 */
internal val libraryJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    coerceInputValues = true
}

private object LibrarySerializer : Serializer<LibraryData> {
    override val defaultValue = LibraryData()

    override suspend fun readFrom(input: InputStream): LibraryData =
        try {
            libraryJson.decodeFromString(
                LibraryData.serializer(),
                input.readBytes().decodeToString(),
            )
        } catch (e: SerializationException) {
            throw CorruptionException("Library store is unreadable", e)
        }

    override suspend fun writeTo(t: LibraryData, output: OutputStream) {
        output.write(libraryJson.encodeToString(LibraryData.serializer(), t).encodeToByteArray())
    }
}

/**
 * Search history and bookmarks, stored as a single JSON document so that a write is
 * always atomic across both lists.
 */
class LibraryRepository(
    private val context: Context,
    scope: CoroutineScope,
) {
    private val store: DataStore<LibraryData> = DataStoreFactory.create(
        serializer = LibrarySerializer,
        scope = scope,
        // A corrupt file must never take the app down: fall back to an empty library.
        corruptionHandler = androidx.datastore.core.handlers.ReplaceFileCorruptionHandler {
            LibraryData()
        },
        produceFile = { context.dataStoreFile("library.json") },
    )

    val data: Flow<LibraryData> = store.data.catch { cause ->
        if (cause is IOException) emit(LibraryData()) else throw cause
    }

    suspend fun recordSearch(
        query: String,
        engineId: SearchEngineId,
        vertical: SearchVertical,
        aiFree: Boolean,
    ) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        store.updateData { current ->
            // A repeat of the same query on the same engine moves to the top rather than
            // stacking up, but keeps its pinned state.
            val existing = current.history.firstOrNull {
                it.query.equals(trimmed, ignoreCase = true) &&
                    it.engineId == engineId &&
                    it.vertical == vertical
            }
            val entry = HistoryEntry(
                id = existing?.id ?: UUID.randomUUID().toString(),
                query = trimmed,
                engineId = engineId,
                vertical = vertical,
                aiFree = aiFree,
                timestamp = System.currentTimeMillis(),
                pinned = existing?.pinned == true,
            )
            val rest = current.history.filterNot { it.id == entry.id }
            current.copy(history = capHistory(listOf(entry) + rest))
        }
    }

    suspend fun deleteHistoryEntry(id: String) = store.updateData { current ->
        current.copy(history = current.history.filterNot { it.id == id })
    }

    suspend fun restoreHistoryEntry(entry: HistoryEntry) = store.updateData { current ->
        if (current.history.any { it.id == entry.id }) {
            current
        } else {
            current.copy(
                history = capHistory((current.history + entry).sortedByDescending { it.timestamp }),
            )
        }
    }

    suspend fun togglePinned(id: String) = store.updateData { current ->
        current.copy(
            history = current.history.map {
                if (it.id == id) it.copy(pinned = !it.pinned) else it
            },
        )
    }

    /** Clears history but deliberately keeps pinned entries. */
    suspend fun clearHistory(keepPinned: Boolean = true) = store.updateData { current ->
        current.copy(history = if (keepPinned) current.history.filter { it.pinned } else emptyList())
    }

    suspend fun addBookmark(title: String, url: String, engineId: SearchEngineId) =
        store.updateData { current ->
            if (current.bookmarks.any { it.url == url }) {
                current
            } else {
                val bookmark = Bookmark(
                    id = UUID.randomUUID().toString(),
                    title = title.ifBlank { url },
                    url = url,
                    engineId = engineId,
                    timestamp = System.currentTimeMillis(),
                )
                current.copy(bookmarks = (listOf(bookmark) + current.bookmarks).take(MAX_BOOKMARKS))
            }
        }

    suspend fun removeBookmarkByUrl(url: String) = store.updateData { current ->
        current.copy(bookmarks = current.bookmarks.filterNot { it.url == url })
    }

    suspend fun removeBookmark(id: String) = store.updateData { current ->
        current.copy(bookmarks = current.bookmarks.filterNot { it.id == id })
    }

    suspend fun restoreBookmark(bookmark: Bookmark) = store.updateData { current ->
        if (current.bookmarks.any { it.id == bookmark.id }) {
            current
        } else {
            current.copy(
                bookmarks = (current.bookmarks + bookmark).sortedByDescending { it.timestamp },
            )
        }
    }

    suspend fun clearBookmarks() = store.updateData { it.copy(bookmarks = emptyList()) }

    /**
     * Imports the history that versions up to 3.0 kept in SharedPreferences, then marks the
     * import done so it only ever runs once.
     */
    suspend fun migrateLegacyHistoryIfNeeded() {
        store.updateData { current ->
            if (current.migratedLegacyHistory) return@updateData current
            val imported = readLegacyHistory()
            current.copy(
                history = capHistory((current.history + imported).sortedByDescending { it.timestamp }),
                migratedLegacyHistory = true,
            )
        }
    }

    private fun readLegacyHistory(): List<HistoryEntry> {
        val prefs = context.getSharedPreferences("search_prefs", Context.MODE_PRIVATE)
        val raw = prefs.getString("search_history", null) ?: return emptyList()
        return runCatching {
            libraryJson.parseToJsonElement(raw).let { element ->
                element.jsonArrayOrNull()?.mapNotNull { item ->
                    val obj = item.objectOrNull() ?: return@mapNotNull null
                    val query = obj.stringOrNull("query")?.takeIf { it.isNotBlank() }
                        ?: return@mapNotNull null
                    HistoryEntry(
                        id = UUID.randomUUID().toString(),
                        query = query,
                        engineId = SearchEngineId.GOOGLE,
                        vertical = when (obj.stringOrNull("searchType")) {
                            "Images" -> SearchVertical.IMAGES
                            "Videos" -> SearchVertical.VIDEOS
                            "News" -> SearchVertical.NEWS
                            else -> SearchVertical.WEB
                        },
                        // The old flag meant "AI was allowed", which is the inverse of aiFree.
                        aiFree = obj.booleanOrNull("useAI") != true,
                        timestamp = obj.longOrNull("timestamp") ?: System.currentTimeMillis(),
                    )
                }
            }
        }.getOrNull().orEmpty()
    }

    private fun capHistory(entries: List<HistoryEntry>): List<HistoryEntry> {
        val (pinned, loose) = entries.partition { it.pinned }
        return pinned.sortedByDescending { it.timestamp } +
            loose.sortedByDescending { it.timestamp }.take(MAX_HISTORY)
    }
}

// --- Small helpers so the legacy import never throws on unexpected JSON ---

private fun kotlinx.serialization.json.JsonElement.jsonArrayOrNull() =
    this as? kotlinx.serialization.json.JsonArray

private fun kotlinx.serialization.json.JsonElement.objectOrNull() =
    this as? kotlinx.serialization.json.JsonObject

private fun kotlinx.serialization.json.JsonObject.stringOrNull(key: String): String? =
    (this[key] as? kotlinx.serialization.json.JsonPrimitive)?.takeIf { it.isString }?.content

private fun kotlinx.serialization.json.JsonObject.booleanOrNull(key: String): Boolean? =
    (this[key] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toBooleanStrictOrNull()

private fun kotlinx.serialization.json.JsonObject.longOrNull(key: String): Long? =
    (this[key] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toLongOrNull()
