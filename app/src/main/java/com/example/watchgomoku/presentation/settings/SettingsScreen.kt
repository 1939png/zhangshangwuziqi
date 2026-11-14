package com.example.watchgomoku.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.watchgomoku.R
import com.example.watchgomoku.domain.model.AppColorTheme
import com.example.watchgomoku.presentation.components.ScreenTitle
import com.example.watchgomoku.presentation.components.ScrollContent
import com.example.watchgomoku.presentation.mode.difficultyLabel
import com.example.watchgomoku.ui.theme.scaledDp

/**
 * 设置一级页：只展示分类入口（图标 + 名称 + 摘要），不再堆叠全部控件；
 * 摘要随当前设置动态变化，点击进入对应二级详情页。
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onCategory: (SettingsCategory) -> Unit,
    onDone: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTitle(title = stringResource(R.string.settings_title), onBack = onDone)
        ScrollContent {
            CategoryEntry(
                index = 1,
                glyph = "◐",
                title = stringResource(R.string.cat_appearance),
                summary = themeName(settings.colorTheme) + " · " +
                    modeName(settings.themeMode)
            ) { onCategory(SettingsCategory.APPEARANCE) }
            CategoryEntry(
                index = 2,
                glyph = "✦",
                title = stringResource(R.string.cat_graphics),
                summary = presetName(settings.graphicsPreset)
            ) { onCategory(SettingsCategory.GRAPHICS) }
            CategoryEntry(
                index = 3,
                glyph = "●",
                title = stringResource(R.string.cat_game),
                summary = "${settings.boardSize}×${settings.boardSize} · " +
                    difficultyName(settings.difficulty)
            ) { onCategory(SettingsCategory.GAME) }
            CategoryEntry(
                index = 4,
                glyph = "♪",
                title = stringResource(R.string.cat_sound),
                summary = stringResource(R.string.settings_sound) + " " +
                    onOffLabel(settings.sound) + " · " +
                    stringResource(R.string.settings_vibration) + " " +
                    onOffLabel(settings.vibration)
            ) { onCategory(SettingsCategory.SOUND) }
            CategoryEntry(
                index = 5,
                glyph = "☰",
                title = stringResource(R.string.cat_data),
                summary = stringResource(R.string.settings_autosave) + " " +
                    onOffLabel(settings.autosave)
            ) { onCategory(SettingsCategory.DATA) }
            CategoryEntry(
                index = 6,
                glyph = "ℹ",
                title = stringResource(R.string.cat_about),
                summary = stringResource(R.string.splash_brand)
            ) { onCategory(SettingsCategory.ABOUT) }
            Spacer(Modifier.height(14.dp))
        }
    }
}

/** 分类入口卡片：图标 + 名称 + 一行摘要，位于圆角安全区内（ScrollContent 已处理） */
@Composable
private fun CategoryEntry(
    index: Int,
    glyph: String,
    title: String,
    summary: String,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = scaledDp(52.dp))
            .clip(shape)
            .background(cs.surfaceVariant.copy(alpha = 0.55f))
            .border(1.dp, cs.outline.copy(alpha = 0.35f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = glyph,
                fontSize = 16.sp,
                color = cs.primary,
                modifier = Modifier.width(28.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onSurface
                )
                Text(
                    text = summary,
                    fontSize = 11.sp,
                    color = cs.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Text(text = "›", fontSize = 16.sp, color = cs.onSurfaceVariant)
        }
    }
    Spacer(Modifier.height(6.dp))
}

@Composable
internal fun themeName(theme: AppColorTheme): String = stringResource(
    when (theme) {
        AppColorTheme.FOREST -> R.string.theme_forest
        AppColorTheme.OCEAN -> R.string.theme_ocean
        AppColorTheme.STARLIGHT -> R.string.theme_starlight
        AppColorTheme.SUNSET -> R.string.theme_sunset
        AppColorTheme.ROSE -> R.string.theme_rose
        AppColorTheme.CELADON -> R.string.theme_celadon
        AppColorTheme.AMBER -> R.string.theme_amber
        AppColorTheme.CHERRY -> R.string.theme_cherry
        AppColorTheme.GRAPHITE -> R.string.theme_graphite
        AppColorTheme.MIDNIGHT -> R.string.theme_midnight
    }
)

@Composable
internal fun modeName(mode: com.example.watchgomoku.domain.model.ThemeMode): String = stringResource(
    when (mode) {
        com.example.watchgomoku.domain.model.ThemeMode.SYSTEM -> R.string.theme_system
        com.example.watchgomoku.domain.model.ThemeMode.LIGHT -> R.string.theme_light
        com.example.watchgomoku.domain.model.ThemeMode.DARK -> R.string.theme_dark
    }
)

@Composable
internal fun presetName(preset: com.example.watchgomoku.domain.model.GraphicsPreset): String =
    stringResource(
        when (preset) {
            com.example.watchgomoku.domain.model.GraphicsPreset.DELICATE -> R.string.preset_delicate
            com.example.watchgomoku.domain.model.GraphicsPreset.BALANCED -> R.string.preset_balanced
            com.example.watchgomoku.domain.model.GraphicsPreset.MINIMAL -> R.string.preset_minimal
            com.example.watchgomoku.domain.model.GraphicsPreset.CUSTOM -> R.string.preset_custom
        }
    )

@Composable
internal fun difficultyName(difficulty: com.example.watchgomoku.domain.model.AiDifficulty): String =
    difficultyLabel(difficulty)

@Composable
internal fun onOffLabel(value: Boolean): String =
    stringResource(if (value) R.string.on else R.string.off)
