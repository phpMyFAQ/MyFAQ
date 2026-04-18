package app.myfaq.android.screens

import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.os.LocaleListCompat
import app.myfaq.android.LocalThemeState
import app.myfaq.android.R
import app.myfaq.android.ThemeMode
import app.myfaq.shared.data.ActiveInstanceManager
import app.myfaq.shared.data.MyFaqDatabase
import kotlinx.coroutines.launch
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
    var availableLanguages by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var languageMenuExpanded by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(activeInstance?.id) {
        availableLanguages = emptyMap()
        try {
            availableLanguages = aim.repository.meta().availableLanguages
        } catch (_: Exception) {
            // Offline or unreachable — leave map empty; selector won't render.
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirmation) {
        activeInstance?.let { instance ->
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text(stringResource(R.string.delete_instance_title)) },
                text = { Text(stringResource(R.string.delete_instance_message, instance.displayName)) },
                confirmButton = {
                    TextButton(onClick = {
                        db.instancesQueries.deleteById(instance.id)
                        aim.clear()
                        showDeleteConfirmation = false
                        onSwitchInstance()
                    }) {
                        Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmation = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                },
            )
        }
    }

    val themeState = LocalThemeState.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text(stringResource(R.string.settings)) })
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Active instance info
            activeInstance?.let { instance ->
                ListItem(
                    overlineContent = { Text(stringResource(R.string.active_instance)) },
                    headlineContent = { Text(instance.displayName) },
                    supportingContent = { Text(instance.baseUrl) },
                )
                HorizontalDivider()
            }

            // Appearance
            AppearancePickerItem(
                currentMode = themeState.mode,
                onModeSelected = { themeState.setMode(it) },
            )
            HorizontalDivider()

            // Content language
            activeInstance?.let { instance ->
                if (availableLanguages.isNotEmpty()) {
                    LanguagePickerItem(
                        currentCode = instance.language,
                        languages = availableLanguages,
                        expanded = languageMenuExpanded,
                        onExpandedChange = { languageMenuExpanded = it },
                        onSelected = { code ->
                            if (code != instance.language) {
                                coroutineScope.launch {
                                    val now = System.currentTimeMillis() / 1000
                                    db.instancesQueries.updateLanguage(code, now, instance.id)
                                    aim.setLanguage(code)
                                    AppCompatDelegate.setApplicationLocales(
                                        LocaleListCompat.forLanguageTags(code),
                                    )
                                }
                            }
                        },
                    )
                    HorizontalDivider()
                }
            }

            // Switch instance
            ListItem(
                headlineContent = { Text(stringResource(R.string.switch_instance)) },
                modifier = Modifier.clickable(onClick = onSwitchInstance),
            )
            HorizontalDivider()

            // Clear cache
            val cacheClearedMessage = stringResource(R.string.cache_cleared)
            ListItem(
                headlineContent = { Text(stringResource(R.string.clear_cache)) },
                modifier =
                    Modifier.clickable {
                        try {
                            aim.repository.clearCache()
                            Toast.makeText(context, cacheClearedMessage, Toast.LENGTH_SHORT).show()
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
                            text = stringResource(R.string.delete_instance),
                            color = MaterialTheme.colorScheme.error,
                        )
                    },
                    modifier = Modifier.clickable { showDeleteConfirmation = true },
                )
                HorizontalDivider()
            }

            // App version
            ListItem(
                headlineContent = { Text(stringResource(R.string.app_version)) },
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
                headlineContent = { Text(stringResource(R.string.about)) },
                supportingContent = {
                    Text(
                        text = stringResource(R.string.about_subtitle),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
            HorizontalDivider()

            // Open source licenses
            ListItem(
                headlineContent = { Text(stringResource(R.string.open_source_licenses)) },
                modifier = Modifier.clickable(onClick = onLicenses),
            )
        }
    }
}

@Composable
private fun AppearancePickerItem(
    currentMode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit,
) {
    ListItem(
        overlineContent = { Text(stringResource(R.string.appearance)) },
        headlineContent = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                ThemeMode.entries.forEach { mode ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onModeSelected(mode) },
                    ) {
                        RadioButton(
                            selected = currentMode == mode,
                            onClick = { onModeSelected(mode) },
                        )
                        Text(
                            text = stringResource(mode.labelRes),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun LanguagePickerItem(
    currentCode: String,
    languages: Map<String, String>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelected: (String) -> Unit,
) {
    ListItem(
        overlineContent = { Text(stringResource(R.string.content_language)) },
        headlineContent = { Text(languages[currentCode] ?: currentCode) },
        trailingContent = {
            Box {
                TextButton(onClick = { onExpandedChange(true) }) {
                    Text(stringResource(R.string.change))
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { onExpandedChange(false) },
                ) {
                    languages.entries
                        .sortedBy { it.value }
                        .forEach { (code, name) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    onExpandedChange(false)
                                    onSelected(code)
                                },
                            )
                        }
                }
            }
        },
    )
}

private fun getAppVersion(context: android.content.Context): String =
    try {
        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        pInfo.versionName ?: "Unknown"
    } catch (_: Exception) {
        "Unknown"
    }
