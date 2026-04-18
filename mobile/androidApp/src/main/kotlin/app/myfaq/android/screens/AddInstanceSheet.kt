package app.myfaq.android.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.myfaq.android.R
import app.myfaq.shared.data.ActiveInstanceManager
import app.myfaq.shared.ui.AddInstanceState
import app.myfaq.shared.ui.WorkspacesViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddInstanceSheet(
    onDismiss: () -> Unit,
    onInstanceAdded: () -> Unit,
    aim: ActiveInstanceManager = koinInject(),
    db: app.myfaq.shared.data.MyFaqDatabase = koinInject(),
) {
    val vm = remember { WorkspacesViewModel(db, aim) }
    val addState by vm.addState.collectAsState()
    var url by remember { mutableStateOf("") }

    DisposableEffect(Unit) {
        onDispose { vm.resetAddState() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_instance)) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxWidth(),
        ) {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text(stringResource(R.string.url_label)) },
                placeholder = { Text(stringResource(R.string.url_placeholder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            when (val state = addState) {
                AddInstanceState.Idle -> {
                    Button(
                        onClick = { vm.probeInstance(url) },
                        enabled = url.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.connect))
                    }
                }

                AddInstanceState.Probing -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                }

                is AddInstanceState.Confirmed -> {
                    ConfirmationCard(
                        title = state.meta.title,
                        version = state.meta.version,
                        onConfirm = {
                            vm.confirmAdd(state.url, state.meta)
                            onInstanceAdded()
                        },
                    )
                }

                is AddInstanceState.Failed -> {
                    Text(
                        text = state.reason,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { vm.probeInstance(url) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.retry))
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfirmationCard(
    title: String,
    version: String,
    onConfirm: () -> Unit,
) {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.phpmyfaq_version, version),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.add_instance))
            }
        }
    }
}
