package com.podcasts.app.data.remote

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.podcasts.app.data.settingsDataStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Fetches and parses a feed. Uses ETag / Last-Modified so a refresh of 50 shows
 * costs almost nothing when nothing has changed.
 */
/** A fetched feed plus the validators to remember once it is stored. */
data class FeedResponse(
    val parsed: ParsedFeed,
    val etag: String?,
    val lastModified: String?,
)

@Singleton
class FeedService @Inject constructor(
    private val client: OkHttpClient,
    private val parser: FeedParser,
    private val context: Context,
) {

    /**
     * Returns null when the server answers 304 Not Modified.
     *
     * The caching validators are deliberately NOT saved here. They are only
     * safe to remember once the episodes are actually in the database - see
     * [commitValidators].
     */
    suspend fun fetch(feedUrl: String, useCache: Boolean = true): FeedResponse? =
        withContext(Dispatchers.IO) {
            val builder = Request.Builder().url(feedUrl).header("User-Agent", USER_AGENT)
            if (useCache) {
                validatorFor(feedUrl)?.let { (etag, lastModified) ->
                    etag?.let { builder.header("If-None-Match", it) }
                    lastModified?.let { builder.header("If-Modified-Since", it) }
                }
            }

            client.newCall(builder.build()).execute().use { response ->
                when {
                    response.code == 304 -> null
                    !response.isSuccessful ->
                        throw FeedException("HTTP ${response.code} for $feedUrl")
                    else -> {
                        val body = response.body ?: throw FeedException("Empty body for $feedUrl")
                        FeedResponse(
                            parsed = parser.parse(feedUrl, body.byteStream()),
                            etag = response.header("ETag"),
                            lastModified = response.header("Last-Modified"),
                        )
                    }
                }
            }
        }

    private suspend fun validatorFor(feedUrl: String): Pair<String?, String?>? {
        val prefs = context.settingsDataStore.data.first()
        val etag = prefs[stringPreferencesKey(etagKey(feedUrl))]
        val modified = prefs[stringPreferencesKey(modifiedKey(feedUrl))]
        return if (etag == null && modified == null) null else etag to modified
    }

    /**
     * Remember the validators for this feed. Call only after the episodes have
     * been stored.
     *
     * Saving them any earlier is a trap: if the parse throws, or the process is
     * killed part-way through a large feed, the ETag is on disk while the
     * episodes are not. Every later refresh then sends it, gets a correct 304,
     * and the show is frozen at its old episode list forever while every other
     * podcast app keeps updating.
     */
    suspend fun commitValidators(feedUrl: String, etag: String?, lastModified: String?) {
        context.settingsDataStore.edit { prefs ->
            etag?.let { prefs[stringPreferencesKey(etagKey(feedUrl))] = it }
            lastModified?.let { prefs[stringPreferencesKey(modifiedKey(feedUrl))] = it }
        }
    }

    // v2: keys were bumped when the commit ordering above was fixed, so any
    // validator saved by the buggy build is ignored exactly once.
    private fun etagKey(feedUrl: String) = "etag_v2_" + feedUrl.hashCode()
    private fun modifiedKey(feedUrl: String) = "modified_v2_" + feedUrl.hashCode()

    companion object {
        const val USER_AGENT = "PodcastsClone/1.0 (+https://github.com/)"
    }
}
