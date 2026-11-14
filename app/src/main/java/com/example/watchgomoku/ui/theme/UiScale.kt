package com.example.watchgomoku.ui.theme

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 全局文字缩放：由设置页滑块驱动（0.8~1.5），
 * 在 GomokuTheme 中写入 LocalDensity.fontScale 实现全应用 sp 文字统一缩放；
 * 需要随文字放大的按钮高度等尺寸通过 [scaledDp] 计算避免裁切。
 */
val LocalUiFontScale = staticCompositionLocalOf { 1f }

/**
 * 手表圆角安全区域参数：
 * 内缩量按屏幕宽度比例动态计算，不写死固定像素，
 * 适配 240~390 等方形屏幕与不同圆角半径。
 */
@Immutable
data class WatchSafeArea(
    val top: Dp,
    val bottom: Dp,
    val start: Dp,
    val end: Dp,
    val cornerInset: Dp
)

/**
 * 圆角安全区：内容区域 = 屏幕区域 - 系统内边距 - 圆角安全边距。
 * cornerInset ≈ 屏幕宽度的 6%（240 屏 ≈ 14dp，390 屏 ≈ 23dp）。
 */
@Composable
fun rememberWatchSafeArea(): WatchSafeArea {
    val widthDp = LocalConfiguration.current.screenWidthDp.coerceAtLeast(1)
    val inset = (widthDp * 0.06f).dp
    return WatchSafeArea(
        top = inset,
        bottom = inset,
        start = inset,
        end = inset,
        cornerInset = inset
    )
}

/**
 * 按当前安全区域应用内边距：取「调用方期望值」与「圆角内缩」的较大者，
 * 保证按钮与文字不会贴近圆角被裁切。
 */
fun Modifier.watchSafePadding(horizontal: Dp = 8.dp, vertical: Dp = 4.dp): Modifier = composed {
    val safe = rememberWatchSafeArea()
    padding(
        horizontal = maxOf(horizontal.value, safe.cornerInset.value).dp,
        vertical = maxOf(vertical.value, safe.cornerInset.value).dp
    )
}

/** 当前是否处于高对比模式：光晕等装饰按需减弱，保证极简高对比视觉 */
val LocalHighContrast = staticCompositionLocalOf { false }

/** 基准 dp 随全局文字缩放（用于按钮高度等关键尺寸，防止大字号被裁切） */
@Composable
fun scaledDp(base: Dp): Dp =
    (base.value * LocalUiFontScale.current).dp

/**
 * 紧凑布局判定：小屏幕（<250dp 宽）或文字放大 ≥130% 时启用——
 * 隐藏次要文字、按钮只保留图标，保证核心操作不溢出。
 */
@Composable
fun isCompactLayout(): Boolean {
    val widthDp = LocalConfiguration.current.screenWidthDp
    return widthDp < 250 || LocalUiFontScale.current >= 1.3f
}
