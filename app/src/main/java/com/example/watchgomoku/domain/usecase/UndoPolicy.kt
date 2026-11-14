package com.example.watchgomoku.domain.usecase

import com.example.watchgomoku.domain.model.GameMode
import com.example.watchgomoku.domain.model.Player

/**
 * 悔棋策略：
 * - 多人/练习模式：撤销一手；
 * - 单人模式：默认撤销“玩家上一手 + AI 上一手”两步。
 * 调用方需与棋盘剩余步数取 min，并自行扣除悔棋配额。
 */
object UndoPolicy {

    fun pliesToRemove(mode: GameMode, humanColor: Player?, currentPlayer: Player): Int {
        return when (mode) {
            GameMode.SINGLE_PLAYER -> {
                val human = humanColor ?: return 0
                if (currentPlayer == human) 2 else 1
            }
            else -> 1
        }
    }
}
