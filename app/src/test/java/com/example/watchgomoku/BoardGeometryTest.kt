package com.example.watchgomoku

import com.example.watchgomoku.util.BoardGeometry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 棋盘几何测试：坐标转换、缩放后的落点计算、边界吸附。
 */
class BoardGeometryTest {

    @Test
    fun standardLayoutHasOneCellMargin() {
        val layout = BoardGeometry.standardLayout(15, 480f)
        assertEquals(30f, layout.cellSize, 0.001f)
        assertEquals(30f, layout.originX, 0.001f)
        assertEquals(30f, layout.originY, 0.001f)
    }

    @Test
    fun cellCenterMatchesOrigin() {
        val layout = BoardGeometry.standardLayout(15, 480f)
        val (x, y) = BoardGeometry.cellCenter(0, 0, layout)
        assertEquals(30f, x, 0.001f)
        assertEquals(30f, y, 0.001f)
        val (x9, y9) = BoardGeometry.cellCenter(8, 8, layout)
        assertEquals(30f + 8 * 30f, x9, 0.001f)
        assertEquals(30f + 8 * 30f, y9, 0.001f)
    }

    @Test
    fun cellAtSnapsToNearestIntersection() {
        val layout = BoardGeometry.standardLayout(15, 480f)
        // 精确交点
        assertEquals(0 to 0, BoardGeometry.cellAt(30f, 30f, layout)?.let { it.row to it.column })
        // 半格内吸附
        val cell = BoardGeometry.cellAt(44f, 44f, layout)
        assertEquals(0, cell?.row)
        assertEquals(0, cell?.column)
        val snapped = BoardGeometry.cellAt(46f, 46f, layout)
        assertEquals(1, snapped?.row)
        assertEquals(1, snapped?.column)
    }

    @Test
    fun cellAtRejectsOutsideBoard() {
        val layout = BoardGeometry.standardLayout(15, 480f)
        assertNull(BoardGeometry.cellAt(-5f, 30f, layout))
        assertNull(BoardGeometry.cellAt(30f, 470f, layout)) // 最后一行是 450
        assertNull(BoardGeometry.cellAt(470f, 30f, layout))
    }

    @Test
    fun screenToLocalWithZoomAndOffset() {
        val (localX, localY) = BoardGeometry.screenToBoardLocal(
            screenX = 150f, screenY = 250f,
            scale = 2f, offsetX = 50f, offsetY = 50f
        )
        assertEquals(50f, localX, 0.001f)
        assertEquals(100f, localY, 0.001f)
    }

    @Test
    fun zoomedTapMapsToCorrectCell() {
        // 480px 视口，放大 2 倍，缩放中心附近点击
        val layout0 = BoardGeometry.standardLayout(15, 480f)
        // 屏幕点 (270,270)：local = (270-90)/2 = 90 → 交叉点 (2,2)（origin 30 + 2*30 = 90）
        val scale = 2f
        val offsetX = 90f
        val (localX, localY) = BoardGeometry.screenToBoardLocal(270f, 270f, scale, offsetX, offsetX)
        val layout = BoardGeometry.standardLayout(15, 480f)
        val cell = BoardGeometry.cellAt(localX, localY, layout)
        assertEquals(2, cell?.row)
        assertEquals(2, cell?.column)
        // 反向验证：交点中心绘制位置
        val (px, py) = BoardGeometry.cellCenter(cell!!.row, cell.column, layout0)
        assertEquals(90f, px, 0.001f)
        assertEquals(90f, py, 0.001f)
    }

    @Test
    fun starPointsForStandardSizes() {
        assertTrue(BoardGeometry.starPoints(15).contains(
            com.example.watchgomoku.domain.model.Position(7, 7)
        ))
        assertTrue(BoardGeometry.starPoints(9).contains(
            com.example.watchgomoku.domain.model.Position(4, 4)
        ))
        assertTrue(BoardGeometry.starPoints(19).contains(
            com.example.watchgomoku.domain.model.Position(3, 3)
        ))
        assertTrue(BoardGeometry.starPoints(5).isEmpty())
    }
}
