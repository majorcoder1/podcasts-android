package com.podcasts.app.data.local

import com.podcasts.app.domain.DownloadState
import com.podcasts.app.domain.Episode
import com.podcasts.app.domain.Podcast

private const val CATEGORY_DELIMITER = "|"

fun PodcastEntity.toDomain() = Podcast(
    feedUrl = feedUrl,
    title = title,
    author = author,
    description = description,
    imageUrl = imageUrl,
    link = link,
    categories = if (categories.isBlank()) emptyList() else categories.split(CATEGORY_DELIMITER),
    isSubscribed = isSubscribed,
    lastRefreshed = lastRefreshed,
    newEpisodeNotifications = newEpisodeNotifications,
    autoDownload = autoDownload,
    playbackSpeedOverride = playbackSpeedOverride,
)

fun Podcast.toEntity(subscribedAt: Long = System.currentTimeMillis()) = PodcastEntity(
    feedUrl = feedUrl,
    title = title,
    author = author,
    description = description,
    imageUrl = imageUrl,
    link = link,
    categories = categories.joinToString(CATEGORY_DELIMITER),
    isSubscribed = isSubscribed,
    subscribedAt = subscribedAt,
    lastRefreshed = lastRefreshed,
    newEpisodeNotifications = newEpisodeNotifications,
    autoDownload = autoDownload,
    playbackSpeedOverride = playbackSpeedOverride,
)

fun EpisodeEntity.toDomain() = Episode(
    guid = guid,
    feedUrl = feedUrl,
    title = title,
    description = description,
    audioUrl = audioUrl,
    imageUrl = imageUrl,
    publishedAt = publishedAt,
    durationMs = durationMs,
    positionMs = positionMs,
    isCompleted = isCompleted,
    isArchived = isArchived,
    downloadState = runCatching { DownloadState.valueOf(downloadState) }
        .getOrDefault(DownloadState.NOT_DOWNLOADED),
    localPath = localPath,
    fileSizeBytes = fileSizeBytes,
    seasonNumber = seasonNumber,
    episodeNumber = episodeNumber,
)

fun Episode.toEntity(now: Long = System.currentTimeMillis()) = EpisodeEntity(
    guid = guid,
    feedUrl = feedUrl,
    title = title,
    description = description,
    audioUrl = audioUrl,
    imageUrl = imageUrl,
    publishedAt = publishedAt,
    durationMs = durationMs,
    positionMs = positionMs,
    isCompleted = isCompleted,
    isArchived = isArchived,
    downloadState = downloadState.name,
    localPath = localPath,
    fileSizeBytes = fileSizeBytes,
    seasonNumber = seasonNumber,
    episodeNumber = episodeNumber,
    lastPlayedAt = 0L,
    addedAt = now,
)

fun List<EpisodeEntity>.toDomainEpisodes() = map { it.toDomain() }
fun List<PodcastEntity>.toDomainPodcasts() = map { it.toDomain() }
