package com.podcasts.app.ui.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.podcasts.app.domain.Episode
import com.podcasts.app.ui.components.EpisodeRow
import com.podcasts.app.ui.components.formatBytes
import com.podcasts.app.ui.player.PlayerViewModel

private val TABS = listOf("Queue", "Downloads", "History")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    playerViewModel: PlayerViewModel,
    onShowClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: ActivityViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val nowPlaying by playerViewModel.nowPlaying.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            windowInsets = WindowInsets(0, 0, 0, 0),
            title = { Text("Activity") },
            actions = {
                if (selectedTab == 1 && state.downloads.isNotEmpty()) {
                    IconButton(onClick = viewModel::removeAllDownloads) {
                        Icon(Icons.Outlined.DeleteSweep, contentDescription = "Remove all downloads")
                    }
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Outlined.AccountCircle, contentDescription = "Settings")
                }
            },
        )

        TabRow(selectedTabIndex = selectedTab) {
            TABS.forEachIndexed { index, label ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(label) },
                )
            }
        }

        val episodes = when (selectedTab) {
            0 -> state.queue
            1 -> state.downloads
            else -> state.history
        }

        if (episodes.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = when (selectedTab) {
                        0 -> "Episodes you add to your queue show up here"
                        1 -> "Downloaded episodes show up here"
                        else -> "Episodes you finish show up here"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Column
        }

        if (selectedTab == 1 && state.downloadBytes > 0) {
            Text(
                text = "${formatBytes(state.downloadBytes)} used",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        LazyColumn(Modifier.fillMaxSize()) {
            items(episodes, key = { it.guid }) { episode ->
                ActivityEpisode(
                    episode = episode,
                    inQueue = selectedTab == 0,
                    playerViewModel = playerViewModel,
                    playingGuid = nowPlaying.episode?.guid,
                    onShowClick = onShowClick,
                )
            }
        }
    }
}

@Composable
private fun ActivityEpisode(
    episode: Episode,
    inQueue: Boolean,
    playerViewModel: PlayerViewModel,
    playingGuid: String?,
    onShowClick: (String) -> Unit,
) {
    Column {
        EpisodeRow(
            episode = episode,
            isPlaying = playingGuid == episode.guid && playerViewModel.isPlaying(episode),
            onClick = { onShowClick(episode.feedUrl) },
            onPlayPause = { playerViewModel.toggle(episode) },
            onDownload = { playerViewModel.toggleDownload(episode) },
            onAddToQueue = {
                if (inQueue) playerViewModel.removeFromQueue(episode.guid)
                else playerViewModel.addToQueue(episode)
            },
            onMore = { playerViewModel.markPlayed(episode) },
            showArtwork = true,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}
