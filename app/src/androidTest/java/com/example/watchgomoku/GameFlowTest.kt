package com.example.watchgomoku

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.watchgomoku.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 多人对局流程 UI 测试（需设备 / 模拟器运行）：
 * 进入多人模式 → 棋盘落子 → 当前手切换 → 悔棋回退。
 */
@RunWith(AndroidJUnit4::class)
class GameFlowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun multiplayerPlaceAndUndo() {
        composeRule.waitForIdle()
        composeRule.onNodeWithText("多人对弈").performClick()
        composeRule.waitForIdle()

        // 黑方先手
        composeRule.onNodeWithText("当前：黑方").assertExists()

        // 点击棋盘中心（棋盘有 contentDescription 前缀）
        composeRule.onNodeWithContentDescription("五子棋棋盘", substring = true)
            .performTouchInput { click(center) }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("当前：白方").assertExists()

        // 悔棋回退到黑方
        composeRule.onNodeWithText("↶ 悔棋").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("当前：黑方").assertExists()
    }
}
