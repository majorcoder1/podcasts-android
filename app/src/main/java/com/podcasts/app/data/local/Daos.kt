package com.podcasts.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PodcastDao {
    @Query("SELECT * FROM podcasts WHERE isSubscribed = 1 ORDER BY subscribedAt DESC")
    fun observeSubscriptions(): Flow<List<PodcastEntity>>

    @Query("SELECT * FROM podcasts WHERE feedUrl = :feedUrl")
    fun observe(feedUrl: String): Flow<PodcastEntity?>

    @Query("SELECT * FROM podcasts WHERE feedUrl = :feedUrl")
    suspend fun get(feedUrl: String): PodcastEntity?

    @Query("SELECT * FROM podcasts WHERE isSubscribed = 1")
    suspend fun subscriptions(): List<PodcastEntity>

    /**
     * Must be @Upsert, not @Insert(REPLACE): REPLACE deletes the conflicting row
     * first, and the episodes foreign key cascades — every refresh would wipe the
     * show's episodes and their playback state.
     */
    @Upsert
    suspend fun upsert(podcast: PodcastEntity)

    @Query("UPDATE podcasts SET isSubscribed = :subscribed, subscribedAt = :now WHERE feedUrl = :feedUrl")
    suspend fun setSubscribed(feedUrl: String, subscribed: Boolean, now: Long)

    @Query("UPDATE podcasts SET autoDownload = :enabled WHERE feedUrl = :feedUrl")
    suspend fun setAutoDownload(feedUrl: String, enabled: Boolean)

    @Query("UPDATE podcasts SET newEpisodeNotifications = :enabled WHERE feedUrl = :feedUrl")
    suspend fun setNotifications(feedUrl: String, enabled: Boolean)

    @Query("UPDATE podcasts SET playbackSpeedOverride = :speed WHERE feedUrl = :feedUrl")
    suspend fun setSpeedOverride(feedUrl: String, speed: Float?)

    @Query("UPDATE podcasts SET lastRefreshed = :now WHERE feedUrl = :feedUrl")
    suspend fun markRefreshed(feedUrl: String, now: Long)

    @Query("DELETE FROM podcasts WHERE isSubscribed = 0 AND feedUrl NOT IN (SELECT feedUrl FROM episodes WHERE positionMs > 0 OR downloadState = 'DOWNLOADED')")
    suspend fun pruneUnsubscribed()
}

@Dao
interface EpisodeDao {
    @Query("SELECT * FROM episodes WHERE feedUrl = :feedUrl AND isArchived = 0 ORDER BY publishedAt DESC")
    fun observeForPodcast(feedUrl: String): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE guid = :guid")
    fun observe(guid: String): Flow<EpisodeEntity?>

    @Query("SELECT * FROM episodes WHERE guid = :guid")
    suspend fun get(guid: String): EpisodeEntity?

    /** "New episodes" row: unplayed, from subscribed shows, last 14 days. */
    @Query(
        """
        SELECT e.* FROM episodes e
        JOIN podcasts p ON p.feedUrl = e.feedUrl
        WHERE p.isSubscribed = 1
          AND e.isCompleted = 0
          AND e.positionMs = 0
          AND e.isArchived = 0
          AND e.publishedAt >= :since
        ORDER BY e.publishedAt DESC
        LIMIT :limit
        """,
    )
    fun observeNewEpisodes(since: Long, limit: Int): Flow<List<EpisodeEntity>>

    /** "Continue listening": started but not finished, most recently played first. */
    @Query(
        """
        SELECT * FROM episodes
        WHERE positionMs > 0 AND isCompleted = 0 AND isArchived = 0
        ORDER BY lastPlayedAt DESC
        LIMIT :limit
        """,
    )
    fun observeInProgress(limit: Int): Flow<List<EpisodeEntity>>

    /**
     * What was playing when the app was last alive. Android reclaims the
     * process a while after playback pauses, so without this the player comes
     * back empty and you have to go and find your episode again.
     */
    @Query(
        """
        SELECT * FROM episodes
        WHERE positionMs > 0 AND isCompleted = 0 AND isArchived = 0
        ORDER BY lastPlayedAt DESC
        LIMIT 1
        """,
    )
    suspend fun mostRecentlyPlayed(): EpisodeEntity?

    @Query("SELECT * FROM episodes WHERE downloadState = 'DOWNLOADED' ORDER BY publishedAt DESC")
    fun observeDownloads(): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE isCompleted = 1 ORDER BY lastPlayedAt DESC LIMIT :limit")
    fun observeHistory(limit: Int): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE downloadState = 'DOWNLOADED'")
    suspend fun downloaded(): List<EpisodeEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(episodes: List<EpisodeEntity>): List<Long>

    @Update
    suspend fun update(episode: EpisodeEntity)

    /** Feed refresh must not clobber local playback/download state. */
    @Query(
        """
        UPDATE episodes SET title = :title, description = :description, audioUrl = :audioUrl,
            imageUrl = :imageUrl, publishedAt = :publishedAt, durationMs = :durationMs
        WHERE guid = :guid
        """,
    )
    suspend fun updateMetadata(
        guid: String,
        title: String,
        description: String,
        audioUrl: String,
        imageUrl: String?,
        publishedAt: Long,
        durationMs: Long,
    )

    @Query("UPDATE episodes SET positionMs = :positionMs, lastPlayedAt = :now WHERE guid = :guid")
    suspend fun savePosition(guid: String, positionMs: Long, now: Long)

    @Query("UPDATE episodes SET isCompleted = :completed, positionMs = :positionMs, lastPlayedAt = :now WHERE guid = :guid")
    suspend fun setCompleted(guid: String, completed: Boolean, positionMs: Long, now: Long)

    @Query("UPDATE episodes SET downloadState = :state, localPath = :path, fileSizeBytes = :size WHERE guid = :guid")
    suspend fun setDownloadState(guid: String, state: String, path: String?, size: Long)

    @Query("UPDATE episodes SET isArchived = 1 WHERE guid = :guid")
    suspend fun archive(guid: String)

    @Query("SELECT COALESCE(SUM(fileSizeBytes), 0) FROM episodes WHERE downloadState = 'DOWNLOADED'")
    fun observeDownloadBytes(): Flow<Long>

    /** Auto-download candidates for a show, newest first. */
    @Query(
        """
        SELECT e.* FROM episodes e
        JOIN podcasts p ON p.feedUrl = e.feedUrl
        WHERE p.autoDownload = 1 AND p.isSubscribed = 1
          AND e.downloadState = 'NOT_DOWNLOADED'
          AND e.isCompleted = 0 AND e.isArchived = 0
        ORDER BY e.publishedAt DESC
        LIMIT :limit
        """,
    )
    suspend fun autoDownloadCandidates(limit: Int): List<EpisodeEntity>

    /** Downloads eligible for the "remove after played / after N days" rules. */
    @Query(
        """
        SELECT * FROM episodes
        WHERE downloadState = 'DOWNLOADED'
          AND ((:removeWhenPlayed = 1 AND isCompleted = 1) OR (:expiryCutoff > 0 AND publishedAt < :expiryCutoff))
        """,
    )
    suspend fun expiredDownloads(removeWhenPlayed: Boolean, expiryCutoff: Long): List<EpisodeEntity>
}

@Dao
interface QueueDao {
    @Transaction
    @Query(
        """
        SELECT e.* FROM episodes e
        JOIN queue q ON q.guid = e.guid
        ORDER BY q.position ASC
        """,
    )
    fun observeQueue(): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM queue ORDER BY position ASC")
    suspend fun rows(): List<QueueEntity>

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM queue")
    suspend fun nextPosition(): Int

    @Query("SELECT COALESCE(MIN(position), 0) - 1 FROM queue")
    suspend fun frontPosition(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: QueueEntity)

    @Query("DELETE FROM queue WHERE guid = :guid")
    suspend fun remove(guid: String)

    @Query("DELETE FROM queue")
    suspend fun clear()

    @Query("SELECT EXISTS(SELECT 1 FROM queue WHERE guid = :guid)")
    fun observeIsQueued(guid: String): Flow<Boolean>

    @Transaction
    suspend fun reorder(guids: List<String>) {
        val now = System.currentTimeMillis()
        clear()
        guids.forEachIndexed { index, guid -> insert(QueueEntity(guid, index, now)) }
    }
}
