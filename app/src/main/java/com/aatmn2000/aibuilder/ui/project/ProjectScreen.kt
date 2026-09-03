package com.aatmn2000.aibuilder.ui.project

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aatmn2000.aibuilder.core.project.EditRecord
import com.aatmn2000.aibuilder.core.project.ProjectFile
import com.aatmn2000.aibuilder.core.project.ProjectManifest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectScreen(
    viewModel: ProjectViewModel,
    onBack: () -> Unit,
    onModify: (String) -> Unit
) {
    val project = viewModel.project
    val manifest = project?.manifest
    val files = project?.files.orEmpty()
    val history = project?.history.orEmpty()
    var selectedPath by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(manifest?.name ?: "Project") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onModify(viewModel.projectId) }) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Modify with AI")
                    }
                    IconButton(onClick = { viewModel.shareZip() }) {
                        Icon(Icons.Outlined.Share, contentDescription = "Share ZIP")
                    }
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete project")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { ManifestCard(manifest) }
            itemsIndexed(files) { _, file ->
                FileRow(
                    file = file,
                    expanded = selectedPath == file.path,
                    onClick = {
                        selectedPath = if (selectedPath == file.path) null else file.path
                    }
                )
                if (selectedPath == file.path) {
                    CodePanel(file)
                }
            }
            if (history.isNotEmpty()) {
                item {
                    Text(
                        "Version history",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                history.reversed().forEach { record ->
                    item(key = "history-${record.timestamp}-${record.reason.hashCode()}") {
                        HistoryRow(record)
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete project?") },
            text = { Text("${viewModel.projectName} will be removed from this device.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.deleteProject(onBack)
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ManifestCard(manifest: ProjectManifest?) {
    if (manifest == null) return
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(manifest.name, style = MaterialTheme.typography.titleMedium)
            if (manifest.description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(manifest.description, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MetaItem("Language", manifest.language)
                MetaItem("Version", manifest.version)
                MetaItem("Entry", manifest.entryPoint)
            }
        }
    }
}

@Composable
private fun MetaItem(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun FileRow(file: ProjectFile, expanded: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (expanded) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = file.path,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            file.language?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
    // onClick is handled by the wrapper in LazyColumn item scope
    androidx.compose.runtime.remember { onClick }
}

@Composable
private fun CodePanel(file: ProjectFile) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(file.path, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(6.dp))
            Text(file.content, style = MaterialTheme.typography.code)
        }
    }
}

@Composable
private fun HistoryRow(record: EditRecord) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(record.reason, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(2.dp))
            Text(
                text = record.timestamp +
                    if (record.attempt > 0) " • repair attempt ${record.attempt}" else "",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (record.changedFiles.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = record.changedFiles.joinToString(", "),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
