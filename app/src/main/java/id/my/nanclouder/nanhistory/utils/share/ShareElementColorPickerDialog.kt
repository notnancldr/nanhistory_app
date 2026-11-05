package id.my.nanclouder.nanhistory.utils.share

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.skydoves.colorpicker.compose.AlphaSlider
import com.github.skydoves.colorpicker.compose.AlphaTile
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import id.my.nanclouder.nanhistory.R
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Parses a hex string (#RRGGBB, #AARRGGBB, or raw hex) to a Compose [Color].
 */
fun parseHexToColor(hex: String, fallback: Color = Color.White): Color {
    val clean = hex.removePrefix("#").trim()
    return runCatching {
        when (clean.length) {
            6 -> Color(android.graphics.Color.parseColor("#FF$clean"))
            8 -> Color(android.graphics.Color.parseColor("#$clean"))
            else -> fallback
        }
    }.getOrDefault(fallback)
}

/**
 * Formats a Compose [Color] to an 8-character uppercase hex string (#AARRGGBB).
 */
fun Color.toArgbHex(): String {
    val a = (alpha * 255f).roundToInt().coerceIn(0, 255)
    val r = (red * 255f).roundToInt().coerceIn(0, 255)
    val g = (green * 255f).roundToInt().coerceIn(0, 255)
    val b = (blue * 255f).roundToInt().coerceIn(0, 255)
    return String.format(Locale.ROOT, "#%02X%02X%02X%02X", a, r, g, b)
}

/**
 * Dedicated color picker dialog providing HSV wheel, Brightness and Alpha sliders,
 * direct hex input, and quick color presets.
 */
@Composable
fun ShareElementColorPickerDialog(
    title: String,
    initialHexColor: String,
    onDismissRequest: () -> Unit,
    onColorSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val initialColor = remember(initialHexColor) { parseHexToColor(initialHexColor) }
    val controller = rememberColorPickerController()
    var selectedHex by remember(initialHexColor) { mutableStateOf(initialHexColor) }
    var inputHex by remember(initialHexColor) { mutableStateOf(initialHexColor) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(title, style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Live Swatch & Hex indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                    ) {
                        AlphaTile(
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            controller = controller
                        )
                    }

                    OutlinedTextField(
                        value = inputHex,
                        onValueChange = { newText ->
                            inputHex = newText
                            val parsed = parseHexToColor(newText, fallback = Color.Unspecified)
                            if (parsed != Color.Unspecified) {
                                selectedHex = newText
                                controller.selectByColor(parsed, fromUser = false)
                            }
                        },
                        label = { Text(stringResource(R.string.share_hex_code)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Quick Presets Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val presets = listOf(
                        stringResource(R.string.share_color_transparent) to "#00000000",
                        "Dark Pill" to "#80000000",
                        "White" to "#FFFFFFFF",
                        "Black" to "#FF000000",
                        "Accent" to MaterialTheme.colorScheme.primary.toArgbHex()
                    )
                    presets.forEach { (label, hex) ->
                        FilterChip(
                            selected = selectedHex.equals(hex, ignoreCase = true),
                            onClick = {
                                selectedHex = hex
                                inputHex = hex
                                val col = parseHexToColor(hex)
                                controller.selectByColor(col, fromUser = false)
                            },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                // HSV Picker Wheel
                HsvColorPicker(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(vertical = 4.dp),
                    controller = controller,
                    initialColor = initialColor,
                    onColorChanged = { envelope ->
                        if (envelope.fromUser) {
                            val hex = envelope.color.toArgbHex()
                            selectedHex = hex
                            inputHex = hex
                        }
                    }
                )

                // Brightness Slider
                BrightnessSlider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp),
                    controller = controller,
                    initialColor = initialColor
                )

                // Alpha Slider
                AlphaSlider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp),
                    controller = controller,
                    initialColor = initialColor
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onColorSelected(selectedHex) }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(android.R.string.cancel))
            }
        },
        modifier = modifier
    )
}

@Preview(name = "Light Mode")
@Composable
private fun ShareElementColorPickerDialogPreview() {
    MaterialTheme {
        ShareElementColorPickerDialog(
            title = "Element Color",
            initialHexColor = "#802196F3",
            onDismissRequest = {},
            onColorSelected = {}
        )
    }
}

@Preview(name = "Dark Mode", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun ShareElementColorPickerDialogDarkPreview() {
    MaterialTheme {
        ShareElementColorPickerDialog(
            title = "Element Color",
            initialHexColor = "#802196F3",
            onDismissRequest = {},
            onColorSelected = {}
        )
    }
}
