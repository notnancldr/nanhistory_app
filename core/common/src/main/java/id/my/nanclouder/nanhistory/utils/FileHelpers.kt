package id.my.nanclouder.nanhistory.utils

import android.content.Context
import java.io.File

fun getAudioFile(context: Context, path: String): File =
    File(context.filesDir, "audio/$path")

fun getLocationFile(context: Context, path: String): File =
    File(context.filesDir, "locations/$path")

fun getAudioFiles(context: Context): List<File> =
    File(context.filesDir, "audio")
        .walkTopDown().filter { it.isFile }.toList()

fun getLocationFiles(context: Context): List<File> =
    File(context.filesDir, "locations")
        .walkTopDown().filter { it.isFile }.toList()
