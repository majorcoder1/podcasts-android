package com.podcasts.app.playback

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * Custom session commands so the notification and Android Auto expose the same
 * controls the original app did: back 10s, forward 30s, speed, sleep timer.
 */
@OptIn(UnstableApi::class)
class PlaybackSessionCallback : MediaSession.Callback {

    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
    ): MediaSession.ConnectionResult {
        val commands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
            .add(SessionCommand(ACTION_SET_SPEED, android.os.Bundle.EMPTY))
            .add(SessionCommand(ACTION_SET_SKIP_SILENCE, android.os.Bundle.EMPTY))
            .build()
        return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
            .setAvailableSessionCommands(commands)
            .build()
    }

    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: android.os.Bundle,
    ): ListenableFuture<SessionResult> {
        when (customCommand.customAction) {
            ACTION_SET_SPEED -> {
                val speed = args.getFloat(EXTRA_SPEED, 1.0f)
                session.player.setPlaybackSpeed(speed)
            }
            ACTION_SET_SKIP_SILENCE -> {
                val enabled = args.getBoolean(EXTRA_SKIP_SILENCE, false)
                (session.player as? androidx.media3.exoplayer.ExoPlayer)?.skipSilenceEnabled = enabled
            }
            else -> return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))
        }
        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
    }

    companion object {
        const val ACTION_SET_SPEED = "com.podcasts.app.SET_SPEED"
        const val ACTION_SET_SKIP_SILENCE = "com.podcasts.app.SET_SKIP_SILENCE"
        const val EXTRA_SPEED = "speed"
        const val EXTRA_SKIP_SILENCE = "skip_silence"
    }
}
