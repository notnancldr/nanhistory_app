package id.my.nanclouder.nanhistory.utils

import java.io.InputStream
import java.security.MessageDigest

open class StreamDigest(
    private val algorithm: String = "SHA-256",
    private var digest: MessageDigest = MessageDigest.getInstance(algorithm)
) {

    fun reset() {
        digest.reset()
    }

    fun update(data: ByteArray, offset: Int = 0, length: Int = data.size) {
        digest.update(data, offset, length)
    }

    fun update(stream: InputStream, bufferSize: Int = 8 * 1024) {
        val buffer = ByteArray(bufferSize)
        var bytesRead: Int

        while (stream.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }
    }

    /**
     * Create an exact copy of the current digestor state.
     */
    fun copy(): StreamDigest {
        val clonedDigest = digest.clone() as MessageDigest
        return StreamDigest(algorithm, clonedDigest)
    }

    fun getDigestState(): ByteArray {
        val cloned = digest.clone() as MessageDigest
        return cloned.digest()
    }

    fun finalizeDigest(): ByteArray {
        return digest.digest()
    }
}
