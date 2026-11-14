package com.example.watchgomoku.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 棋盘调色板：木纹 / 深色 / 高对比三种主题。
 * 黑白棋子除颜色外还依赖描边与外圈标记区分（无障碍要求：不只依赖颜色）。
 */
data class BoardPalette(
    val boardBackground: Color,
    val boardBorder: Color,
    val gridLine: Color,
    val starPoint: Color,
    val blackStone: Color,
    val blackStoneOutline: Color,
    val whiteStone: Color,
    val whiteStoneOutline: Color,
    val stoneShadow: Color,
    val lastMoveMarker: Color,
    val coordinateText: Color,
    val winLine: Color,
    val hintText: Color
)

object BoardPalettes {

    fun of(theme: com.example.watchgomoku.domain.model.BoardTheme): BoardPalette =
        when (theme) {
            com.example.watchgomoku.domain.model.BoardTheme.WOOD -> WOOD
            com.example.watchgomoku.domain.model.BoardTheme.DARK -> DARK
            com.example.watchgomoku.domain.model.BoardTheme.HIGH_CONTRAST -> HIGH_CONTRAST
        }

    /** 木纹：浅棕背景 + 深棕棋盘线 + 黑白棋子 */
    val WOOD = BoardPalette(
        boardBackground = Color(0xFFE3B874),
        boardBorder = Color(0xFF8A5A2B),
        gridLine = Color(0xFF6B4423),
        starPoint = Color(0xFF5D4037),
        blackStone = Color(0xFF17140F),
        blackStoneOutline = Color.Transparent,
        whiteStone = Color(0xFFFBF7EF),
        whiteStoneOutline = Color(0x66432B14),
        stoneShadow = Color(0x33000000),
        lastMoveMarker = Color(0xFFD32F2F),
        coordinateText = Color(0xFF6D4C41),
        winLine = Color(0xFFD32F2F),
        hintText = Color(0xFF1565C0)
    )

    /** 深色：深灰背景 + 浅灰棋盘线 + 高对比黑白棋子 */
    val DARK = BoardPalette(
        boardBackground = Color(0xFF232227),
        boardBorder = Color(0xFF4A4950),
        gridLine = Color(0xFF9A99A2),
        starPoint = Color(0xFFC7C7CC),
        blackStone = Color(0xFF101014),
        blackStoneOutline = Color(0xFF57565E),
        whiteStone = Color(0xFFF4F4F6),
        whiteStoneOutline = Color(0xFF88878F),
        stoneShadow = Color(0x55000000),
        lastMoveMarker = Color(0xFFFFD54F),
        coordinateText = Color(0xFFA5A4AC),
        winLine = Color(0xFFFF7043),
        hintText = Color(0xFF64B5F6)
    )

    /** 高对比：纯黑背景 + 亮黄棋盘线 + 亮色标记 */
    val HIGH_CONTRAST = BoardPalette(
        boardBackground = Color(0xFF000000),
        boardBorder = Color(0xFFFFE100),
        gridLine = Color(0xFFFFE100),
        starPoint = Color(0xFFFFFFFF),
        blackStone = Color(0xFF000000),
        blackStoneOutline = Color(0xFFFFFFFF),
        whiteStone = Color(0xFFFFFFFF),
        whiteStoneOutline = Color(0xFF9E9E9E),
        stoneShadow = Color(0x00000000),
        lastMoveMarker = Color(0xFF00E5FF),
        coordinateText = Color(0xFFFFFFFF),
        winLine = Color(0xFF00E5FF),
        hintText = Color(0xFF00E5FF)
    )
}
