package live.fourthepeople.podcasts.data.remote

import android.util.Xml
import live.fourthepeople.podcasts.domain.Episode
import live.fourthepeople.podcasts.domain.Podcast
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException

data class ParsedFeed(val podcast: Podcast, val episodes: List<Episode>)

/**
 * Streaming RSS 2.0 / iTunes-namespace parser. Also tolerates Atom feeds well enough
 * to pull title/link, since a few shows still publish them.
 */
class FeedParser {

    fun parse(feedUrl: String, input: InputStream): ParsedFeed {
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            setInput(input, null)
        }

        var channelTitle = ""
        var channelAuthor = ""
        var channelDescription = ""
        var channelImage: String? = null
        var channelLink: String? = null
        val categories = LinkedHashSet<String>()

        val episodes = mutableListOf<Episode>()
        var item: ItemBuilder? = null
        var insideImageTag = false
        var text = StringBuilder()

        try {
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> {
                        val name = parser.name.lowercase(Locale.ROOT)
                        text = StringBuilder()
                        when (name) {
                            "item", "entry" -> item = ItemBuilder()
                            "image" -> insideImageTag = true
                            "itunes:image", "media:thumbnail" -> {
                                val href = parser.getAttributeValue(null, "href")
                                    ?: parser.getAttributeValue(null, "url")
                                if (item != null) item.image = href ?: item.image
                                else channelImage = href ?: channelImage
                            }
                            "enclosure", "media:content" -> {
                                item?.let {
                                    val type = parser.getAttributeValue(null, "type").orEmpty()
                                    val url = parser.getAttributeValue(null, "url")
                                    if (url != null && (type.startsWith("audio") || type.isEmpty() || it.audioUrl.isEmpty())) {
                                        it.audioUrl = url
                                        it.fileSize = parser.getAttributeValue(null, "length")
                                            ?.toLongOrNull() ?: 0L
                                    }
                                }
                            }
                            "itunes:category" -> parser.getAttributeValue(null, "text")
                                ?.let(categories::add)
                            "link" -> {
                                // Atom-style <link href="..."/>
                                parser.getAttributeValue(null, "href")?.let { href ->
                                    if (item == null) channelLink = channelLink ?: href
                                }
                            }
                        }
                    }

                    XmlPullParser.TEXT -> text.append(parser.text)

                    XmlPullParser.END_TAG -> {
                        val name = parser.name.lowercase(Locale.ROOT)
                        val value = text.toString().trim()
                        text = StringBuilder()
                        val current = item
                        if (current == null) {
                            when (name) {
                                "title" -> if (channelTitle.isEmpty()) channelTitle = value
                                "itunes:author", "managingeditor" ->
                                    if (channelAuthor.isEmpty()) channelAuthor = value
                                "description", "itunes:summary" ->
                                    if (channelDescription.isEmpty()) channelDescription = stripHtml(value)
                                "link" -> if (channelLink == null && value.isNotEmpty()) channelLink = value
                                "url" -> if (insideImageTag && channelImage == null) channelImage = value
                                "image" -> insideImageTag = false
                            }
                        } else {
                            when (name) {
                                "title" -> current.title = value
                                "description", "itunes:summary", "content:encoded", "summary" ->
                                    if (current.description.isEmpty()) current.description = stripHtml(value)
                                "guid", "id" -> if (current.guid.isEmpty()) current.guid = value
                                "pubdate", "published", "updated" ->
                                    current.publishedAt = parseDate(value)
                                "itunes:duration" -> current.durationMs = parseDuration(value)
                                "itunes:season" -> current.season = value.toIntOrNull()
                                "itunes:episode" -> current.episode = value.toIntOrNull()
                                "itunes:author" -> current.author = value
                                "item", "entry" -> {
                                    if (current.audioUrl.isNotEmpty()) {
                                        episodes += current.build(feedUrl, channelImage)
                                    }
                                    item = null
                                }
                            }
                        }
                    }
                }
                event = parser.next()
            }
        } catch (e: XmlPullParserException) {
            throw FeedException("Malformed feed at $feedUrl", e)
        }

        if (channelTitle.isEmpty()) throw FeedException("No channel title in $feedUrl")

        val podcast = Podcast(
            feedUrl = feedUrl,
            title = channelTitle,
            author = channelAuthor,
            description = channelDescription,
            imageUrl = channelImage,
            link = channelLink,
            categories = categories.toList(),
        )
        return ParsedFeed(podcast, episodes.sortedByDescending { it.publishedAt })
    }

    private class ItemBuilder {
        var guid = ""
        var title = ""
        var description = ""
        var audioUrl = ""
        var image: String? = null
        var author = ""
        var publishedAt = 0L
        var durationMs = 0L
        var fileSize = 0L
        var season: Int? = null
        var episode: Int? = null

        fun build(feedUrl: String, fallbackImage: String?) = Episode(
            // Feeds without a <guid> are common; the audio URL is the stable fallback.
            guid = guid.ifEmpty { audioUrl },
            feedUrl = feedUrl,
            title = title,
            description = description,
            audioUrl = audioUrl,
            imageUrl = image ?: fallbackImage,
            publishedAt = publishedAt,
            durationMs = durationMs,
            fileSizeBytes = fileSize,
            seasonNumber = season,
            episodeNumber = episode,
        )
    }

    companion object {
        private val DATE_FORMATS = listOf(
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "EEE, dd MMM yyyy HH:mm:ss z",
            "EEE, dd MMM yyyy HH:mm Z",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd",
        )

        fun parseDate(raw: String): Long {
            if (raw.isBlank()) return 0L
            for (pattern in DATE_FORMATS) {
                try {
                    val format = SimpleDateFormat(pattern, Locale.US)
                    if (pattern.endsWith("'Z'")) format.timeZone = TimeZone.getTimeZone("UTC")
                    return format.parse(raw)?.time ?: continue
                } catch (_: Exception) {
                    // try the next pattern
                }
            }
            return 0L
        }

        /** iTunes duration is "SS", "MM:SS" or "HH:MM:SS". */
        fun parseDuration(raw: String): Long {
            if (raw.isBlank()) return 0L
            val parts = raw.trim().split(":").mapNotNull { it.trim().toLongOrNull() }
            return when (parts.size) {
                1 -> parts[0] * 1000
                2 -> (parts[0] * 60 + parts[1]) * 1000
                3 -> (parts[0] * 3600 + parts[1] * 60 + parts[2]) * 1000
                else -> 0L
            }
        }

        private val TAG_REGEX = Regex("<[^>]*>")
        private val ENTITIES = mapOf(
            "&amp;" to "&", "&lt;" to "<", "&gt;" to ">",
            "&quot;" to "\"", "&#39;" to "'", "&apos;" to "'", "&nbsp;" to " ",
        )

        fun stripHtml(raw: String): String {
            var out = raw.replace("<br>", "\n", ignoreCase = true)
                .replace("<br/>", "\n", ignoreCase = true)
                .replace("</p>", "\n\n", ignoreCase = true)
            out = TAG_REGEX.replace(out, "")
            ENTITIES.forEach { (entity, replacement) -> out = out.replace(entity, replacement) }
            return out.trim()
        }
    }
}

class FeedException(message: String, cause: Throwable? = null) : Exception(message, cause)
