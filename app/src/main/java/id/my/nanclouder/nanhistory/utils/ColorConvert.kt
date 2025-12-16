package id.my.nanclouder.nanhistory.utils

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

fun Color.getValue(): Float {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(toArgb(), hsv)
    return hsv[2]
}

fun Color.getSaturation(): Float {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(toArgb(), hsv)
    return hsv[1]
}

fun Color.backgroundTagColor(darkTheme: Boolean = false): Color {
    return copyWith(
        saturation = this.getValue() * this.getSaturation(),
        value = if (darkTheme) 1f - this.getValue() else this.getValue()
    )
}

fun Color.textTagColor(darkTheme: Boolean): Color {
    val value =
        if (this.backgroundTagColor(darkTheme).getValue() < .7f) .9f else .2f
    val saturation =
        if ((this.backgroundTagColor(darkTheme).getValue() < .7f && !darkTheme) ||
            (this.backgroundTagColor(darkTheme).getValue() > .6f && darkTheme))
            .0f else this.getValue() * this.getSaturation()

    return copyWith(
        saturation = saturation,
        value = value
    )
}

fun Color.borderTagColor(darkTheme: Boolean): Color {
    return copyWith(
        saturation = this.getSaturation() / 2,
        value = 0.5f
    )
}