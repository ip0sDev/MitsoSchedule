package by.iposdev.watchso.presentation.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * High-definition haptics engine for Wear OS.
 * Provides distinct, subtle, and satisfying clicks and ticks for rotary and touch events.
 */
class WearBiolumeHaptics(context: Context) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val touchAttributes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        VibrationAttributes.Builder()
            .setUsage(VibrationAttributes.USAGE_TOUCH)
            .build()
    } else {
        null
    }

    fun tick() {
        playPrimitive(
            primitiveId = VibrationEffect.Composition.PRIMITIVE_TICK,
            scale = 0.5f,
            fallbackPredefined = VibrationEffect.EFFECT_TICK,
            fallbackDuration = 10L
        )
    }

    fun click() {
        playPrimitive(
            primitiveId = VibrationEffect.Composition.PRIMITIVE_CLICK,
            scale = 0.7f,
            fallbackPredefined = VibrationEffect.EFFECT_CLICK,
            fallbackDuration = 15L
        )
    }

    fun snap() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && vibrator != null && vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_LOW_TICK)) {
            val effect = VibrationEffect.startComposition()
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.85f)
                .compose()
            vibrate(effect)
        } else {
            click()
        }
    }

    fun success() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && vibrator != null && vibrator.areAllPrimitivesSupported(
                VibrationEffect.Composition.PRIMITIVE_TICK,
                VibrationEffect.Composition.PRIMITIVE_CLICK
            )
        ) {
            val effect = VibrationEffect.startComposition()
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.5f)
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.85f, 60)
                .compose()
            vibrate(effect)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
        } else {
            vibrate(VibrationEffect.createOneShot(25, 180))
        }
    }

    fun error() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && vibrator != null && vibrator.areAllPrimitivesSupported(
                VibrationEffect.Composition.PRIMITIVE_CLICK
            )
        ) {
            val effect = VibrationEffect.startComposition()
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.9f)
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.9f, 90)
                .compose()
            vibrate(effect)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
        } else {
            vibrate(VibrationEffect.createWaveform(longArrayOf(0, 40, 60, 40), intArrayOf(0, 200, 0, 200), -1))
        }
    }

    private fun playPrimitive(
        primitiveId: Int,
        scale: Float,
        fallbackPredefined: Int,
        fallbackDuration: Long
    ) {
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && vib.areAllPrimitivesSupported(primitiveId)) {
            val effect = VibrationEffect.startComposition()
                .addPrimitive(primitiveId, scale)
                .compose()
            vibrate(effect)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrate(VibrationEffect.createPredefined(fallbackPredefined))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrate(VibrationEffect.createOneShot(fallbackDuration, (scale * 255).toInt().coerceIn(1, 255)))
        }
    }

    private fun vibrate(effect: VibrationEffect) {
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && touchAttributes != null) {
                vib.vibrate(effect, touchAttributes)
            } else {
                vib.vibrate(effect)
            }
        } catch (_: Exception) {
            // Ignore hardware/security exceptions
        }
    }
}

@Composable
fun rememberWearBiolumeHaptics(): WearBiolumeHaptics {
    val context = LocalContext.current.applicationContext
    return remember(context) { WearBiolumeHaptics(context) }
}
