package com.example.watchgomoku.data.local

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 本地棋谱：最近 20 局，JSON 文件存储 + StateFlow 通知 UI。
 */
class MatchHistoryStore(private val context: Context) {

    private val file: File get() = File(context.filesDir, FILE_NAME)

    private val _records = MutableStateFlow<List<MatchRecord>>(emptyList())
    val records: StateFlow<List<MatchRecord>> = _records.asStateFlow()

    suspend fun refresh() {
        _records.value = loadFromDisk()
    }

    suspend fun add(record: MatchRecord) {
        val merged = (loadFromDisk() + record)
            .sortedByDescending { it.finishEpochMillis }
            .take(MAX_RECORDS)
        writeToDisk(merged)
        _records.value = merged
    }

    suspend fun clear() {
        writeToDisk(emptyList())
        _records.value = emptyList()
    }

    private suspend fun loadFromDisk(): List<MatchRecord> = withContext(Dispatchers.IO) {
        runCatching {
            if (!file.exists()) emptyList() else SaveCodec.decodeHistory(file.readText())
        }.getOrDefault(emptyList())
    }

    private suspend fun writeToDisk(records: List<MatchRecord>) {
        withContext(Dispatchers.IO) {
            runCatching {
                file.parentFile?.mkdirs()
                file.writeText(SaveCodec.encodeHistory(records))
            }
        }
    }

    companion object {
        private const val FILE_NAME = "history.json"
        private const val MAX_RECORDS = 20
    }
}
