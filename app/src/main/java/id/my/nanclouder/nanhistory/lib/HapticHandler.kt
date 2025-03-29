package id.my.nanclouder.nanhistory.lib

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

fun <T> withHaptic(
    haptic: HapticFeedback,
    type: HapticFeedbackType = HapticFeedbackType.LongPress,
    block: (T) -> Unit
): (T) -> Unit {
    return {
        haptic.performHapticFeedback(type)
        block(it)
    }
}

fun withHaptic(
    haptic: HapticFeedback,
    type: HapticFeedbackType = HapticFeedbackType.LongPress,
    block: () -> Unit
): () -> Unit {
    return { withHaptic<Unit>(haptic, type, { it -> block() }).invoke(Unit) }
}