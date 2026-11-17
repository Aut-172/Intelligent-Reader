package reader.androidmin.app

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * A-E10：Android Keystore 安全凭据最小探针。
 * 验证：密钥生成/读取、加解密往返（硬件级检测 isInsideSecureHardware 为隐藏 API，编译期不可用，故不采）。
 */
object KeystoreProbe {
    private const val ALIAS = "probe-aes"

    fun run(): Map<String, Any?> {
        val out = mutableMapOf<String, Any?>()
        try {
            val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            if (!ks.containsAlias(ALIAS)) {
                val kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
                kg.init(
                    KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .build(),
                )
                kg.generateKey()
                out["keyCreated"] = true
            }
            val key = ks.getKey(ALIAS, null) as SecretKey
            out["algorithm"] = key.algorithm

            // 加解密往返
            val plaintext = "probe-secret".toByteArray()
            val enc = Cipher.getInstance("AES/GCM/NoPadding")
            enc.init(Cipher.ENCRYPT_MODE, key)
            val ciphertext = enc.doFinal(plaintext)
            val iv = enc.iv
            val dec = Cipher.getInstance("AES/GCM/NoPadding")
            dec.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
            val decrypted = dec.doFinal(ciphertext)
            out["roundTripOk"] = plaintext.contentEquals(decrypted)
        } catch (e: Exception) {
            out["error"] = "${e::class.simpleName}: ${e.message}"
        }
        return out
    }
}
