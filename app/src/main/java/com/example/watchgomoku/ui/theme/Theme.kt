package com.example.watchgomoku.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.example.watchgomoku.domain.model.AppColorTheme
import com.example.watchgomoku.domain.model.ThemeMode

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6D4C41),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFCCBC),
    onPrimaryContainer = Color(0xFF3E2723),
    secondary = Color(0xFF5D4037),
    onSecondary = Color.White,
    background = Color(0xFFFAF6F0),
    onBackground = Color(0xFF201A17),
    surface = Color(0xFFFAF6F0),
    onSurface = Color(0xFF201A17),
    surfaceVariant = Color(0xFFEFE3D5),
    onSurfaceVariant = Color(0xFF4F443C),
    outline = Color(0xFF84756A)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB59A),
    onPrimary = Color(0xFF42150A),
    primaryContainer = Color(0xFF5D4037),
    onPrimaryContainer = Color(0xFFFFCCBC),
    secondary = Color(0xFFE0BFB2),
    onSecondary = Color(0xFF3B2B24),
    background = Color(0xFF121212),
    onBackground = Color(0xFFEDE0DA),
    surface = Color(0xFF181818),
    onSurface = Color(0xFFEDE0DA),
    surfaceVariant = Color(0xFF2A2523),
    onSurfaceVariant = Color(0xFFD3C4BC),
    outline = Color(0xFF9F8F84)
)

private val HighContrastColorScheme = darkColorScheme(
    primary = Color(0xFFFFE100),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF1C1C00),
    onPrimaryContainer = Color(0xFFFFE100),
    secondary = Color(0xFF00E5FF),
    onSecondary = Color.Black,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color(0xFF0A0A0A),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF151515),
    onSurfaceVariant = Color(0xFFE0E0E0),
    outline = Color.White
)

/** 中性基底色板套用主题主色：只替换品牌色槽位，背景等中性色保持统一可读 */
private fun androidx.compose.material3.ColorScheme.forTheme(
    t: AppColorTheme,
    dark: Boolean
): androidx.compose.material3.ColorScheme = if (!dark) copy(
    primary = t.lightPrimaryColor,
    onPrimary = Color.White,
    primaryContainer = t.lightContainerColor,
    onPrimaryContainer = Color(0xFF1A1A1A),
    secondary = t.darkPrimaryColor,
    onSecondary = Color(0xFF141414)
) else copy(
    primary = t.darkPrimaryColor,
    onPrimary = Color(0xFF101010),
    primaryContainer = t.darkContainerColor,
    onPrimaryContainer = Color(0xFFEFEFEF),
    secondary = t.lightPrimaryColor,
    onSecondary = Color.White
)

/**
 * 应用主题：深浅模式跟随设置；高对比模式强制深色高对比配色；
 * fontScale（0.8~1.5）通过 LocalDensity.fontScale 全局缩放所有 sp 文字，
 * 并经 LocalUiFontScale 供按钮高度等尺寸自适应，调整后立即生效无需重启。
 */
@Composable
fun GomokuTheme(
    themeMode: ThemeMode,
    highContrast: Boolean,
    fontScale: Float = 1.0f,
    colorTheme: AppColorTheme = AppColorTheme.AMBER,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    } || highContrast
    val colors = when {
        highContrast -> HighContrastColorScheme
        dark -> DarkColorScheme.forTheme(colorTheme, dark = true)
        else -> LightColorScheme.forTheme(colorTheme, dark = false)
    }
    val base = LocalDensity.current
    // 只缩放字体（fontScale），不放大 dp，避免布局被等比挤压
    val scaledDensity = Density(
        density = base.density,
        fontScale = (base.fontScale * fontScale).coerceIn(0.8f, 2.5f)
    )
    CompositionLocalProvider(
        LocalDensity provides scaledDensity,
        LocalUiFontScale provides fontScale,
        LocalHighContrast provides highContrast
    ) {
        MaterialTheme(
            colorScheme = colors,
            content = content
        )
    }
}
