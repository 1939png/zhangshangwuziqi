package com.example.watchgomoku.domain.model

import androidx.compose.ui.graphics.Color

/**
 * 应用配色主题：十套协调色板。
 * 每套提供浅色 / 深色两种主色与容器色；背景等中性槽位保持统一，保证可读性。
 * 不影响黑白棋子含义与棋盘可读性。
 */
enum class AppColorTheme(
    val lightPrimary: Long,
    val lightContainer: Long,
    val darkPrimary: Long,
    val darkContainer: Long
) {
    /** 森林绿：自然柔和 */
    FOREST(0xFF2E7D32, 0xFFB7DFB9, 0xFF86D98C, 0xFF1E4620),

    /** 海洋蓝：清爽沉稳 */
    OCEAN(0xFF1665C0, 0xFFBBDEFB, 0xFF7FB5F5, 0xFF0D3A66),

    /** 星空紫：精致梦幻 */
    STARLIGHT(0xFF6B4FA3, 0xFFD9C9F2, 0xFFB79FE8, 0xFF3B2A66),

    /** 日落橙：温暖活泼 */
    SUNSET(0xFFD9551F, 0xFFFFD5C2, 0xFFFF9E6B, 0xFF5C2A12),

    /** 玫瑰粉：柔和轻盈 */
    ROSE(0xFFC74B7E, 0xFFF8CFDF, 0xFFF093BB, 0xFF5C1F38),

    /** 青瓷青：淡雅清透 */
    CELADON(0xFF2E8B7A, 0xFFBFE8E0, 0xFF7FD0C2, 0xFF14433A),

    /** 琥珀金：温润明亮 */
    AMBER(0xFF9C6B14, 0xFFFFE1A6, 0xFFFFCB6B, 0xFF4A3405),

    /** 樱桃红：醒目有活力 */
    CHERRY(0xFFC0392B, 0xFFF6C6C0, 0xFFF1948A, 0xFF571512),

    /** 石墨灰：简洁低调 */
    GRAPHITE(0xFF4A5A64, 0xFFCFD9DE, 0xFF9FB4BE, 0xFF232E33),

    /** 午夜蓝：深邃安静 */
    MIDNIGHT(0xFF303F9F, 0xFFC0C8F5, 0xFF9FA9E8, 0xFF1A2158);

    val lightPrimaryColor: Color get() = Color(lightPrimary)
    val lightContainerColor: Color get() = Color(lightContainer)
    val darkPrimaryColor: Color get() = Color(darkPrimary)
    val darkContainerColor: Color get() = Color(darkContainer)
}

/** 图形效果预设：精致（默认）/ 均衡 / 简约 / 自定义 */
enum class GraphicsPreset { DELICATE, BALANCED, MINIMAL, CUSTOM }

/** 页面转场风格：完整（淡入淡出+轻微位移）/ 轻柔（仅淡入淡出）/ 关闭 */
enum class TransitionStyle { FULL, SOFT, OFF }

/** 背景模糊 / 柔光等级：0 关闭，3 最高 */
object GraphicsLevels {
    const val OFF = 0
    const val LOW = 1
    const val MID = 2
    const val HIGH = 3

    fun blurDp(level: Int): Float = when (level.coerceIn(0, 3)) {
        1 -> 16f
        2 -> 28f
        3 -> 40f
        else -> 0f
    }

    /** 光晕透明度系数（乘以基准 0.35 / 0.30） */
    fun glowFactor(level: Int): Float = when (level.coerceIn(0, 3)) {
        1 -> 0.4f
        2 -> 0.7f
        3 -> 1f
        else -> 0f
    }
}
