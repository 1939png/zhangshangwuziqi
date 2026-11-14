package com.example.watchgomoku.domain.ai

import com.example.watchgomoku.domain.engine.Board
import com.example.watchgomoku.domain.engine.GomokuRules
import com.example.watchgomoku.domain.model.AiDifficulty
import com.example.watchgomoku.domain.model.CustomAiConfig
import com.example.watchgomoku.domain.model.Player
import com.example.watchgomoku.domain.model.Position
import kotlin.random.Random

/**
 * AI 对弈入口：按难度选择策略。
 * 入门/简单：取胜点 → 必防点 → 启发式随机；
 * 普通/困难/专家/自定义：搜索引擎（深度、时间、候选点宽度随难度递增）。
 */
object AiPlayer {

    private val random = Random(System.nanoTime())

    fun chooseMove(
        board: Board,
        ai: Player,
        difficulty: AiDifficulty,
        custom: CustomAiConfig = CustomAiConfig(),
        thinkTimeOverrideMs: Long? = null,
        cancelled: () -> Boolean = { false }
    ): Position? {
        // 搜索会临时改写棋盘：一律在副本上进行
        val work = board.copy()
        return when (difficulty) {
            AiDifficulty.BEGINNER -> basicMove(work, ai, wideRandom = true, cancelled)
            AiDifficulty.EASY -> basicMove(work, ai, wideRandom = false, cancelled)
            AiDifficulty.NORMAL ->
                SearchEngine(work, 2, 1_200, 24, randomness = 0.05f).search(ai, cancelled)
            AiDifficulty.HARD ->
                SearchEngine(
                    work, 4,
                    minOf(3_000L, thinkTimeOverrideMs ?: 3_000L), 14
                ).search(ai, cancelled)
            AiDifficulty.EXPERT ->
                SearchEngine(work, 8, thinkTimeOverrideMs ?: 3_000L, 12).search(ai, cancelled)
            AiDifficulty.CUSTOM ->
                SearchEngine(
                    work,
                    custom.depth.coerceIn(1, 10),
                    custom.thinkTimeMs,
                    14,
                    custom.randomness
                ).search(ai, cancelled)
        }
    }

    /** 入门 / 简单难度 */
    private fun basicMove(
        board: Board,
        ai: Player,
        wideRandom: Boolean,
        cancelled: () -> Boolean
    ): Position? {
        if (board.isFull()) return null
        // 1. 自己能立即成五 → 取胜
        GomokuRules.hasImmediateWin(board, ai)?.let { return it }
        // 2. 对手下一手能成五 → 拦截
        GomokuRules.hasImmediateWin(board, ai.opponent())?.let { return it }
        if (cancelled()) return null
        // 3. 邻域候选点
        val limit = if (wideRandom) 10 else 6
        val candidates = MoveGenerator.candidates(board, ai, 2, limit)
        if (candidates.isEmpty()) return firstEmpty(board)
        return if (wideRandom) {
            candidates[random.nextInt(candidates.size)]
        } else {
            candidates[random.nextInt(minOf(3, candidates.size))]
        }
    }

    private fun firstEmpty(board: Board): Position? {
        for (r in 0 until board.size) {
            for (c in 0 until board.size) {
                if (board.isEmpty(r, c)) return Position(r, c)
            }
        }
        return null
    }
}
