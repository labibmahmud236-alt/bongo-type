package com.example.typing

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import kotlinx.coroutines.delay

class TextInjector(private val context: Context) {

    private val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager

    fun injectToClipboard(text: String, notifyUser: Boolean = false) {
        try {
            val clip = ClipData.newPlainText("Bongo Type Dictation", text)
            clipboardManager?.setPrimaryClip(clip)
            if (notifyUser) {
                Toast.makeText(context, "Copied & Ready to Paste: $text", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun triggerHapticFeedback() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(30)
                }
            }
        } catch (_: Exception) {
            // Haptics optional
        }
    }

    suspend fun streamTextIntoField(
        text: String,
        speedMs: Long,
        onCharTyped: (String) -> Unit
    ) {
        if (speedMs <= 0) {
            onCharTyped(text)
            return
        }
        val sb = StringBuilder()
        for (ch in text) {
            sb.append(ch)
            onCharTyped(sb.toString())
            delay(speedMs)
        }
    }
}
