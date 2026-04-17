package app.myfaq.android.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import app.myfaq.android.LocalThemeState
import app.myfaq.android.ThemeMode
import app.myfaq.shared.data.ActiveInstanceManager
import app.myfaq.shared.data.MyFaqDatabase
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onSwitchInstance: () -> Unit,
    onLicenses: () -> Unit = {},
    aim: ActiveInstanceManager = koinInject(),
    db: MyFaqDatabase = koinInject(),
) {
    val context = LocalContext.current
    val activeInstance by aim.activeInstance.collectAsState()
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    // Delete confirmation dialog
    if (showDeleteConfirmation) {
        activeInstance?.let { instance ->
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text("Delete Instance") },
                text = { Text("Are you sure you want to delete \"${instance.displayName}\"? This cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        db.instancesQueries.deleteById(instance.id)
                        aim.clear()
                        showDeleteConfirmation = false
                        onSwitchInstance()
                    }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmation = false }) {
                        Text("Cancel")
                    }
                },
            )
        }
    }

    val themeState = LocalThemeState.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Settings") })
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Active instance info
            activeInstance?.let { instance ->
                ListItem(
                    overlineContent = { Text("Active instance") },
                    headlineContent = { Text(instance.displayName) },
                    supportingContent = { Text(instance.baseUrl) },
                )
                HorizontalDivider()
            }

            // Appearance
            ListItem(
                overlineContent = { Text("Appearance") },
                headlineContent = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier =
                                    Modifier.clickable {
                                        themeState.setMode(mode)
                                    },
                            ) {
                                RadioButton(
                                    selected = themeState.mode == mode,
                                    onClick = { themeState.setMode(mode) },
                                )
                                Text(
                                    text = mode.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                },
            )
            HorizontalDivider()

            // Switch instance
            ListItem(
                headlineContent = { Text("Switch instance") },
                modifier = Modifier.clickable(onClick = onSwitchInstance),
            )
            HorizontalDivider()

            // Clear cache
            ListItem(
                headlineContent = { Text("Clear cache") },
                modifier =
                    Modifier.clickable {
                        try {
                            aim.repository.clearCache()
                            Toast.makeText(context, "Cache cleared", Toast.LENGTH_SHORT).show()
                        } catch (_: IllegalStateException) {
                            // No active instance
                        }
                    },
            )
            HorizontalDivider()

            // Delete instance
            if (activeInstance != null) {
                ListItem(
                    headlineContent = {
                        Text(
                            text = "Delete instance",
                            color = MaterialTheme.colorScheme.error,
                        )
                    },
                    modifier = Modifier.clickable { showDeleteConfirmation = true },
                )
                HorizontalDivider()
            }

            // App version
            ListItem(
                headlineContent = { Text("App version") },
                supportingContent = {
                    Text(
                        text = getAppVersion(context),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
            HorizontalDivider()

            // About
            ListItem(
                headlineContent = { Text("About") },
                supportingContent = {
                    Text(
                        text = "MyFAQ.app - Native client for phpMyFAQ",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
            HorizontalDivider()

            // Open source licenses
            ListItem(
                headlineContent = { Text("Open source licenses") },
                modifier = Modifier.clickable(onClick = onLicenses),
            )
        }
    }
}

private fun getAppVersion(context: android.content.Context): String =
    try {
        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        pInfo.versionName ?: "Unknown"
    } catch (_: Exception) {
        "Unknown"
    }
