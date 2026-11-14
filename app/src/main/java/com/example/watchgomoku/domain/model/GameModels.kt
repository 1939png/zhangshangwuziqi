package com.example.watchgomoku.domain.model

/**
 * 领域层核心模型：与 UI 完全解耦，纯 Kotlin，可独立单元测试。
 */

/** 执子方。黑方永远先手。 */
enum class Player {
    BLACK, WHITE;

    fun opponent(): Player = if (this == BLACK) WHITE else BLACK

    /** 内部棋盘编码：黑 1 / 白 2 */
    fun code(): Int = if (this == BLACK) BLACK_CODE else WHITE_CODE

    companion object {
        const val BLACK_CODE = 1
        const val WHITE_CODE = 2

        /** 非法编码回退为黑 */
        fun fromCode(code: Int): Player = if (code == WHITE_CODE) WHITE else BLACK
    }
}

enum class CellState { EMPTY, BLACK, WHITE }

data class Position(val row: Int, val column: Int)

data class Move(
    val position: Position,
    val player: Player,
    val moveNumber: Int,
    val timestamp: Long = System.currentTimeMillis()
)

enum class GameMode { SINGLE_PLAYER, MULTIPLAYER, PRACTICE }

/** 单人模式先后手策略 */
enum class FirstMovePolicy { PLAYER_BLACK, PLAYER_WHITE, RANDOM }

/** 悔棋次数限制：null 表示不限 */
enum class UndoQuota(val maxCount: Int?) {
    UNLIMITED(null), ONE(1), THREE(3), OFF(0)
}

/** 棋子直径系数（相对网格间距） */
enum class StoneSize(val diameterFactor: Float) {
    SMALL(0.78f), MEDIUM(0.84f), LARGE(0.88f)
}

/** 应用深浅模式 */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** 棋盘主题 */
enum class BoardTheme { WOOD, DARK, HIGH_CONTRAST }

/**
 * AI 难度档案：
 * 入门/简单走启发式快照逻辑；普通及以上走搜索。
 */
enum class AiDifficulty(
    val searchDepth: Int,
    val thinkTimeMs: Long,
    val candidateLimit: Int
) {
    BEGINNER(0, 0, 0),
    EASY(0, 0, 0),
    NORMAL(2, 1_200, 24),
    HARD(4, 3_000, 14),
    EXPERT(8, 3_000, 12),
    CUSTOM(4, 1_500, 14)
}

/** 自定义 AI 参数 */
data class CustomAiConfig(
    val depth: Int = 4,
    val thinkTimeMs: Long = 1_500,
    val randomness: Float = 0.2f
)

/**
 * 不可变游戏状态快照，UI 只读。
 * 胜负判断不依赖 UI；本类不承载任何逻辑。
 */
@Suppress("ArrayInDataClass")
data class GameState(
    val boardSize: Int,
    val board: Array<Array<CellState>>,
    val currentPlayer: Player,
    val moves: List<Move>,
    val winner: Player?,
    val isDraw: Boolean,
    val isGameOver: Boolean,
    val lastMove: Position?,
    val mode: GameMode,
    val winningLine: List<Position> = emptyList(),
    /** 单人模式下玩家执子颜色；多人/练习为 null */
    val humanColor: Player? = null,
    val difficulty: AiDifficulty? = null
)
