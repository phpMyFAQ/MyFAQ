package app.myfaq.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.myfaq.shared.api.MetaLoader
import app.myfaq.shared.entitlements.Entitlements
import org.koin.compose.koinInject

/**
 * Phase 0 placeholder screen. Proves that the shared module, Koin
 * graph, generated client, Ktor pipeline, and serialization all
 * boot end-to-end on Android. It does nothing else — real screens
 * land in Phase 1.
 */
@Composable
fun PhaseZeroScreen(loader: MetaLoader = koinInject()) {
    var state by remember { mutableStateOf<MetaUiState>(MetaUiState.Loading) }

    LaunchedEffect(Unit) {
        loader.load(
            onSuccess = { state = MetaUiState.Loaded(it) },
            onError = { state = MetaUiState.Failed(it) },
        )
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "MyFAQ.app",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = "Phase 0 foundations",
                style = MaterialTheme.typography.titleMedium,
            )
            when (val s = state) {
                MetaUiState.Loading -> CircularProgressIndicator()
                is MetaUiState.Loaded -> Text(
                    text = s.summary,
                    style = MaterialTheme.typography.bodyLarge,
                )
                is MetaUiState.Failed -> Text(
                    text = "Bootstrap failed: ${s.reason}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Text(
                text = "Pro unlocked: ${Entitlements.isPro()}",
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

private sealed interface MetaUiState {
    data object Loading : MetaUiState
    data class Loaded(val summary: String) : MetaUiState
    data class Failed(val reason: String) : MetaUiState
}
