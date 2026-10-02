package live.fourthepeople.podcasts.ui.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import live.fourthepeople.podcasts.data.repository.DownloadRepository
import live.fourthepeople.podcasts.data.repository.EpisodeRepository
import live.fourthepeople.podcasts.data.repository.PodcastRepository
import live.fourthepeople.podcasts.domain.Episode
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
