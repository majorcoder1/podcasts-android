package live.fourthepeople.podcasts.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Mirrors the original Settings screen, option for option. */
data class AppSettings(
    val defaultSpeed: Float = 1.0f,
    val skipForwardSeconds: Int = 30,
    val skipBackSeconds: Int = 10,
    val skipSilence: Boolean = false,
    val autoPlayNext: Boolean = true,
    val downloadOnWifiOnly: Boolean = true,
    val autoDownloadEnabled: Boolean = false,
    val autoDownloadLimitPerShow: Int = 3,
    val removeDownloadWhenPlayed: Boolean = true,
    val removeDownloadAfterDays: Int = 0,
    val refreshIntervalHours: Int = 6,
    val notifyNewEpisodes: Boolean = true,
    val darkTheme: ThemeMode = ThemeMode.SYSTEM,
)

enum class ThemeMode { LIGHT, DARK, SYSTEM }

@Singleton
class SettingsStore @Inject constructor(private val context: Context) {

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            defaultSpeed = prefs[Keys.SPEED] ?: 1.0f,
            skipForwardSeconds = prefs[Keys.SKIP_FORWARD] ?: 30,
            skipBackSeconds = prefs[Keys.SKIP_BACK] ?: 10,
            skipSilence = prefs[Keys.SKIP_SILENCE] ?: false,
            autoPlayNext = prefs[Keys.AUTO_PLAY_NEXT] ?: true,
            downloadOnWifiOnly = prefs[Keys.WIFI_ONLY] ?: true,
            autoDownloadEnabled = prefs[Keys.AUTO_DOWNLOAD] ?: false,
            autoDownloadLimitPerShow = prefs[Keys.AUTO_DOWNLOAD_LIMIT] ?: 3,
            removeDownloadWhenPlayed = prefs[Keys.REMOVE_WHEN_PLAYED] ?: true,
            removeDownloadAfterDays = prefs[Keys.REMOVE_AFTER_DAYS] ?: 0,
            refreshIntervalHours = prefs[Keys.REFRESH_HOURS] ?: 6,
            notifyNewEpisodes = prefs[Keys.NOTIFY] ?: true,
            darkTheme = runCatching { ThemeMode.valueOf(prefs[Keys.THEME] ?: "") }
                .getOrDefault(ThemeMode.SYSTEM),
        )
    }

    suspend fun setSpeed(value: Float) = put { it[Keys.SPEED] = value }
    suspend fun setSkipForward(seconds: Int) = put { it[Keys.SKIP_FORWARD] = seconds }
    suspend fun setSkipBack(seconds: Int) = put { it[Keys.SKIP_BACK] = seconds }
    suspend fun setSkipSilence(enabled: Boolean) = put { it[Keys.SKIP_SILENCE] = enabled }
    suspend fun setAutoPlayNext(enabled: Boolean) = put { it[Keys.AUTO_PLAY_NEXT] = enabled }
    suspend fun setWifiOnly(enabled: Boolean) = put { it[Keys.WIFI_ONLY] = enabled }
    suspend fun setAutoDownload(enabled: Boolean) = put { it[Keys.AUTO_DOWNLOAD] = enabled }
    suspend fun setAutoDownloadLimit(count: Int) = put { it[Keys.AUTO_DOWNLOAD_LIMIT] = count }
    suspend fun setRemoveWhenPlayed(enabled: Boolean) = put { it[Keys.REMOVE_WHEN_PLAYED] = enabled }
    suspend fun setRemoveAfterDays(days: Int) = put { it[Keys.REMOVE_AFTER_DAYS] = days }
    suspend fun setRefreshHours(hours: Int) = put { it[Keys.REFRESH_HOURS] = hours }
    suspend fun setNotify(enabled: Boolean) = put { it[Keys.NOTIFY] = enabled }
    suspend fun setTheme(mode: ThemeMode) = put { it[Keys.THEME] = mode.name }

    private suspend fun put(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.settingsDataStore.edit(block)
    }

    private object Keys {
        val SPEED = floatPreferencesKey("default_speed")
        val SKIP_FORWARD = intPreferencesKey("skip_forward")
        val SKIP_BACK = intPreferencesKey("skip_back")
        val SKIP_SILENCE = booleanPreferencesKey("skip_silence")
        val AUTO_PLAY_NEXT = booleanPreferencesKey("auto_play_next")
        val WIFI_ONLY = booleanPreferencesKey("wifi_only")
        val AUTO_DOWNLOAD = booleanPreferencesKey("auto_download")
        val AUTO_DOWNLOAD_LIMIT = intPreferencesKey("auto_download_limit")
        val REMOVE_WHEN_PLAYED = booleanPreferencesKey("remove_when_played")
        val REMOVE_AFTER_DAYS = intPreferencesKey("remove_after_days")
        val REFRESH_HOURS = intPreferencesKey("refresh_hours")
        val NOTIFY = booleanPreferencesKey("notify_new_episodes")
        val THEME = androidx.datastore.preferences.core.stringPreferencesKey("theme")
    }
}
