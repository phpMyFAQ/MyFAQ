package app.myfaq.android.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.myfaq.android.R
import app.myfaq.shared.data.ActiveInstanceManager
import app.myfaq.shared.domain.Instance
import app.myfaq.shared.ui.WorkspacesViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspacesScreen(
    onInstanceSelected: () -> Unit,
    onAddInstance: () -> Unit,
    aim: ActiveInstanceManager = koinInject(),
    db: app.myfaq.shared.data.MyFaqDatabase = koinInject(),
) {
    val vm = WorkspacesViewModel(db, aim)
    val instances by vm.instances.collectAsState()
    var instanceToDelete by remember { mutableStateOf<Instance?>(null) }
    var instanceToRename by remember { mutableStateOf<Instance?>(null) }
    var renameText by remember { mutableStateOf("") }

    // Delete confirmation dialog
    instanceToDelete?.let { instance ->
        AlertDialog(
            onDismissRequest = { instanceToDelete = null },
            title = { Text("Delete Instance") },
            text = { Text("Are you sure you want to delete \"${instance.displayName}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteInstance(instance.id)
                    instanceToDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { instanceToDelete = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    // Rename dialog
    instanceToRename?.let { instance ->
        AlertDialog(
            onDismissRequest = { instanceToRename = null },
            title = { Text("Rename Instance") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Name") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val trimmed = renameText.trim()
                    if (trimmed.isNotEmpty()) vm.renameInstance(instance.id, trimmed)
                    instanceToRename = null
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { instanceToRename = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Workspaces") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddInstance) {
                Icon(Icons.Default.Add, contentDescription = "Add instance")
            }
        },
    ) { padding ->
        if (instances.isEmpty()) {
            EmptyWorkspacesState(
                modifier = Modifier.padding(padding),
                onAdd = onAddInstance,
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(instances, key = { it.id }) { instance ->
                    InstanceCard(
                        instance = instance,
                        onClick = {
                            vm.selectInstance(instance)
                            onInstanceSelected()
                        },
                        onRename = {
                            renameText = instance.displayName
                            instanceToRename = instance
                        },
                        onDelete = { instanceToDelete = instance },
                    )
                }
            }
        }
    }
}

@Composable
private fun InstanceCard(
    instance: Instance,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = instance.displayName,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = instance.baseUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onRename) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Rename",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun EmptyWorkspacesState(
    modifier: Modifier = Modifier,
    onAdd: () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "phpMyFAQ logo",
                modifier = Modifier.height(120.dp),
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "No instances yet",
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Add your phpMyFAQ instance to get started.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onAdd) {
                Text("Add Instance")
            }
        }
    }
}
