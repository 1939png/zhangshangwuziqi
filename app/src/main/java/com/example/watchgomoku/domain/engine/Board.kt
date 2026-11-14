package com.example.watchgomoku.domain.engine

import com.example.watchgomoku.domain.model.CellState
import com.example.watchgomoku.domain.model.Move
import com.example.watchgomoku.domain.model.Player
import com.example.watchgomoku.domain.model.Position

/**
 * 可变棋盘：领域引擎内部数据结构。
 * 内部使用一维 IntArray 存储（0 空 / 1 黑 / 2 白），并提供落子栈支持撤销与回放。
 */
class Board(val size: Int) {

    private val cells = IntArray(size * size)
    private val stack = ArrayList<Move>()

    fun get(row: Int, column: Int): Int =
        if (inBounds(row, column)) cells[row * size + column] else OUT_OF_BOARD

    fun inBounds(row: Int, column: Int): Boolean =
        row in 0 until size && column in 0 until size

    fun isEmpty(row: Int, column: Int): Boolean = get(row, column) == EMPTY

    /**
     * 落子。仅允许落在界内空点上；重复/越界一律返回 false。
     */
    fun place(row: Int, column: Int, player: Player): Boolean {
        if (!inBounds(row, column) || !isEmpty(row, column)) return false
        cells[row * size + column] = player.code()
        stack.add(Move(Position(row, column), player, stack.size + 1))
        return true
    }

    fun canUndo(): Boolean = stack.isNotEmpty()

    fun undoLast(): Move? {
        if (stack.isEmpty()) return null
        val last = stack.removeAt(stack.size - 1)
        cells[last.position.row * size + last.position.column] = EMPTY
        return last
    }

    fun lastMove(): Move? = stack.lastOrNull()

    fun moveCount(): Int = stack.size

    fun moves(): List<Move> = stack.toList()

    fun isFull(): Boolean = stack.size == size * size

    fun clear() {
        java.util.Arrays.fill(cells, EMPTY)
        stack.clear()
    }

    fun copy(): Board {
        val other = Board(size)
        System.arraycopy(cells, 0, other.cells, 0, cells.size)
        other.stack.addAll(stack)
        return other
    }

    fun toCellStates(): Array<Array<CellState>> = Array(size) { r ->
        Array(size) { c ->
            when (get(r, c)) {
                Player.BLACK_CODE -> CellState.BLACK
                Player.WHITE_CODE -> CellState.WHITE
                else -> CellState.EMPTY
            }
        }
    }

    /** 用既有落子序列重建棋盘（存档恢复 / 测试） */
    fun replay(moves: List<Move>) {
        clear()
        moves.forEach { place(it.position.row, it.position.column, it.player) }
    }

    companion object {
        const val EMPTY = 0
        const val OUT_OF_BOARD = -1
    }
}
