package live.fourthepeople.podcasts.data.repository

import live.fourthepeople.podcasts.data.local.EpisodeDao
import live.fourthepeople.podcasts.data.local.QueueDao
import live.fourthepeople.podcasts.data.local.QueueEntity
import live.fourthepeople.podcasts.data.local.toDomain
import live.fourthepeople.podcasts.data.local.toDomainEpisodes
import live.fourthepeople.podcasts.domain.Episode
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class EpisodeRepository @Inject constructor(
    private val episodeDao: EpisodeDao,
    private val queueDao: QueueDao,
) {

    fun episode(guid: String): Flow<Episode?> = episodeDao.observe(guid).map { it?.toDomain() }

    suspend fun get(guid: String): Episode? = episodeDao.get(guid)?.toDomain()

    /** The episode to put back in the player after the process was killed. */
    suspend fun lastPlayed(): Episode? = episodeDao.mostRecentlyPlayed()?.toDomain()

    fun queue(): Flow<List<Episode>> = queueDao.observeQueue().map { it.toDomainEpisodes() }

    fun isQueued(guid: String): Flow<Boolean> = queueDao.observeIsQueued(guid)

    suspend fun savePosition(guid: String, positionMs: Long) {
        episodeDao.savePosition(guid, positionMs, System.currentTimeMillis())
    }

    suspend fun markCompleted(guid: String) {
        val episode = episodeDao.get(guid) ?: return
        episodeDao.setCompleted(guid, true, episode.durationMs, System.currentTimeMillis())
        queueDao.remove(guid)
    }

    suspend fun markUnplayed(guid: String) {
        episodeDao.setCompleted(guid, false, 0L, System.currentTimeMillis())
    }

    suspend fun archive(guid: String) {
        episodeDao.archive(guid)
        queueDao.remove(guid)
    }

    /** "Add to queue" appends; "Play next" jumps the line. */
    suspend fun addToQueue(guid: String, playNext: Boolean = false) {
        val position = if (playNext) queueDao.frontPosition() else queueDao.nextPosition()
        queueDao.insert(QueueEntity(guid, position, System.currentTimeMillis()))
    }

    suspend fun removeFromQueue(guid: String) = queueDao.remove(guid)

    suspend fun reorderQueue(guids: List<String>) = queueDao.reorder(guids)

    suspend fun clearQueue() = queueDao.clear()

    /** The episode the player should advance to when the current one ends. */
    suspend fun nextInQueue(afterGuid: String?): Episode? {
        val rows = queueDao.rows()
        if (rows.isEmpty()) return null
        val index = rows.indexOfFirst { it.guid == afterGuid }
        val nextGuid = if (index >= 0) rows.getOrNull(index + 1)?.guid else rows.first().guid
        return nextGuid?.let { episodeDao.get(it)?.toDomain() }
    }
}
