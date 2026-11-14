package com.example.watchgomoku.util

import android.view.HapticFeedbackConstants
import android.view.View

/**
 * 触感反馈：走系统 HapticFeedback，无需 VIBRATE 权限。
 */
object Haptics {

    fun tap(view: View, enabled: Boolean) {
        if (enabled) runCatching {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    /** 成功落子 / 悔棋 */
    fun success(view: View, enabled: Boolean) {
        if (enabled) runCatching {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    /** 无效操作 / 警示 */
    fun error(view: View, enabled: Boolean) {
        if (enabled) runCatching {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }
}
