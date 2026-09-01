package com.podcasts.app.data.repository

import com.podcasts.app.data.local.EpisodeDao
import com.podcasts.app.data.local.PodcastDao
import com.podcasts.app.data.local.toDomain
import com.podcasts.app.data.local.toDomainEpisodes
import com.podcasts.app.data.local.toDomainPodcasts
import com.podcasts.app.data.local.toEntity
import com.podcasts.app.data.remote.FeedException
import com.podcasts.app.data.remote.FeedService
import com.podcasts.app.data.remote.Opml
import com.podcasts.app.data.remote.SearchApi
import com.podcasts.app.domain.Episode
import com.podcasts.app.domain.Podcast
import com.podcasts.app.domain.SearchResult
import java.io.InputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class PodcastRepository @Inject constructor(
    private val podcastDao: PodcastDao,
    private val episodeDao: EpisodeDao,
    private val feedService: FeedService,
    private val searchApi: SearchApi,
) {

    fun subscriptions(): Flow<List<Podcast>> =
        podcastDao.observeSubscriptions().map { it.toDomainPodcasts() }

    fun podcast(feedUrl: String): Flow<Podcast?> =
        podcastDao.observe(feedUrl).map { it?.toDomain() }

    fun episodes(feedUrl: String): Flow<List<Episode>> =
        episodeDao.observeForPodcast(feedUrl).map { it.toDomainEpisodes() }

    fun newEpisodes(): Flow<List<Episode>> =
        episodeDao.observeNewEpisodes(System.currentTimeMillis() - NEW_WINDOW_MS, NEW_EPISODE_LIMIT)
            .map { it.toDomainEpisodes() }

    fun inProgress(): Flow<List<Episode>> =
        episodeDao.observeInProgress(IN_PROGRESS_LIMIT).map { it.toDomainEpisodes() }

    fun downloads(): Flow<List<Episode>> =
        episodeDao.observeDownloads().map { it.toDomainEpisodes() }

    fun history(): Flow<List<Episode>> =
        episodeDao.observeHistory(HISTORY_LIMIT).map { it.toDomainEpisodes() }

    fun downloadBytes(): Flow<Long> = episodeDao.observeDownloadBytes()

    suspend fun search(term: String): List<SearchResult> = searchApi.search(term)

    suspend fun topShows(genreId: Int?): List<SearchResult> = searchApi.top(genreId)

    /**
     * Loads a show the user tapped in Explore. Stores it unsubscribed so the show
     * page renders instantly, exactly like the original's preview behaviour.
     */
    suspend fun loadPreview(feedUrl: String): Podcast {
        val cached = podcastDao.get(feedUrl)
        if (cached != null && System.currentTimeMillis() - cached.lastRefreshed < STALE_MS) {
            return cached.toDomain()
        }
        return refresh(feedUrl) ?: cached?.toDomain()
            ?: error("Could not load $feedUrl")
    }

    suspend fun subscribe(feedUrl: String) {
        if (podcastDao.get(feedUrl) == null) {
            // A 304 would leave nothing to subscribe to, so ignore the validators.
            refresh(feedUrl, useCache = false) ?: throw FeedException("Could not load $feedUrl")
        }
        podcastDao.setSubscribed(feedUrl, true, System.currentTimeMillis())
    }

    suspend fun unsubscribe(feedUrl: String) {
        podcastDao.setSubscribed(feedUrl, false, System.currentTimeMillis())
    }

    suspend fun setAutoDownload(feedUrl: String, enabled: Boolean) =
        podcastDao.setAutoDownload(feedUrl, enabled)

    suspend fun setNotifications(feedUrl: String, enabled: Boolean) =
        podcastDao.setNotifications(feedUrl, enabled)

    suspend fun setSpeedOverride(feedUrl: String, speed: Float?) =
        podcastDao.setSpeedOverride(feedUrl, speed)

    /**
     * Pulls the feed and merges. Returns the podcast, or null if the server said
     * 304 Not Modified. New episodes are returned via [refreshAllSubscriptions].
     */
    suspend fun refresh(feedUrl: String, useCache: Boolean = true): Podcast? {
        val parsed = feedService.fetch(feedUrl, useCache) ?: return null
        val existing = podcastDao.get(feedUrl)
        val merged = parsed.podcast.copy(
            isSubscribed = existing?.isSubscribed ?: false,
            autoDownload = existing?.autoDownload ?: false,
            newEpisodeNotifications = existing?.newEpisodeNotifications ?: true,
            playbackSpeedOverride = existing?.playbackSpeedOverride,
            lastRefreshed = System.currentTimeMillis(),
        )
        podcastDao.upsert(merged.toEntity(subscribedAt = existing?.subscribedAt ?: 0L))
        mergeEpisodes(parsed.episodes)
        return merged
    }

    /** @return episodes that are new since the last refresh, for notifications. */
    suspend fun refreshAllSubscriptions(): List<Episode> = coroutineScope {
        podcastDao.subscriptions().map { podcast ->
            async {
                runCatching {
                    val parsed = feedService.fetch(podcast.feedUrl)
                        ?: return@runCatching emptyList<Episode>()
                    val fresh = mergeEpisodes(parsed.episodes)
                    podcastDao.markRefreshed(podcast.feedUrl, System.currentTimeMillis())
                    fresh
                }.getOrDefault(emptyList())
            }
        }.awaitAll().flatten()
    }

    /** Inserts unseen episodes and refreshes metadata on ones already stored. */
    private suspend fun mergeEpisodes(episodes: List<Episode>): List<Episode> {
        if (episodes.isEmpty()) return emptyList()
        val entities = episodes.map { it.toEntity() }
        val rowIds = episodeDao.insertAll(entities)
        val inserted = mutableListOf<Episode>()
        rowIds.forEachIndexed { index, rowId ->
            val episode = episodes[index]
            if (rowId == -1L) {
                episodeDao.updateMetadata(
                    guid = episode.guid,
                    title = episode.title,
                    description = episode.description,
                    audioUrl = episode.audioUrl,
                    imageUrl = episode.imageUrl,
                    publishedAt = episode.publishedAt,
                    durationMs = episode.durationMs,
                )
            } else {
                inserted += episode
            }
        }
        return inserted
    }

    suspend fun importOpml(input: InputStream): Int {
        val entries = Opml.parse(input)
        var imported = 0
        entries.forEach { entry ->
            runCatching { subscribe(entry.feedUrl) }.onSuccess { imported++ }
        }
        return imported
    }

    suspend fun exportOpml(): String = Opml.write(podcastDao.subscriptions().toDomainPodcasts())

    companion object {
        private const val NEW_EPISODE_LIMIT = 30
        private const val IN_PROGRESS_LIMIT = 20
        private const val HISTORY_LIMIT = 100
        private val NEW_WINDOW_MS = TimeUnit.DAYS.toMillis(14)
        private val STALE_MS = TimeUnit.HOURS.toMillis(1)
    }
}
