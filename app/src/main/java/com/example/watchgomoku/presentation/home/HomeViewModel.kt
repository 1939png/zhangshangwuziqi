package com.example.watchgomoku.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.watchgomoku.data.AppContainer
import com.example.watchgomoku.data.local.SavedGame
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 首页 ViewModel：探测自动存档 + 提供默认棋盘大小。
 */
class HomeViewModel(container: AppContainer) : ViewModel() {

    private val repository = container.repository

    private val _savedGame = MutableStateFlow<SavedGame?>(null)
    val savedGame: StateFlow<SavedGame?> = _savedGame.asStateFlow()

    private val _boardSize = MutableStateFlow(15)
    val boardSize: StateFlow<Int> = _boardSize.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            repository.settings.collect { _boardSize.value = it.boardSize }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _savedGame.value = repository.loadGame()
                ?.takeIf { !it.isGameOver && it.moves.isNotEmpty() }
        }
    }
}
