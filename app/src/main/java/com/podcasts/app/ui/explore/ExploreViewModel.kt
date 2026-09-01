package com.podcasts.app.ui.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.podcasts.app.data.remote.ExploreCategory
import com.podcasts.app.data.repository.PodcastRepository
import com.podcasts.app.domain.SearchResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class ExploreUiState(
    val query: String = "",
    val category: ExploreCategory = ExploreCategory.TOP,
    val results: List<SearchResult> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
)

@OptIn(FlowPreview::class)
@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val repository: PodcastRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ExploreUiState())
    val state: StateFlow<ExploreUiState> = _state.asStateFlow()

    private val queryFlow = MutableStateFlow("")
    private var loadJob: Job? = null

    init {
        // Search-as-you-type, throttled the way the original's suggestions were.
        queryFlow
            .debounce(300)
            .distinctUntilChanged()
            .onEach { term -> if (term.isBlank()) loadCategory(_state.value.category) else search(term) }
            .launchIn(viewModelScope)
    }

    fun onQueryChange(query: String) {
        _state.value = _state.value.copy(query = query)
        queryFlow.value = query.trim()
    }

    fun clearQuery() = onQueryChange("")

    fun selectCategory(category: ExploreCategory) {
        _state.value = _state.value.copy(category = category)
        if (_state.value.query.isBlank()) loadCategory(category)
    }

    private fun search(term: String) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching { repository.search(term) }
                .onSuccess { _state.value = _state.value.copy(results = it, loading = false) }
                .onFailure {
                    _state.value = _state.value.copy(
                        loading = false,
                        error = "Couldn't search. Check your connection.",
                    )
                }
        }
    }

    private fun loadCategory(category: ExploreCategory) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching { repository.topShows(category.genreId) }
                .onSuccess { _state.value = _state.value.copy(results = it, loading = false) }
                .onFailure {
                    _state.value = _state.value.copy(
                        loading = false,
                        error = "Couldn't load shows. Check your connection.",
                    )
                }
        }
    }
}
