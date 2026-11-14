package com.example.watchgomoku.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.watchgomoku.domain.model.AiDifficulty
import com.example.watchgomoku.domain.model.AppColorTheme
import com.example.watchgomoku.domain.model.BoardTheme
import com.example.watchgomoku.domain.model.FirstMovePolicy
import com.example.watchgomoku.domain.model.GraphicsPreset
import com.example.watchgomoku.domain.model.StoneSize
import com.example.watchgomoku.domain.model.ThemeMode
import com.example.watchgomoku.domain.model.TransitionStyle
import com.example.watchgomoku.domain.model.UndoQuota
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.gomokuDataStore by preferencesDataStore(name = "gomoku_settings")

/**
 * DataStore 设置存储：只保存本机偏好，不上传任何数据。
 * 读取时对非法值做防御性回退（存档损坏 / 版本不兼容场景）。
 */
class SettingsStore(private val context: Context) {

    object Keys {
        val BOARD_SIZE = intPreferencesKey("board_size")
        val DIFFICULTY = stringPreferencesKey("difficulty")
        val FIRST_POLICY = stringPreferencesKey("first_policy")
        val ALLOW_UNDO = booleanPreferencesKey("allow_undo")
        val UNDO_QUOTA = stringPreferencesKey("undo_quota")
        val SHOW_NUMBERS = booleanPreferencesKey("show_move_numbers")
        val SHOW_LAST = booleanPreferencesKey("show_last_move")
        val VIBRATION = booleanPreferencesKey("vibration")
        val SOUND = booleanPreferencesKey("sound")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val BOARD_THEME = stringPreferencesKey("board_theme")
        val STONE_SIZE = stringPreferencesKey("stone_size")
        val HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
        val AI_TIME_SECONDS = intPreferencesKey("ai_time_seconds")
        val CUSTOM_DEPTH = intPreferencesKey("custom_depth")
        val CUSTOM_RANDOM = floatPreferencesKey("custom_randomness")
        val SHOW_THINKING = booleanPreferencesKey("show_ai_thinking")
        val SHOW_HINTS = booleanPreferencesKey("show_hints")
        val HIDE_BOARD = booleanPreferencesKey("hide_board_on_turn")
        val SHOW_COORDS = booleanPreferencesKey("show_coordinates")
        val AUTOSAVE = booleanPreferencesKey("autosave")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val COLOR_THEME = stringPreferencesKey("color_theme")
        val GRAPHICS_PRESET = stringPreferencesKey("graphics_preset")
        val BG_BLUR = intPreferencesKey("bg_blur_level")
        val GLOW_LEVEL = intPreferencesKey("glow_level")
        val BUTTON_OPACITY = floatPreferencesKey("button_opacity")
        val BG_ANIMATION = booleanPreferencesKey("bg_animation")
        val TRANSITION_STYLE = stringPreferencesKey("transition_style")
        val BUTTON_FEEDBACK = booleanPreferencesKey("button_feedback")
    }

    val settings: Flow<UiSettings> =
        context.gomokuDataStore.data
            .catch { emit(emptyPreferences()) }
            .map { it.toUiSettings() }

    suspend fun set(settings: UiSettings) {
        context.gomokuDataStore.edit { p ->
            p[Keys.BOARD_SIZE] = settings.boardSize.coerceIn(9, 19)
            p[Keys.DIFFICULTY] = settings.difficulty.name
            p[Keys.FIRST_POLICY] = settings.firstMovePolicy.name
            p[Keys.ALLOW_UNDO] = settings.allowUndo
            p[Keys.UNDO_QUOTA] = settings.undoQuota.name
            p[Keys.SHOW_NUMBERS] = settings.showMoveNumbers
            p[Keys.SHOW_LAST] = settings.showLastMove
            p[Keys.VIBRATION] = settings.vibration
            p[Keys.SOUND] = settings.sound
            p[Keys.THEME_MODE] = settings.themeMode.name
            p[Keys.BOARD_THEME] = settings.boardTheme.name
            p[Keys.STONE_SIZE] = settings.stoneSize.name
            p[Keys.HIGH_CONTRAST] = settings.highContrast
            p[Keys.AI_TIME_SECONDS] = settings.aiThinkTimeSeconds.coerceIn(1, 30)
            p[Keys.CUSTOM_DEPTH] = settings.customDepth.coerceIn(1, 10)
            p[Keys.CUSTOM_RANDOM] = settings.customRandomness.coerceIn(0f, 1f)
            p[Keys.SHOW_THINKING] = settings.showAiThinking
            p[Keys.SHOW_HINTS] = settings.showHints
            p[Keys.HIDE_BOARD] = settings.hideBoardOnTurn
            p[Keys.SHOW_COORDS] = settings.showCoordinates
            p[Keys.AUTOSAVE] = settings.autosave
            p[Keys.FONT_SCALE] = settings.fontScale.coerceIn(0.8f, 1.5f)
            p[Keys.COLOR_THEME] = settings.colorTheme.name
            p[Keys.GRAPHICS_PRESET] = settings.graphicsPreset.name
            p[Keys.BG_BLUR] = settings.bgBlurLevel.coerceIn(0, 3)
            p[Keys.GLOW_LEVEL] = settings.glowLevel.coerceIn(0, 3)
            p[Keys.BUTTON_OPACITY] = settings.buttonOpacity.coerceIn(0.80f, 0.95f)
            p[Keys.BG_ANIMATION] = settings.bgAnimation
            p[Keys.TRANSITION_STYLE] = settings.transitionStyle.name
            p[Keys.BUTTON_FEEDBACK] = settings.buttonFeedback
        }
    }
}

private fun emptyPreferences(): androidx.datastore.preferences.core.Preferences =
    androidx.datastore.preferences.core.emptyPreferences()

private fun androidx.datastore.preferences.core.Preferences.toUiSettings(): UiSettings {
    val k = SettingsStore.Keys
    return UiSettings(
        boardSize = this[k.BOARD_SIZE]?.takeIf { it == 9 || it == 13 || it == 15 || it == 19 } ?: 15,
        difficulty = enumOr(this[k.DIFFICULTY], AiDifficulty.NORMAL),
        firstMovePolicy = enumOr(this[k.FIRST_POLICY], FirstMovePolicy.PLAYER_BLACK),
        allowUndo = this[k.ALLOW_UNDO] ?: true,
        undoQuota = enumOr(this[k.UNDO_QUOTA], UndoQuota.UNLIMITED),
        showMoveNumbers = this[k.SHOW_NUMBERS] ?: false,
        showLastMove = this[k.SHOW_LAST] ?: true,
        vibration = this[k.VIBRATION] ?: true,
        sound = this[k.SOUND] ?: true,
        themeMode = enumOr(this[k.THEME_MODE], ThemeMode.SYSTEM),
        boardTheme = enumOr(this[k.BOARD_THEME], BoardTheme.WOOD),
        stoneSize = enumOr(this[k.STONE_SIZE], StoneSize.MEDIUM),
        highContrast = this[k.HIGH_CONTRAST] ?: false,
        aiThinkTimeSeconds = (this[k.AI_TIME_SECONDS] ?: 3).coerceIn(1, 30),
        customDepth = (this[k.CUSTOM_DEPTH] ?: 4).coerceIn(1, 10),
        customRandomness = (this[k.CUSTOM_RANDOM] ?: 0.2f).coerceIn(0f, 1f),
        showAiThinking = this[k.SHOW_THINKING] ?: true,
        showHints = this[k.SHOW_HINTS] ?: false,
        hideBoardOnTurn = this[k.HIDE_BOARD] ?: false,
        showCoordinates = this[k.SHOW_COORDS] ?: false,
        autosave = this[k.AUTOSAVE] ?: true,
        fontScale = (this[k.FONT_SCALE] ?: 1.0f).coerceIn(0.8f, 1.5f),
        colorTheme = enumOr(this[k.COLOR_THEME], AppColorTheme.AMBER),
        graphicsPreset = enumOr(this[k.GRAPHICS_PRESET], GraphicsPreset.DELICATE),
        bgBlurLevel = (this[k.BG_BLUR] ?: 3).coerceIn(0, 3),
        glowLevel = (this[k.GLOW_LEVEL] ?: 3).coerceIn(0, 3),
        buttonOpacity = (this[k.BUTTON_OPACITY] ?: 0.90f).coerceIn(0.80f, 0.95f),
        bgAnimation = this[k.BG_ANIMATION] ?: true,
        transitionStyle = enumOr(this[k.TRANSITION_STYLE], TransitionStyle.FULL),
        buttonFeedback = this[k.BUTTON_FEEDBACK] ?: true
    )
}

private inline fun <reified T : Enum<T>> enumOr(name: String?, fallback: T): T =
    name?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: fallback
