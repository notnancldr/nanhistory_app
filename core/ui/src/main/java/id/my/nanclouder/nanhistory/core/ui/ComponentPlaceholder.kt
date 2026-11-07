package id.my.nanclouder.nanhistory.core.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ComponentPlaceholder(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "gradient")

    val width = 128.dp

    val offset by infiniteTransition.animateValue(
        initialValue = 0.dp,
        targetValue = width * 2,
        label = "offset",
        typeConverter = Dp.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val tileSize = with(LocalDensity.current) {
        width.toPx()
    }

    val listColors = listOf(
        Color(0xFF707070),
        Color(0xFF909090)
    )

    val gradient = with(LocalDensity.current) {
        Brush.horizontalGradient(
            colors = listColors,
            startX = 0f + offset.toPx(),
            endX = tileSize + offset.toPx(),
            tileMode = TileMode.Mirror
        )
    }

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "placeholderAlpha"
    )

    Box(
        modifier
            .alpha(alpha)
            .background(gradient, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
    )
}
