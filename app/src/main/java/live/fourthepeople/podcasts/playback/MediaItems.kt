package live.fourthepeople.podcasts.playback

import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import live.fourthepeople.podcasts.domain.DownloadState
import live.fourthepeople.podcasts.domain.Episode

/** Downloaded episodes play from disk; everything else streams. */
fun Episode.playbackUri(): String =
    if (downloadState == DownloadState.DOWNLOADED && localPath != null) localPath else audioUrl

fun Episode.toMediaItem(
    showTitle: String? = null,
    showArtworkUrl: String? = null,
): MediaItem = MediaItem.Builder()
    .setMediaId(guid)
    .setUri(playbackUri())
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(showTitle ?: "")
            .setAlbumTitle(showTitle ?: "")
            .setArtworkUri((showArtworkUrl ?: imageUrl)?.toUri())
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .setMediaType(MediaMetadata.MEDIA_TYPE_PODCAST_EPISODE)
            .build(),
    )
    .build()
