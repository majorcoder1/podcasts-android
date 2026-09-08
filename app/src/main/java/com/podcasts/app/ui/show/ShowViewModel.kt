package com.podcasts.app.ui.show

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.podcasts.app.data.repository.PodcastRepository
import com.podcasts.app.domain.Episode
import com.podcasts.app.domain.Podcast
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ShowUiState(
    val podcast: Podcast? = null,
    val episodes: List<Episode> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class ShowViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: PodcastRepository,
) : ViewModel() {

    private val feedUrl: String =
        Uri.decode(checkNotNull(savedStateHandle["feedUrl"]) { "Missing feedUrl" })

    private val loading = MutableStateFlow(true)
    private val error = MutableStateFlow<String?>(null)

    private val newestFirst = MutableStateFlow(true)
    val sortNewestFirst: StateFlow<Boolean> = newestFirst.asStateFlow()

    val uiState: StateFlow<ShowUiState> = combine(
        repository.podcast(feedUrl),
        repository.episodes(feedUrl),
        loading,
        error,
        newestFirst,
    ) { podcast, episodes, isLoading, message, newest ->
        ShowUiState(
            podcast = podcast,
            episodes = if (newest) episodes else episodes.sortedBy { it.publishedAt },
            loading = isLoading && podcast == null,
            error = message,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShowUiState())

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            loading.value = true
            error.value = null
            runCatching { repository.loadPreview(feedUrl) }
                .onFailure { error.value = "Couldn't load this show." }
            loading.value = false
        }
    }

    /** Menu > Refresh: ignore the cached validators and re-read the whole feed. */
    fun forceRefresh() {
        viewModelScope.launch {
            loading.value = true
            error.value = null
            runCatching { repository.refresh(feedUrl, useCache = false) }
                .onFailure { error.value = "Couldn't refresh this show." }
            loading.value = false
        }
    }

    fun toggleSubscription() {
        val current = uiState.value.podcast ?: return
        viewModelScope.launch {
            if (current.isSubscribed) repository.unsubscribe(feedUrl)
            else repository.subscribe(feedUrl)
        }
    }

    fun toggleSort() {
        newestFirst.value = !newestFirst.value
    }

    fun setAutoDownload(enabled: Boolean) {
        viewModelScope.launch { repository.setAutoDownload(feedUrl, enabled) }
    }

    fun setNotifications(enabled: Boolean) {
        viewModelScope.launch { repository.setNotifications(feedUrl, enabled) }
    }
}
