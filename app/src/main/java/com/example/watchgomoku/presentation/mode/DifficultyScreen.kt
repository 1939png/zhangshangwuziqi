package com.example.watchgomoku.presentation.mode

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.watchgomoku.R
import com.example.watchgomoku.domain.model.AiDifficulty
import com.example.watchgomoku.domain.model.FirstMovePolicy
import com.example.watchgomoku.presentation.components.OptionRow
import com.example.watchgomoku.presentation.components.PrimaryMenuButton
import com.example.watchgomoku.presentation.components.ScreenTitle
import com.example.watchgomoku.presentation.components.SectionLabel
import com.example.watchgomoku.presentation.components.ScrollContent

/**
 * 难度选择页（单人模式）：入门→自定义 六档 + 先后手 + 棋盘大小。
 * 自定义难度的深度 / 随机性 / 思考时间在“设置”中调整。
 */
@Composable
fun DifficultyScreen(
    mode: com.example.watchgomoku.domain.model.GameMode,
    defaultDifficulty: AiDifficulty,
    defaultFirstPolicy: FirstMovePolicy,
    defaultBoardSize: Int,
    onStart: (AiDifficulty, FirstMovePolicy, Int) -> Unit,
    onBack: () -> Unit
) {
    val difficulties = AiDifficulty.entries
    var selectedDifficulty by remember { mutableStateOf(defaultDifficulty) }
    var firstPolicy by remember { mutableStateOf(defaultFirstPolicy) }
    var boardSize by remember { mutableIntStateOf(defaultBoardSize) }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTitle(title = stringResource(R.string.difficulty_title), onBack = onBack)
        ScrollContent {
            SectionLabel(stringResource(R.string.difficulty_title))
            difficulties.forEach { difficulty ->
                RadioRow(
                    label = difficultyLabel(difficulty),
                    selected = selectedDifficulty == difficulty,
                    onClick = { selectedDifficulty = difficulty }
                )
            }
            SectionLabel(stringResource(R.string.first_move_title))
            FirstMovePolicy.entries.forEach { policy ->
                RadioRow(
                    label = firstPolicyLabel(policy),
                    selected = firstPolicy == policy,
                    onClick = { firstPolicy = policy }
                )
            }
            SectionLabel(stringResource(R.string.settings_board_size))
            val sizes = listOf(9, 13, 15, 19)
            OptionRow(
                label = "",
                options = sizes.map { "${it}×$it" },
                selectedIndex = sizes.indexOf(boardSize).coerceAtLeast(0),
                onSelect = { boardSize = sizes[it] }
            )
            Spacer(Modifier.height(10.dp))
            PrimaryMenuButton(
                text = stringResource(R.string.btn_start_game),
                onClick = { onStart(selectedDifficulty, firstPolicy, boardSize) }
            )
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
private fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 0.dp)
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text = label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
internal fun difficultyLabel(difficulty: AiDifficulty): String = stringResource(
    when (difficulty) {
        AiDifficulty.BEGINNER -> R.string.difficulty_beginner
        AiDifficulty.EASY -> R.string.difficulty_easy
        AiDifficulty.NORMAL -> R.string.difficulty_normal
        AiDifficulty.HARD -> R.string.difficulty_hard
        AiDifficulty.EXPERT -> R.string.difficulty_expert
        AiDifficulty.CUSTOM -> R.string.difficulty_custom
    }
)

@Composable
internal fun firstPolicyLabel(policy: FirstMovePolicy): String = stringResource(
    when (policy) {
        FirstMovePolicy.PLAYER_BLACK -> R.string.first_player_black
        FirstMovePolicy.PLAYER_WHITE -> R.string.first_player_white
        FirstMovePolicy.RANDOM -> R.string.first_random
    }
)
