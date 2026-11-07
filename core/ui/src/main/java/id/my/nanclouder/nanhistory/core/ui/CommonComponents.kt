package id.my.nanclouder.nanhistory.core.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun ColorIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier.size(24.dp)
    ) {
        drawCircle(
            color = Color.Gray,
            style = Stroke(
                width = 3.0f * density
            ),
            radius = size.minDimension / 3.0f
        )
        drawCircle(
            color = color,
            style = androidx.compose.ui.graphics.drawscope.Fill,
            radius = size.minDimension / 3.0f
        )
    }
}

@Composable
fun SelectableButton(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val cornerRadius by animateDpAsState(
        targetValue = if (selected) 64.dp else 12.dp,
        label = "cornerRadius"
    )

    val containerColor by animateColorAsState(
        targetValue = if (selected) colors.containerColor else Color.Transparent,
        label = "bgColor"
    )

    val contentColor by animateColorAsState(
        targetValue = if (selected) colors.contentColor else MaterialTheme.colorScheme.primary,
        label = "contentColor"
    )

    val borderWidth by animateDpAsState(
        targetValue = if (selected) 0.dp else 1.dp,
        label = "borderWidth"
    )

    val shape = RoundedCornerShape(cornerRadius)

    Button(
        onClick = onClick,
        content = content,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        modifier = modifier,
        border = BorderStroke(
            width = borderWidth,
            color = Color.Gray
        ),
        enabled = enabled,
        elevation = elevation,
        contentPadding = contentPadding,
        interactionSource = interactionSource
    )
}
