package com.example.watchgomoku.domain.ai

import com.example.watchgomoku.domain.engine.Board
import kotlin.random.Random

/**
 * Zobrist 哈希：增量维护局面哈希，用于置换表。
 */
class ZobristTable(private val size: Int) {

    private val table = LongArray(size * size * 2)

    init {
        val rnd = Random(size * 1_000_003L)
        for (i in table.indices) table[i] = rnd.nextLong()
    }

    /** 落子/提子后的增量哈希：code 为 1（黑）或 2（白） */
    fun update(hash: Long, row: Int, col: Int, code: Int): Long {
        val idx = (row * size + col) * 2 + (code - 1)
        return hash xor table[idx]
    }

    /** 全量计算棋盘哈希 */
    fun hash(board: Board): Long {
        var h = 0L
        for (r in 0 until size) {
            for (c in 0 until size) {
                val v = board.get(r, c)
                if (v != Board.EMPTY) {
                    h = h xor table[(r * size + c) * 2 + (v - 1)]
                }
            }
        }
        return h
    }
}
