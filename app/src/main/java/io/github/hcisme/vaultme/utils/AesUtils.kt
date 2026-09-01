package io.github.hcisme.vaultme.utils

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object AesUtils {
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"


    private const val FIXED_KEY = "p2L9mQ5vR7tX3kZ8"

    fun encrypt(plainText: String): String {
        val key = getSecretKey()
        val cipher = Cipher.getInstance(ALGORITHM)

        val iv = IvParameterSpec(ByteArray(16))
        cipher.init(Cipher.ENCRYPT_MODE, key, iv)
        val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
    }

    fun decrypt(encryptedText: String): String {
        val key = getSecretKey()
        val cipher = Cipher.getInstance(ALGORITHM)
        val iv = IvParameterSpec(ByteArray(16))
        cipher.init(Cipher.DECRYPT_MODE, key, iv)
        val decodedBytes = Base64.decode(encryptedText, Base64.DEFAULT)
        val decryptedBytes = cipher.doFinal(decodedBytes)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    private fun getSecretKey(): SecretKeySpec {

        val keyBytes = FIXED_KEY.toByteArray(Charsets.UTF_8).copyOf(16)
        return SecretKeySpec(keyBytes, "AES")
    }
}
