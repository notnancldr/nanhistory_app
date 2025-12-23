package id.my.nanclouder.nanhistory.utils

import java.io.ByteArrayOutputStream
import java.io.RandomAccessFile

fun RandomAccessFile.readLineWithNewline(): ByteArray? {
    val buffer = ByteArrayOutputStream()

    while (true) {
        val b = read()
        if (b == -1) {
            return if (buffer.size() == 0) null else buffer.toByteArray()
        }

        buffer.write(b)

        when (b) {
            '\n'.code -> {
                // LF ends the line
                break
            }
            '\r'.code -> {
                // CR may be followed by LF
                val next = read()
                if (next == '\n'.code) {
                    buffer.write(next)
                } else if (next != -1) {
                    // unread the byte
                    seek(filePointer - 1)
                }
                break
            }
        }
    }

    return buffer.toByteArray()
}
