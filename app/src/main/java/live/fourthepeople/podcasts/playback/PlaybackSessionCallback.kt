package live.fourthepeople.podcasts.playback

import android.os.Process
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

    /**
     * The service is exported - it has to be, or the notification, Bluetooth
     * controls and Android Auto could not reach it. Exported means any app on
     * the device can try to bind, so connections are vetted here instead of
     * being accepted blindly: an unvetted session hands a stranger both
     * playback control and a live feed of what is being listened to.
     */
    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
    ): MediaSession.ConnectionResult {
        if (!isTrusted(session, controller)) {
            return MediaSession.ConnectionResult.reject()
        }

        val commands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
            .add(SessionCommand(ACTION_SET_SPEED, android.os.Bundle.EMPTY))
            .add(SessionCommand(ACTION_SET_SKIP_SILENCE, android.os.Bundle.EMPTY))
            .build()
        return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
            .setAvailableSessionCommands(commands)
            .build()
    }

    /**
     * Trusted means: this app's own UI (same UID, which is a stronger check
     * than comparing package names), the platform itself (media buttons,
     * headset and Bluetooth controls all arrive as the system UID), or one of
     * the companions Media3 recognises for the notification, Auto and
     * Automotive surfaces. Everything else is a third-party app and is refused.
     */
    private fun isTrusted(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
    ): Boolean = controller.uid == Process.myUid() ||
        controller.uid == Process.SYSTEM_UID ||
        session.isMediaNotificationController(controller) ||
        session.isAutoCompanionController(controller) ||
        session.isAutomotiveController(controller)

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
            else -> return Futures.immediateFuture(SessionResult(androidx.media3.session.SessionError.ERROR_NOT_SUPPORTED))
        }
        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
    }

    companion object {
        const val ACTION_SET_SPEED = "live.fourthepeople.podcasts.SET_SPEED"
        const val ACTION_SET_SKIP_SILENCE = "live.fourthepeople.podcasts.SET_SKIP_SILENCE"
        const val EXTRA_SPEED = "speed"
        const val EXTRA_SKIP_SILENCE = "skip_silence"
    }
}
