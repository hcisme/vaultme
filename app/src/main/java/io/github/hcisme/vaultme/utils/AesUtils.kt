package io.github.hcisme.vaultme.utils

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 简单的 AES 加密工具类
 * 使用 AES/CBC/PKCS5Padding 模式
 */
object AesUtils {
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"

    // 密钥必须是 16, 24 或 32 字节长
    private const val FIXED_KEY = "p2L9mQ5vR7tX3kZ8"

    /**
     * 加密字符串
     * @param plainText 明文
     * @return Base64 编码后的密文
     */
    fun encrypt(plainText: String): String {
        val key = getSecretKey()
        val cipher = Cipher.getInstance(ALGORITHM)
        // 简单起见，使用全 0 的 IV（生产环境建议使用随机 IV 并与密文一起存储）
        val iv = IvParameterSpec(ByteArray(16))
        cipher.init(Cipher.ENCRYPT_MODE, key, iv)
        val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
    }

    /**
     * 解密字符串
     * @param encryptedText Base64 编码后的密文
     * @return 解密后的明文
     */
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
        // 截取或填充至 16 字节 (AES-128)
        val keyBytes = FIXED_KEY.toByteArray(Charsets.UTF_8).copyOf(16)
        return SecretKeySpec(keyBytes, "AES")
    }
}
