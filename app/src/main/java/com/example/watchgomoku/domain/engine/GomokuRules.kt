package com.example.watchgomoku.domain.engine

import com.example.watchgomoku.domain.model.Player
import com.example.watchgomoku.domain.model.Position

/**
 * 五子棋规则引擎：胜负判定、长连、和棋、禁手（简化实现，默认关闭）。
 * 全部为纯函数，不依赖 UI。
 */
object GomokuRules {

    /** 四条判定方向：横、纵、主对角、反对角 */
    val DIRECTIONS: List<Pair<Int, Int>> = listOf(0 to 1, 1 to 0, 1 to 1, 1 to -1)

    const val WIN_LENGTH = 5

    /**
     * 从最后一手出发寻找获胜连线。
     * @param overlineWins 长连（>5）是否算胜；false 时只有恰好五连获胜
     * @return 获胜连子的坐标列表（至少 5 个），未获胜返回空列表
     */
    fun winningLine(
        board: Board,
        last: Position,
        player: Player,
        overlineWins: Boolean = true
    ): List<Position> {
        for ((dr, dc) in DIRECTIONS) {
            val line = ArrayList<Position>()
            line.add(last)
            // 正方向延伸
            var r = last.row + dr
            var c = last.column + dc
            while (board.inBounds(r, c) && board.get(r, c) == player.code()) {
                line.add(Position(r, c)); r += dr; c += dc
            }
            // 反方向延伸
            r = last.row - dr
            c = last.column - dc
            while (board.inBounds(r, c) && board.get(r, c) == player.code()) {
                line.add(0, Position(r, c)); r -= dr; c -= dc
            }
            if (line.size >= WIN_LENGTH && (overlineWins || line.size == WIN_LENGTH)) return line
        }
        return emptyList()
    }

    fun isWinningMove(
        board: Board,
        last: Position,
        player: Player,
        overlineWins: Boolean = true
    ): Boolean = winningLine(board, last, player, overlineWins).isNotEmpty()

    fun isBoardFull(board: Board): Boolean = board.isFull()

    /** 和棋：棋盘填满且无人获胜 */
    fun isDraw(board: Board): Boolean = board.isFull()

    /**
     * 遍历全部空点，寻找 player 下一手即可成五的位置（立即取胜点 / 必防点）。
     */
    fun hasImmediateWin(board: Board, player: Player): Position? {
        for (row in 0 until board.size) {
            for (col in 0 until board.size) {
                if (!board.isEmpty(row, col)) continue
                for ((dr, dc) in DIRECTIONS) {
                    val (run, _) = placementRunInfo(board, row, col, player, dr, dc)
                    if (run >= WIN_LENGTH) return Position(row, col)
                }
            }
        }
        return null
    }

    /**
     * 假设在 (row, col) 落子后，沿方向 (dr, dc) 的连子长度与开放端数量。
     * 不修改棋盘。
     */
    fun placementRunInfo(
        board: Board,
        row: Int,
        col: Int,
        player: Player,
        dr: Int,
        dc: Int
    ): Pair<Int, Int> {
        val code = player.code()
        var run = 1
        var r = row + dr
        var c = col + dc
        while (board.inBounds(r, c) && board.get(r, c) == code) {
            run++; r += dr; c += dc
        }
        val forwardOpen = board.inBounds(r, c) && board.get(r, c) == Board.EMPTY
        r = row - dr
        c = col - dc
        while (board.inBounds(r, c) && board.get(r, c) == code) {
            run++; r -= dr; c -= dc
        }
        val backwardOpen = board.inBounds(r, c) && board.get(r, c) == Board.EMPTY
        val openEnds = (if (forwardOpen) 1 else 0) + (if (backwardOpen) 1 else 0)
        return run to openEnds
    }

    /**
     * 禁手判定（简化实现，供高级设置预留，默认关闭）：
     * 仅统计“连续棋型”，不识别跳三/跳四等断连棋型。
     * 黑方落子若同时形成 ≥2 个活三 → 三三禁手；同时形成 ≥2 个四 → 四四禁手；
     * 若该点直接成五则不算禁手。
     */
    fun isForbidden(
        board: Board,
        pos: Position,
        player: Player,
        forbidDoubleThree: Boolean,
        forbidDoubleFour: Boolean,
        @Suppress("UNUSED_PARAMETER") overlineWins: Boolean = true
    ): Boolean {
        if (!board.inBounds(pos.row, pos.column) || !board.isEmpty(pos.row, pos.column)) return false
        if (player != Player.BLACK) return false
        if (!forbidDoubleThree && !forbidDoubleFour) return false
        var wouldWin = false
        var fours = 0
        var openThrees = 0
        for ((dr, dc) in DIRECTIONS) {
            val (run, openEnds) = placementRunInfo(board, pos.row, pos.column, player, dr, dc)
            when {
                run >= WIN_LENGTH -> wouldWin = true
                run == 4 && openEnds >= 1 -> fours++
                run == 3 && openEnds == 2 -> openThrees++
            }
        }
        if (wouldWin) return false
        return (forbidDoubleFour && fours >= 2) || (forbidDoubleThree && openThrees >= 2)
    }
}
