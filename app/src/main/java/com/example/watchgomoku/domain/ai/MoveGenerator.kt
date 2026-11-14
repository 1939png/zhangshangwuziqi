package com.example.watchgomoku.domain.ai

import com.example.watchgomoku.domain.engine.Board
import com.example.watchgomoku.domain.engine.GomokuRules
import com.example.watchgomoku.domain.model.Player
import com.example.watchgomoku.domain.model.Position

/**
 * 候选点生成器：取已有棋子切比雪夫距离 radius 内的空点，
 * 按攻防启发式分排序后截取 limit 个，控制搜索宽度。
 */
object MoveGenerator {

    fun candidates(
        board: Board,
        player: Player,
        radius: Int = 2,
        limit: Int = 20
    ): List<Position> {
        if (board.isFull()) return emptyList()
        if (board.moveCount() == 0) return listOf(Position(board.size / 2, board.size / 2))

        val opponent = player.opponent()
        data class Scored(val pos: Position, val score: Long)

        val seen = HashSet<Position>()
        val list = ArrayList<Scored>()
        for (r in 0 until board.size) {
            for (c in 0 until board.size) {
                if (board.isEmpty(r, c)) continue
                for (dr in -radius..radius) {
                    for (dc in -radius..radius) {
                        val nr = r + dr
                        val nc = c + dc
                        if (!board.inBounds(nr, nc) || !board.isEmpty(nr, nc)) continue
                        val pos = Position(nr, nc)
                        if (!seen.add(pos)) continue
                        val attack = pointScore(board, nr, nc, player)
                        val defend = pointScore(board, nr, nc, opponent)
                        list.add(Scored(pos, attack + defend * 8L / 10L))
                    }
                }
            }
        }
        list.sortByDescending { it.score }
        return list.take(limit).map { it.pos }
    }

    /** 单点启发式分：假设落子后四方向棋型分之和 */
    fun pointScore(board: Board, row: Int, col: Int, player: Player): Long {
        var total = 0L
        for ((dr, dc) in GomokuRules.DIRECTIONS) {
            val (run, openEnds) = GomokuRules.placementRunInfo(board, row, col, player, dr, dc)
            total += patternScore(run, openEnds)
        }
        return total
    }

    /** 与评估器同口径的棋型分 */
    fun patternScore(run: Int, openEnds: Int): Long = when {
        run >= 5 -> Evaluator.SCORE_FIVE
        run == 4 -> when (openEnds) {
            2 -> Evaluator.SCORE_OPEN_FOUR
            1 -> Evaluator.SCORE_FOUR
            else -> 0L
        }
        run == 3 -> when (openEnds) {
            2 -> Evaluator.SCORE_OPEN_THREE
            1 -> Evaluator.SCORE_SLEEP_THREE
            else -> 0L
        }
        run == 2 -> when (openEnds) {
            2 -> Evaluator.SCORE_OPEN_TWO
            1 -> Evaluator.SCORE_SLEEP_TWO
            else -> 0L
        }
        else -> if (openEnds > 0) Evaluator.SCORE_SINGLE else 0L
    }
}
