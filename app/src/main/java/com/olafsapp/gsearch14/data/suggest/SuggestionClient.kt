package com.olafsapp.gsearch14.data.suggest

import com.olafsapp.gsearch14.data.SearchEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import java.net.HttpURLConnection
import java.net.URL
import java.util.Collections
import kotlin.coroutines.coroutineContext

private const val CONNECT_TIMEOUT_MS = 4_000
private const val READ_TIMEOUT_MS = 4_000
private const val MAX_SUGGESTIONS = 8
private const val CACHE_SIZE = 64

/**
 * Fetches autocomplete suggestions from the active engine.
 *
 * Deliberately built on [HttpURLConnection] rather than a networking library: the app makes
 * exactly one kind of request, and the payload is a short JSON array. Responses are cached
 * in memory for the session so that backspacing through a query is free.
 */
class SuggestionClient {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val cache: MutableMap<String, List<String>> =
        Collections.synchronizedMap(
            object : LinkedHashMap<String, List<String>>(CACHE_SIZE, 0.75f, true) {
                override fun removeEldestEntry(
                    eldest: MutableMap.MutableEntry<String, List<String>>?,
                ): Boolean = size > CACHE_SIZE
            },
        )

    /**
     * Returns up to [MAX_SUGGESTIONS] completions for [prefix], or an empty list if the
     * request fails. Suggestions are a convenience — a network hiccup must never surface
     * as an error in the UI.
     */
    suspend fun suggestions(engine: SearchEngine, prefix: String): List<String> {
        val query = prefix.trim()
        if (query.length < 2) return emptyList()

        val key = "${engine.id}:${query.lowercase()}"
        cache[key]?.let { return it }

        return withContext(Dispatchers.IO) {
            val result = runCatching { fetch(engine.suggestUrl(query)) }
                .getOrDefault(emptyList())
            coroutineContext.ensureActive()
            if (result.isNotEmpty()) cache[key] = result
            result
        }
    }

    private suspend fun fetch(url: String): List<String> {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = true
            // Some suggest endpoints answer with an HTML error page unless a browser-ish
            // Accept header is present.
            setRequestProperty("Accept", "application/json, text/javascript, */*")
            setRequestProperty("User-Agent", SUGGEST_USER_AGENT)
        }
        try {
            if (connection.responseCode !in 200..299) return emptyList()
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            coroutineContext.ensureActive()
            return parseOpenSearch(body)
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Every endpoint the app talks to answers in the OpenSearch shape
     * `["typed", ["suggestion", ...], ...]`, so only the second element matters.
     */
    private fun parseOpenSearch(body: String): List<String> {
        val root = runCatching { json.parseToJsonElement(body) }.getOrNull() as? JsonArray
            ?: return emptyList()
        val entries = root.getOrNull(1) as? JsonArray ?: return emptyList()
        return entries
            .mapNotNull { (it as? JsonPrimitive)?.takeIf { p -> p.isString }?.content }
            .filter { it.isNotBlank() }
            .distinct()
            .take(MAX_SUGGESTIONS)
    }

    private companion object {
        // A current, plausible mobile UA. Some endpoints tailor or reject unknown agents.
        const val SUGGEST_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 16) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/140.0.0.0 Mobile Safari/537.36"
    }
}
