package reader.windowsmin.shared.identity

import java.security.MessageDigest

/** JVM (Windows) implementation of the shared hashing contract. */
actual fun sha256Hex(bytes: ByteArray): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
    return digest.joinToString("") { b -> (b.toInt() and 0xff).toString(16).padStart(2, '0') }
}
