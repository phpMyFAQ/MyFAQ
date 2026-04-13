package app.myfaq.android.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import app.myfaq.shared.data.ActiveInstanceManager
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onSwitchInstance: () -> Unit,
    aim: ActiveInstanceManager = koinInject(),
) {
    val context = LocalContext.current
    val activeInstance by aim.activeInstance.collectAsState()

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

            // Switch instance
            ListItem(
                headlineContent = { Text("Switch instance") },
                modifier = Modifier.clickable(onClick = onSwitchInstance),
            )
            HorizontalDivider()

            // Clear cache
            ListItem(
                headlineContent = { Text("Clear cache") },
                modifier = Modifier.clickable {
                    try {
                        aim.repository.clearCache()
                        Toast.makeText(context, "Cache cleared", Toast.LENGTH_SHORT).show()
                    } catch (_: IllegalStateException) {
                        // No active instance
                    }
                },
            )
            HorizontalDivider()

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
