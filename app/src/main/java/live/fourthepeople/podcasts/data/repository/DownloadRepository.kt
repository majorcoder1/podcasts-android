package live.fourthepeople.podcasts.data.repository

import android.content.Context
import live.fourthepeople.podcasts.data.SettingsStore
import live.fourthepeople.podcasts.data.local.EpisodeDao
import live.fourthepeople.podcasts.data.local.toDomain
import live.fourthepeople.podcasts.data.remote.FeedService
import live.fourthepeople.podcasts.domain.DownloadState
import live.fourthepeople.podcasts.domain.Episode
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink

/**
 * Plain OkHttp downloads into app-private storage. Files live under
 * `filesDir/episodes/<feed hash>/<guid hash>.mp3` so a show can be purged whole.
 */
@Singleton
class DownloadRepository @Inject constructor(
    private val context: Context,
    private val client: OkHttpClient,
    private val episodeDao: EpisodeDao,
    private val settings: SettingsStore,
) {

    private val root: File get() = File(context.filesDir, "episodes")

    suspend fun download(guid: String): Result<File> = withContext(Dispatchers.IO) {
        val episode = episodeDao.get(guid)?.toDomain()
            ?: return@withContext Result.failure(IllegalStateException("Unknown episode $guid"))

        setState(guid, DownloadState.DOWNLOADING, null, 0L)
        val target = fileFor(episode)
        target.parentFile?.mkdirs()

        runCatching {
            val request = Request.Builder()
                .url(episode.audioUrl)
                .header("User-Agent", FeedService.USER_AGENT)
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("HTTP ${response.code}")
                val body = response.body
                target.sink().buffer().use { sink -> sink.writeAll(body.source()) }
            }
            target
        }.onSuccess { file ->
            setState(guid, DownloadState.DOWNLOADED, file.absolutePath, file.length())
        }.onFailure {
            target.delete()
            setState(guid, DownloadState.FAILED, null, 0L)
        }
    }

    suspend fun remove(guid: String) = withContext(Dispatchers.IO) {
        val episode = episodeDao.get(guid)?.toDomain() ?: return@withContext
        episode.localPath?.let { File(it).delete() }
        setState(guid, DownloadState.NOT_DOWNLOADED, null, 0L)
    }

    suspend fun removeAll() = withContext(Dispatchers.IO) {
        episodeDao.downloaded().forEach { remove(it.guid) }
        root.deleteRecursively()
    }

    /** Applied by [live.fourthepeople.podcasts.sync.DownloadMaintenanceWorker]. */
    suspend fun applyRetentionRules() {
        val config = settings.settings.first()
        val cutoff = if (config.removeDownloadAfterDays > 0) {
            System.currentTimeMillis() - TimeUnit.DAYS.toMillis(config.removeDownloadAfterDays.toLong())
        } else {
            0L
        }
        episodeDao.expiredDownloads(config.removeDownloadWhenPlayed, cutoff)
            .forEach { remove(it.guid) }
    }

    suspend fun queueAutoDownloads(): List<String> {
        val config = settings.settings.first()
        if (!config.autoDownloadEnabled) return emptyList()
        val candidates = episodeDao.autoDownloadCandidates(config.autoDownloadLimitPerShow * 10)
        return candidates
            .groupBy { it.feedUrl }
            .flatMap { (_, items) -> items.take(config.autoDownloadLimitPerShow) }
            .map { it.guid }
            .onEach { setState(it, DownloadState.QUEUED, null, 0L) }
    }

    private fun fileFor(episode: Episode): File {
        val showDir = File(root, episode.feedUrl.hashCode().toString())
        val extension = episode.audioUrl.substringAfterLast('.', "mp3")
            .substringBefore('?')
            .take(4)
            .ifBlank { "mp3" }
        return File(showDir, "${episode.guid.hashCode()}.$extension")
    }

    private suspend fun setState(guid: String, state: DownloadState, path: String?, size: Long) {
        episodeDao.setDownloadState(guid, state.name, path, size)
    }
}
