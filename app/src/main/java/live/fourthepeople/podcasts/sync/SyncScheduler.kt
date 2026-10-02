package live.fourthepeople.podcasts.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncScheduler @Inject constructor(private val context: Context) {

    fun schedule(refreshIntervalHours: Int, wifiOnly: Boolean) {
        val manager = WorkManager.getInstance(context)

        val refresh = PeriodicWorkRequestBuilder<RefreshWorker>(
            refreshIntervalHours.coerceAtLeast(1).toLong(),
            TimeUnit.HOURS,
        ).setConstraints(
            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
        ).build()

        val download = PeriodicWorkRequestBuilder<AutoDownloadWorker>(6, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(
                        if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED,
                    )
                    .setRequiresStorageNotLow(true)
                    .build(),
            )
            .build()

        manager.enqueueUniquePeriodicWork(
            RefreshWorker.NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            refresh,
        )
        manager.enqueueUniquePeriodicWork(
            AutoDownloadWorker.NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            download,
        )
    }
}
