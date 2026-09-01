package com.podcasts.app.ui.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.podcasts.app.data.repository.DownloadRepository
import com.podcasts.app.data.repository.EpisodeRepository
import com.podcasts.app.data.repository.PodcastRepository
import com.podcasts.app.domain.Episode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ActivityUiState(
    val queue: List<Episode> = emptyList(),
    val downloads: List<Episode> = emptyList(),
    val history: List<Episode> = emptyList(),
    val downloadBytes: Long = 0L,
)

/** Backs the Activity tab: Queue / Downloads / History, as in the original. */
@HiltViewModel
class ActivityViewModel @Inject constructor(
    episodeRepository: EpisodeRepository,
    podcastRepository: PodcastRepository,
    private val downloadRepository: DownloadRepository,
) : ViewModel() {

    val uiState: StateFlow<ActivityUiState> = combine(
        episodeRepository.queue(),
        podcastRepository.downloads(),
        podcastRepository.history(),
        podcastRepository.downloadBytes(),
    ) { queue, downloads, history, bytes ->
        ActivityUiState(queue, downloads, history, bytes)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActivityUiState())

    fun removeAllDownloads() {
        viewModelScope.launch { downloadRepository.removeAll() }
    }
}
