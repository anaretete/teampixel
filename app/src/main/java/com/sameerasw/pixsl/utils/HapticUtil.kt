package com.sameerasw.pixsl.utils

import android.content.Context
import android.os.Vibrator
import android.view.View
import androidx.compose.runtime.mutableStateOf

object HapticUtil {
    val isAppHapticsEnabled = mutableStateOf(true)

    fun performUIHaptic(view: View) {
        if (!isAppHapticsEnabled.value) return
        view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
    }

    fun performLightHaptic(view: View) {
        if (!isAppHapticsEnabled.value) return
        view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
    }

    fun performMediumHaptic(view: View) {
        if (!isAppHapticsEnabled.value) return
        view.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
    }

    fun performVirtualKeyHaptic(view: View) {
        if (!isAppHapticsEnabled.value) return
        view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
    }

    fun performSliderHaptic(view: View) {
        if (!isAppHapticsEnabled.value) return
        view.performHapticFeedback(android.view.HapticFeedbackConstants.SEGMENT_FREQUENT_TICK)
    }

    fun performCustomHaptic(view: View, strength: Float) {
        if (!isAppHapticsEnabled.value) return

        val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vibratorManager =
                view.context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            view.context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            try {
                if (vibrator.areAllPrimitivesSupported(android.os.VibrationEffect.Composition.PRIMITIVE_CLICK)) {
                    val effect = android.os.VibrationEffect.startComposition()
                        .addPrimitive(
                            android.os.VibrationEffect.Composition.PRIMITIVE_CLICK,
                            strength
                        )
                        .compose()
                    val attrs =
                        android.os.VibrationAttributes.createForUsage(android.os.VibrationAttributes.USAGE_TOUCH)
                    vibrator.vibrate(effect, attrs)
                    return
                }
            } catch (e: Exception) {
                // Fallback
            }
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (vibrator.hasAmplitudeControl()) {
                val amplitude = (strength * strength * 255).toInt().coerceIn(1, 255)
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(12, amplitude))
            } else {
                if (strength < 0.5f) {
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                } else {
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                }
            }
        } else {
            view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    fun loadAppHapticsEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences("pixsl_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("app_haptics_enabled", true)
    }

    fun saveAppHapticsEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences("pixsl_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("app_haptics_enabled", enabled).apply()
        isAppHapticsEnabled.value = enabled
    }

    fun initialize(context: Context) {
        isAppHapticsEnabled.value = loadAppHapticsEnabled(context)
    }
}
