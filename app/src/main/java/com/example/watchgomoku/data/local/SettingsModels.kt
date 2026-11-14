package com.example.watchgomoku.data.local

import com.example.watchgomoku.domain.model.AiDifficulty
import com.example.watchgomoku.domain.model.AppColorTheme
import com.example.watchgomoku.domain.model.BoardTheme
import com.example.watchgomoku.domain.model.FirstMovePolicy
import com.example.watchgomoku.domain.model.GraphicsPreset
import com.example.watchgomoku.domain.model.StoneSize
import com.example.watchgomoku.domain.model.ThemeMode
import com.example.watchgomoku.domain.model.TransitionStyle
import com.example.watchgomoku.domain.model.UndoQuota

/**
 * 全部用户设置的内存快照，DataStore 之外不落任何全局可变状态。
 */
data class UiSettings(
    val boardSize: Int = 15,
    val difficulty: AiDifficulty = AiDifficulty.NORMAL,
    val firstMovePolicy: FirstMovePolicy = FirstMovePolicy.PLAYER_BLACK,
    val allowUndo: Boolean = true,
    val undoQuota: UndoQuota = UndoQuota.UNLIMITED,
    val showMoveNumbers: Boolean = false,
    val showLastMove: Boolean = true,
    val vibration: Boolean = true,
    val sound: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val boardTheme: BoardTheme = BoardTheme.WOOD,
    val stoneSize: StoneSize = StoneSize.MEDIUM,
    val highContrast: Boolean = false,
    /** AI 最大思考时间（秒），作用于专家与自定义难度 */
    val aiThinkTimeSeconds: Int = 3,
    val customDepth: Int = 4,
    val customRandomness: Float = 0.2f,
    val showAiThinking: Boolean = true,
    val showHints: Boolean = false,
    val hideBoardOnTurn: Boolean = false,
    val showCoordinates: Boolean = false,
    val autosave: Boolean = true,
    /** 全局文字缩放（0.8~1.5，步进 0.1，1.0 = 默认） */
    val fontScale: Float = 1.0f,
    /** 应用配色主题（十套） */
    val colorTheme: AppColorTheme = AppColorTheme.AMBER,
    /** 图形效果预设 */
    val graphicsPreset: GraphicsPreset = GraphicsPreset.DELICATE,
    /** 背景模糊等级 0~3 */
    val bgBlurLevel: Int = 3,
    /** 背景柔光等级 0~3 */
    val glowLevel: Int = 3,
    /** 按钮不透明度 0.80~0.95 */
    val buttonOpacity: Float = 0.90f,
    /** 装饰性背景动画（光晕漂移） */
    val bgAnimation: Boolean = true,
    /** 页面转场风格 */
    val transitionStyle: TransitionStyle = TransitionStyle.FULL,
    /** 按钮反馈动画（按下缩放） */
    val buttonFeedback: Boolean = true
)

/** 预设对应的具体图形参数（精致/均衡/简约直接映射，自定义读独立字段） */
data class ResolvedGraphics(
    val blurLevel: Int,
    val glowLevel: Int,
    val buttonOpacity: Float,
    val bgAnimation: Boolean,
    val transitionStyle: TransitionStyle,
    val buttonFeedback: Boolean
)

fun UiSettings.resolvedGraphics(): ResolvedGraphics = when (graphicsPreset) {
    GraphicsPreset.DELICATE -> ResolvedGraphics(3, 3, 0.90f, true, TransitionStyle.FULL, true)
    GraphicsPreset.BALANCED -> ResolvedGraphics(2, 2, 0.92f, true, TransitionStyle.SOFT, true)
    GraphicsPreset.MINIMAL -> ResolvedGraphics(0, 1, 0.95f, false, TransitionStyle.SOFT, false)
    GraphicsPreset.CUSTOM -> ResolvedGraphics(
        bgBlurLevel, glowLevel, buttonOpacity, bgAnimation, transitionStyle, buttonFeedback
    )
}

/** 默认设置（含“恢复默认设置”入口使用的基准） */
val DefaultSettings = UiSettings()
