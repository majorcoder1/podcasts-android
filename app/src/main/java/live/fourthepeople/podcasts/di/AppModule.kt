package live.fourthepeople.podcasts.di

import android.content.Context
import androidx.room.Room
import live.fourthepeople.podcasts.data.local.EpisodeDao
import live.fourthepeople.podcasts.data.local.PodcastDao
import live.fourthepeople.podcasts.data.local.PodcastDatabase
import live.fourthepeople.podcasts.data.local.QueueDao
import live.fourthepeople.podcasts.data.remote.FeedParser
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun context(@ApplicationContext context: Context): Context = context

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): PodcastDatabase =
        Room.databaseBuilder(context, PodcastDatabase::class.java, PodcastDatabase.NAME)
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()

    @Provides fun podcastDao(db: PodcastDatabase): PodcastDao = db.podcastDao()

    @Provides fun episodeDao(db: PodcastDatabase): EpisodeDao = db.episodeDao()

    @Provides fun queueDao(db: PodcastDatabase): QueueDao = db.queueDao()

    @Provides
    @Singleton
    fun okHttp(@ApplicationContext context: Context): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .cache(okhttp3.Cache(java.io.File(context.cacheDir, "http"), 20L * 1024 * 1024))
        .build()

    @Provides
    @Singleton
    fun json(): Json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Provides
    @Singleton
    fun feedParser(): FeedParser = FeedParser()
}
