package com.podcasts.app.ui.show

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.podcasts.app.ui.components.EpisodeRow
import com.podcasts.app.ui.components.ShowArt
import com.podcasts.app.ui.player.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowScreen(
    playerViewModel: PlayerViewModel,
    onBack: () -> Unit,
    viewModel: ShowViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val nowPlaying by playerViewModel.nowPlaying.collectAsStateWithLifecycle()
    val newestFirst by viewModel.sortNewestFirst.collectAsStateWithLifecycle()
    var menuOpen by remember { mutableStateOf(false) }
    var descriptionExpanded by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            windowInsets = WindowInsets(0, 0, 0, 0),
            title = {},
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "More")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    val podcast = state.podcast
                    DropdownMenuItem(
                        text = { Text(if (podcast?.autoDownload == true) "Turn off auto download" else "Auto download new episodes") },
                        onClick = {
                            viewModel.setAutoDownload(podcast?.autoDownload != true)
                            menuOpen = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(if (podcast?.newEpisodeNotifications == true) "Turn off notifications" else "Notify about new episodes") },
                        onClick = {
                            viewModel.setNotifications(podcast?.newEpisodeNotifications != true)
                            menuOpen = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Refresh") },
                        onClick = { viewModel.forceRefresh(); menuOpen = false },
                    )
                }
            },
        )

        if (state.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        val podcast = state.podcast
        if (podcast == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.error ?: "Couldn't load this show.")
            }
            return@Column
        }

        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Row {
                        ShowArt(podcast.imageUrl, podcast.title, size = 120.dp)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = podcast.title,
                                style = MaterialTheme.typography.headlineSmall,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = podcast.author,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    if (podcast.isSubscribed) {
                        OutlinedButton(onClick = viewModel::toggleSubscription) {
                            Icon(Icons.Outlined.Check, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Subscribed")
                        }
                    } else {
                        Button(
                            onClick = viewModel::toggleSubscription,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                            ),
                        ) {
                            Text("Subscribe")
                        }
                    }

                    if (podcast.description.isNotBlank()) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = podcast.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = if (descriptionExpanded) Int.MAX_VALUE else 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable { descriptionExpanded = !descriptionExpanded },
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Episodes", style = MaterialTheme.typography.titleMedium)
                        Row(
                            modifier = Modifier.clickable { viewModel.toggleSort() }.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.Sort, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (newestFirst) "Newest first" else "Oldest first",
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            items(state.episodes, key = { it.guid }) { episode ->
                Column {
                    EpisodeRow(
                        episode = episode,
                        isPlaying = nowPlaying.episode?.guid == episode.guid &&
                            playerViewModel.isPlaying(episode),
                        onClick = { playerViewModel.toggle(episode) },
                        onPlayPause = { playerViewModel.toggle(episode) },
                        onDownload = { playerViewModel.toggleDownload(episode) },
                        onAddToQueue = { playerViewModel.addToQueue(episode) },
                        onMore = { playerViewModel.markPlayed(episode) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
