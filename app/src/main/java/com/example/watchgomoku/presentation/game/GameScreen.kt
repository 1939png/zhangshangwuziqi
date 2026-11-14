package com.example.watchgomoku.presentation.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.watchgomoku.GomokuApplication
import com.example.watchgomoku.R
import com.example.watchgomoku.data.local.UiSettings
import com.example.watchgomoku.domain.model.AiDifficulty
import com.example.watchgomoku.domain.model.FirstMovePolicy
import com.example.watchgomoku.domain.model.GameMode
import com.example.watchgomoku.domain.model.Player
import com.example.watchgomoku.domain.model.UndoQuota
import com.example.watchgomoku.presentation.components.ConfirmDialog
import com.example.watchgomoku.presentation.game.GameViewModel.GameUiState
import com.example.watchgomoku.presentation.settings.SettingsViewModel
import com.example.watchgomoku.ui.theme.BoardPalettes
import com.example.watchgomoku.ui.theme.isCompactLayout
import com.example.watchgomoku.ui.theme.scaledDp
import com.example.watchgomoku.ui.theme.watchSafePadding
import com.example.watchgomoku.util.Haptics
import com.example.watchgomoku.util.SoundPlayer
import com.example.watchgomoku.util.TimeFormat
import kotlinx.coroutines.delay

/**
 * 对局页：顶部状态区 / 中部棋盘（缩放按钮、提示条、暂停与轮流遮罩）/
 * 底部操作区 / 结果面板 / 退出与重开确认弹窗。
 *
 * 布局策略：
 * - 状态栏与操作栏按内容自适应高度，棋盘占据全部剩余空间（文字放大不挤压棋盘）；
 * - 根布局应用圆角安全区，按钮远离屏幕四角；
 * - 小屏 / 文字 ≥130% 进入紧凑模式：状态合并为一行、操作按钮只保留图标。
 */
@Composable
fun GameRoute(
    mode: GameMode,
    difficulty: AiDifficulty,
    firstPolicy: FirstMovePolicy,
    boardSize: Int,
    restore: Boolean,
    settingsViewModel: SettingsViewModel,
    onExit: () -> Unit,
    onViewHistory: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as GomokuApplication
    val viewModel: GameViewModel = viewModel(
        key = "game_view_model",
        factory = viewModelFactory {
            initializer {
                GameViewModel(
                    application = app,
                    container = app.container,
                    config = GameViewModel.GameLaunchConfig(
                        mode = mode,
                        difficulty = difficulty,
                        firstPolicy = firstPolicy,
                        boardSize = boardSize,
                        restore = restore
                    )
                )
            }
        }
    )
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    GameScreen(
        viewModel = viewModel,
        settingsViewModel = settingsViewModel,
        settings = settings,
        state = state,
        onExit = onExit,
        onViewHistory = onViewHistory
    )
}

@Composable
private fun GameScreen(
    viewModel: GameViewModel,
    settingsViewModel: SettingsViewModel,
    settings: UiSettings,
    state: GameUiState,
    onExit: () -> Unit,
    onViewHistory: () -> Unit
) {
    val view = LocalView.current
    val gameState = state.gameState
    val showResult = gameState.isGameOver
    val compact = isCompactLayout()
    var boardBoxPx by remember { mutableFloatStateOf(0f) }

    // 消息提示（伴随错误触感 + 音效）
    LaunchedEffect(state.messageRes) {
        state.messageRes?.let {
            Haptics.error(view, settings.vibration)
            SoundPlayer.play(settings.sound, SoundPlayer.Effect.INVALID)
            delay(1200)
            viewModel.consumeMessage()
        }
    }
    // 落子反馈
    LaunchedEffect(gameState.moves.size) {
        if (gameState.moves.isNotEmpty() && !gameState.isGameOver) {
            Haptics.success(view, settings.vibration)
            SoundPlayer.play(settings.sound, SoundPlayer.Effect.STONE)
        }
    }
    // 终局反馈
    LaunchedEffect(gameState.isGameOver) {
        if (gameState.isGameOver) {
            Haptics.success(view, settings.vibration)
            SoundPlayer.play(settings.sound, SoundPlayer.Effect.WIN)
        }
    }
    // 进入后台自动保存
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) viewModel.onHostStop()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler(enabled = true) {
        when {
            state.showExitConfirm -> viewModel.dismissExit()
            state.showRestartConfirm -> viewModel.dismissRestart()
            state.paused -> {
                viewModel.exitAndSave()
                onExit()
            }
            else -> viewModel.requestExit()
        }
    }

    val palette = BoardPalettes.of(settings.boardTheme)
    val boardEnabled = !state.paused && !showResult && !state.thinking &&
        !(gameState.mode == GameMode.SINGLE_PLAYER &&
            gameState.humanColor != null &&
            gameState.currentPlayer != gameState.humanColor)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .watchSafePadding()
    ) {
        GameStatusBar(state, compact)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .onSizeChanged { boardBoxPx = it.width.toFloat() },
            contentAlignment = Alignment.Center
        ) {
            LaunchedEffect(boardBoxPx) {
                if (boardBoxPx > 0f) viewModel.boardView.clampTo(boardBoxPx)
            }
            GomokuBoardCanvas(
                gameState = gameState,
                viewState = viewModel.boardView,
                palette = palette,
                showLastMove = settings.showLastMove,
                showNumbers = settings.showMoveNumbers,
                showCoordinates = settings.showCoordinates,
                stoneFactor = settings.stoneSize.diameterFactor,
                hints = state.hints,
                enabled = boardEnabled,
                onTap = { row, col ->
                    Haptics.tap(view, settings.vibration)
                    viewModel.onBoardTap(row, col)
                },
                modifier = Modifier.fillMaxSize()
            )
            // 缩放按钮：位于棋盘右缘垂直居中，远离屏幕四角圆角区
            BoardZoomControls(
                onZoomIn = { viewModel.boardView.zoomBy(1.25f, boardBoxPx) },
                onZoomOut = { viewModel.boardView.zoomBy(0.8f, boardBoxPx) },
                onFit = { viewModel.boardView.fit() },
                onCenter = { viewModel.boardView.center(boardBoxPx) },
                modifier = Modifier.align(Alignment.CenterEnd)
            )
            if (state.messageRes != null) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.inverseSurface,
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Text(
                        text = stringResource(state.messageRes),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
            if (state.showTurnOverlay) {
                TurnOverlay(
                    sideText = stringResource(
                        if (gameState.currentPlayer == Player.BLACK) {
                            R.string.side_black
                        } else {
                            R.string.side_white
                        }
                    ),
                    onDismiss = { viewModel.dismissTurnOverlay() }
                )
            }
            if (state.paused && !showResult) {
                PauseOverlay(
                    onResume = { viewModel.resume() },
                    onSaveExit = {
                        viewModel.exitAndSave()
                        onExit()
                    }
                )
            }
        }

        if (showResult) {
            ResultPanel(
                state = state,
                onAgain = { viewModel.restartNow() },
                onHome = {
                    viewModel.exitAndSave()
                    onExit()
                },
                onViewHistory = onViewHistory
            )
        } else {
            GameControlBar(
                viewModel = viewModel,
                state = state,
                settings = settings,
                settingsViewModel = settingsViewModel,
                compact = compact,
                onExit = onExit
            )
        }
    }

    if (state.showExitConfirm) {
        ConfirmDialog(
            title = stringResource(R.string.exit_title),
            message = stringResource(R.string.exit_confirm_msg),
            confirmText = stringResource(R.string.exit_save),
            cancelText = stringResource(R.string.exit_cancel),
            onConfirm = {
                viewModel.exitAndSave()
                onExit()
            },
            onDismiss = { viewModel.dismissExit() }
        )
    }
    if (state.showRestartConfirm) {
        ConfirmDialog(
            title = stringResource(R.string.new_game_title),
            message = stringResource(R.string.new_game_confirm_msg),
            confirmText = stringResource(R.string.confirm_ok),
            cancelText = stringResource(R.string.confirm_cancel),
            onConfirm = { viewModel.restartNow() },
            onDismiss = { viewModel.dismissRestart() }
        )
    }
}

@Composable
private fun GameStatusBar(state: GameUiState, compact: Boolean) {
    val gameState = state.gameState
    val humanSideName = stringResource(R.string.player_human)
    val aiName = stringResource(R.string.player_ai)
    val blackName = if (gameState.humanColor == Player.WHITE) aiName else humanSideName
    val whiteName = if (gameState.humanColor == Player.WHITE) humanSideName else aiName
    val turnSide = stringResource(
        if (gameState.currentPlayer == Player.BLACK) R.string.side_black else R.string.side_white
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 紧凑模式隐藏次要的双方身份行，优先保证当前回合与棋盘空间
        if (!compact) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.status_black_fmt, blackName),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.status_white_fmt, whiteName),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(R.string.status_turn_fmt, turnSide),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
            if (state.thinking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(13.dp),
                    strokeWidth = 2.dp
                )
                Text(
                    text = stringResource(R.string.ai_thinking),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun GameControlBar(
    viewModel: GameViewModel,
    state: GameUiState,
    settings: UiSettings,
    settingsViewModel: SettingsViewModel,
    compact: Boolean,
    onExit: () -> Unit
) {
    val view = LocalView.current
    val gameState = state.gameState
    val undoEnabled = !gameState.isGameOver && !state.thinking &&
        gameState.moves.isNotEmpty() && settings.allowUndo &&
        settings.undoQuota != UndoQuota.OFF &&
        (state.undoLeft == null || state.undoLeft > 0)
    var menuOpen by remember { mutableStateOf(false) }

    fun feedback() {
        Haptics.tap(view, settings.vibration)
        SoundPlayer.play(settings.sound, SoundPlayer.Effect.BUTTON)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ControlButton(
            label = stringResource(R.string.btn_undo),
            glyph = "↶",
            enabled = undoEnabled,
            compact = compact,
            modifier = Modifier.weight(1f)
        ) {
            feedback()
            viewModel.undo()
        }
        ControlButton(
            label = stringResource(R.string.btn_restart),
            glyph = "⟳",
            enabled = true,
            compact = compact,
            modifier = Modifier.weight(1f)
        ) {
            feedback()
            viewModel.requestRestart()
        }
        ControlButton(
            label = stringResource(
                if (state.paused) R.string.btn_resume else R.string.btn_pause
            ),
            glyph = if (state.paused) "▶" else "Ⅱ",
            enabled = !gameState.isGameOver,
            compact = compact,
            modifier = Modifier.weight(1f)
        ) {
            feedback()
            if (state.paused) viewModel.resume() else viewModel.pause()
        }
        Box(modifier = Modifier.weight(1f)) {
            ControlButton(
                label = stringResource(R.string.btn_more),
                glyph = "⋮",
                enabled = true,
                compact = compact,
                modifier = Modifier.fillMaxWidth()
            ) {
                feedback()
                menuOpen = true
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.settings_sound) + ": " +
                                stringResource(if (settings.sound) R.string.on else R.string.off),
                            fontSize = 13.sp
                        )
                    },
                    onClick = {
                        menuOpen = false
                        settingsViewModel.update { it.copy(sound = !it.sound) }
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.settings_vibration) + ": " +
                                stringResource(if (settings.vibration) R.string.on else R.string.off),
                            fontSize = 13.sp
                        )
                    },
                    onClick = {
                        menuOpen = false
                        settingsViewModel.update { it.copy(vibration = !it.vibration) }
                    }
                )
                if (gameState.mode == GameMode.PRACTICE) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.btn_hint), fontSize = 13.sp) },
                        onClick = {
                            menuOpen = false
                            viewModel.toggleHints()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.btn_clear_board), fontSize = 13.sp) },
                        onClick = {
                            menuOpen = false
                            viewModel.restartNow()
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_exit_game), fontSize = 13.sp) },
                    onClick = {
                        menuOpen = false
                        viewModel.exitAndSave()
                        onExit()
                    }
                )
            }
        }
    }
}

/**
 * 底部操作按钮：紧凑模式只显示图标（保留 contentDescription）；
 * 高度随文字缩放自适应（最小 40dp），文字不裁切。
 */
@Composable
private fun ControlButton(
    label: String,
    glyph: String,
    enabled: Boolean,
    compact: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .heightIn(min = scaledDp(40.dp))
            .semantics { contentDescription = label },
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        Text(
            text = if (compact) glyph else "$glyph $label",
            fontSize = if (compact) 16.sp else 12.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun PauseOverlay(onResume: () -> Unit, onSaveExit: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.72f))
            .clickable(onClick = onResume),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.pause_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onResume,
                modifier = Modifier.widthIn(min = scaledDp(120.dp))
            ) {
                Text(stringResource(R.string.pause_resume), fontSize = 13.sp, maxLines = 1)
            }
            TextButton(onClick = onSaveExit) {
                Text(stringResource(R.string.exit_save), fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun TurnOverlay(sideText: String, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.96f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.watchSafePadding()
        ) {
            Text(
                text = stringResource(R.string.turn_overlay_fmt, sideText),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.turn_overlay_tap),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ResultPanel(
    state: GameUiState,
    onAgain: () -> Unit,
    onHome: () -> Unit,
    onViewHistory: () -> Unit
) {
    val gameState = state.gameState
    val resultText = when {
        gameState.winner == Player.BLACK -> stringResource(R.string.result_black_win)
        gameState.winner == Player.WHITE -> stringResource(R.string.result_white_win)
        else -> stringResource(R.string.result_draw)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = resultText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1
        )
        Text(
            text = stringResource(R.string.history_moves_fmt, gameState.moves.size) +
                " · " + TimeFormat.formatDuration(state.durationMillis),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onAgain,
                modifier = Modifier.heightIn(min = scaledDp(42.dp))
            ) {
                Text(stringResource(R.string.btn_again), fontSize = 13.sp, maxLines = 1)
            }
            FilledTonalButton(
                onClick = onHome,
                modifier = Modifier.heightIn(min = scaledDp(42.dp))
            ) {
                Text(stringResource(R.string.btn_home), fontSize = 13.sp, maxLines = 1)
            }
        }
        TextButton(
            onClick = onViewHistory,
            modifier = Modifier.heightIn(min = scaledDp(34.dp))
        ) {
            Text(stringResource(R.string.btn_view_history), fontSize = 12.sp, maxLines = 1)
        }
    }
}
