package com.example.watchgomoku.data.local

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * 本地存档 / 棋谱序列化模型（kotlinx-serialization，纯 JVM 可测试）。
 * 棋盘内容不直接序列化，而是由落子序列回放重建，天然防止棋盘数据与棋谱不一致。
 */
@Serializable
data class SavedMove(val r: Int, val c: Int, val p: Int)

@Serializable
data class SavedGame(
    val version: Int = SaveCodec.VERSION,
    val boardSize: Int,
    val mode: String,
    val difficulty: String? = null,
    /** 单人模式玩家执子：1 黑 2 白 */
    val humanColor: Int? = null,
    val firstPolicy: String? = null,
    val moves: List<SavedMove> = emptyList(),
    val winner: Int? = null,
    val isDraw: Boolean = false,
    val isGameOver: Boolean = false,
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val startEpochMillis: Long = 0L,
    /** 扣除暂停后的净对局时长 */
    val playedMillis: Long = 0L,
    val undoUsed: Int = 0,
    val paused: Boolean = false
)

/** 本地棋谱记录（最近 20 局） */
@Serializable
data class MatchRecord(
    val id: Long,
    val finishEpochMillis: Long,
    val mode: String,
    val difficulty: String? = null,
    val result: String,
    val totalMoves: Int,
    val durationMillis: Long,
    val humanColor: Int? = null,
    val boardSize: Int = 15
)

object SaveCodec {
    const val VERSION = 1
    const val RESULT_BLACK_WIN = "BLACK_WIN"
    const val RESULT_WHITE_WIN = "WHITE_WIN"
    const val RESULT_DRAW = "DRAW"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encodeGame(game: SavedGame): String =
        json.encodeToString(SavedGame.serializer(), game)

    /** 存档损坏或版本不兼容时返回 null */
    fun decodeGame(text: String): SavedGame? =
        runCatching { json.decodeFromString(SavedGame.serializer(), text) }.getOrNull()

    fun encodeHistory(records: List<MatchRecord>): String =
        json.encodeToString(ListSerializer(MatchRecord.serializer()), records)

    fun decodeHistory(text: String): List<MatchRecord> =
        runCatching {
            json.decodeFromString(ListSerializer(MatchRecord.serializer()), text)
        }.getOrDefault(emptyList())
}
