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
@Singleton
class FeedService @Inject constructor(
    private val client: OkHttpClient,
    private val parser: FeedParser,
    private val context: Context,
) {

    /** Returns null when the server answers 304 Not Modified. */
    suspend fun fetch(feedUrl: String, useCache: Boolean = true): ParsedFeed? =
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
                        storeValidator(
                            feedUrl,
                            response.header("ETag"),
                            response.header("Last-Modified"),
                        )
                        val body = response.body ?: throw FeedException("Empty body for $feedUrl")
                        parser.parse(feedUrl, body.byteStream())
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

    private suspend fun storeValidator(feedUrl: String, etag: String?, lastModified: String?) {
        context.settingsDataStore.edit { prefs ->
            etag?.let { prefs[stringPreferencesKey(etagKey(feedUrl))] = it }
            lastModified?.let { prefs[stringPreferencesKey(modifiedKey(feedUrl))] = it }
        }
    }

    private fun etagKey(feedUrl: String) = "etag_" + feedUrl.hashCode()
    private fun modifiedKey(feedUrl: String) = "modified_" + feedUrl.hashCode()

    companion object {
        const val USER_AGENT = "PodcastsClone/1.0 (+https://github.com/)"
    }
}
