package com.podcasts.app.data.remote

import android.util.Xml
import com.podcasts.app.domain.Podcast
import java.io.InputStream
import java.io.StringWriter
import org.xmlpull.v1.XmlPullParser

/**
 * OPML import/export — the same interchange format Google Podcasts' "Export
 * subscriptions" produced, so an export from the old app imports here directly.
 */
object Opml {

    data class Entry(val title: String, val feedUrl: String)

    fun parse(input: InputStream): List<Entry> {
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            setInput(input, null)
        }
        val entries = mutableListOf<Entry>()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name.equals("outline", true)) {
                val url = parser.getAttributeValue(null, "xmlUrl")
                if (!url.isNullOrBlank()) {
                    val title = parser.getAttributeValue(null, "text")
                        ?: parser.getAttributeValue(null, "title")
                        ?: url
                    entries += Entry(title, url)
                }
            }
            event = parser.next()
        }
        return entries.distinctBy { it.feedUrl }
    }

    fun write(podcasts: List<Podcast>): String {
        val writer = StringWriter()
        val serializer = Xml.newSerializer().apply {
            setOutput(writer)
            setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)
            startDocument("UTF-8", true)
        }
        with(serializer) {
            startTag(null, "opml")
            attribute(null, "version", "1.0")
            startTag(null, "head")
            startTag(null, "title").text("Podcast subscriptions").endTag(null, "title")
            endTag(null, "head")
            startTag(null, "body")
            podcasts.forEach { podcast ->
                startTag(null, "outline")
                attribute(null, "type", "rss")
                attribute(null, "text", podcast.title)
                attribute(null, "title", podcast.title)
                attribute(null, "xmlUrl", podcast.feedUrl)
                podcast.link?.let { attribute(null, "htmlUrl", it) }
                endTag(null, "outline")
            }
            endTag(null, "body")
            endTag(null, "opml")
            endDocument()
        }
        return writer.toString()
    }
}
