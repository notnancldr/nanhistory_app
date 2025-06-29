package id.my.nanclouder.nanhistory.lib

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

fun Color.copyWith(hue: Float? = null, saturation: Float? = null, value: Float? = null): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(toArgb(), hsv)

    // Update hue while keeping saturation and value the same
    if (hue != null) hsv[0] = hue
    if (saturation != null) hsv[1] = saturation
    if (value != null) hsv[2] = value

    // Create a new color from the updated HSL values
    val newArgb = android.graphics.Color.HSVToColor(hsv)
    return Color(newArgb)
}

fun Color.backgroundTagColor(darkTheme: Boolean = false): Color {
    val backgroundValue = if (darkTheme) .2f else .95f
    val backgroundSaturation = if (darkTheme) .2f else .2f
    return copyWith(
        saturation = backgroundSaturation,
        value = backgroundValue
    )
}

fun Color.textTagColor(darkTheme: Boolean): Color {
    val onBackgroundValue = if (darkTheme) .9f else .2f
    val onBackgroundSaturation = if (darkTheme) .2f else .98f

    return copyWith(
        saturation = onBackgroundSaturation,
        value = onBackgroundValue
    )
}