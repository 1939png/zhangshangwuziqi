package com.example.watchgomoku.data

import android.content.Context
import com.example.watchgomoku.data.repository.GameRepository
import com.example.watchgomoku.data.local.GameSaveStore
import com.example.watchgomoku.data.local.MatchHistoryStore
import com.example.watchgomoku.data.local.SettingsStore

/**
 * 轻量依赖容器：由 GomokuApplication 持有。
 */
class AppContainer(context: Context) {
    val repository = GameRepository(
        settingsStore = SettingsStore(context),
        saveStore = GameSaveStore(context),
        historyStore = MatchHistoryStore(context)
    )
}
