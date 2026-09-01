package com.podcasts.app.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.podcasts.app.data.SettingsStore
import com.podcasts.app.data.repository.PodcastRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/** Periodic feed refresh. Mirrors the original app's background update. */
@HiltWorker
class RefreshWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: PodcastRepository,
    private val settings: SettingsStore,
    private val notifier: NewEpisodeNotifier,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = runCatching {
        val newEpisodes = repository.refreshAllSubscriptions()
        if (settings.settings.first().notifyNewEpisodes && newEpisodes.isNotEmpty()) {
            notifier.notify(newEpisodes)
        }
        Result.success()
    }.getOrElse { if (runAttemptCount < 3) Result.retry() else Result.failure() }

    companion object {
        const val NAME = "refresh-subscriptions"
    }
}
