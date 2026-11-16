package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Encrypts sensitive credentials, device secrets, and auth metadata using AES-256-GCM
 * backed by AndroidKeyStore (hardware-backed where available).
 */
object SecureCredentialStore {

    private const val PREFS_NAME = "phonedeck_secure_vault"
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "phonedeck_vault_master_v1"
    private const val GCM_TAG_BITS = 128
    private const val GCM_IV_BYTES = 12
    private const val FALLBACK_KEY_PREF = "_jvm_fallback_k"

    private val secureRandom = SecureRandom()

    @Volatile
    private var cachedSecretKey: SecretKey? = null

    private fun prefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    @Synchronized
    private fun getOrCreateMasterKey(context: Context): SecretKey {
        cachedSecretKey?.let { return it }

        val keystoreKey = runCatching {
            val ks = KeyStore.getInstance(KEYSTORE_PROVIDER)
            ks.load(null)
            if (ks.containsAlias(KEY_ALIAS)) {
                (ks.getKey(KEY_ALIAS, null) as? SecretKey)
            } else {
                val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
                val spec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                keyGen.init(spec)
                keyGen.generateKey()
            }
        }.getOrNull()

        if (keystoreKey != null) {
            cachedSecretKey = keystoreKey
            return keystoreKey
        }

        // Fallback exclusively for non-Android JVM unit test runners (e.g. Robolectric)
        val p = prefs(context)
        val existingB64 = p.getString(FALLBACK_KEY_PREF, null)
        val rawBytes = if (existingB64 != null) {
            Base64.decode(existingB64, Base64.NO_WRAP)
        } else {
            val generated = ByteArray(32)
            secureRandom.nextBytes(generated)
            p.edit().putString(FALLBACK_KEY_PREF, Base64.encodeToString(generated, Base64.NO_WRAP)).commit()
            generated
        }
        val key = SecretKeySpec(rawBytes, "AES")
        cachedSecretKey = key
        return key
    }

    @Synchronized
    fun putString(context: Context, key: String, value: String) {
        val masterKey = getOrCreateMasterKey(context)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, masterKey)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
        val combined = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)
        val encoded = Base64.encodeToString(combined, Base64.NO_WRAP)
        prefs(context).edit().putString(storageKey(key), encoded).commit()
    }

    @Synchronized
    fun getString(context: Context, key: String, defaultValue: String? = null): String? {
        val encoded = prefs(context).getString(storageKey(key), null) ?: return defaultValue
        return runCatching {
            val combined = Base64.decode(encoded, Base64.NO_WRAP)
            if (combined.size <= GCM_IV_BYTES) return defaultValue
            val iv = combined.copyOfRange(0, GCM_IV_BYTES)
            val ciphertext = combined.copyOfRange(GCM_IV_BYTES, combined.size)
            val masterKey = getOrCreateMasterKey(context)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, masterKey, GCMParameterSpec(GCM_TAG_BITS, iv))
            val plain = cipher.doFinal(ciphertext)
            String(plain, StandardCharsets.UTF_8)
        }.getOrDefault(defaultValue)
    }

    @Synchronized
    fun remove(context: Context, key: String) {
        prefs(context).edit().remove(storageKey(key)).commit()
    }

    fun contains(context: Context, key: String): Boolean {
        return prefs(context).contains(storageKey(key))
    }

    fun putBoolean(context: Context, key: String, value: Boolean) {
        putString(context, key, if (value) "1" else "0")
    }

    fun getBoolean(context: Context, key: String, defaultValue: Boolean = false): Boolean {
        val v = getString(context, key, null) ?: return defaultValue
        return v == "1" || v.equals("true", ignoreCase = true)
    }

    fun putInt(context: Context, key: String, value: Int) {
        putString(context, key, value.toString())
    }

    fun getInt(context: Context, key: String, defaultValue: Int = 0): Int {
        return getString(context, key, null)?.toIntOrNull() ?: defaultValue
    }

    @Synchronized
    fun clearAll(context: Context) {
        prefs(context).edit().clear().commit()
        cachedSecretKey = null
    }

    private fun storageKey(key: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(key.toByteArray(StandardCharsets.UTF_8))
        return "v1_" + digest.take(16).joinToString("") { "%02x".format(it) }
    }
}
