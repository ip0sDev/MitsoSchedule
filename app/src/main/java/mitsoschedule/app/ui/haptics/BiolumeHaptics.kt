package mitsoschedule.app.ui.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * Premium Biolume Haptics engine.
 * Employs hardware-accelerated vibration primitives on Android 12+ (API 31+)
 * to deliver crisp, precise, and organic tactile feedback.
 */
class BiolumeHaptics(context: Context) {
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

    /**
     * Ultra-crisp light tick. Perfect for:
     * - Tab switching (Schedule / Cabinet / Settings)
     * - Day of week selector chips
     * - Radio options & list items
     */
    fun tick() {
        playPrimitive(
            primitiveId = VibrationEffect.Composition.PRIMITIVE_TICK,
            scale = 0.55f,
            fallbackPredefined = VibrationEffect.EFFECT_TICK,
            fallbackDuration = 8L
        )
    }

    /**
     * Tactile click. Perfect for:
     * - Expanding/collapsing lesson cards
     * - Standard action buttons
     * - Week navigation arrows
     */
    fun click() {
        playPrimitive(
            primitiveId = VibrationEffect.Composition.PRIMITIVE_CLICK,
            scale = 0.70f,
            fallbackPredefined = VibrationEffect.EFFECT_CLICK,
            fallbackDuration = 14L
        )
    }

    /**
     * Distinct tactile pop. Perfect for:
     * - Dialog openings
     * - Modal sheet triggers
     * - Primary buttons (Save, Select)
     */
    fun mediumClick() {
        playPrimitive(
            primitiveId = VibrationEffect.Composition.PRIMITIVE_CLICK,
            scale = 1.0f,
            fallbackPredefined = VibrationEffect.EFFECT_HEAVY_CLICK,
            fallbackDuration = 22L
        )
    }

    /**
     * Snappy threshold release. Perfect for:
     * - Pull-to-refresh crossing trigger threshold
     * - Drag snapping into slot
     */
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

    /**
     * Quick crisp toggle pop. Perfect for:
     * - Theme switches (dark mode, OLED mode)
     * - Checkboxes and toggles
     */
    fun toggle() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && vibrator != null && vibrator.areAllPrimitivesSupported(
                VibrationEffect.Composition.PRIMITIVE_TICK,
                VibrationEffect.Composition.PRIMITIVE_CLICK
            )
        ) {
            val effect = VibrationEffect.startComposition()
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.45f)
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.75f, 40)
                .compose()
            vibrate(effect)
        } else {
            click()
        }
    }

    /**
     * Double soft affirmative pulse. Perfect for:
     * - Schedule refresh completion
     * - Successful login
     * - Cache cleared
     */
    fun success() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && vibrator != null && vibrator.areAllPrimitivesSupported(
                VibrationEffect.Composition.PRIMITIVE_TICK,
                VibrationEffect.Composition.PRIMITIVE_CLICK
            )
        ) {
            val effect = VibrationEffect.startComposition()
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.55f)
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.90f, 65)
                .compose()
            vibrate(effect)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
        } else {
            vibrate(VibrationEffect.createWaveform(longArrayOf(0, 15, 60, 25), intArrayOf(0, 120, 0, 180), -1))
        }
    }

    /**
     * Distinct dual warning pulse. Perfect for:
     * - Network/login errors
     * - Invalid input
     */
    fun error() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && vibrator != null && vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_CLICK)) {
            val effect = VibrationEffect.startComposition()
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.95f)
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.95f, 85)
                .compose()
            vibrate(effect)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrate(VibrationEffect.createWaveform(longArrayOf(0, 28, 70, 28), intArrayOf(0, 200, 0, 200), -1))
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
            // Ignore security or hardware exceptions gracefully
        }
    }
}

val LocalBiolumeHaptics: ProvidableCompositionLocal<BiolumeHaptics> = staticCompositionLocalOf {
    error("No BiolumeHaptics provided")
}

@Composable
fun rememberBiolumeHaptics(): BiolumeHaptics {
    val context = LocalContext.current.applicationContext
    return remember(context) { BiolumeHaptics(context) }
}
