package id.my.nanclouder.nanhistory.activity.shareMap.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.view.Window
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap
import id.my.nanclouder.nanhistory.config.Config
import id.my.nanclouder.nanhistory.config.ShareMapExportEngine
import id.my.nanclouder.nanhistory.state.ShareMapUiState
import id.my.nanclouder.nanhistory.ui.map.maplibre.TripMapSnapshotter
import id.my.nanclouder.nanhistory.utils.share.PixelCopyCapture
import id.my.nanclouder.nanhistory.utils.share.ShareCardAspectRatio

internal fun Bitmap.toSoftwareBitmap(): Bitmap =
    if (config == Bitmap.Config.HARDWARE) copy(Bitmap.Config.ARGB_8888, false) else this

/**
 * Composites background map snapshot, base card container, and canvas elements
 * into a single high-resolution export bitmap.
 */
internal suspend fun renderCardBitmap(
    context: Context,
    window: Window?,
    cardBoundsInWindow: Rect?,
    uiState: ShareMapUiState,
    elementsBitmap: Bitmap,
    baseDimensionPx: Int,
    isDark: Boolean,
    cardBgColor: Color
): Bitmap {
    val (targetWidth, targetHeight) = when (uiState.aspectRatio) {
        ShareCardAspectRatio.SQUARE -> Pair(baseDimensionPx, baseDimensionPx)
        ShareCardAspectRatio.STORY -> Pair(baseDimensionPx, (baseDimensionPx * 16) / 9)
        ShareCardAspectRatio.LANDSCAPE -> Pair((baseDimensionPx * 16) / 9, baseDimensionPx)
    }

    val exportEngine = Config.shareMapExportEngine.get(context)

    if (exportEngine == ShareMapExportEngine.PixelCopy && window != null && cardBoundsInWindow != null) {
        val pixelCopyBitmap = PixelCopyCapture.captureWindowBounds(
            window, cardBoundsInWindow, targetWidth, targetHeight
        )
        if (pixelCopyBitmap != null) return pixelCopyBitmap
    }

    var mapBitmap: Bitmap? = null
    if (uiState.showBackgroundMap && uiState.locations.isNotEmpty()) {
        val resolvedIsDark = when (uiState.backgroundMapTheme) {
            "LIGHT" -> false
            "DARK" -> true
            else -> isDark
        }
        val density = context.resources.displayMetrics.density
        val wDp = uiState.previewWidthDp ?: (if (cardBoundsInWindow != null && density > 0f) {
            cardBoundsInWindow.width() / density
        } else 360f)
        val hDp = uiState.previewHeightDp ?: (if (cardBoundsInWindow != null && density > 0f) {
            cardBoundsInWindow.height() / density
        } else 360f)
        mapBitmap = TripMapSnapshotter.captureMapSnapshot(
            locations = uiState.locations,
            widthPx = targetWidth,
            heightPx = targetHeight,
            isDark = resolvedIsDark,
            customCenterLon = uiState.customMapCenterLon,
            customCenterLat = uiState.customMapCenterLat,
            customZoom = uiState.customMapZoom,
            previewWidthDp = wDp,
            previewHeightDp = hDp
        )
    }

    if (mapBitmap == null && uiState.showBackgroundMap && uiState.locations.isNotEmpty() &&
        window != null && cardBoundsInWindow != null
    ) {
        val fallbackPixelCopy = PixelCopyCapture.captureWindowBounds(
            window, cardBoundsInWindow, targetWidth, targetHeight
        )
        if (fallbackPixelCopy != null) return fallbackPixelCopy
    }

    val finalBitmap = createBitmap(targetWidth, targetHeight)
    val canvas = Canvas(finalBitmap)
    val cornerRadiusPx = 16f * (baseDimensionPx / 360f).coerceAtLeast(1f)

    val path = Path().apply {
        addRoundRect(
            RectF(0f, 0f, targetWidth.toFloat(), targetHeight.toFloat()),
            cornerRadiusPx,
            cornerRadiusPx,
            Path.Direction.CW
        )
    }
    canvas.clipPath(path)

    val bgPaint = Paint().apply {
        color = cardBgColor.toArgb()
        style = Paint.Style.FILL
    }
    canvas.drawRect(0f, 0f, targetWidth.toFloat(), targetHeight.toFloat(), bgPaint)

    if (mapBitmap != null) {
        val safeMap = mapBitmap.toSoftwareBitmap()
        val mapPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        val src = Rect(0, 0, safeMap.width, safeMap.height)
        val dst = Rect(0, 0, targetWidth, targetHeight)
        canvas.drawBitmap(safeMap, src, dst, mapPaint)
        if (safeMap !== mapBitmap) safeMap.recycle()
        mapBitmap.recycle()
    }

    val safeElements = elementsBitmap.toSoftwareBitmap()
    val overlayPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    val srcOverlay = Rect(0, 0, safeElements.width, safeElements.height)
    val dstOverlay = Rect(0, 0, targetWidth, targetHeight)
    canvas.drawBitmap(safeElements, srcOverlay, dstOverlay, overlayPaint)
    if (safeElements !== elementsBitmap) safeElements.recycle()

    return finalBitmap
}
