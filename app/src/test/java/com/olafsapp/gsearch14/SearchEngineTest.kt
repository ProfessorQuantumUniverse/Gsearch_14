package com.olafsapp.gsearch14

import com.olafsapp.gsearch14.data.AiFreeSupport
import com.olafsapp.gsearch14.data.SearchEngine
import com.olafsapp.gsearch14.data.SearchEngineId
import com.olafsapp.gsearch14.data.SearchVertical
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The URL builder is the one piece of this app that must be exactly right — a wrong
 * parameter silently brings back the AI answers the whole app exists to avoid.
 */
class SearchEngineTest {

    @Test
    fun `google web search applies the udm=14 filter when ai-free`() {
        val url = SearchEngine.GOOGLE.resultUrl("kotlin flows", SearchVertical.WEB, aiFree = true)
        assertTrue(url, url.startsWith("https://www.google.com/search?q="))
        assertTrue(url, url.contains("&udm=14"))
    }

    @Test
    fun `google web search omits udm when ai answers are allowed`() {
        val url = SearchEngine.GOOGLE.resultUrl("kotlin flows", SearchVertical.WEB, aiFree = false)
        assertFalse(url, url.contains("udm="))
    }

    @Test
    fun `google verticals use their own udm values regardless of the ai switch`() {
        fun udmFor(vertical: SearchVertical, aiFree: Boolean) =
            SearchEngine.GOOGLE.resultUrl("cats", vertical, aiFree)
                .substringAfter("&udm=", "")

        assertEquals("2", udmFor(SearchVertical.IMAGES, aiFree = true))
        assertEquals("2", udmFor(SearchVertical.IMAGES, aiFree = false))
        assertEquals("7", udmFor(SearchVertical.VIDEOS, aiFree = true))
        assertEquals("12", udmFor(SearchVertical.NEWS, aiFree = true))
    }

    @Test
    fun `wikipedia web search goes straight to the article`() {
        val url = SearchEngine.WIKIPEDIA.resultUrl("katze", SearchVertical.WEB, aiFree = true)
        assertTrue(url, url.contains(".wikipedia.org/w/index.php?search=katze"))
        // Without fulltext an exact title match redirects to the article, which is the
        // point of the Web vertical here.
        assertFalse(url, url.contains("fulltext="))
    }

    @Test
    fun `wikipedia image search restricts to the file namespace`() {
        val url = SearchEngine.WIKIPEDIA.resultUrl("katze", SearchVertical.IMAGES, aiFree = true)
        // fulltext=1 keeps the results page from redirecting; ns6 is the File namespace.
        assertTrue(url, url.contains("&fulltext=1"))
        assertTrue(url, url.contains("&ns6=1"))
    }

    @Test
    fun `queries are percent-encoded`() {
        val url = SearchEngine.GOOGLE.resultUrl("größe & maß", SearchVertical.WEB, aiFree = true)
        assertFalse(url, url.contains(" "))
        assertTrue(url, url.contains("%C3%B6"))
        // A bare ampersand would terminate the query parameter and drop the rest.
        assertTrue(url, url.contains("%26"))
    }

    @Test
    fun `an unsupported vertical falls back to web`() {
        // Marginalia indexes text pages only, so an image search must not produce a
        // parameter it would ignore or choke on.
        val url = SearchEngine.MARGINALIA.resultUrl("gardening", SearchVertical.IMAGES, true)
        assertEquals("https://marginalia-search.com/search?query=gardening", url)
    }

    @Test
    fun `every engine builds an https url for every vertical it claims to support`() {
        SearchEngine.ALL.forEach { engine ->
            engine.verticals.forEach { vertical ->
                listOf(true, false).forEach { aiFree ->
                    val url = engine.resultUrl("test query", vertical, aiFree)
                    assertTrue("${engine.id}/$vertical: $url", url.startsWith("https://"))
                    assertTrue("${engine.id}/$vertical: $url", url.contains("test+query"))
                }
            }
        }
    }

    @Test
    fun `every engine has a reachable suggest endpoint`() {
        SearchEngine.ALL.forEach { engine ->
            val url = engine.suggestUrl("and")
            assertTrue("${engine.id}: $url", url.startsWith("https://"))
        }
    }

    @Test
    fun `only google can switch ai answers off by url`() {
        assertEquals(AiFreeSupport.VIA_URL, SearchEngine.GOOGLE.aiFreeSupport)
        assertEquals(AiFreeSupport.ALWAYS_AI_FREE, SearchEngine.WIKIPEDIA.aiFreeSupport)
        assertEquals(AiFreeSupport.ALWAYS_AI_FREE, SearchEngine.MARGINALIA.aiFreeSupport)
    }

    @Test
    fun `an unknown stored engine id falls back to the default`() {
        // A history entry written before an engine was dropped from the catalogue.
        assertEquals(SearchEngine.DEFAULT.id, SearchEngine.byIdOrDefault("DUCKDUCKGO").id)
        assertEquals(SearchEngine.DEFAULT.id, SearchEngine.byIdOrDefault("YAHOO").id)
        assertEquals(SearchEngine.DEFAULT.id, SearchEngine.byIdOrDefault(null).id)
        assertEquals(SearchEngineId.MARGINALIA, SearchEngine.byIdOrDefault("MARGINALIA").id)
    }

    @Test
    fun `every engine id in the catalogue is resolvable`() {
        SearchEngineId.entries.forEach { id ->
            assertEquals(id, SearchEngine.byId(id).id)
        }
    }
}
