package live.fourthepeople.podcasts.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import live.fourthepeople.podcasts.data.repository.DownloadRepository
import live.fourthepeople.podcasts.data.repository.EpisodeRepository
import live.fourthepeople.podcasts.data.repository.PodcastRepository
import live.fourthepeople.podcasts.domain.DownloadState
import live.fourthepeople.podcasts.domain.Episode
import live.fourthepeople.podcasts.domain.NowPlaying
import live.fourthepeople.podcasts.domain.Podcast
import live.fourthepeople.podcasts.playback.PlayerConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Shared across every screen — one instance is scoped to the Activity so tapping
 * play on a list row and opening the full player address the same session.
 */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerConnection: PlayerConnection,
    private val episodeRepository: EpisodeRepository,
    private val podcastRepository: PodcastRepository,
    private val downloadRepository: DownloadRepository,
) : ViewModel() {

    val nowPlaying: StateFlow<NowPlaying> = playerConnection.state

    val queue: StateFlow<List<Episode>> = episodeRepository.queue()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * The show behind whatever is playing.
     *
     * Publishers often attach tiny per-episode artwork - one feed ships 316px
     * images against a 3000px cover - and the full-screen player blows those up
     * until they are visibly soft. The show's own cover is almost always the
     * high-resolution one, so the player prefers it.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentShow: StateFlow<Podcast?> = nowPlaying
        .map { it.episode?.feedUrl }
        .distinctUntilChanged()
        .flatMapLatest { feedUrl ->
            if (feedUrl == null) flowOf(null) else podcastRepository.podcast(feedUrl)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch { playerConnection.connect() }
    }

    fun play(episode: Episode) {
        viewModelScope.launch {
            val show = podcastRepository.podcast(episode.feedUrl).first()
            playerConnection.play(episode, show?.title, show?.imageUrl)
        }
    }

    fun toggle(episode: Episode) {
        val state = nowPlaying.value
        if (state.episode?.guid == episode.guid) playerConnection.playPause() else play(episode)
    }

    fun isPlaying(episode: Episode): Boolean {
        val state = nowPlaying.value
        return state.isPlaying && state.episode?.guid == episode.guid
    }

    /** Launcher "Resume" shortcut: pick up the last episode and play it. */
    fun resumeLastPlayed() {
        viewModelScope.launch {
            val episode = nowPlaying.value.episode ?: episodeRepository.lastPlayed() ?: return@launch
            play(episode)
        }
    }

    fun playPause() = playerConnection.playPause()
    fun seekTo(positionMs: Long) = playerConnection.seekTo(positionMs)
    fun skipForward() = playerConnection.skipForward()
    fun skipBack() = playerConnection.skipBack()
    fun setSpeed(speed: Float) = playerConnection.setSpeed(speed)
    fun setSkipSilence(enabled: Boolean) = playerConnection.setSkipSilence(enabled)
    fun startSleepTimer(minutes: Int?) = playerConnection.startSleepTimer(minutes)
    fun cancelSleepTimer() = playerConnection.cancelSleepTimer()

    fun addToQueue(episode: Episode, playNext: Boolean = false) {
        viewModelScope.launch { episodeRepository.addToQueue(episode.guid, playNext) }
    }

    fun removeFromQueue(guid: String) {
        viewModelScope.launch { episodeRepository.removeFromQueue(guid) }
    }

    fun reorderQueue(guids: List<String>) {
        viewModelScope.launch { episodeRepository.reorderQueue(guids) }
    }

    fun toggleDownload(episode: Episode) {
        viewModelScope.launch {
            if (episode.downloadState == DownloadState.DOWNLOADED) {
                downloadRepository.remove(episode.guid)
            } else {
                downloadRepository.download(episode.guid)
            }
        }
    }

    fun markPlayed(episode: Episode) {
        viewModelScope.launch {
            if (episode.effectivelyFinished) {
                episodeRepository.markUnplayed(episode.guid)
            } else {
                episodeRepository.markCompleted(episode.guid)
            }
        }
    }

    fun archive(episode: Episode) {
        viewModelScope.launch { episodeRepository.archive(episode.guid) }
    }

    override fun onCleared() {
        playerConnection.release()
        super.onCleared()
    }
}
