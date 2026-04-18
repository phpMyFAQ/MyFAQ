package app.myfaq.android.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.myfaq.android.R

private data class LibraryEntry(
    val name: String,
    val license: String,
    val url: String,
)

private val libraries: List<LibraryEntry> =
    listOf(
        LibraryEntry("Kotlin", "Apache 2.0", "https://kotlinlang.org"),
        LibraryEntry("Kotlin Coroutines", "Apache 2.0", "https://github.com/Kotlin/kotlinx.coroutines"),
        LibraryEntry("kotlinx.serialization", "Apache 2.0", "https://github.com/Kotlin/kotlinx.serialization"),
        LibraryEntry("Ktor", "Apache 2.0", "https://ktor.io"),
        LibraryEntry("SQLDelight", "Apache 2.0", "https://github.com/cashapp/sqldelight"),
        LibraryEntry("Koin", "Apache 2.0", "https://insert-koin.io"),
        LibraryEntry("Jetpack Compose", "Apache 2.0", "https://developer.android.com/jetpack/compose"),
        LibraryEntry("Material Icons", "Apache 2.0", "https://fonts.google.com/icons"),
        LibraryEntry("AndroidX Security Crypto", "Apache 2.0", "https://developer.android.com/jetpack/androidx"),
        LibraryEntry("SQLCipher", "BSD-Style", "https://www.zetetic.net/sqlcipher/"),
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.licenses)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.Top,
        ) {
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.licenses_app_name),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        stringResource(R.string.licenses_app_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        stringResource(R.string.licenses_app_license),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                HorizontalDivider()
            }
            items(libraries, key = { it.name }) { entry ->
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(entry.name, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "${entry.license} • ${entry.url}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                HorizontalDivider()
            }
        }
    }
}
