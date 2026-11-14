package com.example.watchgomoku

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.watchgomoku.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 首页流程 UI 测试（需设备 / 模拟器运行）。
 */
@RunWith(AndroidJUnit4::class)
class HomeFlowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun homeShowsMainButtons() {
        composeRule.waitForIdle()
        composeRule.onNodeWithText("单人对弈").assertIsDisplayed()
        composeRule.onNodeWithText("多人对弈").assertIsDisplayed()
        composeRule.onNodeWithText("设置").assertExists()
        composeRule.onNodeWithText("棋谱记录").assertExists()
        composeRule.onNodeWithText("关于").assertExists()
    }

    @Test
    fun soloNavigatesToModeSelect() {
        composeRule.waitForIdle()
        composeRule.onNodeWithText("单人对弈").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("选择模式").assertIsDisplayed()
    }
}
