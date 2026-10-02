package live.fourthepeople.podcasts.data.remote

import live.fourthepeople.podcasts.domain.SearchResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Directory search. Google's own catalog API was never public, so this uses the
 * free iTunes Search API, which returns the same `feedUrl` values the original
 * app's crawler indexed. Swap this class to change directories.
 */
@Singleton
class SearchApi @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json,
) {

    suspend fun search(term: String, limit: Int = 50): List<SearchResult> =
        withContext(Dispatchers.IO) {
            if (term.isBlank()) return@withContext emptyList()
            val url = "https://itunes.apple.com/search".toHttpUrl().newBuilder()
                .addQueryParameter("term", term)
                .addQueryParameter("media", "podcast")
                .addQueryParameter("entity", "podcast")
                .addQueryParameter("limit", limit.toString())
                .build()
            request(url.toString())
        }

    /**
     * Backs the Explore tab's category chips. The chart endpoint returns show
     * names rather than feed URLs, so each name is resolved through search.
     */
    suspend fun top(genreId: Int? = null, limit: Int = 30): List<SearchResult> =
        withContext(Dispatchers.IO) {
            val chartUrl = buildString {
                append("https://itunes.apple.com/us/rss/toppodcasts/limit=")
                append(limit)
                if (genreId != null) append("/genre=").append(genreId)
                append("/json")
            }
            val names = runCatching { chartNames(chartUrl) }.getOrDefault(emptyList())
            names.take(limit)
                .mapNotNull { name -> search(name, limit = 1).firstOrNull() }
                .distinctBy { it.feedUrl }
        }

    private fun chartNames(url: String): List<String> {
        return client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val feed = json.parseToJsonElement(response.body.string())
                .jsonObject["feed"]?.jsonObject ?: return emptyList()
            val entries = feed["entry"]?.jsonArray ?: return emptyList()
            entries.mapNotNull { entry ->
                entry.jsonObject["im:name"]?.jsonObject?.get("label")
                    ?.jsonPrimitive?.contentOrNull
            }
        }
    }

    private fun request(url: String): List<SearchResult> {
        return client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            parseResults(response.body.string())
        }
    }

    /**
     * A feed URL is the show's identity, so a result list must never carry it
     * twice — the directory does return the same show under more than one
     * collection, and Explore keys its grid by it.
     */
    internal fun parseResults(payload: String): List<SearchResult> {
        val decoded = runCatching { json.decodeFromString<ITunesResponse>(payload) }
            .getOrElse { return emptyList() }
        return decoded.results.mapNotNull { it.toDomain() }.distinctBy { it.feedUrl }
    }

    @Serializable
    internal data class ITunesResponse(val results: List<ITunesPodcast> = emptyList())

    @Serializable
    internal data class ITunesPodcast(
        val feedUrl: String? = null,
        @SerialName("collectionName") val name: String? = null,
        @SerialName("artistName") val artist: String? = null,
        @SerialName("artworkUrl600") val artworkLarge: String? = null,
        @SerialName("artworkUrl100") val artwork: String? = null,
        @SerialName("trackCount") val trackCount: Int = 0,
        @SerialName("primaryGenreName") val genre: String? = null,
    ) {
        fun toDomain(): SearchResult? {
            val feed = feedUrl ?: return null
            return SearchResult(
                feedUrl = feed,
                title = name.orEmpty(),
                author = artist.orEmpty(),
                imageUrl = artworkLarge ?: artwork,
                trackCount = trackCount,
                genre = genre,
            )
        }
    }

}

/** Explore tab categories, matching the original app's browse rows. */
enum class ExploreCategory(val label: String, val genreId: Int?) {
    TOP("Top shows", null),
    NEWS("News", 1489),
    COMEDY("Comedy", 1303),
    TRUE_CRIME("True crime", 1488),
    SPORTS("Sports", 1545),
    SOCIETY("Society & culture", 1324),
    BUSINESS("Business", 1321),
    TECHNOLOGY("Technology", 1318),
    HEALTH("Health & fitness", 1512),
    RELIGION("Religion & spirituality", 1314),
    HISTORY("History", 1487),
    EDUCATION("Education", 1304),
}
