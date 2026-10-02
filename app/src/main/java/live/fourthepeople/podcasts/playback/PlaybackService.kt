package live.fourthepeople.podcasts.playback

import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import live.fourthepeople.podcasts.data.SettingsStore
import live.fourthepeople.podcasts.data.repository.EpisodeRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground playback service. Holds the one ExoPlayer instance; the UI talks to
 * it through a MediaController, so playback survives the Activity.
 */
@OptIn(UnstableApi::class)
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject lateinit var episodeRepository: EpisodeRepository
    @Inject lateinit var settingsStore: SettingsStore

    // ExoPlayer must only be touched on its application thread, which is main here.
    // Repository calls inside this scope are suspend functions that dispatch off it.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var session: MediaSession? = null
    private var progressJob: Job? = null

    override fun onCreate() {
        super.onCreate()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setSeekForwardIncrementMs(30_000)
            .setSeekBackIncrementMs(10_000)
            .build()

        scope.launch {
            val config = settingsStore.settings.first()
            player.setPlaybackSpeed(config.defaultSpeed)
            player.skipSilenceEnabled = config.skipSilence
        }

        player.addListener(PlaybackListener(player))

        session = MediaSession.Builder(this, player)
            .setCallback(PlaybackSessionCallback())
            .build()

        startProgressPersistence(player)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = session?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        progressJob?.cancel()
        session?.run {
            persistPosition(player)
            player.release()
            release()
        }
        session = null
        scope.cancel()
        super.onDestroy()
    }

    /** Google Podcasts resumed to the second; a 5s write cadence matches that closely. */
    private fun startProgressPersistence(player: Player) {
        progressJob = scope.launch {
            while (isActive) {
                delay(POSITION_SAVE_INTERVAL_MS)
                persistPosition(player)
            }
        }
    }

    private fun persistPosition(player: Player) {
        val guid = player.currentMediaItem?.mediaId ?: return
        val position = player.currentPosition
        if (position <= 0) return
        scope.launch { episodeRepository.savePosition(guid, position) }
    }

    private inner class PlaybackListener(private val player: Player) : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                // The finished item is the previous one in the timeline.
                scope.launch {
                    val previous = player.getPreviousMediaItemId()
                    previous?.let { episodeRepository.markCompleted(it) }
                }
            }
        }

        override fun onPlaybackStateChanged(state: Int) {
            if (state == Player.STATE_ENDED) {
                val guid = player.currentMediaItem?.mediaId ?: return
                scope.launch {
                    episodeRepository.markCompleted(guid)
                    if (settingsStore.settings.first().autoPlayNext) {
                        episodeRepository.nextInQueue(guid)?.let { next ->
                            player.setMediaItem(next.toMediaItem())
                            player.prepare()
                            player.play()
                        }
                    }
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (!isPlaying) persistPosition(player)
        }
    }

    private fun Player.getPreviousMediaItemId(): String? {
        val index = currentMediaItemIndex - 1
        return if (index >= 0) getMediaItemAt(index).mediaId else null
    }

    companion object {
        private const val POSITION_SAVE_INTERVAL_MS = 5_000L
    }
}
