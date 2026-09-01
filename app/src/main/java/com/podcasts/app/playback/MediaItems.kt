package com.podcasts.app.playback

import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.podcasts.app.domain.DownloadState
import com.podcasts.app.domain.Episode

/** Downloaded episodes play from disk; everything else streams. */
fun Episode.playbackUri(): String =
    if (downloadState == DownloadState.DOWNLOADED && localPath != null) localPath else audioUrl

fun Episode.toMediaItem(showTitle: String? = null): MediaItem = MediaItem.Builder()
    .setMediaId(guid)
    .setUri(playbackUri())
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(showTitle ?: "")
            .setAlbumTitle(showTitle ?: "")
            .setArtworkUri(imageUrl?.toUri())
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .setMediaType(MediaMetadata.MEDIA_TYPE_PODCAST_EPISODE)
            .build(),
    )
    .build()
