package com.example.watchgomoku.presentation.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 棋盘视图状态：缩放 + 平移。
 * - 缩放范围 1.0~3.0；
 * - 以手指中点（或视口中心）为缩放中心，棋子位置保持稳定；
 * - 平移时棋盘不能完全拖出屏幕（至少覆盖视口）。
 * 使用 Compose 状态：变化时仅重组棋盘绘制层，不影响页面其他区域。
 */
class BoardViewState {

    var scale by mutableStateOf(1f)
        private set
    var offsetX by mutableStateOf(0f)
        private set
    var offsetY by mutableStateOf(0f)
        private set

    /** 从存档恢复 */
    fun load(scale: Float, offsetX: Float, offsetY: Float) {
        this.scale = scale.coerceIn(MIN_SCALE, MAX_SCALE)
        this.offsetX = offsetX
        this.offsetY = offsetY
    }

    /** 双指手势：以手指中点为缩放中心，并叠加平移 */
    fun applyTransform(
        centroidX: Float,
        centroidY: Float,
        panX: Float,
        panY: Float,
        zoom: Float,
        viewportPx: Float
    ) {
        if (zoom != 1f) {
            val newScale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
            zoomAround(centroidX, centroidY, newScale)
        }
        offsetX += panX
        offsetY += panY
        clamp(viewportPx)
    }

    /** 按钮缩放：以视口中心为中心 */
    fun zoomBy(factor: Float, viewportPx: Float) {
        val newScale = (scale * factor).coerceIn(MIN_SCALE, MAX_SCALE)
        zoomAround(viewportPx / 2f, viewportPx / 2f, newScale)
        clamp(viewportPx)
    }

    /** 适应屏幕：重置缩放与偏移 */
    fun fit() {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }

    /** 居中：保持当前缩放，平移回中心 */
    fun center(viewportPx: Float) {
        offsetX = (viewportPx - viewportPx * scale) / 2f
        offsetY = (viewportPx - viewportPx * scale) / 2f
        clamp(viewportPx)
    }

    /** 布局首帧后校正存档偏移 */
    fun clampTo(viewportPx: Float) {
        clamp(viewportPx)
    }

    private fun zoomAround(cx: Float, cy: Float, newScale: Float) {
        val boardX = (cx - offsetX) / scale
        val boardY = (cy - offsetY) / scale
        offsetX = cx - boardX * newScale
        offsetY = cy - boardY * newScale
        scale = newScale
    }

    private fun clamp(viewportPx: Float) {
        if (viewportPx <= 0f) return
        val content = viewportPx * scale
        offsetX = if (content <= viewportPx) {
            (viewportPx - content) / 2f
        } else {
            offsetX.coerceIn(viewportPx - content, 0f)
        }
        offsetY = if (content <= viewportPx) {
            (viewportPx - content) / 2f
        } else {
            offsetY.coerceIn(viewportPx - content, 0f)
        }
    }

    companion object {
        const val MIN_SCALE = 1f
        const val MAX_SCALE = 3f
    }
}
