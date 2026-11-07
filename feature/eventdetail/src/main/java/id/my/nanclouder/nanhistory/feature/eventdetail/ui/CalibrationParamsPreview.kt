package id.my.nanclouder.nanhistory.feature.eventdetail.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import id.my.nanclouder.nanhistory.domain.logic.transport.CalibrationModel
import kotlin.math.abs

@Composable
fun CalibrationParamsPreview(
    original: CalibrationModel?,
    current: CalibrationModel
) {
    data class ParamDiff(val label: String, val old: Float?, val new: Float)

    val diffs = listOf(
        ParamDiff("Avg Speed Min", original?.avgSpeedMin, current.avgSpeedMin),
        ParamDiff("Avg Speed Max", original?.avgSpeedMax, current.avgSpeedMax),
        ParamDiff("Top Speed Min", original?.topSpeedMin, current.topSpeedMin),
        ParamDiff("Top Speed Max", original?.topSpeedMax, current.topSpeedMax),
        ParamDiff("Avg Accel Min", original?.avgAccelMin, current.avgAccelMin),
        ParamDiff("Avg Accel Max", original?.avgAccelMax, current.avgAccelMax)
    )

    Column {
        diffs.forEach { diff ->
            val changed = diff.old != null && abs(diff.new - diff.old) > 0.0001f

            Row {
                Text(
                    text = diff.label + ": ",
                    style = MaterialTheme.typography.labelMedium
                )

                if (changed) {
                    val color = if (diff.new > diff.old) Color(0xFF00AA33) else Color(0xFFCC0033)
                    Text(
                        text = "${diff.old} → ${diff.new}",
                        color = color,
                        style = MaterialTheme.typography.labelMedium
                    )
                } else {
                    Text(
                        text = diff.new.toString(),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}
