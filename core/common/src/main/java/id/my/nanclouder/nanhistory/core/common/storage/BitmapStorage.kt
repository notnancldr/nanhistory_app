package id.my.nanclouder.nanhistory.core.common.storage

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File

/**
 * Saves a Bitmap to the public Pictures/NanHistory directory using MediaStore scoped storage.
 */
fun saveBitmapToPictures(context: Context, bitmap: Bitmap, fileName: String, format: String): Uri? {
    val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/${format.lowercase()}")
        put(MediaStore.Images.Media.IS_PENDING, 1)
        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + "NanHistory")
    }

    val resolver = context.contentResolver
    var uri: Uri? = null
    try {
        uri = resolver.insert(collection, contentValues)
        uri?.let {
            resolver.openOutputStream(it)?.use { outputStream ->
                val compressFormat = if (format == "PNG") {
                    Bitmap.CompressFormat.PNG
                } else {
                    Bitmap.CompressFormat.JPEG
                }
                bitmap.compress(compressFormat, 95, outputStream)
            }
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(it, contentValues, null, null)
        }
    } catch (e: Exception) {
        uri?.let { orphanUri ->
            resolver.delete(orphanUri, null, null)
        }
        Log.e("saveBitmapToPictures", "Failed to save bitmap", e)
    }

    return uri
}
