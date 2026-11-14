package com.example.watchgomoku

import com.example.watchgomoku.domain.engine.Board
import com.example.watchgomoku.domain.engine.GomokuRules
import com.example.watchgomoku.domain.model.Player
import com.example.watchgomoku.domain.model.Position
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 禁手判定测试（简化棋型实现，默认关闭；开启后按本组用例语义）。
 */
class ForbiddenTest {

    private fun placeAll(board: Board, positions: List<Pair<Int, Int>>, player: Player) {
        positions.forEach { (r, c) -> assertTrue(board.place(r, c, player)) }
    }

    @Test
    fun doubleThreeForbiddenForBlack() {
        val board = Board(15)
        // (6,6),(8,8) 主对角 + (7,6),(7,8) 行 —— 黑落 (7,7) 同时形成两个活三
        placeAll(board, listOf(6 to 6, 8 to 8, 7 to 6, 7 to 8), Player.BLACK)
        assertTrue(
            GomokuRules.isForbidden(
                board, Position(7, 7), Player.BLACK,
                forbidDoubleThree = true, forbidDoubleFour = false
            )
        )
    }

    @Test
    fun singleThreeAllowed() {
        val board = Board(15)
        placeAll(board, listOf(7 to 5, 7 to 6), Player.BLACK)
        assertFalse(
            GomokuRules.isForbidden(
                board, Position(7, 7), Player.BLACK,
                forbidDoubleThree = true, forbidDoubleFour = false
            )
        )
    }

    @Test
    fun doubleFourForbiddenForBlack() {
        val board = Board(15)
        // 行 (7,4)(7,5)(7,6) + 列 (4,7)(5,7)(6,7) —— 黑落 (7,7) 同时形成两个四
        placeAll(board, listOf(7 to 4, 7 to 5, 7 to 6, 4 to 7, 5 to 7, 6 to 7), Player.BLACK)
        assertTrue(
            GomokuRules.isForbidden(
                board, Position(7, 7), Player.BLACK,
                forbidDoubleThree = false, forbidDoubleFour = true
            )
        )
    }

    @Test
    fun winningMoveIsNeverForbidden() {
        val board = Board(15)
        placeAll(board, listOf(7 to 4, 7 to 5, 7 to 6, 7 to 7), Player.BLACK)
        // 落 (7,8) 直接成五，不判禁手
        assertFalse(
            GomokuRules.isForbidden(
                board, Position(7, 8), Player.BLACK,
                forbidDoubleThree = true, forbidDoubleFour = true
            )
        )
    }

    @Test
    fun whiteNeverForbidden() {
        val board = Board(15)
        placeAll(board, listOf(6 to 6, 8 to 8, 7 to 6, 7 to 8), Player.WHITE)
        assertFalse(
            GomokuRules.isForbidden(
                board, Position(7, 7), Player.WHITE,
                forbidDoubleThree = true, forbidDoubleFour = true
            )
        )
    }

    @Test
    fun disabledByDefault() {
        val board = Board(15)
        placeAll(board, listOf(6 to 6, 8 to 8, 7 to 6, 7 to 8), Player.BLACK)
        assertFalse(
            GomokuRules.isForbidden(
                board, Position(7, 7), Player.BLACK,
                forbidDoubleThree = false, forbidDoubleFour = false
            )
        )
    }

    @Test
    fun occupiedPointIsIllegal() {
        val board = Board(15)
        placeAll(board, listOf(7 to 7), Player.BLACK)
        assertFalse(
            GomokuRules.isForbidden(
                board, Position(7, 7), Player.BLACK,
                forbidDoubleThree = true, forbidDoubleFour = true
            )
        )
    }
}
