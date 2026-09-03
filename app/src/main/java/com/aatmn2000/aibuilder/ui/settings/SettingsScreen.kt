package com.aatmn2000.aibuilder.ui.settings

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.aatmn2000.aibuilder.data.AiSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("AI provider", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = settings.preferredProvider == AiSettings.PROVIDER_MOCK,
                        onClick = {
                            viewModel.update { s ->
                                s.copy(preferredProvider = AiSettings.PROVIDER_MOCK)
                            }
                        },
                        label = { Text("Mock") }
                    )
                    FilterChip(
                        selected = settings.preferredProvider == AiSettings.PROVIDER_OLLAMA,
                        onClick = {
                            viewModel.update { s ->
                                s.copy(preferredProvider = AiSettings.PROVIDER_OLLAMA)
                            }
                        },
                        label = { Text("Ollama") }
                    )
                    FilterChip(
                        selected = settings.preferredProvider == AiSettings.PROVIDER_OPENAI,
                        onClick = {
                            viewModel.update { s ->
                                s.copy(preferredProvider = AiSettings.PROVIDER_OPENAI)
                            }
                        },
                        label = { Text("OpenAI-compatible") }
                    )
                }
                Text(
                    "The mock provider works offline and is deterministic. " +
                        "The gateway falls back to the next available provider " +
                        "when the preferred one is unreachable.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Text("Ollama (local AI)", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = settings.ollamaBaseUrl,
                    onValueChange = { value ->
                        viewModel.update { s -> s.copy(ollamaBaseUrl = value) }
                    },
                    label = { Text("Base URL") },
                    placeholder = { Text(AiSettings.DEFAULT_OLLAMA_URL) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = settings.ollamaModel,
                    onValueChange = { value ->
                        viewModel.update { s -> s.copy(ollamaModel = value) }
                    },
                    label = { Text("Model") },
                    placeholder = { Text(AiSettings.DEFAULT_OLLAMA_MODEL) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Text("OpenAI-compatible API", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = settings.openAiBaseUrl,
                    onValueChange = { value ->
                        viewModel.update { s -> s.copy(openAiBaseUrl = value) }
                    },
                    label = { Text("Base URL") },
                    placeholder = { Text("https://api.openai.com/v1") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = settings.openAiApiKey,
                    onValueChange = { value ->
                        viewModel.update { s -> s.copy(openAiApiKey = value) }
                    },
                    label = { Text("API key") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = settings.openAiModel,
                    onValueChange = { value ->
                        viewModel.update { s -> s.copy(openAiModel = value) }
                    },
                    label = { Text("Model") },
                    placeholder = { Text("gpt-4o-mini") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "The API key is stored in Android encrypted storage and is " +
                        "never written into generated projects.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Text(
                    "Maximum automatic repairs",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..5).forEach { attempts ->
                        FilterChip(
                            selected = settings.maxDebugAttempts == attempts,
                            onClick = {
                                viewModel.update { s -> s.copy(maxDebugAttempts = attempts) }
                            },
                            label = { Text("$attempts") }
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "The debugging agent re-runs at most this many times per " +
                        "build, touching only the affected modules.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
