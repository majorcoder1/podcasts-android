package com.podcasts.app

import com.podcasts.app.data.remote.SearchApi
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchApiTest {

    private val api = SearchApi(
        OkHttpClient(),
        Json { ignoreUnknownKeys = true; isLenient = true },
    )

    @Test
    fun `the same feed listed twice collapses to one result`() {
        val results = api.parseResults(DUPLICATE_FEED)

        assertEquals(1, results.size)
        assertEquals("https://example.com/feed.xml", results[0].feedUrl)
        // Whichever listing came first wins, so the collection order is preserved.
        assertEquals("The Sample Show", results[0].title)
    }

    @Test
    fun `entries without a feed url are dropped`() {
        val results = api.parseResults(MISSING_FEED)

        assertEquals(1, results.size)
        assertEquals("https://example.com/only.xml", results[0].feedUrl)
    }

    @Test
    fun `large artwork is preferred over the thumbnail`() {
        val results = api.parseResults(DUPLICATE_FEED)
        assertEquals("https://example.com/600.jpg", results[0].imageUrl)
    }

    @Test
    fun `a malformed payload yields no results instead of throwing`() {
        assertTrue(api.parseResults("not json at all").isEmpty())
        assertTrue(api.parseResults("").isEmpty())
    }

    private companion object {
        val DUPLICATE_FEED = """
            {"resultCount":2,"results":[
              {"feedUrl":"https://example.com/feed.xml","collectionName":"The Sample Show",
               "artistName":"Sample Media","artworkUrl600":"https://example.com/600.jpg",
               "artworkUrl100":"https://example.com/100.jpg","trackCount":10,
               "primaryGenreName":"Technology"},
              {"feedUrl":"https://example.com/feed.xml","collectionName":"The Sample Show (Reruns)",
               "artistName":"Sample Media","artworkUrl100":"https://example.com/100.jpg",
               "trackCount":10,"primaryGenreName":"Technology"}
            ]}
        """.trimIndent()

        val MISSING_FEED = """
            {"resultCount":2,"results":[
              {"collectionName":"No Feed Here","artistName":"Nobody"},
              {"feedUrl":"https://example.com/only.xml","collectionName":"Real Show",
               "artistName":"Somebody"}
            ]}
        """.trimIndent()
    }
}
