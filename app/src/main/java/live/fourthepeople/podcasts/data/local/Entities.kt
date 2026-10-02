package live.fourthepeople.podcasts.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "podcasts")
data class PodcastEntity(
    @PrimaryKey val feedUrl: String,
    val title: String,
    val author: String,
    val description: String,
    val imageUrl: String?,
    val link: String?,
    val categories: String,
    val isSubscribed: Boolean,
    val subscribedAt: Long,
    val lastRefreshed: Long,
    val newEpisodeNotifications: Boolean,
    val autoDownload: Boolean,
    val playbackSpeedOverride: Float?,
)

@Entity(
    tableName = "episodes",
    primaryKeys = ["guid"],
    foreignKeys = [
        ForeignKey(
            entity = PodcastEntity::class,
            parentColumns = ["feedUrl"],
            childColumns = ["feedUrl"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("feedUrl"), Index("publishedAt"), Index("downloadState")],
)
data class EpisodeEntity(
    val guid: String,
    val feedUrl: String,
    val title: String,
    val description: String,
    val audioUrl: String,
    val imageUrl: String?,
    val publishedAt: Long,
    val durationMs: Long,
    val positionMs: Long,
    val isCompleted: Boolean,
    val isArchived: Boolean,
    val downloadState: String,
    val localPath: String?,
    val fileSizeBytes: Long,
    val seasonNumber: Int?,
    val episodeNumber: Int?,
    val lastPlayedAt: Long,
    val addedAt: Long,
)

@Entity(tableName = "queue")
data class QueueEntity(
    @PrimaryKey val guid: String,
    val position: Int,
    val addedAt: Long,
)
