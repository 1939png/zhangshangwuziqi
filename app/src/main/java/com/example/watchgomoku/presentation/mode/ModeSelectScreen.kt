package com.example.watchgomoku.presentation.mode

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.watchgomoku.R
import com.example.watchgomoku.presentation.components.PrimaryMenuButton
import com.example.watchgomoku.presentation.components.ScreenTitle
import com.example.watchgomoku.presentation.components.ScrollContent

/**
 * 模式选择页：单人对弈 / 多人对弈 / 练习模式。
 */
@Composable
fun ModeSelectScreen(
    onSolo: () -> Unit,
    onMulti: () -> Unit,
    onPractice: () -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTitle(title = stringResource(R.string.mode_title), onBack = onBack)
        ScrollContent {
            ModeCard(
                title = stringResource(R.string.mode_solo),
                desc = stringResource(R.string.mode_solo_desc),
                glyph = "●",
                onClick = onSolo
            )
            ModeCard(
                title = stringResource(R.string.mode_multi),
                desc = stringResource(R.string.mode_multi_desc),
                glyph = "●○",
                onClick = onMulti
            )
            ModeCard(
                title = stringResource(R.string.mode_practice),
                desc = stringResource(R.string.mode_practice_desc),
                glyph = "✎",
                onClick = onPractice
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ModeCard(title: String, desc: String, glyph: String, onClick: () -> Unit) {
    PrimaryMenuButton(text = title, glyph = glyph, onClick = onClick)
    Text(
        text = desc,
        fontSize = 10.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 6.dp, top = 1.dp, bottom = 6.dp)
    )
}
