package com.podcasts.app

import com.podcasts.app.data.remote.FeedParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FeedParserTest {

    private val parser = FeedParser()

    @Test
    fun `parses channel and episodes`() {
        val result = parser.parse(FEED_URL, SAMPLE.byteInputStream())

        assertEquals("The Sample Show", result.podcast.title)
        assertEquals("Sample Media", result.podcast.author)
        assertEquals("https://example.com/art.jpg", result.podcast.imageUrl)
        assertEquals(2, result.episodes.size)
    }

    @Test
    fun `sorts episodes newest first`() {
        val episodes = parser.parse(FEED_URL, SAMPLE.byteInputStream()).episodes
        assertTrue(episodes[0].publishedAt > episodes[1].publishedAt)
        assertEquals("Episode two", episodes[0].title)
    }

    @Test
    fun `falls back to enclosure url when guid is missing`() {
        val episodes = parser.parse(FEED_URL, SAMPLE.byteInputStream()).episodes
        val noGuid = episodes.first { it.title == "Episode one" }
        assertEquals("https://example.com/1.mp3", noGuid.guid)
    }

    @Test
    fun `inherits channel art when an item has none`() {
        val episodes = parser.parse(FEED_URL, SAMPLE.byteInputStream()).episodes
        assertEquals("https://example.com/art.jpg", episodes.first { it.title == "Episode one" }.imageUrl)
    }

    @Test
    fun `parses itunes durations in every documented form`() {
        assertEquals(45_000L, FeedParser.parseDuration("45"))
        assertEquals(125_000L, FeedParser.parseDuration("2:05"))
        assertEquals(3_725_000L, FeedParser.parseDuration("1:02:05"))
        assertEquals(0L, FeedParser.parseDuration(""))
    }

    @Test
    fun `strips html from descriptions`() {
        assertEquals("Bold and plain", FeedParser.stripHtml("<p><b>Bold</b> and plain</p>"))
        assertEquals("A & B", FeedParser.stripHtml("A &amp; B"))
    }

    @Test
    fun `parses rfc822 and iso dates`() {
        assertTrue(FeedParser.parseDate("Mon, 04 Mar 2024 09:00:00 +0000") > 0)
        assertTrue(FeedParser.parseDate("2024-03-04T09:00:00Z") > 0)
        assertEquals(0L, FeedParser.parseDate("not a date"))
    }

    private companion object {
        const val FEED_URL = "https://example.com/feed.xml"

        val SAMPLE = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0" xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd">
              <channel>
                <title>The Sample Show</title>
                <link>https://example.com</link>
                <description>A show for tests</description>
                <itunes:author>Sample Media</itunes:author>
                <itunes:image href="https://example.com/art.jpg"/>
                <itunes:category text="Technology"/>
                <item>
                  <title>Episode one</title>
                  <description>&lt;p&gt;First&lt;/p&gt;</description>
                  <pubDate>Mon, 04 Mar 2024 09:00:00 +0000</pubDate>
                  <itunes:duration>32:10</itunes:duration>
                  <enclosure url="https://example.com/1.mp3" length="1000" type="audio/mpeg"/>
                </item>
                <item>
                  <title>Episode two</title>
                  <guid>episode-two</guid>
                  <description>Second</description>
                  <pubDate>Mon, 11 Mar 2024 09:00:00 +0000</pubDate>
                  <itunes:duration>1:02:05</itunes:duration>
                  <itunes:image href="https://example.com/two.jpg"/>
                  <enclosure url="https://example.com/2.mp3" length="2000" type="audio/mpeg"/>
                </item>
              </channel>
            </rss>
        """.trimIndent()
    }
}
