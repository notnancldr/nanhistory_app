package id.my.nanclouder.nanhistory.feature.sharemap.canvas

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.PixelCopy
import android.view.Window
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Utility to capture window regions directly from the hardware compositor via PixelCopy.
 */
object PixelCopyCapture {
    private const val TAG = "PixelCopyCapture"

    /**
     * Captures a specified rectangular region of a window via PixelCopy,
     * and scales it to [targetWidth] x [targetHeight] with bilinear filtering.
     */
    suspend fun captureWindowBounds(
        window: Window,
        boundsInWindow: Rect,
        targetWidth: Int = boundsInWindow.width(),
        targetHeight: Int = boundsInWindow.height()
    ): Bitmap? {
        if (boundsInWindow.width() <= 0 || boundsInWindow.height() <= 0) {
            Log.w(TAG, "Invalid bounds for PixelCopy: $boundsInWindow")
            return null
        }

        val rawBitmap = Bitmap.createBitmap(
            boundsInWindow.width(),
            boundsInWindow.height(),
            Bitmap.Config.ARGB_8888
        )

        val success = suspendCancellableCoroutine { cont ->
            try {
                PixelCopy.request(
                    window,
                    boundsInWindow,
                    rawBitmap,
                    { result ->
                        if (result == PixelCopy.SUCCESS) {
                            if (cont.isActive) cont.resume(true)
                        } else {
                            Log.e(TAG, "PixelCopy failed with result: $result")
                            if (cont.isActive) cont.resume(false)
                        }
                    },
                    Handler(Looper.getMainLooper())
                )
            } catch (e: Exception) {
                Log.e(TAG, "PixelCopy exception", e)
                if (cont.isActive) cont.resume(false)
            }
        }

        if (!success) {
            rawBitmap.recycle()
            return null
        }

        if (targetWidth == boundsInWindow.width() && targetHeight == boundsInWindow.height()) {
            return rawBitmap
        }

        val scaledBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(scaledBitmap)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        val src = Rect(0, 0, rawBitmap.width, rawBitmap.height)
        val dst = Rect(0, 0, targetWidth, targetHeight)
        canvas.drawBitmap(rawBitmap, src, dst, paint)
        rawBitmap.recycle()

        return scaledBitmap
    }
}
