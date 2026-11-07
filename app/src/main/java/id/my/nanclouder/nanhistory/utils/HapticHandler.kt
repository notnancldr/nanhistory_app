package id.my.nanclouder.nanhistory.utils

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

fun <T> withHaptic(
    haptic: HapticFeedback,
    type: HapticFeedbackType = HapticFeedbackType.LongPress,
    block: (T) -> Unit
): (T) -> Unit = id.my.nanclouder.nanhistory.core.ui.utils.withHaptic(haptic, type, block)

fun withHaptic(
    haptic: HapticFeedback,
    type: HapticFeedbackType = HapticFeedbackType.LongPress,
    block: () -> Unit
): () -> Unit = id.my.nanclouder.nanhistory.core.ui.utils.withHaptic(haptic, type, block)
