package com.aatmn2000.aibuilder.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aatmn2000.aibuilder.AppContainer
import com.aatmn2000.aibuilder.data.AiSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    private val _settings = MutableStateFlow(container.settingsRepository.load())
    val settings: StateFlow<AiSettings> = _settings.asStateFlow()

    fun update(transform: (AiSettings) -> AiSettings) {
        _settings.update { current ->
            val next = transform(current)
            container.settingsRepository.save(next)
            next
        }
    }

    /** Re-reads settings after they may have changed elsewhere. */
    fun reload() {
        viewModelScope.launch {
            val fresh = withContext(Dispatchers.IO) {
                container.settingsRepository.load()
            }
            _settings.value = fresh
        }
    }
}
