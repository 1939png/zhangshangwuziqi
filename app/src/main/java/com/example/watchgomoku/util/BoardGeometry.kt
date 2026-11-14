package com.example.watchgomoku.util

import com.example.watchgomoku.domain.model.Position

/**
 * 棋盘几何：绘制坐标与触摸坐标的统一换算。
 * 纯数学函数，便于单元测试（含缩放/偏移后的落点计算）。
 */
object BoardGeometry {

    /**
     * 棋盘局部坐标系布局：
     * - 局部坐标 = (屏幕坐标 - 偏移量) / 缩放比例；
     * - 交叉点 (row, col) 位于 (originX + col * cellSize, originY + row * cellSize)。
     */
    data class Layout(
        val boardSize: Int,
        val originX: Float,
        val originY: Float,
        val cellSize: Float
    )

    /** 标准布局：四周留白一格 */
    fun standardLayout(boardSize: Int, boardPixel: Float): Layout {
        val cell = boardPixel / (boardSize + 1)
        return Layout(boardSize, cell, cell, cell)
    }

    fun cellCenter(row: Int, column: Int, layout: Layout): Pair<Float, Float> =
        Pair(
            layout.originX + column * layout.cellSize,
            layout.originY + row * layout.cellSize
        )

    /**
     * 局部坐标 → 最近交叉点；越界返回 null（边界自动吸附到最近交叉点）。
     */
    fun cellAt(localX: Float, localY: Float, layout: Layout): Position? {
        if (layout.cellSize <= 0f) return null
        // floor(x + 0.5)：负坐标时也能正确四舍五入（toInt() 会向零截断）
        val column = kotlin.math.floor((localX - layout.originX) / layout.cellSize + 0.5f).toInt()
        val row = kotlin.math.floor((localY - layout.originY) / layout.cellSize + 0.5f).toInt()
        if (row !in 0 until layout.boardSize || column !in 0 until layout.boardSize) return null
        return Position(row, column)
    }

    /** 屏幕坐标 → 棋盘局部坐标（考虑缩放与偏移） */
    fun screenToBoardLocal(
        screenX: Float,
        screenY: Float,
        scale: Float,
        offsetX: Float,
        offsetY: Float
    ): Pair<Float, Float> =
        Pair((screenX - offsetX) / scale, (screenY - offsetY) / scale)

    /**
     * 星位：9/13/15/19 路常用星位集合。
     */
    fun starPoints(boardSize: Int): List<Position> {
        if (boardSize < 7) return emptyList()
        val edge = if (boardSize >= 13) 3 else 2
        val mid = boardSize / 2
        val far = boardSize - 1 - edge
        val indices = linkedSetOf(edge, mid, far)
        return indices.flatMap { r -> indices.map { c -> Position(r, c) } }
    }

    fun round(value: Float): Int = kotlin.math.floor(value + 0.5f).toInt()
}
