package cn.tinyhai.aes

import org.apache.tomcat.util.codec.binary.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object AESUtils {
    private const val KEY_ALGORITHM = "AES"

    private const val CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding"

    fun encrypt(source: String, key: String): String {
        val sourceBytes = "a".repeat(64).plus(source).toByteArray()
        val keyBytes = key.toByteArray()
        val cipher = Cipher.getInstance(CIPHER_ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(keyBytes, KEY_ALGORITHM),
            IvParameterSpec("a".repeat(16).toByteArray()))
        return Base64.encodeBase64String(cipher.doFinal(sourceBytes))
    }

    fun decrypt(encryptedString: String, key: String): String {
        val encryptedBytes = Base64.decodeBase64(encryptedString)
        val keyBytes = key.toByteArray()
        val cipher = Cipher.getInstance(CIPHER_ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(keyBytes, KEY_ALGORITHM),
            IvParameterSpec("a".repeat(16).toByteArray()))
        return cipher.doFinal(encryptedBytes).toString(Charsets.UTF_8).substring(64);
    }
}