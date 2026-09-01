package com.podcasts.app.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.podcasts.app.domain.PLAYBACK_SPEEDS
import com.podcasts.app.domain.SLEEP_TIMER_PRESETS
import com.podcasts.app.ui.components.formatSpeed

@Composable
fun SpeedDialog(current: Float, onSelect: (Float) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Playback speed") },
        text = {
            Column {
                PLAYBACK_SPEEDS.forEach { speed ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(speed) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(formatSpeed(speed), style = MaterialTheme.typography.bodyLarge)
                        if (speed == current) {
                            Icon(
                                Icons.Outlined.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerSheet(
    activeUntil: Long?,
    endOfEpisode: Boolean,
    onSelect: (Int?) -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
            Text("Sleep timer", style = MaterialTheme.typography.titleMedium)

            SLEEP_TIMER_PRESETS.forEach { minutes ->
                Text(
                    text = "$minutes minutes",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(minutes) }
                        .padding(vertical = 14.dp),
                )
            }

            Text(
                text = "End of episode",
                style = MaterialTheme.typography.bodyLarge,
                color = if (endOfEpisode) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(null) }
                    .padding(vertical = 14.dp),
            )

            if (activeUntil != null || endOfEpisode) {
                TextButton(onClick = onCancel) { Text("Turn off timer") }
            }
        }
    }
}
