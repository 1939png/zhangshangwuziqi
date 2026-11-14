package com.example.watchgomoku.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.watchgomoku.data.AppContainer
import com.example.watchgomoku.data.local.MatchRecord
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 棋谱页 ViewModel。
 */
class HistoryViewModel(container: AppContainer) : ViewModel() {

    private val repository = container.repository

    val records: StateFlow<List<MatchRecord>> = repository.historyStore.records

    init {
        viewModelScope.launch {
            repository.refreshHistory()
        }
    }

    fun clear() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}
