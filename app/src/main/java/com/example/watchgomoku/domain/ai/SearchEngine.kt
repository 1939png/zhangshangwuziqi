package com.example.watchgomoku.domain.ai

import com.example.watchgomoku.domain.engine.Board
import com.example.watchgomoku.domain.engine.GomokuRules
import com.example.watchgomoku.domain.model.Player
import com.example.watchgomoku.domain.model.Position
import kotlin.random.Random

/**
 * 搜索引擎：负极大 Alpha-Beta + 迭代加深 + Zobrist 置换表 + 思考时间控制。
 *
 * 执行流程（对应提示词推荐）：
 * 1. 生成候选点并按启发式排序；
 * 2. 立即获胜点直接返回；
 * 3. 迭代加深（2,4,6…层），每层按上一层分数排序根节点；
 * 4. 超时 / 被取消时立即中断，返回当前已知最佳着法；
 * 5. 搜索内部对根节点的棋盘副本进行 place/undo，被中断时副本作废，不影响对局。
 *
 * 纯 Kotlin，无 Android 依赖，必须在后台协程执行。
 */
class SearchEngine(
    private val board: Board,
    private val maxDepth: Int,
    private val timeBudgetMs: Long,
    private val candidateLimit: Int,
    private val randomness: Float = 0f
) {

    private data class TtEntry(val depth: Int, val flag: Int, val score: Long) {
        companion object {
            const val FLAG_EXACT = 0
            const val FLAG_LOWER = 1
            const val FLAG_UPPER = 2
        }
    }

    private class SearchInterrupt : Exception()

    private val zobrist = ZobristTable(board.size)
    private val tt = HashMap<Long, TtEntry>()
    private val random = Random(System.nanoTime())
    private var deadlineNanos = 0L
    private var nodes = 0L
    private lateinit var isCancelled: () -> Boolean

    /**
     * 搜索 ai 的最佳着法。
     * @param cancelled 取消信号（每次检查节点/时限时调用）
     */
    fun search(ai: Player, cancelled: () -> Boolean): Position? {
        isCancelled = cancelled
        deadlineNanos = System.nanoTime() + timeBudgetMs.coerceAtLeast(MIN_TIME_MS) * 1_000_000L
        nodes = 0L
        tt.clear()
        if (board.isFull()) return null

        // 立即获胜点直接返回
        GomokuRules.hasImmediateWin(board, ai)?.let { return it }

        val root = MoveGenerator.candidates(
            board, ai, ROOT_RADIUS, maxOf(candidateLimit, ROOT_MIN_LIMIT)
        )
        if (root.isEmpty()) return null

        // 根节点启发式排序分（攻 1.0 + 防 0.8）
        val orderScores = LongArray(root.size) { idx ->
            val p = root[idx]
            MoveGenerator.pointScore(board, p.row, p.column, ai) +
                MoveGenerator.pointScore(board, p.row, p.column, ai.opponent()) * 8L / 10L
        }

        var best = root[0]
        try {
            var depth = 2
            while (depth <= maxOf(maxDepth, 2)) {
                var alpha = -INF
                var depthBest: Position? = null
                for (idx in root.indices.sortedByDescending { orderScores[it] }) {
                    val move = root[idx]
                    board.place(move.row, move.column, ai)
                    val score = if (GomokuRules.isWinningMove(board, move, ai, true)) {
                        WIN_SCORE - 1
                    } else {
                        -alphabeta(
                            depth - 1, -INF, -alpha,
                            ai.opponent(), zobrist.hash(board), 1
                        )
                    }
                    board.undoLast()
                    orderScores[idx] = score
                    if (score > alpha) {
                        alpha = score
                        depthBest = move
                    }
                    checkInterrupt()
                }
                if (depthBest != null) best = depthBest
                if (alpha >= WIN_THRESHOLD) break
                if (System.nanoTime() >= deadlineNanos) break
                depth += 2
            }
        } catch (_: SearchInterrupt) {
            // 超时或取消：返回当前最佳
        }
        return applyRandomness(best, root)
    }

    /** 小概率在候选点内随机，制造多样性（自定义难度“随机性”旋钮） */
    private fun applyRandomness(best: Position, root: List<Position>): Position {
        if (randomness <= 0f || root.size <= 1) return best
        return if (random.nextFloat() < randomness) {
            root[random.nextInt(root.size)]
        } else {
            best
        }
    }

    private fun checkInterrupt() {
        nodes++
        if (nodes and 0xFFL == 0L) {
            if (isCancelled() || System.nanoTime() >= deadlineNanos) throw SearchInterrupt()
        }
    }

    private fun alphabeta(
        depth: Int,
        alphaIn: Long,
        beta: Long,
        player: Player,
        hash: Long,
        ply: Int
    ): Long {
        checkInterrupt()
        var alpha = alphaIn
        var beta = beta

        // 置换表探查（哈希只编码棋子，需混入行棋方）
        val key = if (player == Player.BLACK) hash else hash xor TURN_KEY
        val entry = tt[key]
        if (entry != null && entry.depth >= depth) {
            when (entry.flag) {
                TtEntry.FLAG_EXACT -> return entry.score
                TtEntry.FLAG_LOWER -> if (entry.score > alpha) alpha = entry.score
                TtEntry.FLAG_UPPER -> if (entry.score < beta) beta = entry.score
            }
            if (alpha >= beta) return entry.score
        }

        if (depth == 0) return Evaluator.evaluate(board, player)

        val moves = MoveGenerator.candidates(board, player, INNER_RADIUS, candidateLimit)
        if (moves.isEmpty()) return Evaluator.evaluate(board, player)

        var bestScore = -INF
        var flag = TtEntry.FLAG_UPPER
        for (move in moves) {
            board.place(move.row, move.column, player)
            val childHash = zobrist.update(hash, move.row, move.column, player.code())
            val score = if (GomokuRules.isWinningMove(board, move, player, true)) {
                WIN_SCORE - ply
            } else {
                -alphabeta(depth - 1, -beta, -alpha, player.opponent(), childHash, ply + 1)
            }
            board.undoLast()
            if (score > bestScore) {
                bestScore = score
                if (score > alpha) {
                    alpha = score
                    flag = TtEntry.FLAG_EXACT
                }
            }
            if (alpha >= beta) {
                flag = TtEntry.FLAG_LOWER
                break
            }
        }
        if (tt.size > TT_MAX_ENTRIES) tt.clear()
        tt[key] = TtEntry(depth, flag, bestScore)
        return bestScore
    }

    companion object {
        private const val ROOT_RADIUS = 2
        private const val INNER_RADIUS = 1
        private const val ROOT_MIN_LIMIT = 8
        private const val TT_MAX_ENTRIES = 250_000
        private const val INF = 1_000_000_000L
        private const val WIN_SCORE = 9_000_000L
        private const val WIN_THRESHOLD = 8_000_000L
        private const val MIN_TIME_MS = 200L
        private val TURN_KEY = Random(0x5EED1D5EL).nextLong()
    }
}
