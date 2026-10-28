package com.rebecca.sunplayer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rebecca.sunplayer.update.UpdateInfo

/**
 * Offers a downloaded-from-GitHub release. Rendering only: downloading and
 * installing are driven by the caller.
 *
 * @param currentVersion    Installed version, shown for context.
 * @param downloadProgress  `null` when idle, otherwise 0..100.
 */
@Composable
fun UpdateAvailableDialog(
    info: UpdateInfo,
    currentVersion: String,
    downloadProgress: Int?,
    errorMessage: String?,
    onDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    val progressPercent = downloadProgress
    val isDownloading = progressPercent != null

    AlertDialog(
        onDismissRequest = { if (!isDownloading) onDismiss() },
        title = { Text("Update available") },
        text = {
            Column {
                Text(
                    text = "SunPlayer ${info.versionName} is available. You are on version $currentVersion.",
                    style = MaterialTheme.typography.bodyMedium
                )

                if (info.releaseNotes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("What's new", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = info.releaseNotes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .heightIn(max = 220.dp)
                            .verticalScroll(rememberScrollState())
                    )
                }

                if (progressPercent != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { progressPercent / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Downloading… $progressPercent%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDownload, enabled = !isDownloading) {
                Text(if (isDownloading) "Downloading…" else "Download")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isDownloading) {
                Text("Later")
            }
        }
    )
}
