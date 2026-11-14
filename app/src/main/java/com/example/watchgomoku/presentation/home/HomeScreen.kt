package com.example.watchgomoku.presentation.home

import android.os.Build
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.watchgomoku.R
import com.example.watchgomoku.data.local.ResolvedGraphics
import com.example.watchgomoku.ui.theme.HomeTypography
import com.example.watchgomoku.ui.theme.LocalHighContrast
import com.example.watchgomoku.ui.theme.rememberReducedMotion
import com.example.watchgomoku.ui.theme.scaledDp
import com.example.watchgomoku.ui.theme.watchSafePadding

/**
 * 首页：纵向居中的大按钮布局 + 模糊光晕背景 + 错峰进入动画。
 *
 * 优先保证一屏可见：标题、单人对弈、多人对弈、继续游戏、设置；
 * 练习模式 / 棋谱记录 / 关于 收入“更多”菜单；内容超出时可垂直滚动兜底。
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    graphics: ResolvedGraphics,
    onSolo: () -> Unit,
    onMulti: () -> Unit,
    onPractice: () -> Unit,
    onContinue: () -> Unit,
    onSettings: () -> Unit,
    onHistory: () -> Unit,
    onAbout: () -> Unit
) {
    val savedGame by viewModel.savedGame.collectAsStateWithLifecycle()
    val reducedMotion = rememberReducedMotion()
    val highContrast = LocalHighContrast.current
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    Box(modifier = Modifier.fillMaxSize()) {
        // 中层：柔和光晕（进入时淡入；API 31+ 实时高斯模糊，低版本静态渐变）
        HomeGlowBackground(
            reducedMotion = reducedMotion,
            entered = entered,
            highContrast = highContrast
        )

        // 上层：清晰内容；高度充足时整体垂直居中，小屏改为滚动兑底
        val canCenter = LocalConfiguration.current.screenHeightDp >= 400
        if (canCenter) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .watchSafePadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                HomeContent(
                    savedMoves = savedGame?.moves?.size,
                    graphics = graphics,
                    entered = entered,
                    reducedMotion = reducedMotion,
                    onSolo = onSolo,
                    onMulti = onMulti,
                    onContinue = onContinue,
                    onSettings = onSettings,
                    onPractice = onPractice,
                    onHistory = onHistory,
                    onAbout = onAbout
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .watchSafePadding()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HomeContent(
                    savedMoves = savedGame?.moves?.size,
                    graphics = graphics,
                    entered = entered,
                    reducedMotion = reducedMotion,
                    onSolo = onSolo,
                    onMulti = onMulti,
                    onContinue = onContinue,
                    onSettings = onSettings,
                    onPractice = onPractice,
                    onHistory = onHistory,
                    onAbout = onAbout
                )
            }
        }
    }
}

/** 主页内容：标题 + 主按钮列 + 状态提示 + 底部辅助说明（居中与滚动两种容器共用） */
@Composable
private fun HomeContent(
    savedMoves: Int?,
    graphics: ResolvedGraphics,
    entered: Boolean,
    reducedMotion: Boolean,
    onSolo: () -> Unit,
    onMulti: () -> Unit,
    onContinue: () -> Unit,
    onSettings: () -> Unit,
    onPractice: () -> Unit,
    onHistory: () -> Unit,
    onAbout: () -> Unit
) {
    StaggerItem(index = 0, entered = entered, reducedMotion = reducedMotion) {
        Text(
            text = stringResource(R.string.app_name),
            fontSize = HomeTypography.titleSize(),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
        )
    }
    Spacer(Modifier.height(8.dp))
    Column(
        verticalArrangement = Arrangement.spacedBy(scaledDp(9.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        HomeMenuButton(
            index = 1, text = stringResource(R.string.home_solo), glyph = "●",
            graphics = graphics,
            entered = entered, reducedMotion = reducedMotion, onClick = onSolo
        )
        HomeMenuButton(
            index = 2, text = stringResource(R.string.home_multi), glyph = "●○",
            graphics = graphics,
            entered = entered, reducedMotion = reducedMotion, onClick = onMulti
        )
        HomeMenuButton(
            index = 3, text = stringResource(R.string.home_continue), glyph = "▶",
            enabled = savedMoves != null,
            graphics = graphics,
            entered = entered, reducedMotion = reducedMotion, onClick = onContinue
        )
        if (savedMoves != null) {
            StaggerItem(index = 4, entered = entered, reducedMotion = reducedMotion) {
                Text(
                    text = stringResource(R.string.home_saved_fmt, savedMoves),
                    fontSize = HomeTypography.captionSize(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
        HomeMenuButton(
            index = 5, text = stringResource(R.string.home_settings), glyph = "⚙",
            graphics = graphics,
            entered = entered, reducedMotion = reducedMotion, onClick = onSettings
        )
        HomeMoreButton(
            index = 6,
            graphics = graphics,
            entered = entered,
            reducedMotion = reducedMotion,
            onPractice = onPractice,
            onHistory = onHistory,
            onAbout = onAbout
        )
        StaggerItem(index = 7, entered = entered, reducedMotion = reducedMotion) {
            Text(
                text = stringResource(R.string.home_footer),
                fontSize = HomeTypography.captionSize(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        Spacer(Modifier.height(10.dp))
    }
}

/**
 * 首页主按钮：主题渐变半透明背景 + 细描边 + ≥48dp 触达；
 * 按下 0.97 缩放（可由图形设置关闭）、进入时错峰淡入上移；
 * 高对比模式用更实的背景与更明确描边。
 */
@Composable
private fun HomeMenuButton(
    index: Int,
    text: String,
    glyph: String,
    graphics: ResolvedGraphics,
    enabled: Boolean = true,
    entered: Boolean,
    reducedMotion: Boolean,
    onClick: () -> Unit,
    contentDescription: String = text
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed && graphics.buttonFeedback && !reducedMotion) 0.97f else 1f,
        animationSpec = tween(120, easing = EaseOutCubic),
        label = "pressScale"
    )
    val duration = if (reducedMotion) 1 else 350
    val delayMs = if (reducedMotion) 0 else index * 60
    val alpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(duration, delayMillis = delayMs, easing = EaseOutCubic),
        label = "enterAlpha"
    )
    val offsetY by animateFloatAsState(
        targetValue = if (entered) 0f else 24f,
        animationSpec = tween(duration, delayMillis = delayMs, easing = EaseOutCubic),
        label = "enterOffset"
    )

    val highContrast = LocalHighContrast.current
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)
    // 不透明度：高对比模式更实，其余由“按钮通透程度”控制
    val opacity = if (highContrast) 0.95f else graphics.buttonOpacity
    val backgroundBrush = if (enabled) {
        Brush.linearGradient(
            listOf(
                cs.primary.copy(alpha = opacity),
                cs.secondary.copy(alpha = opacity)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                cs.surfaceVariant.copy(alpha = opacity),
                cs.surfaceVariant.copy(alpha = opacity)
            )
        )
    }
    val borderColor = cs.outline.copy(alpha = if (highContrast) 0.9f else 0.45f)
    val textColor = if (enabled) cs.onPrimary else cs.onSurfaceVariant
    val glyphColor = if (enabled) cs.onPrimary else cs.onSurfaceVariant

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = scaledDp(48.dp))
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
                this.alpha = alpha
                translationY = offsetY
            }
            .clip(shape)
            .background(backgroundBrush)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .clickable(
                interactionSource = interaction,
                indication = androidx.compose.material3.ripple(),
                enabled = enabled,
                onClick = onClick
            )
            .semantics { this.contentDescription = contentDescription }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = glyph, fontSize = HomeTypography.glyphSize(), color = glyphColor)
            Spacer(Modifier.size(10.dp))
            Text(
                text = text,
                fontSize = HomeTypography.primaryButtonSize(),
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                maxLines = 1
            )
        }
    }
}

/** “更多”按钮：外观与主按钮一致，点击展开练习模式 / 棋谱记录 / 关于 */
@Composable
private fun HomeMoreButton(
    index: Int,
    graphics: ResolvedGraphics,
    entered: Boolean,
    reducedMotion: Boolean,
    onPractice: () -> Unit,
    onHistory: () -> Unit,
    onAbout: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    Box {
        HomeMenuButton(
            index = index,
            text = stringResource(R.string.home_more),
            glyph = "⋯",
            graphics = graphics,
            entered = entered,
            reducedMotion = reducedMotion,
            onClick = { menuOpen = true }
        )
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.mode_practice), fontSize = 13.sp) },
                onClick = {
                    menuOpen = false
                    onPractice()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.home_history), fontSize = 13.sp) },
                onClick = {
                    menuOpen = false
                    onHistory()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.home_about), fontSize = 13.sp) },
                onClick = {
                    menuOpen = false
                    onAbout()
                }
            )
        }
    }
}

/** 错峰出现容器：淡入 + 轻微上移 */
@Composable
private fun StaggerItem(
    index: Int,
    entered: Boolean,
    reducedMotion: Boolean,
    content: @Composable () -> Unit
) {
    val duration = if (reducedMotion) 1 else 350
    val delayMs = if (reducedMotion) 0 else index * 60
    val alpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(duration, delayMillis = delayMs, easing = EaseOutCubic),
        label = "staggerAlpha"
    )
    val offsetY by animateFloatAsState(
        targetValue = if (entered) 0f else 16f,
        animationSpec = tween(duration, delayMillis = delayMs, easing = EaseOutCubic),
        label = "staggerOffset"
    )
    Column(modifier = Modifier.graphicsLayer { this.alpha = alpha; translationY = offsetY }) {
        content()
    }
}

/**
 * 首页背景：底层深浅渐变 + 中层低透明度光晕。
 * 高斯模糊降级策略：
 * - API 31+：RenderEffect 实时模糊（blur 40dp）；
 * - API 26~30：无实时模糊，直接使用静态径向渐变（视觉接近、零额外渲染负担）；
 * - “减少动态效果”开启时光晕静止。
 */
@Composable
private fun HomeGlowBackground(
    reducedMotion: Boolean,
    entered: Boolean,
    highContrast: Boolean
) {
    // 光晕随主页进入淡入（动画序列第一步）；减少动效时立即显示
    val glowAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(if (reducedMotion) 1 else 300, easing = EaseOutCubic),
        label = "glowAlpha"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = glowAlpha }
    ) {
        // 底层：纵向渐变
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )
        if (reducedMotion) {
            StaticGlow(useBlur = Build.VERSION.SDK_INT >= 31, highContrast = highContrast)
        } else {
            AnimatedGlow(useBlur = Build.VERSION.SDK_INT >= 31, highContrast = highContrast)
        }
    }
}

@Composable
private fun AnimatedGlow(useBlur: Boolean, highContrast: Boolean) {
    val transition = rememberInfiniteTransition(label = "glow")
    val drift by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift"
    )
    GlowCircles(
        useBlur = useBlur,
        drift1 = drift,
        drift2 = -drift,
        highContrast = highContrast
    )
}

@Composable
private fun StaticGlow(useBlur: Boolean, highContrast: Boolean) {
    GlowCircles(useBlur = useBlur, drift1 = 0f, drift2 = 0f, highContrast = highContrast)
}

@Composable
private fun GlowCircles(useBlur: Boolean, drift1: Float, drift2: Float, highContrast: Boolean) {
    // 高对比模式：光晕透明度减半，保持极简高对比视觉
    val glowAlphaFactor = if (highContrast) 0.5f else 1f
    val glow = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f * glowAlphaFactor)
    val glow2 = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.30f * glowAlphaFactor)

    fun Modifier.glowBlur(): Modifier = if (useBlur) this.blur(40.dp) else this

    Box(modifier = Modifier.fillMaxSize()) {
        // 光晕 1：左上
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = ((-48) + drift1 * 22).dp, y = (-60).dp)
                .size(220.dp)
                .glowBlur()
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(glow, Color.Transparent)))
        )
        // 光晕 2：右下
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (36 + drift2 * 22).dp, y = 60.dp)
                .size(240.dp)
                .glowBlur()
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(glow2, Color.Transparent)))
        )
    }
}
