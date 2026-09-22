package com.olafsapp.gsearch14.data

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.olafsapp.gsearch14.R
import java.net.URLEncoder
import java.util.Locale

/**
 * The kind of results a search asks for. Not every engine supports every vertical —
 * see [SearchEngine.verticals].
 */
enum class SearchVertical(@param:StringRes val labelRes: Int) {
    WEB(R.string.vertical_web),
    IMAGES(R.string.vertical_images),
    VIDEOS(R.string.vertical_videos),
    NEWS(R.string.vertical_news),
}

/**
 * Stable identifiers — these are persisted, so never rename the constants.
 *
 * Earlier versions also shipped DUCKDUCKGO, STARTPAGE, BRAVE, ECOSIA and MOJEEK. Their ids
 * can still appear in a stored history or bookmark; [LibraryRepository]'s JSON reader
 * coerces unknown values back to [GOOGLE] rather than failing the whole document.
 */
enum class SearchEngineId {
    GOOGLE,
    MARGINALIA,
    WIKIPEDIA,
}

/**
 * How an engine responds to the "no AI" switch.
 *
 * The whole point of this app is reaching plain, human-written results, so every engine
 * declares honestly what it can actually do about AI answers.
 */
enum class AiFreeSupport {
    /** The engine takes a URL parameter or host that reliably suppresses AI answers. */
    VIA_URL,

    /** The engine has no AI answers to begin with, so the switch is a no-op. */
    ALWAYS_AI_FREE,

    /** AI answers exist but can only be turned off in the engine's own settings. */
    ENGINE_SETTING_ONLY,
}

/**
 * A search provider plus everything needed to build result and suggestion URLs for it.
 *
 * URL shapes were verified against the live services; where a service offers no
 * autocomplete of its own, [suggestUrl] falls back to DuckDuckGo, which is the most
 * privacy-preserving of the endpoints that do work.
 */
data class SearchEngine(
    val id: SearchEngineId,
    val displayName: String,
    /** Shown as the monogram inside the engine badge. */
    val monogram: String,
    val brandColor: Color,
    val verticals: Set<SearchVertical>,
    val aiFreeSupport: AiFreeSupport,
    /** Short, factual note about this engine's AI behaviour, shown in the engine sheet. */
    @param:StringRes val noteRes: Int,
) {
    fun supports(vertical: SearchVertical): Boolean = vertical in verticals

    /**
     * Builds the result URL.
     *
     * @param aiFree when true, apply whatever this engine offers to suppress AI answers.
     */
    fun resultUrl(query: String, vertical: SearchVertical, aiFree: Boolean): String {
        val q = encode(query)
        val effective = if (supports(vertical)) vertical else SearchVertical.WEB
        return when (id) {
            // udm is Google's current filter parameter. udm=14 is the plain "Web" tab,
            // which is what actually removes AI Overviews; the other values pick a vertical
            // and carry no AI answers anyway.
            SearchEngineId.GOOGLE -> {
                val udm = when (effective) {
                    SearchVertical.WEB -> if (aiFree) "14" else null
                    SearchVertical.IMAGES -> "2"
                    SearchVertical.VIDEOS -> "7"
                    SearchVertical.NEWS -> "12"
                }
                "https://www.google.com/search?q=$q" + (udm?.let { "&udm=$it" } ?: "")
            }

            SearchEngineId.MARGINALIA -> "https://marginalia-search.com/search?query=$q"

            // A plain search jumps straight to the article on an exact title match, which
            // is what you want for Web. Images needs fulltext=1 to stay on the results page
            // and ns6=1 to restrict it to the File namespace.
            SearchEngineId.WIKIPEDIA -> {
                val base = "https://${wikiLanguage()}.wikipedia.org/w/index.php?search=$q"
                if (effective == SearchVertical.IMAGES) "$base&fulltext=1&ns6=1" else base
            }
        }
    }

    /**
     * Autocomplete endpoint. All of these answer with an OpenSearch-shaped JSON array —
     * `["typed", ["suggestion", ...]]` — which is what [com.olafsapp.gsearch14.data.suggest]
     * parses.
     */
    fun suggestUrl(prefix: String): String {
        val q = encode(prefix)
        return when (id) {
            SearchEngineId.GOOGLE ->
                "https://suggestqueries.google.com/complete/search?client=firefox&q=$q"

            SearchEngineId.WIKIPEDIA ->
                "https://${wikiLanguage()}.wikipedia.org/w/api.php" +
                    "?action=opensearch&format=json&limit=8&search=$q"

            // Marginalia publishes no autocomplete of its own, so it borrows DuckDuckGo's —
            // the least identifying of the endpoints that actually work.
            SearchEngineId.MARGINALIA -> "https://duckduckgo.com/ac/?q=$q&type=list"
        }
    }

    private fun wikiLanguage(): String {
        val tag = Locale.getDefault().language.lowercase(Locale.ROOT)
        return if (tag in SUPPORTED_WIKI_LANGUAGES) tag else "en"
    }

    companion object {
        private val SUPPORTED_WIKI_LANGUAGES =
            setOf("en", "de", "fr", "es", "it", "nl", "pl", "pt", "sv", "ru", "ja", "zh")

        private fun encode(value: String): String =
            URLEncoder.encode(value, Charsets.UTF_8.name())

        private val ALL_VERTICALS = SearchVertical.entries.toSet()

        val GOOGLE = SearchEngine(
            id = SearchEngineId.GOOGLE,
            displayName = "Google",
            monogram = "G",
            brandColor = Color(0xFF4285F4),
            verticals = ALL_VERTICALS,
            aiFreeSupport = AiFreeSupport.VIA_URL,
            noteRes = R.string.engine_note_google,
        )

        val MARGINALIA = SearchEngine(
            id = SearchEngineId.MARGINALIA,
            displayName = "Marginalia",
            monogram = "M",
            brandColor = Color(0xFF8B5E3C),
            verticals = setOf(SearchVertical.WEB),
            aiFreeSupport = AiFreeSupport.ALWAYS_AI_FREE,
            noteRes = R.string.engine_note_marginalia,
        )

        val WIKIPEDIA = SearchEngine(
            id = SearchEngineId.WIKIPEDIA,
            displayName = "Wikipedia",
            monogram = "W",
            brandColor = Color(0xFF636466),
            verticals = setOf(SearchVertical.WEB, SearchVertical.IMAGES),
            aiFreeSupport = AiFreeSupport.ALWAYS_AI_FREE,
            noteRes = R.string.engine_note_wikipedia,
        )

        /** Display order in the engine switcher. */
        val ALL = listOf(GOOGLE, WIKIPEDIA, MARGINALIA)

        val DEFAULT = GOOGLE

        /**
         * Never throws: a stored id that is no longer in the catalogue resolves to the
         * default, so old history entries stay usable instead of crashing the list.
         */
        fun byId(id: SearchEngineId): SearchEngine =
            ALL.firstOrNull { it.id == id } ?: DEFAULT

        fun byIdOrDefault(name: String?): SearchEngine {
            val id = runCatching { SearchEngineId.valueOf(name ?: "") }.getOrNull()
            return id?.let(::byId) ?: DEFAULT
        }
    }
}
