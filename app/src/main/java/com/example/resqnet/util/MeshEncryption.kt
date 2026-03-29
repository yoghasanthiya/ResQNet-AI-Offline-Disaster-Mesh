package com.example.resqnet.util

import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object MeshEncryption {
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val KEY_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 12_000
    private const val KEY_LENGTH = 256
    private const val IV_LENGTH = 12
    private const val TAG_LENGTH = 128
    private const val PASSPHRASE = "ResQNetMeshEmergencyKey"
    private val salt = "resqnet.mesh".toByteArray(StandardCharsets.UTF_8)

    private fun keySpec(): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance(KEY_ALGORITHM)
        val key = factory.generateSecret(
            PBEKeySpec(PASSPHRASE.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        )
        return SecretKeySpec(key.encoded, "AES")
    }

    fun encrypt(value: String): String {
        val iv = ByteArray(IV_LENGTH).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec(), GCMParameterSpec(TAG_LENGTH, iv))
        val encrypted = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
        val payload = iv + encrypted
        return Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    fun decrypt(value: String): String {
        val payload = Base64.decode(value, Base64.NO_WRAP)
        val iv = payload.copyOfRange(0, IV_LENGTH)
        val encrypted = payload.copyOfRange(IV_LENGTH, payload.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, keySpec(), GCMParameterSpec(TAG_LENGTH, iv))
        return String(cipher.doFinal(encrypted), StandardCharsets.UTF_8)
    }
}
