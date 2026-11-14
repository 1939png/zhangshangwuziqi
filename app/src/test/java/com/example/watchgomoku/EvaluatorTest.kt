package com.example.watchgomoku

import com.example.watchgomoku.domain.ai.Evaluator
import com.example.watchgomoku.domain.engine.Board
import com.example.watchgomoku.domain.model.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 棋型评分测试：分值表与大小关系。
 */
class EvaluatorTest {

    @Test
    fun patternScoresFollowSpecTable() {
        assertEquals(10_000_000L, Evaluator.SCORE_FIVE)
        assertEquals(1_000_000L, Evaluator.SCORE_OPEN_FOUR)
        assertEquals(100_000L, Evaluator.SCORE_FOUR)
        assertEquals(10_000L, Evaluator.SCORE_OPEN_THREE)
        assertEquals(1_000L, Evaluator.SCORE_SLEEP_THREE)
        assertEquals(500L, Evaluator.SCORE_OPEN_TWO)
        assertEquals(100L, Evaluator.SCORE_SLEEP_TWO)
        assertEquals(10L, Evaluator.SCORE_SINGLE)
    }

    @Test
    fun openThreeScoresAboveOpenTwo() {
        val three = Board(15)
        three.place(7, 5, Player.BLACK)
        three.place(7, 6, Player.BLACK)
        three.place(7, 7, Player.BLACK)
        val two = Board(15)
        two.place(7, 6, Player.BLACK)
        two.place(7, 7, Player.BLACK)
        val threeScore = Evaluator.evaluateSide(three, Player.BLACK)
        val twoScore = Evaluator.evaluateSide(two, Player.BLACK)
        assertTrue("活三($threeScore) 应高于活二($twoScore)", threeScore > twoScore)
    }

    @Test
    fun fiveScoresHighest() {
        val five = Board(15)
        for (c in 3..7) five.place(7, c, Player.BLACK)
        val four = Board(15)
        for (c in 3..6) four.place(7, c, Player.BLACK)
        assertTrue(
            Evaluator.evaluateSide(five, Player.BLACK) >
                Evaluator.evaluateSide(four, Player.BLACK)
        )
    }

    @Test
    fun emptyBoardScoresZero() {
        val board = Board(15)
        assertEquals(0L, Evaluator.evaluateSide(board, Player.BLACK))
    }

    @Test
    fun evaluateIsSymmetricBetweenSides() {
        val board = Board(15)
        board.place(7, 7, Player.BLACK)
        val forBlack = Evaluator.evaluate(board, Player.BLACK)
        val forWhite = Evaluator.evaluate(board, Player.WHITE)
        assertTrue("黑方视角($forBlack) 应优于白方视角($forWhite)", forBlack > forWhite)
    }
}
