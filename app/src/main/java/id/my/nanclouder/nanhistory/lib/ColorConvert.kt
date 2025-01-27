package id.my.nanclouder.nanhistory.lib

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