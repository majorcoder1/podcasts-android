package com.podcasts.app.playback

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.podcasts.app.data.SettingsStore
import com.podcasts.app.data.repository.EpisodeRepository
import com.podcasts.app.domain.Episode
import com.podcasts.app.domain.NowPlaying
import com.podcasts.app.domain.PLAYBACK_SPEEDS
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * The single object the UI uses to drive playback. Wraps a MediaController and
 * publishes [NowPlaying] so the mini player and the full player render from one
 * source of truth.
 */
@Singleton
class PlayerConnection @Inject constructor(
    private val context: Context,
    private val episodeRepository: EpisodeRepository,
    private val settingsStore: SettingsStore,
) {

    // MediaController, like the player it drives, is single-threaded on main.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(NowPlaying())
    val state: StateFlow<NowPlaying> = _state.asStateFlow()

    private var controller: MediaController? = null
    private var tickJob: Job? = null
    private var sleepJob: Job? = null

    suspend fun connect() {
        if (controller != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val newController = MediaController.Builder(context, token).buildAsync().await()
        newController.addListener(ControllerListener())
        controller = newController
        startTicker()
        publish()
    }

    fun release() {
        tickJob?.cancel()
        sleepJob?.cancel()
        controller?.release()
        controller = null
    }

    /** Tapping an episode anywhere in the app lands here. */
    fun play(episode: Episode, showTitle: String? = null) {
        val player = controller ?: return
        if (player.currentMediaItem?.mediaId == episode.guid) {
            player.play()
            return
        }
        scope.launch {
            val config = settingsStore.settings.first()
            player.setMediaItem(episode.toMediaItem(showTitle))
            player.prepare()
            if (episode.hasStarted) player.seekTo(episode.positionMs)
            player.setPlaybackSpeed(config.defaultSpeed)
            player.play()
            publish(episode)
        }
    }

    fun playPause() {
        val player = controller ?: return
        if (player.isPlaying) player.pause() else player.play()
        publish()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
        publish()
    }

    fun skipForward() {
        val player = controller ?: return
        scope.launch {
            val seconds = settingsStore.settings.first().skipForwardSeconds
            player.seekTo((player.currentPosition + seconds * 1000).coerceAtMost(player.duration))
            publish()
        }
    }

    fun skipBack() {
        val player = controller ?: return
        scope.launch {
            val seconds = settingsStore.settings.first().skipBackSeconds
            player.seekTo((player.currentPosition - seconds * 1000).coerceAtLeast(0))
            publish()
        }
    }

    fun setSpeed(speed: Float) {
        val player = controller ?: return
        val clamped = speed.coerceIn(PLAYBACK_SPEEDS.first(), PLAYBACK_SPEEDS.last())
        player.setPlaybackSpeed(clamped)
        player.sendCustomCommand(
            SessionCommand(PlaybackSessionCallback.ACTION_SET_SPEED, Bundle.EMPTY),
            Bundle().apply { putFloat(PlaybackSessionCallback.EXTRA_SPEED, clamped) },
        )
        scope.launch { settingsStore.setSpeed(clamped) }
        publish()
    }

    fun setSkipSilence(enabled: Boolean) {
        controller?.sendCustomCommand(
            SessionCommand(PlaybackSessionCallback.ACTION_SET_SKIP_SILENCE, Bundle.EMPTY),
            Bundle().apply { putBoolean(PlaybackSessionCallback.EXTRA_SKIP_SILENCE, enabled) },
        )
        scope.launch { settingsStore.setSkipSilence(enabled) }
    }

    /** [minutes] null means "stop at the end of this episode". */
    fun startSleepTimer(minutes: Int?) {
        sleepJob?.cancel()
        if (minutes == null) {
            _state.value = _state.value.copy(sleepAtEndOfEpisode = true, sleepTimerEndsAt = null)
            return
        }
        val endsAt = System.currentTimeMillis() + minutes * 60_000L
        _state.value = _state.value.copy(sleepTimerEndsAt = endsAt, sleepAtEndOfEpisode = false)
        sleepJob = scope.launch {
            delay(minutes * 60_000L)
            controller?.pause()
            _state.value = _state.value.copy(sleepTimerEndsAt = null)
            publish()
        }
    }

    fun cancelSleepTimer() {
        sleepJob?.cancel()
        _state.value = _state.value.copy(sleepTimerEndsAt = null, sleepAtEndOfEpisode = false)
    }

    private fun startTicker() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (isActive) {
                if (controller?.isPlaying == true) publish()
                delay(500)
            }
        }
    }

    private fun publish(known: Episode? = null) {
        val player = controller ?: return
        scope.launch {
            val guid = player.currentMediaItem?.mediaId
            val episode = known ?: guid?.let { episodeRepository.get(it) }
            _state.value = _state.value.copy(
                episode = episode,
                isPlaying = player.isPlaying,
                isBuffering = player.playbackState == Player.STATE_BUFFERING,
                positionMs = player.currentPosition.coerceAtLeast(0),
                durationMs = player.duration.takeIf { it > 0 } ?: episode?.durationMs ?: 0L,
                speed = player.playbackParameters.speed,
            )
        }
    }

    private inner class ControllerListener : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) = publish()
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = publish()
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED && _state.value.sleepAtEndOfEpisode) {
                controller?.pause()
                cancelSleepTimer()
            }
            publish()
        }
    }
}
