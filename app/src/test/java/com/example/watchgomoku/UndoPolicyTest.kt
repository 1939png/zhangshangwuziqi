package com.example.watchgomoku

import com.example.watchgomoku.domain.model.GameMode
import com.example.watchgomoku.domain.model.Player
import com.example.watchgomoku.domain.usecase.UndoPolicy
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 悔棋策略测试：多人撤一手，单人撤“玩家+AI”两手。
 */
class UndoPolicyTest {

    @Test
    fun multiplayerRemovesOnePly() {
        assertEquals(
            1,
            UndoPolicy.pliesToRemove(GameMode.MULTIPLAYER, null, Player.BLACK)
        )
    }

    @Test
    fun practiceRemovesOnePly() {
        assertEquals(
            1,
            UndoPolicy.pliesToRemove(GameMode.PRACTICE, null, Player.WHITE)
        )
    }

    @Test
    fun singleModeWhenPlayerTurnRemovesTwo() {
        // 玩家执黑，AI 已落子，轮到玩家 → 撤销 AI + 玩家两步
        assertEquals(
            2,
            UndoPolicy.pliesToRemove(GameMode.SINGLE_PLAYER, Player.BLACK, Player.BLACK)
        )
    }

    @Test
    fun singleModeWhenAiThinkingRemovesOne() {
        // 玩家刚落子，AI 未落（轮到 AI）→ 只撤玩家一步
        assertEquals(
            1,
            UndoPolicy.pliesToRemove(GameMode.SINGLE_PLAYER, Player.BLACK, Player.WHITE)
        )
    }

    @Test
    fun singleModeRequiresHumanColor() {
        assertEquals(
            0,
            UndoPolicy.pliesToRemove(GameMode.SINGLE_PLAYER, null, Player.BLACK)
        )
    }
}
