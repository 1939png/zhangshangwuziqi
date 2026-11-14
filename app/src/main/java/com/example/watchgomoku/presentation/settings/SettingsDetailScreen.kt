package com.example.watchgomoku.presentation.settings

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.watchgomoku.R
import com.example.watchgomoku.data.local.UiSettings
import com.example.watchgomoku.domain.model.AppColorTheme
import com.example.watchgomoku.domain.model.BoardTheme
import com.example.watchgomoku.domain.model.FirstMovePolicy
import com.example.watchgomoku.domain.model.GraphicsLevels
import com.example.watchgomoku.domain.model.GraphicsPreset
import com.example.watchgomoku.domain.model.StoneSize
import com.example.watchgomoku.domain.model.ThemeMode
import com.example.watchgomoku.domain.model.TransitionStyle
import com.example.watchgomoku.domain.model.UndoQuota
import com.example.watchgomoku.presentation.components.OptionRow
import com.example.watchgomoku.presentation.components.PrimaryMenuButton
import com.example.watchgomoku.presentation.components.ScreenTitle
import com.example.watchgomoku.presentation.components.SecondaryMenuButton
import com.example.watchgomoku.presentation.components.ScrollContent
import com.example.watchgomoku.presentation.components.SectionLabel
import com.example.watchgomoku.presentation.components.SliderRow
import com.example.watchgomoku.presentation.components.ToggleRow
import com.example.watchgomoku.presentation.mode.difficultyLabel
import com.example.watchgomoku.presentation.mode.firstPolicyLabel
import kotlin.math.roundToInt

/**
 * 设置二级详情页：按一级分类渲染对应选项。
 * 图形页支持“预设 + 调整细节”渐进展示；对局页低频项收进“高级设置”折叠。
 */
@Composable
fun SettingsDetailScreen(
    category: SettingsCategory,
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTitle(
            title = stringResource(R.string.settings_title_fmt, categoryTitle(category)),
            onBack = onBack
        )
        ScrollContent {
            when (category) {
                SettingsCategory.APPEARANCE -> AppearanceSection(viewModel, settings)
                SettingsCategory.GRAPHICS -> GraphicsSection(viewModel, settings)
                SettingsCategory.GAME -> GameSection(viewModel, settings)
                SettingsCategory.SOUND -> SoundSection(viewModel, settings)
                SettingsCategory.DATA -> DataSection(viewModel, settings, context)
                SettingsCategory.ABOUT -> AboutSection()
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
private fun categoryTitle(category: SettingsCategory): String = stringResource(
    when (category) {
        SettingsCategory.APPEARANCE -> R.string.cat_appearance
        SettingsCategory.GRAPHICS -> R.string.cat_graphics
        SettingsCategory.GAME -> R.string.cat_game
        SettingsCategory.SOUND -> R.string.cat_sound
        SettingsCategory.DATA -> R.string.cat_data
        SettingsCategory.ABOUT -> R.string.cat_about
    }
)

// ---------------------------------------------------------------- 外观与主题

@Composable
private fun AppearanceSection(viewModel: SettingsViewModel, settings: UiSettings) {
    SectionLabel(stringResource(R.string.cat_appearance))
    ThemeColorPicker(
        selected = settings.colorTheme,
        onSelect = { theme -> viewModel.update { it.copy(colorTheme = theme) } }
    )
    OptionRow(
        label = stringResource(R.string.settings_theme_mode),
        options = ThemeMode.entries.map { modeName(it) },
        selectedIndex = ThemeMode.entries.indexOf(settings.themeMode),
        onSelect = { index ->
            viewModel.update { it.copy(themeMode = ThemeMode.entries[index]) }
        }
    )
    // 文字大小：80%~150% 滑块，立即生效
    val fontPercent = (settings.fontScale * 100).roundToInt()
    SliderRow(
        label = stringResource(R.string.settings_font_size),
        valueText = stringResource(R.string.font_scale_fmt, fontPercent),
        value = settings.fontScale,
        valueRange = 0.8f..1.5f,
        steps = 6,
        contentDescription = stringResource(R.string.cd_font_slider),
        onValueChange = { raw ->
            val snapped = ((raw * 10).roundToInt() / 10f).coerceIn(0.8f, 1.5f)
            viewModel.update { it.copy(fontScale = snapped) }
        }
    )
    SecondaryMenuButton(
        text = stringResource(R.string.font_reset),
        onClick = { viewModel.update { it.copy(fontScale = 1.0f) } }
    )
    ToggleRow(
        label = stringResource(R.string.settings_high_contrast),
        checked = settings.highContrast,
        onChange = { value -> viewModel.update { it.copy(highContrast = value) } }
    )
    SectionLabel(stringResource(R.string.settings_board_theme))
    OptionRow(
        label = stringResource(R.string.settings_board_theme),
        options = BoardTheme.entries.map { boardThemeLabel(it) },
        selectedIndex = BoardTheme.entries.indexOf(settings.boardTheme),
        onSelect = { index ->
            viewModel.update { it.copy(boardTheme = BoardTheme.entries[index]) }
        }
    )
    OptionRow(
        label = stringResource(R.string.settings_stone_size),
        options = StoneSize.entries.map { stoneSizeLabel(it) },
        selectedIndex = StoneSize.entries.indexOf(settings.stoneSize),
        onSelect = { index ->
            viewModel.update { it.copy(stoneSize = StoneSize.entries[index]) }
        }
    )
}

/** 主题色卡：带名称的圆形色块 + 选中标记，点击即时生效 */
@Composable
private fun ThemeColorPicker(
    selected: AppColorTheme,
    onSelect: (AppColorTheme) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp)
    ) {
        AppColorTheme.entries.forEach { theme ->
            val isSelected = theme == selected
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onSelect(theme) }
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(theme.lightPrimary)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Text(
                            text = "✓",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                Text(
                    text = themeName(theme),
                    fontSize = 10.sp,
                    maxLines = 1,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

// ---------------------------------------------------------------- 图形与动画

@Composable
private fun GraphicsSection(viewModel: SettingsViewModel, settings: UiSettings) {
    SectionLabel(stringResource(R.string.cat_graphics))
    OptionRow(
        label = "",
        options = GraphicsPreset.entries.map { presetName(it) },
        selectedIndex = GraphicsPreset.entries.indexOf(settings.graphicsPreset),
        onSelect = { index ->
            val preset = GraphicsPreset.entries[index]
            viewModel.update { applyPreset(it, preset) }
        }
    )
    // 调整细节：默认折叠，选择“自定义”时自动展开；折叠不重置数值
    var showDetail by remember { mutableStateOf(settings.graphicsPreset == GraphicsPreset.CUSTOM) }
    TextButton(onClick = { showDetail = !showDetail }) {
        Text(
            text = (if (showDetail) "▾ " else "▸ ") + stringResource(R.string.gfx_advance),
            fontSize = 12.sp
        )
    }
    AnimatedVisibility(visible = showDetail) {
        Column {
            OptionRow(
                label = stringResource(R.string.gfx_blur),
                options = levelOptions(),
                selectedIndex = settings.bgBlurLevel,
                onSelect = { index ->
                    viewModel.update {
                        it.copy(bgBlurLevel = index, graphicsPreset = GraphicsPreset.CUSTOM)
                    }
                }
            )
            OptionRow(
                label = stringResource(R.string.gfx_glow),
                options = levelOptions(),
                selectedIndex = settings.glowLevel,
                onSelect = { index ->
                    viewModel.update {
                        it.copy(glowLevel = index, graphicsPreset = GraphicsPreset.CUSTOM)
                    }
                }
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.gfx_opacity),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${(settings.buttonOpacity * 100).roundToInt()}%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = settings.buttonOpacity,
                onValueChange = { raw ->
                    viewModel.update {
                        it.copy(
                            buttonOpacity = raw.coerceIn(0.80f, 0.95f),
                            graphicsPreset = GraphicsPreset.CUSTOM
                        )
                    }
                },
                valueRange = 0.80f..0.95f
            )
            ToggleRow(
                label = stringResource(R.string.gfx_bg_anim),
                checked = settings.bgAnimation,
                onChange = { value ->
                    viewModel.update {
                        it.copy(bgAnimation = value, graphicsPreset = GraphicsPreset.CUSTOM)
                    }
                }
            )
            OptionRow(
                label = stringResource(R.string.gfx_transition),
                options = TransitionStyle.entries.map { transitionName(it) },
                selectedIndex = TransitionStyle.entries.indexOf(settings.transitionStyle),
                onSelect = { index ->
                    viewModel.update {
                        it.copy(
                            transitionStyle = TransitionStyle.entries[index],
                            graphicsPreset = GraphicsPreset.CUSTOM
                        )
                    }
                }
            )
            ToggleRow(
                label = stringResource(R.string.gfx_feedback),
                checked = settings.buttonFeedback,
                onChange = { value ->
                    viewModel.update {
                        it.copy(buttonFeedback = value, graphicsPreset = GraphicsPreset.CUSTOM)
                    }
                }
            )
        }
    }
    SecondaryMenuButton(
        text = stringResource(R.string.gfx_reset),
        onClick = { viewModel.update { applyPreset(it, GraphicsPreset.DELICATE) } }
    )
}

/** 预设参数映射：选择预设即写入对应参数；CUSTOM 保留用户数值 */
private fun applyPreset(s: UiSettings, preset: GraphicsPreset): UiSettings = when (preset) {
    GraphicsPreset.DELICATE -> s.copy(
        graphicsPreset = preset, bgBlurLevel = 3, glowLevel = 3,
        buttonOpacity = 0.90f, bgAnimation = true,
        transitionStyle = TransitionStyle.FULL, buttonFeedback = true
    )
    GraphicsPreset.BALANCED -> s.copy(
        graphicsPreset = preset, bgBlurLevel = 2, glowLevel = 2,
        buttonOpacity = 0.92f, bgAnimation = true,
        transitionStyle = TransitionStyle.SOFT, buttonFeedback = true
    )
    GraphicsPreset.MINIMAL -> s.copy(
        graphicsPreset = preset, bgBlurLevel = 0, glowLevel = 1,
        buttonOpacity = 0.95f, bgAnimation = false,
        transitionStyle = TransitionStyle.SOFT, buttonFeedback = false
    )
    GraphicsPreset.CUSTOM -> s.copy(graphicsPreset = preset)
}

@Composable
private fun levelOptions(): List<String> = listOf(
    stringResource(R.string.level_off),
    stringResource(R.string.level_low),
    stringResource(R.string.level_mid),
    stringResource(R.string.level_high)
)

@Composable
private fun transitionName(style: TransitionStyle): String = stringResource(
    when (style) {
        TransitionStyle.FULL -> R.string.transition_full
        TransitionStyle.SOFT -> R.string.transition_soft
        TransitionStyle.OFF -> R.string.transition_off
    }
)

// ---------------------------------------------------------------- 对局设置

@Composable
private fun GameSection(viewModel: SettingsViewModel, settings: UiSettings) {
    val sizes = listOf(9, 13, 15, 19)
    OptionRow(
        label = stringResource(R.string.settings_board_size),
        options = sizes.map { "${it}×$it" },
        selectedIndex = sizes.indexOf(settings.boardSize).coerceAtLeast(0),
        onSelect = { index -> viewModel.update { it.copy(boardSize = sizes[index]) } }
    )
    OptionRow(
        label = stringResource(R.string.settings_default_difficulty),
        options = com.example.watchgomoku.domain.model.AiDifficulty.entries.map {
            difficultyLabel(it)
        },
        selectedIndex = com.example.watchgomoku.domain.model.AiDifficulty.entries.indexOf(
            settings.difficulty
        ),
        onSelect = { index ->
            viewModel.update {
                it.copy(difficulty = com.example.watchgomoku.domain.model.AiDifficulty.entries[index])
            }
        }
    )
    OptionRow(
        label = stringResource(R.string.settings_first_move),
        options = FirstMovePolicy.entries.map { firstPolicyLabel(it) },
        selectedIndex = FirstMovePolicy.entries.indexOf(settings.firstMovePolicy),
        onSelect = { index ->
            viewModel.update { it.copy(firstMovePolicy = FirstMovePolicy.entries[index]) }
        }
    )
    ToggleRow(
        label = stringResource(R.string.settings_allow_undo),
        checked = settings.allowUndo,
        onChange = { value -> viewModel.update { it.copy(allowUndo = value) } }
    )
    OptionRow(
        label = stringResource(R.string.settings_undo_quota),
        options = UndoQuota.entries.map { quotaLabel(it) },
        selectedIndex = UndoQuota.entries.indexOf(settings.undoQuota),
        onSelect = { index ->
            viewModel.update { it.copy(undoQuota = UndoQuota.entries[index]) }
        }
    )
    ToggleRow(
        label = stringResource(R.string.settings_show_numbers),
        checked = settings.showMoveNumbers,
        onChange = { value -> viewModel.update { it.copy(showMoveNumbers = value) } }
    )
    ToggleRow(
        label = stringResource(R.string.settings_show_last),
        checked = settings.showLastMove,
        onChange = { value -> viewModel.update { it.copy(showLastMove = value) } }
    )
    ToggleRow(
        label = stringResource(R.string.settings_show_hint),
        checked = settings.showHints,
        onChange = { value -> viewModel.update { it.copy(showHints = value) } }
    )
    ToggleRow(
        label = stringResource(R.string.settings_show_coords),
        checked = settings.showCoordinates,
        onChange = { value -> viewModel.update { it.copy(showCoordinates = value) } }
    )
    ToggleRow(
        label = stringResource(R.string.settings_hide_board),
        checked = settings.hideBoardOnTurn,
        onChange = { value -> viewModel.update { it.copy(hideBoardOnTurn = value) } }
    )
    // 低频选项：默认折叠的高级设置
    CollapseSection(title = stringResource(R.string.advanced_settings)) {
        val thinkTimes = listOf(1, 3, 5, 8)
        OptionRow(
            label = stringResource(R.string.settings_ai_time),
            options = thinkTimes.map { stringResource(R.string.seconds_fmt, it) },
            selectedIndex = thinkTimes.indexOf(settings.aiThinkTimeSeconds).coerceAtLeast(0),
            onSelect = { index ->
                viewModel.update { it.copy(aiThinkTimeSeconds = thinkTimes[index]) }
            }
        )
        val depths = listOf(2, 3, 4, 6, 8)
        OptionRow(
            label = stringResource(R.string.settings_custom_depth),
            options = depths.map { stringResource(R.string.depth_fmt, it) },
            selectedIndex = depths.indexOf(settings.customDepth).coerceAtLeast(0),
            onSelect = { index ->
                viewModel.update { it.copy(customDepth = depths[index]) }
            }
        )
        val randomness = listOf(0f, 0.2f, 0.5f)
        OptionRow(
            label = stringResource(R.string.settings_custom_random),
            options = listOf("0%", "20%", "50%"),
            selectedIndex = randomness.indexOf(settings.customRandomness).coerceAtLeast(0),
            onSelect = { index ->
                viewModel.update { it.copy(customRandomness = randomness[index]) }
            }
        )
    }
}

@Composable
private fun quotaLabel(quota: UndoQuota): String = stringResource(
    when (quota) {
        UndoQuota.UNLIMITED -> R.string.quota_unlimited
        UndoQuota.ONE -> R.string.quota_one
        UndoQuota.THREE -> R.string.quota_three
        UndoQuota.OFF -> R.string.quota_off
    }
)

@Composable
private fun boardThemeLabel(theme: BoardTheme): String = stringResource(
    when (theme) {
        BoardTheme.WOOD -> R.string.board_wood
        BoardTheme.DARK -> R.string.board_dark
        BoardTheme.HIGH_CONTRAST -> R.string.board_contrast
    }
)

@Composable
private fun stoneSizeLabel(size: StoneSize): String = stringResource(
    when (size) {
        StoneSize.SMALL -> R.string.stone_small
        StoneSize.MEDIUM -> R.string.stone_medium
        StoneSize.LARGE -> R.string.stone_large
    }
)

// ---------------------------------------------------------------- 折叠容器

/** 默认折叠的“高级设置”分组 */
@Composable
private fun CollapseSection(title: String, content: @Composable () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    TextButton(onClick = { expanded = !expanded }) {
        Text(
            text = (if (expanded) "▾ " else "▸ ") + title,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    AnimatedVisibility(visible = expanded) {
        Column { content() }
    }
}

// ---------------------------------------------------------------- 声音与触感

@Composable
private fun SoundSection(viewModel: SettingsViewModel, settings: UiSettings) {
    ToggleRow(
        label = stringResource(R.string.settings_sound),
        checked = settings.sound,
        onChange = { value -> viewModel.update { it.copy(sound = value) } }
    )
    ToggleRow(
        label = stringResource(R.string.settings_vibration),
        checked = settings.vibration,
        onChange = { value -> viewModel.update { it.copy(vibration = value) } }
    )
}

// ---------------------------------------------------------------- 数据与存档

@Composable
private fun DataSection(
    viewModel: SettingsViewModel,
    settings: UiSettings,
    context: android.content.Context
) {
    ToggleRow(
        label = stringResource(R.string.settings_autosave),
        checked = settings.autosave,
        onChange = { value -> viewModel.update { it.copy(autosave = value) } }
    )
    var showClearHistory by remember { mutableStateOf(false) }
    var showClearSave by remember { mutableStateOf(false) }
    SecondaryMenuButton(
        text = stringResource(R.string.settings_clear_history),
        onClick = { showClearHistory = true }
    )
    SecondaryMenuButton(
        text = stringResource(R.string.data_clear_save),
        onClick = { showClearSave = true }
    )

    if (showClearHistory) {
        com.example.watchgomoku.presentation.components.ConfirmDialog(
            title = stringResource(R.string.settings_clear_history),
            message = stringResource(R.string.settings_clear_confirm),
            confirmText = stringResource(R.string.confirm_ok),
            cancelText = stringResource(R.string.confirm_cancel),
            onConfirm = {
                showClearHistory = false
                viewModel.clearHistory()
                Toast.makeText(
                    context,
                    context.getString(R.string.toast_history_cleared),
                    Toast.LENGTH_SHORT
                ).show()
            },
            onDismiss = { showClearHistory = false }
        )
    }
    if (showClearSave) {
        com.example.watchgomoku.presentation.components.ConfirmDialog(
            title = stringResource(R.string.data_clear_save),
            message = stringResource(R.string.data_clear_save_confirm),
            confirmText = stringResource(R.string.confirm_ok),
            cancelText = stringResource(R.string.confirm_cancel),
            onConfirm = {
                showClearSave = false
                viewModel.clearSavedGame()
                Toast.makeText(
                    context,
                    context.getString(R.string.toast_save_cleared),
                    Toast.LENGTH_SHORT
                ).show()
            },
            onDismiss = { showClearSave = false }
        )
    }
}

// ---------------------------------------------------------------- 关于应用

@Composable
private fun AboutSection() {
    val context = LocalContext.current
    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.0.0"
    }
    Text(
        text = stringResource(R.string.app_name),
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
    Text(
        text = stringResource(R.string.about_version_fmt, versionName),
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 4.dp)
    )
    Text(
        text = stringResource(R.string.about_dev),
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    Text(
        text = stringResource(R.string.about_body),
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.about_privacy),
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
