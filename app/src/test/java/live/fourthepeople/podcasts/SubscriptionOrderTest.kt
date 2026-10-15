package live.fourthepeople.podcasts

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import live.fourthepeople.podcasts.data.local.EpisodeEntity
import live.fourthepeople.podcasts.data.local.PodcastDatabase
import live.fourthepeople.podcasts.data.local.PodcastEntity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Subscriptions are ordered by each show's newest episode, so whichever show
 * dropped most recently is first - not whichever was subscribed most recently.
 */
@RunWith(RobolectricTestRunner::class)
class SubscriptionOrderTest {

    private lateinit var db: PodcastDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            PodcastDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    private fun show(feedUrl: String, subscribedAt: Long) = PodcastEntity(
        feedUrl = feedUrl, title = feedUrl, author = "", description = "",
        imageUrl = null, link = null, categories = "", isSubscribed = true,
        subscribedAt = subscribedAt, lastRefreshed = 0,
        newEpisodeNotifications = true, autoDownload = false, playbackSpeedOverride = null,
    )

    private fun episode(feedUrl: String, publishedAt: Long) = EpisodeEntity(
        guid = "$feedUrl-$publishedAt", feedUrl = feedUrl, title = "", description = "",
        audioUrl = "https://example.com/a.mp3", imageUrl = null, publishedAt = publishedAt,
        durationMs = 0, positionMs = 0, isCompleted = false, isArchived = false,
        downloadState = "NONE", localPath = null, fileSizeBytes = 0,
        seasonNumber = null, episodeNumber = null, lastPlayedAt = 0, addedAt = 0,
    )

    @Test
    fun newestDropComesFirst() = runTest {
        val podcasts = db.podcastDao()
        val episodes = db.episodeDao()

        // Subscribed in the opposite order to their latest episodes.
        podcasts.upsert(show("x22", subscribedAt = 1))
        podcasts.upsert(show("pbd", subscribedAt = 2))
        podcasts.upsert(show("arc", subscribedAt = 3))
        podcasts.upsert(show("kingdom", subscribedAt = 4))
        podcasts.upsert(show("empty", subscribedAt = 5))

        episodes.insertAll(
            listOf(
                episode("x22", 400), episode("x22", 100),
                episode("pbd", 300),
                episode("arc", 200),
                episode("kingdom", 150),
            ),
        )
        assertEquals(
            listOf("x22", "pbd", "arc", "kingdom", "empty"),
            podcasts.observeSubscriptions().first().map { it.feedUrl },
        )

        // A new Kingdom episode moves it to the front.
        episodes.insertAll(listOf(episode("kingdom", 500)))
        assertEquals(
            listOf("kingdom", "x22", "pbd", "arc", "empty"),
            podcasts.observeSubscriptions().first().map { it.feedUrl },
        )
    }
}
