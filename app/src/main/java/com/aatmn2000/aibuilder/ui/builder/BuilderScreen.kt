package com.aatmn2000.aibuilder.ui.builder

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuilderScreen(
    viewModel: BuilderViewModel,
    onNavigateHome: () -> Unit,
    onOpenProject: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringArrayExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.takeIf { it.isNotBlank() }
                ?.let { viewModel.appendVoice(it) }
        }
    }

    // Keep the log scrolled to the newest line while the build runs.
    LaunchedEffect(state.log.size) {
        if (state.log.isNotEmpty()) {
            listState.animateScrollToItem(state.log.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.projectName != null) "Modify: ${state.projectName}"
                        else "New project"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateHome) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!state.isRunning && state.success == null && state.log.isEmpty()) {
                IntroPanel(onExample = viewModel::onInputChanged)
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(state.log) { entry ->
                    LogRow(entry)
                    Spacer(Modifier.height(6.dp))
                }
            }

            state.success?.let { success ->
                SuccessCard(
                    success = success,
                    onShare = { viewModel.exportZip() },
                    onOpenProject = { onOpenProject(success.projectId) },
                    onReset = { viewModel.reset() }
                )
            }

            state.error?.let { error ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = state.input,
                    onValueChange = viewModel::onInputChanged,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Describe the software you want…") },
                    enabled = !state.isRunning,
                    maxLines = 3
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = { launchSpeech(speechLauncher) },
                    enabled = !state.isRunning
                ) {
                    Icon(Icons.Filled.Mic, contentDescription = "Voice input")
                }
                Spacer(Modifier.width(8.dp))
                FilledButton(
                    onClick = { viewModel.build() },
                    enabled = !state.isRunning && state.input.isNotBlank()
                ) {
                    Text(if (state.projectName != null) "Modify" else "Build")
                }
            }
        }
    }
}

@Composable
private fun IntroPanel(onExample: (String) -> Unit) {
    Column(Modifier.padding(16.dp)) {
        Text("Describe the software you need.", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            "AI plans it, writes the code, tests it, fixes errors and packages " +
                "it as a ZIP you can share and keep working on.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(14.dp))
        Text("Try one of these:", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        EXAMPLES.forEach { example ->
            SuggestionChip(onClick = { onExample(example) }, label = { Text(example) })
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun LogRow(entry: LogEntry) {
    val color = when (entry.kind) {
        LogEntry.Kind.AGENT -> MaterialTheme.colorScheme.primary
        LogEntry.Kind.WARNING -> MaterialTheme.colorScheme.tertiary
        LogEntry.Kind.ERROR -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(text = entry.text, style = MaterialTheme.typography.bodyMedium, color = color)
}

@Composable
private fun SuccessCard(
    success: SuccessInfo,
    onShare: () -> Unit,
    onOpenProject: () -> Unit,
    onReset: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text("Built ${success.projectName}", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Auto-repairs: ${success.repairAttempts} • Security notes: " +
                    "${success.securityNotes} • ZIP ready to share",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledButton(onClick = onShare) { Text("Share ZIP") }
                OutlinedButton(onClick = onOpenProject) { Text("Open project") }
                TextButton(onClick = onReset) { Text("New build") }
            }
        }
    }
}

private fun launchSpeech(launcher: ActivityResultLauncher<Intent>) {
    try {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Describe your software")
        }
        launcher.launch(intent)
    } catch (e: ActivityNotFoundException) {
        // No speech recognition app installed — the text field still works.
    }
}

private val EXAMPLES = listOf(
    "Create a small clinic appointment system",
    "Build a task manager for my team",
    "Make an inventory app for my workshop",
    "Create a book lending app for our library"
)
