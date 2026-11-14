package com.example.watchgomoku

import com.example.watchgomoku.domain.engine.Board
import com.example.watchgomoku.domain.engine.GomokuRules
import com.example.watchgomoku.domain.model.Player
import com.example.watchgomoku.domain.model.Position
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 规则引擎测试：初始化 / 落子合法性 / 五连 / 长连 / 和棋。
 */
class RulesTest {

    @Test
    fun boardInitializesEmpty() {
        val board = Board(15)
        for (r in 0 until 15) {
            for (c in 0 until 15) {
                assertEquals(Board.EMPTY, board.get(r, c))
            }
        }
        assertEquals(0, board.moveCount())
        assertFalse(board.isFull())
    }

    @Test
    fun legalPlacementStoresStone() {
        val board = Board(15)
        assertTrue(board.place(7, 7, Player.BLACK))
        assertEquals(Player.BLACK_CODE, board.get(7, 7))
        assertEquals(1, board.moveCount())
    }

    @Test
    fun duplicatePlacementRejected() {
        val board = Board(15)
        assertTrue(board.place(7, 7, Player.BLACK))
        assertFalse(board.place(7, 7, Player.WHITE))
        assertEquals(1, board.moveCount())
    }

    @Test
    fun outOfBoundsPlacementRejected() {
        val board = Board(15)
        assertFalse(board.place(-1, 0, Player.BLACK))
        assertFalse(board.place(0, -1, Player.BLACK))
        assertFalse(board.place(15, 0, Player.BLACK))
        assertFalse(board.place(0, 15, Player.BLACK))
        assertEquals(0, board.moveCount())
    }

    @Test
    fun horizontalFiveWins() {
        val board = Board(15)
        for (c in 3..7) board.place(7, c, Player.BLACK)
        board.place(8, 3, Player.WHITE)
        val line = GomokuRules.winningLine(board, Position(7, 7), Player.BLACK)
        assertEquals(5, line.size)
        assertTrue(GomokuRules.isWinningMove(board, Position(7, 7), Player.BLACK))
    }

    @Test
    fun verticalFiveWins() {
        val board = Board(15)
        for (r in 2..6) board.place(r, 10, Player.WHITE)
        val line = GomokuRules.winningLine(board, Position(6, 10), Player.WHITE)
        assertEquals(5, line.size)
    }

    @Test
    fun diagonalFiveWins() {
        val board = Board(15)
        for (i in 0 until 5) board.place(5 + i, 5 + i, Player.BLACK)
        val line = GomokuRules.winningLine(board, Position(9, 9), Player.BLACK)
        assertEquals(5, line.size)
    }

    @Test
    fun antiDiagonalFiveWins() {
        val board = Board(15)
        for (i in 0 until 5) board.place(5 + i, 9 - i, Player.WHITE)
        val line = GomokuRules.winningLine(board, Position(9, 5), Player.WHITE)
        assertEquals(5, line.size)
    }

    @Test
    fun fourIsNotAWin() {
        val board = Board(15)
        for (c in 3..6) board.place(7, c, Player.BLACK)
        assertFalse(GomokuRules.isWinningMove(board, Position(7, 6), Player.BLACK))
    }

    @Test
    fun overlineWinsWhenAllowed() {
        val board = Board(15)
        for (c in 2..7) board.place(7, c, Player.BLACK)
        val line = GomokuRules.winningLine(
            board, Position(7, 7), Player.BLACK, overlineWins = true
        )
        assertEquals(6, line.size)
    }

    @Test
    fun overlineNotWinWhenDisallowed() {
        val board = Board(15)
        for (c in 2..7) board.place(7, c, Player.BLACK)
        val line = GomokuRules.winningLine(
            board, Position(7, 7), Player.BLACK, overlineWins = false
        )
        assertTrue(line.isEmpty())
    }

    @Test
    fun fullBoardWithoutFiveIsDraw() {
        // 3×3 小棋盘不可能连五，填满即和棋
        val board = Board(3)
        val players = listOf(Player.BLACK, Player.WHITE)
        var index = 0
        for (r in 0 until 3) {
            for (c in 0 until 3) {
                board.place(r, c, players[index % 2])
                index++
            }
        }
        assertTrue(board.isFull())
        assertTrue(GomokuRules.isDraw(board))
        for (r in 0 until 3) {
            for (c in 0 until 3) {
                assertFalse(
                    GomokuRules.isWinningMove(board, Position(r, c), Player.BLACK)
                )
            }
        }
    }

    @Test
    fun undoRestoresBoard() {
        val board = Board(15)
        board.place(7, 7, Player.BLACK)
        board.place(7, 8, Player.WHITE)
        assertEquals(2, board.moveCount())
        val last = board.undoLast()
        assertEquals(Position(7, 8), last?.position)
        assertEquals(Board.EMPTY, board.get(7, 8))
        assertEquals(1, board.moveCount())
    }
}
