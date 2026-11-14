package com.example.watchgomoku.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * 主页文字样式集中管理：
 * - 字号基准比旧版整体上调（标题 ×~1.3、按钮 ×~1.15、辅助 ×~1.1）；
 * - 小屏（<250dp 宽，如 240×240）自动降一档，避免溢出；
 * - 全部为 sp：随系统字体缩放与设置页“文字大小”滑块自动生效。
 */
object HomeTypography {

    /** 应用标题：视觉权重最高 */
    @Composable
    fun titleSize(): TextUnit =
        if (LocalConfiguration.current.screenWidthDp < 250) 19.sp else 22.sp

    /** 主按钮文字（不小于 14sp） */
    @Composable
    fun primaryButtonSize(): TextUnit =
        if (LocalConfiguration.current.screenWidthDp < 250) 14.sp else 16.sp

    /** 主按钮符号 */
    @Composable
    fun glyphSize(): TextUnit =
        if (LocalConfiguration.current.screenWidthDp < 250) 15.sp else 17.sp

    /** 次级按钮文字 */
    @Composable
    fun secondaryButtonSize(): TextUnit =
        if (LocalConfiguration.current.screenWidthDp < 250) 12.sp else 13.sp

    /** 辅助说明 / 空状态 */
    @Composable
    fun captionSize(): TextUnit =
        if (LocalConfiguration.current.screenWidthDp < 250) 10.sp else 11.sp
}
