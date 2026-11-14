package com.example.watchgomoku.presentation.game

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.watchgomoku.R
import com.example.watchgomoku.data.AppContainer
import com.example.watchgomoku.data.local.DefaultSettings
import com.example.watchgomoku.data.local.MatchRecord
import com.example.watchgomoku.data.local.SaveCodec
import com.example.watchgomoku.data.local.SavedGame
import com.example.watchgomoku.data.local.SavedMove
import com.example.watchgomoku.data.local.UiSettings
import com.example.watchgomoku.data.repository.GameRepository
import com.example.watchgomoku.domain.ai.AiPlayer
import com.example.watchgomoku.domain.ai.MoveGenerator
import com.example.watchgomoku.domain.engine.Board
import com.example.watchgomoku.domain.engine.GomokuRules
import com.example.watchgomoku.domain.model.AiDifficulty
import com.example.watchgomoku.domain.model.CustomAiConfig
import com.example.watchgomoku.domain.model.FirstMovePolicy
import com.example.watchgomoku.domain.model.GameMode
import com.example.watchgomoku.domain.model.GameState
import com.example.watchgomoku.domain.model.Move
import com.example.watchgomoku.domain.model.Player
import com.example.watchgomoku.domain.model.Position
import com.example.watchgomoku.domain.model.UndoQuota
import com.example.watchgomoku.domain.usecase.UndoPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * 对局 ViewModel：全部游戏流程状态机。
 * - 游戏逻辑走领域层（Board / GomokuRules / AiPlayer），本类只编排；
 * - AI 在 Default 调度器后台执行，可取消，超时返回当前最佳；
 * - 每次落子/暂停/退出自动保存；结束后清存档并写棋谱。
 */
class GameViewModel(
    application: Application,
    container: AppContainer,
    private val config: GameLaunchConfig
) : AndroidViewModel(application) {

    data class GameLaunchConfig(
        val mode: GameMode,
        val difficulty: AiDifficulty = AiDifficulty.NORMAL,
        val firstPolicy: FirstMovePolicy = FirstMovePolicy.PLAYER_BLACK,
        val boardSize: Int = 15,
        val restore: Boolean = false
    )

    data class GameUiState(
        val gameState: GameState,
        val thinking: Boolean = false,
        val paused: Boolean = false,
        val showExitConfirm: Boolean = false,
        val showRestartConfirm: Boolean = false,
        val showTurnOverlay: Boolean = false,
        val hints: List<Position> = emptyList(),
        @StringRes val messageRes: Int? = null,
        /** null 表示不限次数 */
        val undoLeft: Int? = null,
        val startedAtMillis: Long = 0L,
        val playedMillis: Long = 0L,
        val durationMillis: Long = 0L
    )

    private val repository: GameRepository = container.repository

    private var currentSettings: UiSettings = DefaultSettings
    private var board = Board(config.boardSize)

    var humanColor: Player? = null
        private set
    val aiColor: Player? get() = humanColor?.opponent()

    val boardView = BoardViewState()

    private var aiJob: Job? = null
    private var pausedAtMillis = 0L
    private var pausedAccumMillis = 0L
    private var lastAutosaveAt = 0L

    private val _uiState = MutableStateFlow(loadingState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.settings.collect { currentSettings = it }
        }
        if (config.restore) {
            restoreFromSave()
        } else {
            startNewGame()
        }
    }

    // ---------------------------------------------------------------- 新局

    fun startNewGame() {
        aiJob?.cancel()
        aiJob = null
        board = Board(config.boardSize)
        pausedAtMillis = 0L
        pausedAccumMillis = 0L
        humanColor = when (config.mode) {
            GameMode.SINGLE_PLAYER -> when (config.firstPolicy) {
                FirstMovePolicy.PLAYER_BLACK -> Player.BLACK
                FirstMovePolicy.PLAYER_WHITE -> Player.WHITE
                FirstMovePolicy.RANDOM ->
                    if (Random.nextBoolean()) Player.BLACK else Player.WHITE
            }
            else -> null
        }
        _uiState.value = GameUiState(
            gameState = buildGameState(Player.BLACK),
            startedAtMillis = System.currentTimeMillis(),
            undoLeft = undoQuotaTotal(),
            showTurnOverlay = config.mode == GameMode.MULTIPLAYER &&
                currentSettings.hideBoardOnTurn
        )
        if (config.mode == GameMode.SINGLE_PLAYER && aiColor == Player.BLACK) {
            scheduleAiMove()
        }
        autosave(force = true)
    }

    // ---------------------------------------------------------------- 落子

    fun onBoardTap(row: Int, col: Int) {
        val s = _uiState.value
        if (s.paused) return
        if (s.gameState.isGameOver) {
            toast(R.string.msg_game_over)
            return
        }
        if (s.thinking ||
            (config.mode == GameMode.SINGLE_PLAYER && s.gameState.currentPlayer == aiColor)
        ) {
            toast(R.string.msg_wait_ai)
            return
        }
        if (!board.isEmpty(row, col)) {
            toast(R.string.msg_cell_occupied)
            return
        }
        placeStone(row, col, s.gameState.currentPlayer)
    }

    private fun placeStone(row: Int, col: Int, player: Player) {
        if (!board.place(row, col, player)) {
            toast(R.string.msg_cell_occupied)
            return
        }
        val winLine = GomokuRules.winningLine(
            board, Position(row, col), player, overlineWins = true
        )
        val draw = winLine.isEmpty() && GomokuRules.isDraw(board)
        val gameOver = winLine.isNotEmpty() || draw
        _uiState.update {
            it.copy(
                gameState = buildGameState(
                    current = if (gameOver) it.gameState.currentPlayer else player.opponent(),
                    winner = if (winLine.isNotEmpty()) player else null,
                    isDraw = draw,
                    isOver = gameOver,
                    winLine = winLine
                ),
                hints = emptyList(),
                showTurnOverlay = false
            )
        }
        autosave()
        when {
            winLine.isNotEmpty() -> finishGame(player)
            draw -> finishGame(null)
            config.mode == GameMode.SINGLE_PLAYER && player.opponent() == aiColor ->
                scheduleAiMove()
            config.mode == GameMode.MULTIPLAYER && currentSettings.hideBoardOnTurn ->
                _uiState.update { st -> st.copy(showTurnOverlay = true) }
        }
    }

    // ---------------------------------------------------------------- AI

    private fun scheduleAiMove() {
        aiJob?.cancel()
        val ai = aiColor ?: return
        aiJob = viewModelScope.launch {
            _uiState.update { it.copy(thinking = true) }
            val thinkTimeMs = currentSettings.aiThinkTimeSeconds * 1000L
            val move = withContext(Dispatchers.Default) {
                AiPlayer.chooseMove(
                    board = board.copy(),
                    ai = ai,
                    difficulty = config.difficulty,
                    custom = CustomAiConfig(
                        depth = currentSettings.customDepth,
                        thinkTimeMs = thinkTimeMs,
                        randomness = currentSettings.customRandomness
                    ),
                    thinkTimeOverrideMs = thinkTimeMs,
                    cancelled = { !isActive }
                )
            }
            if (!isActive) return@launch
            _uiState.update { it.copy(thinking = false) }
            move?.let { placeStone(it.row, it.column, ai) }
        }
    }

    // ---------------------------------------------------------------- 悔棋

    fun undo() {
        val s = _uiState.value
        if (s.gameState.isGameOver) {
            toast(R.string.msg_game_over)
            return
        }
        if (s.thinking) {
            aiJob?.cancel()
            _uiState.update { it.copy(thinking = false) }
        }
        if (!currentSettings.allowUndo || currentSettings.undoQuota == UndoQuota.OFF) {
            toast(R.string.msg_undo_disabled)
            return
        }
        val left = s.undoLeft
        if (left != null && left <= 0) {
            toast(R.string.msg_undo_exhausted)
            return
        }
        val needed = UndoPolicy.pliesToRemove(config.mode, humanColor, s.gameState.currentPlayer)
        val removeCount = minOf(needed, board.moveCount())
        if (removeCount <= 0) return
        repeat(removeCount) { board.undoLast() }
        val nextPlayer = if (board.moveCount() % 2 == 0) Player.BLACK else Player.WHITE
        _uiState.update {
            it.copy(
                gameState = buildGameState(nextPlayer),
                undoLeft = left?.minus(1),
                hints = emptyList(),
                showTurnOverlay = false
            )
        }
        autosave()
        if (config.mode == GameMode.SINGLE_PLAYER && nextPlayer == aiColor) {
            scheduleAiMove()
        }
    }

    // ---------------------------------------------------------------- 重开

    fun requestRestart() {
        if (_uiState.value.gameState.isGameOver) {
            restartNow()
        } else {
            _uiState.update { it.copy(showRestartConfirm = true) }
        }
    }

    fun dismissRestart() {
        _uiState.update { it.copy(showRestartConfirm = false) }
    }

    fun restartNow() {
        _uiState.update { it.copy(showRestartConfirm = false) }
        startNewGame()
        toast(R.string.msg_new_game)
    }

    // ---------------------------------------------------------------- 暂停

    fun pause() {
        val s = _uiState.value
        if (s.paused || s.gameState.isGameOver) return
        pausedAtMillis = System.currentTimeMillis()
        aiJob?.cancel()
        aiJob = null
        _uiState.update { it.copy(paused = true, thinking = false) }
        autosave(force = true)
    }

    fun resume() {
        val s = _uiState.value
        if (!s.paused) return
        pausedAccumMillis += System.currentTimeMillis() - pausedAtMillis
        _uiState.update { it.copy(paused = false, showTurnOverlay = false) }
        if (config.mode == GameMode.SINGLE_PLAYER && s.gameState.currentPlayer == aiColor) {
            scheduleAiMove()
        }
    }

    // ---------------------------------------------------------------- 退出

    fun requestExit() {
        if (_uiState.value.showTurnOverlay) {
            _uiState.update { it.copy(showTurnOverlay = false) }
            return
        }
        _uiState.update { it.copy(showExitConfirm = true) }
    }

    fun dismissExit() {
        _uiState.update { it.copy(showExitConfirm = false) }
    }

    fun exitAndSave() {
        val s = _uiState.value
        if (s.paused) {
            pausedAccumMillis += System.currentTimeMillis() - pausedAtMillis
        }
        aiJob?.cancel()
        _uiState.update { it.copy(showExitConfirm = false, playedMillis = playedDurationMillis()) }
        autosave(force = true)
    }

    /** 进入后台 / 生命周期保存 */
    fun onHostStop() {
        autosave(force = true)
    }

    fun dismissTurnOverlay() {
        _uiState.update { it.copy(showTurnOverlay = false) }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(messageRes = null) }
    }

    // ---------------------------------------------------------------- 练习模式

    fun toggleHints() {
        val s = _uiState.value
        if (s.hints.isNotEmpty()) {
            _uiState.update { it.copy(hints = emptyList()) }
            return
        }
        if (s.gameState.isGameOver || s.paused || s.thinking) return
        viewModelScope.launch(Dispatchers.Default) {
            val list = MoveGenerator.candidates(board, s.gameState.currentPlayer, 2, 3)
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(hints = list, messageRes = R.string.msg_hint_shown)
                }
            }
        }
    }

    // ---------------------------------------------------------------- 内部

    private fun loadingState(): GameUiState =
        GameUiState(gameState = emptyGameState())

    private fun emptyGameState(): GameState {
        val empty = Board(config.boardSize)
        return GameState(
            boardSize = config.boardSize,
            board = empty.toCellStates(),
            currentPlayer = Player.BLACK,
            moves = emptyList(),
            winner = null,
            isDraw = false,
            isGameOver = false,
            lastMove = null,
            mode = config.mode,
            difficulty = if (config.mode == GameMode.SINGLE_PLAYER) config.difficulty else null
        )
    }

    private fun buildGameState(
        current: Player,
        winner: Player? = null,
        isDraw: Boolean = false,
        isOver: Boolean = false,
        winLine: List<Position> = emptyList()
    ): GameState = GameState(
        boardSize = board.size,
        board = board.toCellStates(),
        currentPlayer = current,
        moves = board.moves(),
        winner = winner,
        isDraw = isDraw,
        isGameOver = isOver,
        lastMove = board.lastMove()?.position,
        mode = config.mode,
        winningLine = winLine,
        humanColor = humanColor,
        difficulty = if (config.mode == GameMode.SINGLE_PLAYER) config.difficulty else null
    )

    private fun finishGame(winner: Player?) {
        aiJob?.cancel()
        aiJob = null
        val duration = playedDurationMillis()
        _uiState.update {
            it.copy(
                gameState = it.gameState.copy(
                    winner = winner,
                    isDraw = winner == null,
                    isGameOver = true
                ),
                thinking = false,
                durationMillis = duration,
                showTurnOverlay = false
            )
        }
        val result = when (winner) {
            Player.BLACK -> SaveCodec.RESULT_BLACK_WIN
            Player.WHITE -> SaveCodec.RESULT_WHITE_WIN
            null -> SaveCodec.RESULT_DRAW
        }
        val finishAt = System.currentTimeMillis()
        viewModelScope.launch {
            repository.addMatch(
                MatchRecord(
                    id = finishAt,
                    finishEpochMillis = finishAt,
                    mode = config.mode.name,
                    difficulty = if (config.mode == GameMode.SINGLE_PLAYER) {
                        config.difficulty.name
                    } else {
                        null
                    },
                    result = result,
                    totalMoves = board.moveCount(),
                    durationMillis = duration,
                    humanColor = humanColor?.code(),
                    boardSize = board.size
                )
            )
            repository.clearSavedGame()
        }
    }

    private fun playedDurationMillis(): Long {
        val pausedTotal = pausedAccumMillis +
            if (_uiState.value.paused) System.currentTimeMillis() - pausedAtMillis else 0L
        return (System.currentTimeMillis() - _uiState.value.startedAtMillis - pausedTotal)
            .coerceAtLeast(0L)
    }

    private fun undoQuotaTotal(): Int? {
        if (!currentSettings.allowUndo) return 0
        return currentSettings.undoQuota.maxCount // null = 不限
    }

    private fun autosave(force: Boolean = false) {
        val s = _uiState.value
        if (s.gameState.isGameOver) return
        if (!currentSettings.autosave && !force) return
        val now = System.currentTimeMillis()
        if (!force && now - lastAutosaveAt < 300L) return
        lastAutosaveAt = now
        viewModelScope.launch {
            repository.saveGame(toSavedGame(s))
        }
    }

    private fun toSavedGame(s: GameUiState): SavedGame {
        val quotaTotal = undoQuotaTotal()
        val undoUsed = if (quotaTotal == null) 0 else quotaTotal - (s.undoLeft ?: quotaTotal)
        return SavedGame(
            boardSize = board.size,
            mode = config.mode.name,
            difficulty = if (config.mode == GameMode.SINGLE_PLAYER) config.difficulty.name else null,
            humanColor = humanColor?.code(),
            firstPolicy = config.firstPolicy.name,
            moves = board.moves().map {
                SavedMove(it.position.row, it.position.column, it.player.code())
            },
            scale = boardView.scale,
            offsetX = boardView.offsetX,
            offsetY = boardView.offsetY,
            startEpochMillis = s.startedAtMillis,
            playedMillis = playedDurationMillis(),
            undoUsed = undoUsed.coerceAtLeast(0),
            paused = s.paused
        )
    }

    private fun restoreFromSave() {
        viewModelScope.launch {
            val saved = repository.loadGame()
            val valid = saved != null &&
                !saved.isGameOver &&
                saved.moves.isNotEmpty() &&
                saved.boardSize in VALID_BOARD_SIZES &&
                saved.mode == config.mode.name
            if (!valid) {
                toast(R.string.msg_restore_failed)
                startNewGame()
                return@launch
            }
            applySavedGame(saved!!)
        }
    }

    private fun applySavedGame(saved: SavedGame) {
        board = Board(saved.boardSize)
        board.replay(
            saved.moves.map { Move(Position(it.r, it.c), Player.fromCode(it.p), 0) }
        )
        humanColor = saved.humanColor?.let { Player.fromCode(it) }
        boardView.load(saved.scale, saved.offsetX, saved.offsetY)
        val nextPlayer = if (board.moveCount() % 2 == 0) Player.BLACK else Player.WHITE
        val quotaTotal = undoQuotaTotal()
        _uiState.value = GameUiState(
            gameState = buildGameState(nextPlayer),
            startedAtMillis = saved.startEpochMillis,
            playedMillis = saved.playedMillis,
            paused = saved.paused,
            undoLeft = quotaTotal?.let { total -> (total - saved.undoUsed).coerceAtLeast(0) },
            showTurnOverlay = saved.paused && config.mode == GameMode.MULTIPLAYER &&
                currentSettings.hideBoardOnTurn
        )
        if (!saved.paused && config.mode == GameMode.SINGLE_PLAYER && nextPlayer == aiColor) {
            scheduleAiMove()
        }
    }

    private fun toast(@StringRes res: Int) {
        _uiState.update { it.copy(messageRes = res) }
    }

    override fun onCleared() {
        aiJob?.cancel()
        super.onCleared()
    }

    companion object {
        val VALID_BOARD_SIZES = listOf(9, 13, 15, 19)
    }
}
