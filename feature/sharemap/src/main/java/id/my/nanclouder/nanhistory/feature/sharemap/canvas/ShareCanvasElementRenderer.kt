package id.my.nanclouder.nanhistory.feature.sharemap.canvas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import id.my.nanclouder.nanhistory.ui.map.maplibre.TripPreviewMap
import id.my.nanclouder.nanhistory.domain.model.HistoryEvent
import id.my.nanclouder.nanhistory.domain.model.LocationData
import id.my.nanclouder.nanhistory.utils.parseFormattedText

/**
 * Renders the internal content of a specific canvas element based on its type.
 */
@Composable
fun RenderElementContent(
    element: CanvasElement,
    eventTitle: String,
    dateString: String,
    eventData: HistoryEvent?,
    metricsText: Map<String, String>,
    locations: List<LocationData>,
    elevationGainMeters: Double?
) {
    val textColor = parseHexToColor(element.style.textColorHex, Color.White)

    when (element.type) {
        CanvasElementType.TITLE -> {
            val format = element.style.templateFormat.ifEmpty { "# {title}\n{date}" }
            val resolved = ShareTokenResolver.resolveTokens(
                template = format,
                eventTitle = eventTitle,
                dateString = dateString,
                eventData = eventData,
                metricsText = metricsText,
                elevationGainMeters = elevationGainMeters
            )
            RenderMarkdownText(resolved, textColor, element.style.fontSizeSp, element.style.hasShadow, isTitle = true)
        }
        CanvasElementType.METRICS_GRID -> {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("distance", "duration", "avg_speed", "max_speed").forEach { key ->
                    val value = metricsText[key] ?: "--"
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = key.replace("_", " ").uppercase(),
                            color = textColor.copy(alpha = 0.7f),
                            fontSize = (element.style.fontSizeSp * 0.75f).sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = value,
                            color = textColor,
                            fontSize = element.style.fontSizeSp.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        CanvasElementType.SINGLE_METRIC -> {
            val format = element.style.templateFormat.ifEmpty { "{distance}" }
            val resolved = ShareTokenResolver.resolveTokens(
                template = format,
                eventTitle = eventTitle,
                dateString = dateString,
                eventData = eventData,
                metricsText = metricsText,
                elevationGainMeters = elevationGainMeters
            )
            RenderMarkdownText(resolved, textColor, element.style.fontSizeSp, element.style.hasShadow, isTitle = true)
        }
        CanvasElementType.FLOATING_MAP -> {
            TripPreviewMap(
                locations = locations,
                forcedTheme = element.style.mapTheme,
                modifier = Modifier.fillMaxSize()
            )
        }
        CanvasElementType.MARKDOWN_TEXT -> {
            val format = element.style.templateFormat.ifEmpty { "Trip: {title}" }
            val resolved = ShareTokenResolver.resolveTokens(
                template = format,
                eventTitle = eventTitle,
                dateString = dateString,
                eventData = eventData,
                metricsText = metricsText,
                elevationGainMeters = elevationGainMeters
            )
            RenderMarkdownText(resolved, textColor, element.style.fontSizeSp, element.style.hasShadow, isTitle = false)
        }
        CanvasElementType.TAGS -> {
            Text(text = "Tags", color = textColor, fontSize = element.style.fontSizeSp.sp)
        }
    }
}

@Composable
fun RenderMarkdownText(
    text: String,
    textColor: Color,
    fontSizeSp: Float,
    hasShadow: Boolean,
    isTitle: Boolean = false
) {
    val parsed = parseFormattedText(
        text = text,
        tagColor = textColor,
        tagBackgroundColor = textColor.copy(alpha = 0.15f)
    )
    val baseStyle = if (isTitle) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium
    val shadow = if (hasShadow) androidx.compose.ui.graphics.Shadow(Color.Black, Offset(2f, 2f), 4f) else null

    Text(
        text = parsed,
        color = textColor,
        fontSize = fontSizeSp.sp,
        fontWeight = if (isTitle) FontWeight.Bold else FontWeight.Normal,
        style = if (shadow != null) baseStyle.copy(shadow = shadow) else baseStyle
    )
}
