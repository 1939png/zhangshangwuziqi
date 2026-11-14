package com.example.watchgomoku.presentation.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.watchgomoku.GomokuApplication
import com.example.watchgomoku.R
import com.example.watchgomoku.domain.ai.AiPlayer
import com.example.watchgomoku.domain.engine.Board
import com.example.watchgomoku.domain.model.AiDifficulty
import com.example.watchgomoku.domain.model.Player
import com.example.watchgomoku.ui.theme.rememberReducedMotion
import com.example.watchgomoku.ui.theme.watchSafePadding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 启动加载页：“落子成局”品牌动画 + 真实初始化 + 品牌声明。
 *
 * 职责分工：
 * - 动画（约 2.2s，点击任意处可跳过）：网格逐条绘出 → 落子 → 涟漪 →
 *   获胜连线扫过 → 标题浮现，同时充当“最短显示时长”，避免页面一闪而过；
 * - 初始化（后台 IO，不阻塞 UI）：① 加载本地设置（预热 DataStore）
 *   ② 探测可恢复的本地棋局；完成后与动画汇合，平滑进入主页；
 * - 失败：显示“初始化失败”+ 重试按钮（不展示任何技术堆栈）；
 * - 品牌声明：底部安全区上方两段渐现，警示红压轴。
 */
private const val SPLASH_DURATION_MS = 2200

private const val GRID_END = 0.55f
private const val RIPPLE_START = 0.74f
private const val RIPPLE_END = 0.90f
private const val LINE_START = 0.76f
private const val LINE_END = 0.92f

/** 迷你盘落子序列：黑三连竖排 col=1，白子点缀 */
private val SPLASH_STONES = listOf(
    SplashStone(row = 1, col = 1, isBlack = true, start = 0.30f, end = 0.46f),
    SplashStone(row = 3, col = 3, isBlack = false, start = 0.42f, end = 0.56f),
    SplashStone(row = 1, col = 3, isBlack = false, start = 0.52f, end = 0.64f),
    SplashStone(row = 2, col = 1, isBlack = true, start = 0.56f, end = 0.68f),
    SplashStone(row = 3, col = 1, isBlack = true, start = 0.62f, end = 0.74f)
)

private data class SplashStone(
    val row: Int,
    val col: Int,
    val isBlack: Boolean,
    val start: Float,
    val end: Float
)

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as GomokuApplication
    var finished by remember { mutableStateOf(false) }
    var animDone by remember { mutableStateOf(false) }
    var initDone by remember { mutableStateOf(false) }
    var initError by remember { mutableStateOf(false) }
    var retryKey by remember { mutableIntStateOf(0) }
    val progress = remember { Animatable(0f) }
    val reducedMotion = rememberReducedMotion()

    fun finish() {
        if (!finished) {
            finished = true
            onFinished()
        }
    }

    // 后台初始化：加载本地设置 → 探测可恢复棋局（可重试）
    LaunchedEffect(retryKey) {
        initDone = false
        initError = false
        val ok = withContext(Dispatchers.IO) {
            runCatching {
                // ① 加载本地设置（预热 DataStore）
                app.container.repository.settings.first()
                // ② 探测可恢复的本地棋局
                app.container.repository.loadGame()
                // ③ 初始化游戏引擎与 AI：微型搜索预热类加载，把首局 AI 首步的卡顿前移
                AiPlayer.chooseMove(
                    board = Board(9),
                    ai = Player.BLACK,
                    difficulty = AiDifficulty.EASY
                )
                Unit
            }.isSuccess
        }
        initDone = ok
        if (!ok) initError = true
    }

    LaunchedEffect(Unit) {
        progress.animateTo(
            1f,
            animationSpec = tween(durationMillis = SPLASH_DURATION_MS, easing = LinearEasing)
        )
        animDone = true
    }

    // 动画播完 + 初始化完成 → 平滑进入主页
    LaunchedEffect(animDone, initDone) {
        if (animDone && initDone && !initError) {
            delay(250)
            finish()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // 初始化失败时禁止带病跳过，只能重试
                if (!finished && !initError) finish()
            },
        contentAlignment = Alignment.Center
    ) {
        val p = progress.value

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            MiniBoardAnimation(progress = p)
            Spacer(Modifier.height(14.dp))
            AnimatedVisibility(
                visible = p > 0.62f,
                enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 2 }
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(Modifier.height(8.dp))
            if (initError) {
                Text(
                    text = stringResource(R.string.splash_init_failed),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = { retryKey++ }) {
                    Text(
                        text = stringResource(R.string.btn_retry),
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                }
            } else {
                AnimatedVisibility(visible = p > 0.35f, enter = fadeIn(tween(300))) {
                    PreparingIndicator(reducedMotion = reducedMotion)
                }
            }
        }

        // 品牌标识：底部安全区上方单行居中，清晰不模糊、不随动画运动
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .watchSafePadding()
                .padding(bottom = 4.dp)
                .semantics { contentDescription = "HIM 开发-免费开源" }
        ) {
            AnimatedVisibility(visible = p > 0.60f, enter = fadeIn(tween(350))) {
                Text(
                    text = stringResource(R.string.splash_brand),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1
                )
            }
        }
    }
}

/** “正在准备棋局…”三点呼吸提示（减少动效时静态） */
@Composable
private fun PreparingIndicator(reducedMotion: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = stringResource(R.string.splash_preparing),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        if (reducedMotion) {
            Text(
                text = "···",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val transition = rememberInfiniteTransition(label = "dots")
            repeat(3) { i ->
                val alpha by transition.animateFloat(
                    initialValue = 0.2f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, delayMillis = i * 180, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "dot$i"
                )
                Text(
                    text = "•",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.graphicsLayer { this.alpha = alpha }
                )
            }
        }
    }
}

/** 迷你 5 路盘：网格绘制 → 落子 → 涟漪 → 连线 */
@Composable
private fun MiniBoardAnimation(progress: Float) {
    Canvas(modifier = Modifier.size(120.dp)) {
        val full = size.minDimension
        val side = full * 0.86f
        val margin = (full - side) / 2f
        val cell = side / 4f
        val wood = Color(0xFFE3B874)
        val grid = Color(0xFF6B4423)
        val blackStone = Color(0xFF17140F)
        val whiteStone = Color(0xFFFBF7EF)
        val whiteOutline = Color(0x66432B14)
        val shadow = Color(0x33000000)
        val accent = Color(0xFFD32F2F)

        // 棋盘底板
        drawRoundRect(
            color = wood,
            topLeft = Offset(margin, margin),
            size = Size(side, side),
            cornerRadius = CornerRadius(full * 0.07f)
        )

        // 网格逐条绘出
        val gridProgress = (progress / GRID_END).coerceIn(0f, 1f)
        val drawnLines = gridProgress * 10f
        for (i in 0 until 5) {
            val x = margin + i * cell
            val y = margin + i * cell
            if (drawnLines > i) {
                drawLine(grid, Offset(x, margin), Offset(x, margin + side), full * 0.012f)
            }
            if (drawnLines > 5 + i) {
                drawLine(grid, Offset(margin, y), Offset(margin + side, y), full * 0.012f)
            }
        }
        // 星位（网格完成后）
        if (gridProgress >= 1f) {
            drawCircle(
                Color(0xFF5D4037),
                full * 0.018f,
                Offset(margin + 2 * cell, margin + 2 * cell)
            )
        }

        // 落子：下落 + 压弹 + 渐显
        SPLASH_STONES.forEach { stone ->
            val t = ((progress - stone.start) / (stone.end - stone.start)).coerceIn(0f, 1f)
            if (t <= 0f) return@forEach
            val (cx, cy) = Pair(
                margin + stone.col * cell,
                margin + stone.row * cell - (1f - t) * (1f - t) * cell * 0.6f
            )
            val radius = cell * 0.36f
            val scale = 1f + 0.25f * (1f - t)
            val alpha = (t * 2f).coerceIn(0f, 1f)
            drawCircle(
                shadow,
                radius * scale,
                Offset(cx + radius * 0.14f, cy + radius * 0.14f),
                alpha = alpha
            )
            drawCircle(
                if (stone.isBlack) blackStone else whiteStone,
                radius * scale,
                Offset(cx, cy),
                alpha = alpha
            )
            if (!stone.isBlack) {
                drawCircle(
                    whiteOutline,
                    radius * scale,
                    Offset(cx, cy),
                    style = Stroke(full * 0.008f),
                    alpha = alpha
                )
            }
        }

        // 最后一手涟漪扩散
        val rippleP = ((progress - RIPPLE_START) / (RIPPLE_END - RIPPLE_START)).coerceIn(0f, 1f)
        if (rippleP > 0f) {
            val lastStone = SPLASH_STONES.last()
            val cx = margin + lastStone.col * cell
            val cy = margin + lastStone.row * cell
            drawCircle(
                accent,
                cell * 0.36f * (1f + rippleP * 0.9f),
                Offset(cx, cy),
                style = Stroke(full * 0.012f),
                alpha = (1f - rippleP).coerceIn(0f, 1f)
            )
        }

        // 获胜连线：黑三连 (1,1) → (3,1) 竖直扫过
        val lineP = ((progress - LINE_START) / (LINE_END - LINE_START)).coerceIn(0f, 1f)
        if (lineP > 0f) {
            val startX = margin + 1 * cell
            val startY = margin + 1 * cell
            val endY = margin + 3 * cell
            drawLine(
                accent,
                Offset(startX, startY),
                Offset(startX, startY + (endY - startY) * lineP),
                strokeWidth = full * 0.02f,
                cap = StrokeCap.Round,
                alpha = 0.85f
            )
        }
    }
}
