package live.fourthepeople.podcasts.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import live.fourthepeople.podcasts.data.repository.EpisodeRepository
import live.fourthepeople.podcasts.data.repository.PodcastRepository
import live.fourthepeople.podcasts.domain.Episode
import live.fourthepeople.podcasts.domain.Podcast
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val subscriptions: List<Podcast> = emptyList(),
    val continueListening: List<Episode> = emptyList(),
    val newEpisodes: List<Episode> = emptyList(),
    val queue: List<Episode> = emptyList(),
) {
    val isEmpty: Boolean
        get() = subscriptions.isEmpty() && newEpisodes.isEmpty() && continueListening.isEmpty()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val podcastRepository: PodcastRepository,
    episodeRepository: EpisodeRepository,
) : ViewModel() {

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    val uiState: StateFlow<HomeUiState> = combine(
        podcastRepository.subscriptions(),
        podcastRepository.inProgress(),
        podcastRepository.newEpisodes(),
        episodeRepository.queue(),
    ) { subscriptions, inProgress, newEpisodes, queue ->
        HomeUiState(subscriptions, inProgress, newEpisodes, queue)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun refresh() {
        if (_refreshing.value) return
        viewModelScope.launch {
            _refreshing.value = true
            runCatching { podcastRepository.refreshAllSubscriptions() }
            _refreshing.value = false
        }
    }
}
