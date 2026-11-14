package com.example.watchgomoku.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.watchgomoku.data.AppContainer
import com.example.watchgomoku.data.local.DefaultSettings
import com.example.watchgomoku.data.local.UiSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 设置页 ViewModel：内存先行更新 + DataStore 异步持久化。
 */
class SettingsViewModel(container: AppContainer) : ViewModel() {

    private val repository = container.repository

    private val _settings = MutableStateFlow(DefaultSettings)
    val settings: StateFlow<UiSettings> = _settings.asStateFlow()

    init {
        viewModelScope.launch {
            repository.settings.collect { _settings.value = it }
        }
    }

    fun update(transform: (UiSettings) -> UiSettings) {
        val next = transform(_settings.value)
        _settings.value = next
        viewModelScope.launch {
            repository.saveSettings(next)
        }
    }

    fun resetToDefaults() {
        _settings.value = DefaultSettings
        viewModelScope.launch {
            repository.saveSettings(DefaultSettings)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun clearSavedGame() {
        viewModelScope.launch {
            repository.clearSavedGame()
        }
    }
}
