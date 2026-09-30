package com.podcasts.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.AddToQueue
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DownloadDone
import androidx.compose.material.icons.outlined.Downloading
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Podcasts
import androidx.compose.material.icons.outlined.RemoveDone
import androidx.compose.material.icons.outlined.RemoveFromQueue
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.podcasts.app.domain.DownloadState
import com.podcasts.app.domain.Episode

/**
 * Everything you can do to an episode, in one sheet.
 *
 * The row used to carry these as separate icons, and "mark played" wore a
 * three-dot icon that promised a menu and never opened one - tapping it made
 * the episode vanish from Home, because Home only lists unplayed episodes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeMenuSheet(
    episode: Episode,
    showTitle: String?,
    inQueue: Boolean,
    onPlayNext: () -> Unit,
    onToggleQueue: () -> Unit,
    onToggleDownload: () -> Unit,
    onTogglePlayed: () -> Unit,
    onArchive: () -> Unit,
    onDismiss: () -> Unit,
    // Null on the show screen, which is already the place this would go.
    onGoToShow: (() -> Unit)? = null,
) {
    val downloaded = episode.downloadState == DownloadState.DOWNLOADED
    val played = episode.effectivelyFinished

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(bottom = 16.dp)) {
            Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 12.dp)) {
                Text(
                    text = episode.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!showTitle.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = showTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            MenuItem(Icons.AutoMirrored.Outlined.PlaylistPlay, "Play next") {
                onPlayNext()
                onDismiss()
            }

            if (inQueue) {
                MenuItem(Icons.Outlined.RemoveFromQueue, "Remove from queue") {
                    onToggleQueue()
                    onDismiss()
                }
            } else {
                MenuItem(Icons.Outlined.AddToQueue, "Add to queue") {
                    onToggleQueue()
                    onDismiss()
                }
            }

            val downloadIcon = when (episode.downloadState) {
                DownloadState.DOWNLOADED -> Icons.Outlined.DownloadDone
                DownloadState.DOWNLOADING, DownloadState.QUEUED -> Icons.Outlined.Downloading
                else -> Icons.Outlined.FileDownload
            }
            MenuItem(downloadIcon, if (downloaded) "Remove download" else "Download") {
                onToggleDownload()
                onDismiss()
            }

            MenuItem(
                icon = if (played) Icons.Outlined.RemoveDone else Icons.Outlined.CheckCircle,
                label = if (played) "Mark as unplayed" else "Mark as played",
            ) {
                onTogglePlayed()
                onDismiss()
            }

            if (onGoToShow != null) {
                MenuItem(Icons.Outlined.Podcasts, "Go to show") {
                    onGoToShow()
                    onDismiss()
                }
            }

            MenuItem(Icons.Outlined.VisibilityOff, "Hide this episode", danger = true) {
                onArchive()
                onDismiss()
            }
        }
    }
}

@Composable
private fun MenuItem(
    icon: ImageVector,
    label: String,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    val tint = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Spacer(Modifier.width(20.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = tint)
    }
}
