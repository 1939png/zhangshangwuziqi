package com.example.watchgomoku.util

import android.media.AudioManager
import android.media.ToneGenerator

/**
 * 极简音效：ToneGenerator 合成音，不引入任何音频资源与网络。
 * 全部调用包裹防御，避免在个别 ROM 上创建失败导致崩溃。
 */
object SoundPlayer {

    enum class Effect { STONE, BUTTON, WIN, INVALID }

    @Volatile
    private var toneGenerator: ToneGenerator? = null

    fun play(enabled: Boolean, effect: Effect) {
        if (!enabled) return
        runCatching {
            val generator = toneGenerator ?: ToneGenerator(AudioManager.STREAM_MUSIC, 60)
                .also { toneGenerator = it }
            when (effect) {
                Effect.STONE -> generator.startTone(ToneGenerator.TONE_PROP_BEEP, 80)
                Effect.BUTTON -> generator.startTone(ToneGenerator.TONE_PROP_ACK, 60)
                Effect.WIN -> generator.startTone(ToneGenerator.TONE_PROP_BEEP2, 250)
                Effect.INVALID -> generator.startTone(ToneGenerator.TONE_PROP_NACK, 120)
            }
        }
    }
}
