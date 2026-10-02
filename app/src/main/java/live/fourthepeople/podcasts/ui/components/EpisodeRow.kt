package live.fourthepeople.podcasts.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.DownloadDone
import androidx.compose.material.icons.outlined.Downloading
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import live.fourthepeople.podcasts.domain.DownloadState
import live.fourthepeople.podcasts.domain.Episode

/**
 * The episode row from the original app: date, two-line title, then a footer of
 * play button + duration + download + menu.
 *
 * Queue and mark-played moved into the menu, so the three dots open one sheet
 * with everything in it rather than sitting beside two shortcuts.
 */
@Composable
fun EpisodeRow(
    episode: Episode,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onPlayPause: () -> Unit,
    onDownload: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier,
    showArtwork: Boolean = false,
    showTitle: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            if (showArtwork) {
                ShowArt(episode.imageUrl, showTitle, size = 56.dp)
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = formatEpisodeDate(episode.publishedAt).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (showTitle != null && showArtwork) {
                    Text(
                        text = showTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = episode.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (episode.description.isNotBlank() && !showArtwork) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = episode.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconButton(onClick = onPlayPause, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            Column(Modifier.weight(1f).padding(start = 4.dp)) {
                Text(
                    text = when {
                        episode.effectivelyFinished -> "Played"
                        episode.hasStarted -> formatRemaining(episode.remainingMs)
                        else -> formatDuration(episode.durationMs)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (episode.hasStarted) {
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { episode.progress },
                        modifier = Modifier.fillMaxWidth().height(2.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }

            IconButton(onClick = onDownload, modifier = Modifier.size(36.dp)) {
                val (icon, description) = when (episode.downloadState) {
                    DownloadState.DOWNLOADED -> Icons.Outlined.DownloadDone to "Remove download"
                    DownloadState.DOWNLOADING, DownloadState.QUEUED ->
                        Icons.Outlined.Downloading to "Downloading"
                    else -> Icons.Outlined.FileDownload to "Download"
                }
                Icon(
                    imageVector = icon,
                    contentDescription = description,
                    tint = if (episode.downloadState == DownloadState.DOWNLOADED) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }

            IconButton(onClick = onMenu, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = "More options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
