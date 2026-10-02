package live.fourthepeople.podcasts

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import live.fourthepeople.podcasts.data.SettingsStore
import live.fourthepeople.podcasts.sync.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn

@HiltAndroidApp
class PodcastsApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var settingsStore: SettingsStore
    @Inject lateinit var syncScheduler: SyncScheduler

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        // Re-schedule whenever the user changes refresh interval or Wi-Fi-only.
        settingsStore.settings
            .map { it.refreshIntervalHours to it.downloadOnWifiOnly }
            .distinctUntilChanged()
            .onEach { (hours, wifiOnly) -> syncScheduler.schedule(hours, wifiOnly) }
            .launchIn(scope)
    }
}
