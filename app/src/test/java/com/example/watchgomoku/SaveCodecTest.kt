package com.example.watchgomoku

import com.example.watchgomoku.data.local.MatchRecord
import com.example.watchgomoku.data.local.SaveCodec
import com.example.watchgomoku.data.local.SavedGame
import com.example.watchgomoku.data.local.SavedMove
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 本地存档 / 棋谱序列化测试：往返一致、损坏数据防御。
 */
class SaveCodecTest {

    @Test
    fun savedGameRoundTrip() {
        val game = SavedGame(
            boardSize = 15,
            mode = "SINGLE_PLAYER",
            difficulty = "NORMAL",
            humanColor = 1,
            firstPolicy = "PLAYER_BLACK",
            moves = listOf(SavedMove(7, 7, 1), SavedMove(7, 8, 2)),
            scale = 1.5f,
            offsetX = 10f,
            offsetY = -4f,
            startEpochMillis = 1_700_000_000_000L,
            playedMillis = 65_000L,
            undoUsed = 1
        )
        val decoded = SaveCodec.decodeGame(SaveCodec.encodeGame(game))
        assertEquals(game, decoded)
    }

    @Test
    fun corruptedGameReturnsNull() {
        assertNull(SaveCodec.decodeGame("{not valid json"))
        assertNull(SaveCodec.decodeGame(""))
    }

    @Test
    fun unknownFieldsAreIgnored() {
        val text = """{"boardSize":9,"mode":"MULTIPLAYER","futureField":123}"""
        val game = SaveCodec.decodeGame(text)
        assertTrue(game != null)
        assertEquals(9, game!!.boardSize)
        assertEquals("MULTIPLAYER", game.mode)
    }

    @Test
    fun historyRoundTrip() {
        val records = listOf(
            MatchRecord(
                id = 1L,
                finishEpochMillis = 1_700_000_000_000L,
                mode = "SINGLE_PLAYER",
                difficulty = "HARD",
                result = SaveCodec.RESULT_BLACK_WIN,
                totalMoves = 31,
                durationMillis = 180_000L,
                humanColor = 1,
                boardSize = 15
            )
        )
        val decoded = SaveCodec.decodeHistory(SaveCodec.encodeHistory(records))
        assertEquals(records, decoded)
    }

    @Test
    fun corruptedHistoryReturnsEmpty() {
        assertTrue(SaveCodec.decodeHistory("not json").isEmpty())
    }
}
