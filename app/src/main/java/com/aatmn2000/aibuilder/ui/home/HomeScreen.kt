package com.aatmn2000.aibuilder.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNewProject: () -> Unit,
    onModifyProject: (String) -> Unit,
    onOpenProject: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importFromUri(uri)
        }
    }

    // After a successful import, jump straight into the builder to continue.
    LaunchedEffect(state.importedProjectId) {
        val imported = state.importedProjectId
        if (imported != null) {
            onModifyProject(imported)
            viewModel.consumeImportedProject()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Software Builder") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewProject) {
                Icon(Icons.Filled.Add, contentDescription = "New project")
            }
        }
    ) { padding ->
        if (state.projects.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "Describe it. AI builds it.",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Create your first project, or import a .zip from another device.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                OutlinedButton(onClick = { importLauncher.launch(arrayOf("*/*")) }) {
                    Icon(Icons.Outlined.FileOpen, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Import ZIP")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedButton(onClick = { importLauncher.launch(arrayOf("*/*")) }) {
                        Icon(Icons.Outlined.FileOpen, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Import ZIP")
                    }
                }
                items(state.projects) { summary ->
                    ProjectCard(
                        name = summary.name,
                        description = summary.description,
                        language = summary.language,
                        version = summary.version,
                        onClick = { onOpenProject(summary.id) },
                        onModify = { onModifyProject(summary.id) },
                        onDelete = { viewModel.deleteProject(summary.id) }
                    )
                }
            }
        }
    }

    state.importError?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.clearImportError() },
            title = { Text("Import failed") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearImportError() }) { Text("OK") }
            }
        )
    }
}

@Composable
private fun ProjectCard(
    name: String,
    description: String,
    language: String,
    version: String,
    onClick: () -> Unit,
    onModify: () -> Unit,
    onDelete: () -> Unit
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = language.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AssistChip(onClick = onModify, label = { Text("Modify with AI") })
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "v$version",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete project")
                }
            }
        }
    }
}
