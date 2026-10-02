package live.fourthepeople.podcasts.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import live.fourthepeople.podcasts.data.AppSettings
import live.fourthepeople.podcasts.data.SettingsStore
import live.fourthepeople.podcasts.data.ThemeMode
import live.fourthepeople.podcasts.data.repository.DownloadRepository
import live.fourthepeople.podcasts.data.repository.PodcastRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: SettingsStore,
    private val podcastRepository: PodcastRepository,
    private val downloadRepository: DownloadRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = store.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun setSpeed(value: Float) = update { store.setSpeed(value) }
    fun setSkipForward(seconds: Int) = update { store.setSkipForward(seconds) }
    fun setSkipBack(seconds: Int) = update { store.setSkipBack(seconds) }
    fun setSkipSilence(enabled: Boolean) = update { store.setSkipSilence(enabled) }
    fun setAutoPlayNext(enabled: Boolean) = update { store.setAutoPlayNext(enabled) }
    fun setWifiOnly(enabled: Boolean) = update { store.setWifiOnly(enabled) }
    fun setAutoDownload(enabled: Boolean) = update { store.setAutoDownload(enabled) }
    fun setAutoDownloadLimit(count: Int) = update { store.setAutoDownloadLimit(count) }
    fun setRemoveWhenPlayed(enabled: Boolean) = update { store.setRemoveWhenPlayed(enabled) }
    fun setRemoveAfterDays(days: Int) = update { store.setRemoveAfterDays(days) }
    fun setRefreshHours(hours: Int) = update { store.setRefreshHours(hours) }
    fun setNotify(enabled: Boolean) = update { store.setNotify(enabled) }
    fun setTheme(mode: ThemeMode) = update { store.setTheme(mode) }

    fun removeAllDownloads() = update {
        downloadRepository.removeAll()
        _message.value = "Downloads removed"
    }

    fun importOpml(uri: Uri) {
        viewModelScope.launch {
            val count = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use {
                        podcastRepository.importOpml(it)
                    } ?: 0
                }.getOrDefault(0)
            }
            _message.value = if (count > 0) "Imported $count shows" else "Nothing to import"
        }
    }

    fun exportOpml(uri: Uri) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val opml = podcastRepository.exportOpml()
                    context.contentResolver.openOutputStream(uri)?.use { it.write(opml.toByteArray()) }
                    true
                }.getOrDefault(false)
            }
            _message.value = if (ok) "Subscriptions exported" else "Export failed"
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    private fun update(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
