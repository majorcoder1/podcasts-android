package com.podcasts.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.podcasts.app.domain.Episode
import com.podcasts.app.domain.Podcast
import com.podcasts.app.ui.components.EpisodeRow
import com.podcasts.app.ui.components.ShowArt
import com.podcasts.app.ui.player.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    playerViewModel: PlayerViewModel,
    onShowClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onSearchClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val nowPlaying by playerViewModel.nowPlaying.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Podcasts", style = MaterialTheme.typography.titleLarge) },
            actions = {
                IconButton(onClick = onSearchClick) {
                    Icon(Icons.Outlined.Search, contentDescription = "Search")
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Outlined.AccountCircle, contentDescription = "Settings")
                }
            },
        )

        if (state.isEmpty) {
            EmptyHome(onSearchClick)
            return@Column
        }

        LazyColumn(Modifier.fillMaxSize()) {
            if (state.subscriptions.isNotEmpty()) {
                item { SubscriptionsRow(state.subscriptions, onShowClick) }
            }

            if (state.queue.isNotEmpty()) {
                item { SectionHeader("Your queue", "${state.queue.size} episodes") }
                items(state.queue.take(3), key = { "queue-${it.guid}" }) { episode ->
                    HomeEpisode(episode, playerViewModel, nowPlaying.episode?.guid, onShowClick)
                }
            }

            if (state.continueListening.isNotEmpty()) {
                item { SectionHeader("Continue listening") }
                items(state.continueListening, key = { "progress-${it.guid}" }) { episode ->
                    HomeEpisode(episode, playerViewModel, nowPlaying.episode?.guid, onShowClick)
                }
            }

            if (state.newEpisodes.isNotEmpty()) {
                item { SectionHeader("New episodes") }
                items(state.newEpisodes, key = { "new-${it.guid}" }) { episode ->
                    HomeEpisode(episode, playerViewModel, nowPlaying.episode?.guid, onShowClick)
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun HomeEpisode(
    episode: Episode,
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
            onAddToQueue = { playerViewModel.addToQueue(episode) },
            onMore = { playerViewModel.markPlayed(episode) },
            showArtwork = true,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun SubscriptionsRow(podcasts: List<Podcast>, onShowClick: (String) -> Unit) {
    Column(Modifier.padding(top = 8.dp)) {
        SectionHeader("Your subscriptions")
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(podcasts, key = { it.feedUrl }) { podcast ->
                Column(
                    modifier = Modifier
                        .width(104.dp)
                        .clickable { onShowClick(podcast.feedUrl) },
                ) {
                    ShowArt(podcast.imageUrl, podcast.title, size = 104.dp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = podcast.title,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SectionHeader(title: String, trailing: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        trailing?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyHome(onSearchClick: () -> Unit) {
    Surface(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Subscribe to a show to see new episodes here",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Explore",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onSearchClick).padding(8.dp),
            )
        }
    }
}
