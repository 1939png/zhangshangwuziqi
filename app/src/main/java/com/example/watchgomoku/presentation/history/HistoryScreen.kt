package com.example.watchgomoku.presentation.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.watchgomoku.R
import com.example.watchgomoku.data.local.MatchRecord
import com.example.watchgomoku.data.local.SaveCodec
import com.example.watchgomoku.domain.model.AiDifficulty
import com.example.watchgomoku.presentation.components.ConfirmDialog
import com.example.watchgomoku.presentation.components.ScreenTitle
import com.example.watchgomoku.presentation.components.SecondaryMenuButton
import com.example.watchgomoku.presentation.components.ScrollContent
import com.example.watchgomoku.presentation.mode.difficultyLabel
import com.example.watchgomoku.util.TimeFormat

/**
 * 棋谱记录页：最近 20 局（时间 / 模式 / 难度 / 结果 / 步数 / 时长）。
 */
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onBack: () -> Unit
) {
    val records by viewModel.records.collectAsStateWithLifecycle()
    var showClear by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTitle(title = stringResource(R.string.history_title), onBack = onBack)
        ScrollContent {
            if (records.isEmpty()) {
                Text(
                    text = stringResource(R.string.history_empty),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 20.dp)
                )
            } else {
                records.forEach { record ->
                    HistoryItem(record)
                }
            }
            Spacer(Modifier.height(6.dp))
            if (records.isNotEmpty()) {
                SecondaryMenuButton(
                    text = stringResource(R.string.history_clear),
                    onClick = { showClear = true }
                )
            }
            Spacer(Modifier.height(14.dp))
        }
    }

    if (showClear) {
        ConfirmDialog(
            title = stringResource(R.string.history_clear),
            message = stringResource(R.string.settings_clear_confirm),
            confirmText = stringResource(R.string.confirm_ok),
            cancelText = stringResource(R.string.confirm_cancel),
            onConfirm = {
                showClear = false
                viewModel.clear()
            },
            onDismiss = { showClear = false }
        )
    }
}

@Composable
private fun HistoryItem(record: MatchRecord) {
    val resultText = when (record.result) {
        SaveCodec.RESULT_BLACK_WIN -> stringResource(R.string.history_result_black)
        SaveCodec.RESULT_WHITE_WIN -> stringResource(R.string.history_result_white)
        else -> stringResource(R.string.history_result_draw)
    }
    val resultColor = when (record.result) {
        SaveCodec.RESULT_DRAW -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.primary
    }
    val modeText = when (record.mode) {
        "SINGLE_PLAYER" -> stringResource(R.string.history_mode_solo)
        "MULTIPLAYER" -> stringResource(R.string.history_mode_multi)
        else -> stringResource(R.string.history_mode_practice)
    }
    val difficultyText = record.difficulty
        ?.let { runCatching { AiDifficulty.valueOf(it) }.getOrNull() }
        ?.let { stringResource(R.string.history_difficulty_fmt, difficultyLabel(it)) }

    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = resultText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = resultColor,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = TimeFormat.formatDateTime(record.finishEpochMillis),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = modeText + (difficultyText?.let { " · $it" } ?: "") +
                    " · ${record.boardSize}路",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.history_moves_fmt, record.totalMoves) +
                    " · " + stringResource(
                        R.string.history_duration_fmt,
                        TimeFormat.formatDuration(record.durationMillis)
                    ),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
