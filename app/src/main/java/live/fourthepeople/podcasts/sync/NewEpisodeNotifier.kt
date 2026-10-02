package live.fourthepeople.podcasts.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import live.fourthepeople.podcasts.R
import live.fourthepeople.podcasts.domain.Episode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewEpisodeNotifier @Inject constructor(private val context: Context) {

    fun notify(episodes: List<Episode>) {
        ensureChannel()
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        val title = if (episodes.size == 1) {
            episodes.first().title
        } else {
            context.getString(R.string.new_episodes_count, episodes.size)
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.new_episodes))
            .setContentText(title)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent())
            .setStyle(
                NotificationCompat.InboxStyle().also { style ->
                    episodes.take(5).forEach { style.addLine(it.title) }
                },
            )
            .build()

        // A denied POST_NOTIFICATIONS permission throws SecurityException, which runCatching absorbs.
        @android.annotation.SuppressLint("MissingPermission")
        val result = runCatching { manager.notify(NOTIFICATION_ID, notification) }
        result.getOrNull()
    }

    private fun openAppIntent() = android.app.PendingIntent.getActivity(
        context,
        0,
        Intent(context, live.fourthepeople.podcasts.ui.MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
        android.app.PendingIntent.FLAG_IMMUTABLE,
    )

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.channel_new_episodes),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
    }

    companion object {
        private const val CHANNEL_ID = "new_episodes"
        private const val NOTIFICATION_ID = 1001
    }
}
