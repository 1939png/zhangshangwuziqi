package com.example.watchgomoku

import com.example.watchgomoku.domain.ai.AiPlayer
import com.example.watchgomoku.domain.engine.Board
import com.example.watchgomoku.domain.model.AiDifficulty
import com.example.watchgomoku.domain.model.CustomAiConfig
import com.example.watchgomoku.domain.model.Player
import com.example.watchgomoku.domain.model.Position
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * AI 攻防测试：立即取胜 / 拦截对手必胜点 / 搜索耗时。
 */
class AiTest {

    private fun boardWith(vararg stones: Pair<Int, Pair<Int, Int>>): Board {
        val board = Board(15)
        stones.forEach { (player, pos) ->
            val color = if (player == 1) Player.BLACK else Player.WHITE
            assertTrue(board.place(pos.first, pos.second, color))
        }
        return board
    }

    @Test
    fun aiTakesImmediateWin() {
        // 白四连 (7,5)-(7,8)，两端 (7,4)/(7,9) 均可成五
        val board = boardWith(
            2 to (7 to 5), 2 to (7 to 6), 2 to (7 to 7), 2 to (7 to 8),
            1 to (5 to 7), 1 to (6 to 8)
        )
        val move = AiPlayer.chooseMove(board, Player.WHITE, AiDifficulty.EASY)
        assertNotNull(move)
        assertTrue(
            "应直接成五，实际 $move",
            move == Position(7, 4) || move == Position(7, 9)
        )
    }

    @Test
    fun beginnerBlocksOpponentWin() {
        // 黑四连，白（入门）必须拦截
        val board = boardWith(
            1 to (7 to 5), 1 to (7 to 6), 1 to (7 to 7), 1 to (7 to 8),
            2 to (5 to 5), 2 to (6 to 6)
        )
        val move = AiPlayer.chooseMove(board, Player.WHITE, AiDifficulty.BEGINNER)
        assertNotNull(move)
        assertTrue(
            "应拦截黑方成五点，实际 $move",
            move == Position(7, 4) || move == Position(7, 9)
        )
    }

    @Test
    fun searchBlocksOpenFour() {
        // 黑活三 (7,6)-(7,8)（两端空），白用普通搜索不应让黑轻松成活四后取胜：
        // 白直接成五点不存在，白应落 (7,5) 或 (7,9) 或 (7,4)/(7,10) 附近防守
        val board = boardWith(
            1 to (7 to 6), 1 to (7 to 7), 1 to (7 to 8),
            2 to (5 to 5), 2 to (6 to 6)
        )
        val move = AiPlayer.chooseMove(board, Player.WHITE, AiDifficulty.NORMAL)
        assertNotNull(move)
        assertTrue(
            "应沿行防守或抢占要点，实际 $move",
            move!!.row == 7 && move.column in 4..10
        )
    }

    @Test(timeout = 8000L)
    fun normalSearchIsFast() {
        val board = boardWith(
            1 to (7 to 7), 2 to (7 to 8), 1 to (8 to 8)
        )
        val start = System.nanoTime()
        val move = AiPlayer.chooseMove(board, Player.WHITE, AiDifficulty.NORMAL)
        val elapsedMs = (System.nanoTime() - start) / 1_000_000L
        assertNotNull(move)
        assertTrue("NORMAL 搜索耗时 ${elapsedMs}ms，应在 6s 内", elapsedMs < 6000L)
    }

    @Test(timeout = 10000L)
    fun hardSearchRespectsTimeBudget() {
        val board = boardWith(
            1 to (7 to 7), 2 to (8 to 8), 1 to (7 to 9), 2 to (8 to 6)
        )
        val start = System.nanoTime()
        val move = AiPlayer.chooseMove(
            board, Player.WHITE, AiDifficulty.HARD,
            thinkTimeOverrideMs = 3000L
        )
        val elapsedMs = (System.nanoTime() - start) / 1_000_000L
        assertNotNull(move)
        assertTrue("HARD 搜索耗时 ${elapsedMs}ms，应在 5s 内", elapsedMs < 5000L)
    }

    @Test(timeout = 8000L)
    fun customConfigIsHonored() {
        val board = boardWith(1 to (7 to 7))
        val move = AiPlayer.chooseMove(
            board, Player.WHITE, AiDifficulty.CUSTOM,
            custom = CustomAiConfig(depth = 2, thinkTimeMs = 800, randomness = 0f)
        )
        assertNotNull(move)
    }

    @Test
    fun emptyBoardCenterFirst() {
        val board = Board(15)
        val move = AiPlayer.chooseMove(board, Player.BLACK, AiDifficulty.EASY)
        assertEquals(Position(7, 7), move)
    }
}
