package com.example.watchgomoku.domain.ai

import com.example.watchgomoku.domain.engine.Board
import com.example.watchgomoku.domain.model.Player

/**
 * 局面评估器：按提示词推荐分值对棋型评分。
 * 评分口径：
 *   五连 10000000 / 活四 1000000 / 冲四 100000 / 活三 10000 /
 *   眠三 1000 / 活二 500 / 眠二 100 / 单子 10
 *
 * 简化说明：仅统计连续棋型（不识别 P.PP 等断连棋型），
 * 战术上由搜索层 + 立即取胜/必防点检测兜底。
 */
object Evaluator {

    const val SCORE_FIVE = 10_000_000L
    const val SCORE_OPEN_FOUR = 1_000_000L
    const val SCORE_FOUR = 100_000L
    const val SCORE_OPEN_THREE = 10_000L
    const val SCORE_SLEEP_THREE = 1_000L
    const val SCORE_OPEN_TWO = 500L
    const val SCORE_SLEEP_TWO = 100L
    const val SCORE_SINGLE = 10L

    private const val ME = 'X'
    private const val OPP = 'O'
    private const val GAP = '.'

    /** 评估局面（以 forPlayer 视角），防守权重 0.9 */
    fun evaluate(board: Board, forPlayer: Player): Long {
        val mine = evaluateSide(board, forPlayer)
        val theirs = evaluateSide(board, forPlayer.opponent())
        return mine - theirs * 9L / 10L
    }

    /** 单侧棋型总分 */
    fun evaluateSide(board: Board, player: Player): Long {
        val meCode = player.code()
        val n = board.size
        var total = 0L
        for (r in 0 until n) total += scoreLine(rowLine(board, r, meCode))
        for (c in 0 until n) total += scoreLine(colLine(board, c, meCode))
        for (d in -(n - 1)..(n - 1)) total += scoreLine(diagLine(board, d, meCode))
        for (s in 0..(2 * n - 2)) total += scoreLine(antiDiagLine(board, s, meCode))
        return total
    }

    private fun cellChar(value: Int, meCode: Int): Char = when (value) {
        Board.EMPTY -> GAP
        meCode -> ME
        else -> OPP
    }

    private fun rowLine(board: Board, row: Int, meCode: Int): CharArray {
        val n = board.size
        return CharArray(n) { c -> cellChar(board.get(row, c), meCode) }
    }

    private fun colLine(board: Board, col: Int, meCode: Int): CharArray {
        val n = board.size
        return CharArray(n) { r -> cellChar(board.get(r, col), meCode) }
    }

    /** 主对角线（row - column = d） */
    private fun diagLine(board: Board, d: Int, meCode: Int): CharArray {
        val n = board.size
        val rStart = maxOf(0, -d)
        val rEnd = minOf(n - 1, n - 1 - d)
        val arr = CharArray(maxOf(0, rEnd - rStart + 1))
        var i = 0
        for (r in rStart..rEnd) arr[i++] = cellChar(board.get(r, r + d), meCode)
        return arr
    }

    /** 反对角线（row + column = s） */
    private fun antiDiagLine(board: Board, s: Int, meCode: Int): CharArray {
        val n = board.size
        val rStart = maxOf(0, s - (n - 1))
        val rEnd = minOf(n - 1, s)
        val arr = CharArray(maxOf(0, rEnd - rStart + 1))
        var i = 0
        for (r in rStart..rEnd) arr[i++] = cellChar(board.get(r, s - r), meCode)
        return arr
    }

    /** 扫描一条线，按“连子长度 + 开放端数量”分类计分 */
    private fun scoreLine(line: CharArray): Long {
        var score = 0L
        var i = 0
        val n = line.size
        while (i < n) {
            if (line[i] != ME) {
                i++
                continue
            }
            var j = i
            while (j < n && line[j] == ME) j++
            val run = j - i
            val leftOpen = i > 0 && line[i - 1] == GAP
            val rightOpen = j < n && line[j] == GAP
            val openEnds = (if (leftOpen) 1 else 0) + (if (rightOpen) 1 else 0)
            score += when {
                run >= 5 -> SCORE_FIVE
                run == 4 -> when (openEnds) {
                    2 -> SCORE_OPEN_FOUR
                    1 -> SCORE_FOUR
                    else -> 0L
                }
                run == 3 -> when (openEnds) {
                    2 -> SCORE_OPEN_THREE
                    1 -> SCORE_SLEEP_THREE
                    else -> 0L
                }
                run == 2 -> when (openEnds) {
                    2 -> SCORE_OPEN_TWO
                    1 -> SCORE_SLEEP_TWO
                    else -> 0L
                }
                else -> if (openEnds > 0) SCORE_SINGLE else 0L
            }
            i = j
        }
        return score
    }
}
