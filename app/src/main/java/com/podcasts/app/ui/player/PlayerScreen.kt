package com.podcasts.app.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Forward30
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.Replay10
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.podcasts.app.ui.components.ShowArt
import com.podcasts.app.ui.components.formatClock
import com.podcasts.app.ui.components.formatSpeed

/**
 * Full-screen now playing. Layout follows the original: collapse chevron, large
 * square art, title, show name, scrubber, then a control row of speed / back 10 /
 * play / forward 30 / sleep timer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    playerViewModel: PlayerViewModel,
    onCollapse: () -> Unit,
    onShowClick: (String) -> Unit,
) {
    val nowPlaying by playerViewModel.nowPlaying.collectAsStateWithLifecycle()
    val episode = nowPlaying.episode

    var speedDialogOpen by remember { mutableStateOf(false) }
    var sleepSheetOpen by remember { mutableStateOf(false) }
    var scrubPosition by remember { mutableFloatStateOf(-1f) }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onCollapse) {
                        Icon(Icons.Outlined.ExpandMore, contentDescription = "Collapse")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Cast route picker */ }) {
                        Icon(Icons.Outlined.Cast, contentDescription = "Cast")
                    }
                    IconButton(onClick = { /* Overflow */ }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "More")
                    }
                },
            )

            if (episode == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nothing is playing", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                return@Column
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(16.dp))
                ShowArt(
                    imageUrl = episode.imageUrl,
                    contentDescription = episode.title,
                    modifier = Modifier.fillMaxWidth(0.85f),
                    cornerRadius = 12.dp,
                )

                Spacer(Modifier.height(32.dp))

                Text(
                    text = episode.title,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = { onShowClick(episode.feedUrl) }) {
                    Text(
                        text = "Go to show",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Spacer(Modifier.height(16.dp))

                val position = if (scrubPosition >= 0) {
                    (scrubPosition * nowPlaying.durationMs).toLong()
                } else {
                    nowPlaying.positionMs
                }

                Slider(
                    value = if (nowPlaying.durationMs > 0) {
                        position.toFloat() / nowPlaying.durationMs
                    } else {
                        0f
                    },
                    onValueChange = { scrubPosition = it },
                    onValueChangeFinished = {
                        playerViewModel.seekTo((scrubPosition * nowPlaying.durationMs).toLong())
                        scrubPosition = -1f
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(formatClock(position), style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = formatClock(nowPlaying.durationMs),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = { speedDialogOpen = true }) {
                        Text(
                            text = formatSpeed(nowPlaying.speed),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }

                    IconButton(onClick = playerViewModel::skipBack) {
                        Icon(
                            Icons.Outlined.Replay10,
                            contentDescription = "Back 10 seconds",
                            modifier = Modifier.size(32.dp),
                        )
                    }

                    Surface(
                        modifier = Modifier.size(72.dp).clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                    ) {
                        IconButton(onClick = playerViewModel::playPause) {
                            Icon(
                                imageVector = if (nowPlaying.isPlaying) {
                                    Icons.Outlined.Pause
                                } else {
                                    Icons.Filled.PlayArrow
                                },
                                contentDescription = if (nowPlaying.isPlaying) "Pause" else "Play",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(36.dp),
                            )
                        }
                    }

                    IconButton(onClick = playerViewModel::skipForward) {
                        Icon(
                            Icons.Outlined.Forward30,
                            contentDescription = "Forward 30 seconds",
                            modifier = Modifier.size(32.dp),
                        )
                    }

                    IconButton(onClick = { sleepSheetOpen = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Bedtime,
                            contentDescription = "Sleep timer",
                            tint = if (nowPlaying.sleepTimerEndsAt != null ||
                                nowPlaying.sleepAtEndOfEpisode
                            ) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.AutoMirrored.Outlined.QueueMusic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(8.dp))
                    val queue by playerViewModel.queue.collectAsStateWithLifecycle()
                    Text(
                        text = if (queue.isEmpty()) "Queue is empty" else "Up next: ${queue.first().title}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }

    if (speedDialogOpen) {
        SpeedDialog(
            current = nowPlaying.speed,
            onSelect = { playerViewModel.setSpeed(it); speedDialogOpen = false },
            onDismiss = { speedDialogOpen = false },
        )
    }

    if (sleepSheetOpen) {
        SleepTimerSheet(
            activeUntil = nowPlaying.sleepTimerEndsAt,
            endOfEpisode = nowPlaying.sleepAtEndOfEpisode,
            onSelect = { playerViewModel.startSleepTimer(it); sleepSheetOpen = false },
            onCancel = { playerViewModel.cancelSleepTimer(); sleepSheetOpen = false },
            onDismiss = { sleepSheetOpen = false },
        )
    }
}
