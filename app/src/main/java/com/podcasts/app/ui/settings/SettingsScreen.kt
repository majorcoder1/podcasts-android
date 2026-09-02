package com.podcasts.app.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.podcasts.app.data.ThemeMode
import com.podcasts.app.domain.PLAYBACK_SPEEDS
import com.podcasts.app.ui.components.formatSpeed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::importOpml) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/xml"),
    ) { uri -> uri?.let(viewModel::exportOpml) }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        // The app Scaffold behind this route owns the system-bar insets already.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionTitle("Playback")
            OptionRow(
                title = "Default playback speed",
                subtitle = formatSpeed(settings.defaultSpeed),
                onClick = {
                    val next = PLAYBACK_SPEEDS[
                        (PLAYBACK_SPEEDS.indexOf(settings.defaultSpeed) + 1) % PLAYBACK_SPEEDS.size,
                    ]
                    viewModel.setSpeed(next)
                },
            )
            OptionRow(
                title = "Skip forward",
                subtitle = "${settings.skipForwardSeconds} seconds",
                onClick = {
                    val options = listOf(10, 15, 30, 45, 60)
                    val next = options[(options.indexOf(settings.skipForwardSeconds) + 1) % options.size]
                    viewModel.setSkipForward(next)
                },
            )
            OptionRow(
                title = "Skip back",
                subtitle = "${settings.skipBackSeconds} seconds",
                onClick = {
                    val options = listOf(5, 10, 15, 30)
                    val next = options[(options.indexOf(settings.skipBackSeconds) + 1) % options.size]
                    viewModel.setSkipBack(next)
                },
            )
            SwitchRow(
                title = "Skip silence",
                subtitle = "Shorten silent gaps in speech",
                checked = settings.skipSilence,
                onChange = viewModel::setSkipSilence,
            )
            SwitchRow(
                title = "Continuous playback",
                subtitle = "Play the next episode in your queue automatically",
                checked = settings.autoPlayNext,
                onChange = viewModel::setAutoPlayNext,
            )

            HorizontalDivider()
            SectionTitle("Downloads")
            SwitchRow(
                title = "Download over Wi-Fi only",
                checked = settings.downloadOnWifiOnly,
                onChange = viewModel::setWifiOnly,
            )
            SwitchRow(
                title = "Auto download new episodes",
                checked = settings.autoDownloadEnabled,
                onChange = viewModel::setAutoDownload,
            )
            OptionRow(
                title = "Keep per show",
                subtitle = "${settings.autoDownloadLimitPerShow} episodes",
                onClick = {
                    val options = listOf(1, 2, 3, 5, 10)
                    val next = options[
                        (options.indexOf(settings.autoDownloadLimitPerShow) + 1) % options.size,
                    ]
                    viewModel.setAutoDownloadLimit(next)
                },
            )
            SwitchRow(
                title = "Remove downloads when played",
                checked = settings.removeDownloadWhenPlayed,
                onChange = viewModel::setRemoveWhenPlayed,
            )
            OptionRow(
                title = "Remove downloads after",
                subtitle = if (settings.removeDownloadAfterDays == 0) {
                    "Never"
                } else {
                    "${settings.removeDownloadAfterDays} days"
                },
                onClick = {
                    val options = listOf(0, 7, 14, 30, 90)
                    val next = options[(options.indexOf(settings.removeDownloadAfterDays) + 1) % options.size]
                    viewModel.setRemoveAfterDays(next)
                },
            )
            OptionRow(
                title = "Remove all downloads",
                subtitle = "Frees up space on this device",
                onClick = viewModel::removeAllDownloads,
            )

            HorizontalDivider()
            SectionTitle("Subscriptions")
            OptionRow(
                title = "Refresh interval",
                subtitle = "Every ${settings.refreshIntervalHours} hours",
                onClick = {
                    val options = listOf(1, 3, 6, 12, 24)
                    val next = options[(options.indexOf(settings.refreshIntervalHours) + 1) % options.size]
                    viewModel.setRefreshHours(next)
                },
            )
            SwitchRow(
                title = "Notify about new episodes",
                checked = settings.notifyNewEpisodes,
                onChange = viewModel::setNotify,
            )
            OptionRow(
                title = "Import subscriptions",
                subtitle = "From an OPML file",
                onClick = { importLauncher.launch(arrayOf("text/xml", "text/x-opml", "*/*")) },
            )
            OptionRow(
                title = "Export subscriptions",
                subtitle = "To an OPML file",
                onClick = { exportLauncher.launch("subscriptions.opml") },
            )

            HorizontalDivider()
            SectionTitle("Appearance")
            OptionRow(
                title = "Theme",
                subtitle = when (settings.darkTheme) {
                    ThemeMode.LIGHT -> "Light"
                    ThemeMode.DARK -> "Dark"
                    ThemeMode.SYSTEM -> "System default"
                },
                onClick = {
                    val next = when (settings.darkTheme) {
                        ThemeMode.SYSTEM -> ThemeMode.LIGHT
                        ThemeMode.LIGHT -> ThemeMode.DARK
                        ThemeMode.DARK -> ThemeMode.SYSTEM
                    }
                    viewModel.setTheme(next)
                },
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun OptionRow(title: String, subtitle: String? = null, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        subtitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    subtitle: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
