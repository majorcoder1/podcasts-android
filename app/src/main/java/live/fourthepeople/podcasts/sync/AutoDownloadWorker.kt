package live.fourthepeople.podcasts.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import live.fourthepeople.podcasts.data.repository.DownloadRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Downloads what auto-download has queued, then applies the retention rules
 * ("remove when played", "remove after N days"). Constrained to unmetered
 * networks by [SyncScheduler] when "download over Wi-Fi only" is on.
 */
@HiltWorker
class AutoDownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val downloads: DownloadRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = runCatching {
        downloads.queueAutoDownloads().forEach { guid -> downloads.download(guid) }
        downloads.applyRetentionRules()
        Result.success()
    }.getOrElse { if (runAttemptCount < 2) Result.retry() else Result.failure() }

    companion object {
        const val NAME = "auto-download"
    }
}
