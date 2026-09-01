package com.podcasts.app.domain

/** A show. `feedUrl` is the identity — there is no proprietary catalog id. */
data class Podcast(
    val feedUrl: String,
    val title: String,
    val author: String,
    val description: String,
    val imageUrl: String?,
    val link: String?,
    val categories: List<String> = emptyList(),
    val isSubscribed: Boolean = false,
    val lastRefreshed: Long = 0L,
    val newEpisodeNotifications: Boolean = true,
    val autoDownload: Boolean = false,
    val playbackSpeedOverride: Float? = null,
)

data class Episode(
    val guid: String,
    val feedUrl: String,
    val title: String,
    val description: String,
    val audioUrl: String,
    val imageUrl: String?,
    val publishedAt: Long,
    val durationMs: Long,
    val positionMs: Long = 0L,
    val isCompleted: Boolean = false,
    val isArchived: Boolean = false,
    val downloadState: DownloadState = DownloadState.NOT_DOWNLOADED,
    val localPath: String? = null,
    val fileSizeBytes: Long = 0L,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
) {
    /** Google Podcasts treated an episode as finished at 95% or with <30s to go. */
    val effectivelyFinished: Boolean
        get() = isCompleted ||
            (durationMs > 0 && (positionMs >= durationMs - 30_000 || positionMs.toDouble() / durationMs >= 0.95))

    val remainingMs: Long get() = (durationMs - positionMs).coerceAtLeast(0)

    val hasStarted: Boolean get() = positionMs > 0 && !effectivelyFinished

    val progress: Float
        get() = if (durationMs <= 0) 0f else (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
}

enum class DownloadState { NOT_DOWNLOADED, QUEUED, DOWNLOADING, DOWNLOADED, FAILED }

data class QueueItem(val episode: Episode, val position: Int)

/** Rows on the Home tab, in the order the original app stacked them. */
sealed interface HomeSection {
    data class ContinueListening(val episodes: List<Episode>) : HomeSection
    data class NewEpisodes(val episodes: List<Episode>) : HomeSection
    data class Queue(val episodes: List<Episode>) : HomeSection
    data class YourSubscriptions(val podcasts: List<Podcast>) : HomeSection
}

data class SearchResult(
    val feedUrl: String,
    val title: String,
    val author: String,
    val imageUrl: String?,
    val trackCount: Int = 0,
    val genre: String? = null,
)
