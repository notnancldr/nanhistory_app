package id.my.nanclouder.nanhistory.utils

import android.content.Context
import java.io.File
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class LogData(
    var path: String = "${LocalDate.now()}/Log_${DateTimeFormatter.ofPattern("HH-mm-ss").format(ZonedDateTime.now())}.log",
) {
    companion object {
        /**
         * Use common path:
         * yyyy-MM-dd/HH-mm-ss_[name].log
         *
         * ```Kotlin
         * path = DateTimeFormatter
         *     .ofPattern("yyyy-MM-dd/HH-mm-ss_'$name.log'")
         *     .format(ZonedDateTime.now())
         * ```
         *
         * @return [LogData] with path set to common path
         */
        fun inCommonPath(name: String) =
            LogData(
                path = DateTimeFormatter
                    .ofPattern("yyyy-MM-dd/HH-mm-ss_'$name.log'")
                    .format(ZonedDateTime.now())
            )

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

    /**
     * Append plain text to buffer
     * @param data Data to be saved into log
     * @param end The char at the end of the appended log, default: `\n`
     */
    fun append(data: String, end: Char = '\n') {
        changes += data + end
    }

    /**
     * Append to buffer with timestamp format: `yyyy-MM-dd HH:mm:ss`
     * @param data Data to be saved into log
     * @param end The char at the end of the appended log, default: `\n`
     */
    fun appendWithTimestamp(data: String, end: Char = '\n') {
        val now = ZonedDateTime.now()
        val formattedDate = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .format(now)

        append("$formattedDate    $data", end)
    }

    /**
     * Clear [LogData] file
     */
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

    /**
     * Save all [LogData] buffer into a file
     */
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