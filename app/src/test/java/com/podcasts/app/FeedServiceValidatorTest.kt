package com.podcasts.app

import androidx.test.core.app.ApplicationProvider
import com.podcasts.app.data.remote.FeedParser
import com.podcasts.app.data.remote.FeedService
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Guards the ordering bug that froze a show's episode list permanently.
 *
 * Saving a feed's ETag before its episodes are stored means an interrupted
 * parse leaves the validator on disk with no episodes behind it. Every later
 * refresh then sends it, receives a perfectly correct 304, and the show never
 * updates again.
 */
@RunWith(RobolectricTestRunner::class)
class FeedServiceValidatorTest {

    private lateinit var server: MockWebServer
    private lateinit var service: FeedService

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        service = FeedService(
            OkHttpClient(),
            FeedParser(),
            ApplicationProvider.getApplicationContext(),
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun enqueueFeed(etag: String) {
        server.enqueue(
            MockResponse()
                .setHeader("ETag", etag)
                .setHeader("Content-Type", "application/xml")
                .setBody(FEED),
        )
    }

    @Test
    fun `fetching alone does not remember the validator`() = runTest {
        enqueueFeed("\"v1\"")
        val url = server.url("/feed.xml").toString()

        val first = service.fetch(url)
        assertNotNull(first)
        assertEquals("\"v1\"", first!!.etag)

        // Second fetch: nothing was committed, so no conditional header may be
        // sent - otherwise a crash mid-parse would freeze the feed forever.
        enqueueFeed("\"v1\"")
        service.fetch(url)

        server.takeRequest()
        val second = server.takeRequest()
        assertNull(second.getHeader("If-None-Match"))
    }

    @Test
    fun `committing the validator makes the next request conditional`() = runTest {
        enqueueFeed("\"v1\"")
        val url = server.url("/feed.xml").toString()

        val response = service.fetch(url)!!
        service.commitValidators(url, response.etag, response.lastModified)

        server.enqueue(MockResponse().setResponseCode(304))
        val repeat = service.fetch(url)

        assertNull("A 304 must surface as null", repeat)
        server.takeRequest()
        assertEquals("\"v1\"", server.takeRequest().getHeader("If-None-Match"))
    }

    @Test
    fun `an explicit refresh ignores a stored validator`() = runTest {
        enqueueFeed("\"v1\"")
        val url = server.url("/feed.xml").toString()
        val response = service.fetch(url)!!
        service.commitValidators(url, response.etag, response.lastModified)

        enqueueFeed("\"v2\"")
        service.fetch(url, useCache = false)

        server.takeRequest()
        val forced = server.takeRequest()
        assertNull(
            "Menu > Refresh is the escape hatch for a stuck feed",
            forced.getHeader("If-None-Match"),
        )
    }

    private companion object {
        val FEED = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0" xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd">
              <channel>
                <title>A Show</title>
                <item>
                  <title>Latest episode</title>
                  <guid>ep-1</guid>
                  <pubDate>Mon, 07 Sep 2026 17:23:00 +0000</pubDate>
                  <enclosure url="https://example.com/1.mp3" length="1" type="audio/mpeg"/>
                </item>
              </channel>
            </rss>
        """.trimIndent()
    }
}
