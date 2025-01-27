package id.my.nanclouder.nanhistory.lib

import android.content.Context
import java.io.File
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class LogData(
    var path: String = "${LocalDate.now()}/Log_${DateTimeFormatter.ofPattern("HH-mm-ss").format(ZonedDateTime.now())}.json",
) {
    companion object {
        fun fromPath(context: Context, path: String): LogData {
            val file = File(path)
            return LogData(
                path = path
            ).apply {
                stringData = file.readText()
            }
        }
    }

    private var stringData: String = ""
    private var changes: String = ""

    val data
        get() = stringData

    fun append(data: String, end: Char = '\n') {
        changes += data + end
    }

    fun clear(context: Context) {
        val logsDir = File(context.applicationContext.filesDir, "logs")
        if (!logsDir.exists()) {
            logsDir.mkdir()
        }
        val file = File(logsDir, path)
        if (!file.exists()) {
            file.parentFile?.mkdirs()
            file.createNewFile()
        }
        file.writeText("")
    }

    fun save(context: Context) {
        val logsDir = File(context.applicationContext.filesDir, "logs")
        if (!logsDir.exists()) {
            logsDir.mkdir()
        }
        val file = File(logsDir, path)
        if (!file.exists()) {
            file.parentFile?.mkdirs()
            file.createNewFile()
        }
        file.appendText(changes)
        stringData += changes
        changes = ""
    }
}