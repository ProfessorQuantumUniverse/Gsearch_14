package com.olafsapp.gsearch14

import com.olafsapp.gsearch14.data.SearchEngineId
import com.olafsapp.gsearch14.data.SearchVertical
import com.olafsapp.gsearch14.data.model.LibraryData
import com.olafsapp.gsearch14.data.repo.libraryJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Version 4.0 shipped with eight search engines and then dropped five of them.
 *
 * Anyone who used one of those engines has its id sitting in their stored history. If the
 * reader rejected that id the whole document would count as corrupt, the corruption handler
 * would swap in an empty library, and the user would silently lose every search and
 * bookmark they had. These tests pin that behaviour down.
 */
class LibraryUpgradeTest {

    @Test
    fun `history written with a since-removed engine still loads`() {
        val stored = """
            {
              "history": [
                {"id":"1","query":"privacy","engineId":"DUCKDUCKGO","vertical":"WEB",
                 "aiFree":true,"timestamp":1000,"pinned":true},
                {"id":"2","query":"kotlin","engineId":"GOOGLE","vertical":"NEWS",
                 "aiFree":true,"timestamp":2000,"pinned":false}
              ],
              "bookmarks": [
                {"id":"b1","title":"Mojeek result","url":"https://www.mojeek.com/","engineId":"MOJEEK","timestamp":3000}
              ],
              "migratedLegacyHistory": true
            }
        """.trimIndent()

        val data = libraryJson.decodeFromString(LibraryData.serializer(), stored)

        assertEquals(2, data.history.size)
        assertEquals(1, data.bookmarks.size)

        // The dropped engine is coerced to the default rather than failing the document.
        assertEquals(SearchEngineId.GOOGLE, data.history[0].engineId)
        assertEquals(SearchEngineId.GOOGLE, data.bookmarks[0].engineId)

        // Everything else survives untouched.
        assertEquals("privacy", data.history[0].query)
        assertTrue(data.history[0].pinned)
        assertEquals(SearchVertical.NEWS, data.history[1].vertical)
        assertEquals("https://www.mojeek.com/", data.bookmarks[0].url)
    }

    @Test
    fun `an unknown vertical also degrades instead of failing`() {
        val stored = """
            {"history":[{"id":"1","query":"x","engineId":"GOOGLE","vertical":"PODCASTS",
             "aiFree":true,"timestamp":1,"pinned":false}],"bookmarks":[]}
        """.trimIndent()

        val data = libraryJson.decodeFromString(LibraryData.serializer(), stored)

        assertEquals(1, data.history.size)
        assertEquals(SearchVertical.WEB, data.history[0].vertical)
    }

    @Test
    fun `unknown fields from a future version are ignored`() {
        val stored = """
            {"history":[],"bookmarks":[],"migratedLegacyHistory":true,"somethingNew":42}
        """.trimIndent()

        val data = libraryJson.decodeFromString(LibraryData.serializer(), stored)
        assertTrue(data.migratedLegacyHistory)
    }

    @Test
    fun `a round trip preserves the library`() {
        val original = libraryJson.decodeFromString(
            LibraryData.serializer(),
            """{"history":[{"id":"1","query":"round trip","engineId":"MARGINALIA",
               "vertical":"WEB","aiFree":true,"timestamp":7,"pinned":true}],"bookmarks":[]}""",
        )
        val encoded = libraryJson.encodeToString(LibraryData.serializer(), original)
        assertEquals(original, libraryJson.decodeFromString(LibraryData.serializer(), encoded))
    }
}
