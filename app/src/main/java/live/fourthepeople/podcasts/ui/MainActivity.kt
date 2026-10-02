package live.fourthepeople.podcasts.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import live.fourthepeople.podcasts.ui.Shortcut
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import live.fourthepeople.podcasts.ui.settings.SettingsViewModel
import live.fourthepeople.podcasts.ui.theme.PodcastsTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val settingsViewModel: SettingsViewModel by viewModels()

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* optional */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        requestNotificationPermission()
        setContent {
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
            // A launcher shortcut can arrive either as a cold start or at an
            // already-running activity, so the request is held in state and
            // updated from onNewIntent as well.
            var request by remember { mutableStateOf(shortcutRequest(intent)) }
            DisposableEffect(Unit) {
                val listener = Consumer<Intent> { request = shortcutRequest(it) }
                addOnNewIntentListener(listener)
                onDispose { removeOnNewIntentListener(listener) }
            }

            PodcastsTheme(themeMode = settings.darkTheme) {
                PodcastsApp(
                    shortcut = request,
                    onShortcutHandled = { request = null },
                )
            }
        }
    }

    /** Maps a launcher shortcut's action onto something the app can navigate to. */
    private fun shortcutRequest(intent: Intent?): Shortcut? = when (intent?.action) {
        ACTION_RESUME -> Shortcut.RESUME
        ACTION_NEW_EPISODES -> Shortcut.NEW_EPISODES
        ACTION_QUEUE -> Shortcut.QUEUE
        ACTION_EXPLORE -> Shortcut.EXPLORE
        else -> null
    }

    companion object {
        const val ACTION_RESUME = "live.fourthepeople.podcasts.action.RESUME"
        const val ACTION_NEW_EPISODES = "live.fourthepeople.podcasts.action.NEW_EPISODES"
        const val ACTION_QUEUE = "live.fourthepeople.podcasts.action.QUEUE"
        const val ACTION_EXPLORE = "live.fourthepeople.podcasts.action.EXPLORE"
    }

    /** Needed on Android 13+ for both the player notification and new-episode alerts. */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
