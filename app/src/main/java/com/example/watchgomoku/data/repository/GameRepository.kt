package com.example.watchgomoku.data.repository

import com.example.watchgomoku.data.local.GameSaveStore
import com.example.watchgomoku.data.local.MatchHistoryStore
import com.example.watchgomoku.data.local.SavedGame
import com.example.watchgomoku.data.local.MatchRecord
import com.example.watchgomoku.data.local.SettingsStore
import com.example.watchgomoku.data.local.UiSettings
import kotlinx.coroutines.flow.Flow

/**
 * 数据层统一门面：设置 / 存档 / 棋谱。
 */
class GameRepository(
    val settingsStore: SettingsStore,
    val saveStore: GameSaveStore,
    val historyStore: MatchHistoryStore
) {
    val settings: Flow<UiSettings> get() = settingsStore.settings

    suspend fun saveSettings(settings: UiSettings) = settingsStore.set(settings)

    suspend fun saveGame(game: SavedGame): Boolean = saveStore.save(game)
    suspend fun loadGame(): SavedGame? = saveStore.load()
    suspend fun clearSavedGame() = saveStore.clear()

    suspend fun addMatch(record: MatchRecord) = historyStore.add(record)
    suspend fun clearHistory() = historyStore.clear()
    suspend fun refreshHistory() = historyStore.refresh()
}
