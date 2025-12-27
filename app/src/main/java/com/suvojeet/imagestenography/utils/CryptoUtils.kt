package com.suvojeet.imagestenography.utils

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {
    
    // AES-256-CBC
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/CBC/PKCS5Padding"
    
    fun encrypt(message: String, password: String): String? {
        try {
            if (message.isEmpty() || password.isEmpty()) return null

            // Generate Key from Password (SHA-256)
            val key = generateKey(password)
            val secretKeySpec = SecretKeySpec(key, ALGORITHM)
            
            // Generate Random IV
            val iv = ByteArray(16)
            SecureRandom().nextBytes(iv)
            val ivSpec = IvParameterSpec(iv)

            // Encrypt
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivSpec)
            val encryptedBytes = cipher.doFinal(message.toByteArray(Charsets.UTF_8))
            
            // Combine IV + Encrypted Data
            val combined = ByteArray(iv.size + encryptedBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)
            
            // Encode to Base64
            return Base64.encodeToString(combined, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun decrypt(encryptedBase64: String, password: String): String? {
        try {
            if (encryptedBase64.isEmpty() || password.isEmpty()) return null

            // Decode from Base64
            val combined = Base64.decode(encryptedBase64, Base64.DEFAULT)
            
            // Extract IV (first 16 bytes)
            val iv = ByteArray(16)
            System.arraycopy(combined, 0, iv, 0, iv.size)
            val ivSpec = IvParameterSpec(iv)
            
            // Extract Encrypted Data
            val encryptedBytes = ByteArray(combined.size - 16)
            System.arraycopy(combined, 16, encryptedBytes, 0, encryptedBytes.size)

            // Generate Key from Password
            val key = generateKey(password)
            val secretKeySpec = SecretKeySpec(key, ALGORITHM)

            // Decrypt
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivSpec)
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            
            return String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            // Decryption failed (Wrong password or corrupted data)
            e.printStackTrace()
            return null
        }
    }

    // Hash password to get 256-bit (32 byte) key
    private fun generateKey(password: String): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(password.toByteArray(Charsets.UTF_8))
    }
}
