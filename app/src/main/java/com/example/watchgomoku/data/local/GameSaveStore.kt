package com.example.watchgomoku.data.local

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 自动存档：单文件 JSON（filesDir/autosave.json）。
 * 每次落子、暂停、进入后台、退出时写入；结束后清除。
 */
class GameSaveStore(private val context: Context) {

    private val file: File get() = File(context.filesDir, FILE_NAME)

    suspend fun save(game: SavedGame): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            file.parentFile?.mkdirs()
            file.writeText(SaveCodec.encodeGame(game))
        }.isSuccess
    }

    /** 文件不存在或内容损坏返回 null */
    suspend fun load(): SavedGame? = withContext(Dispatchers.IO) {
        runCatching {
            if (!file.exists()) return@runCatching null
            SaveCodec.decodeGame(file.readText())
        }.getOrNull()
    }

    suspend fun clear() {
        withContext(Dispatchers.IO) {
            runCatching { file.delete() }
        }
    }

    companion object {
        private const val FILE_NAME = "autosave.json"
    }
}
