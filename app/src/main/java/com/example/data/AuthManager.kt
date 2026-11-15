package com.example.data

import android.content.Context
import android.content.SharedPreferences
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object AuthManager {

    private const val PREFS_NAME = "phonedeck_auth_prefs"
    private const val KEY_SALT = "pwd_salt"
    private const val KEY_HASH = "pwd_hash"
    private const val KEY_SECRET = "hmac_secret"
    private const val KEY_VERSION = "auth_version"
    private const val KEY_IS_DEFAULT = "is_default_pwd"

    const val DEFAULT_PASSWORD = "admin"
    const val COOKIE_NAME = "phonedeck_session"
    private const val COOKIE_MAX_AGE_SECONDS = 365L * 24L * 60L * 60L // 1 year

    private val secureRandom = SecureRandom()

    private fun prefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    @Synchronized
    private fun ensureInitialized(context: Context): SharedPreferences {
        val p = prefs(context)
        if (!p.contains(KEY_HASH) || !p.contains(KEY_SALT) || !p.contains(KEY_SECRET)) {
            val salt = p.getString(KEY_SALT, null) ?: randomHex(16)
            val secret = p.getString(KEY_SECRET, null) ?: randomHex(32)
            val hash = sha256Hex("$salt:$DEFAULT_PASSWORD")
            p.edit()
                .putString(KEY_SALT, salt)
                .putString(KEY_HASH, hash)
                .putString(KEY_SECRET, secret)
                .putInt(KEY_VERSION, p.getInt(KEY_VERSION, 1))
                .putBoolean(KEY_IS_DEFAULT, true)
                .apply()
        }
        return p
    }

    fun verifyPassword(context: Context, password: String): Boolean {
        val p = ensureInitialized(context)
        val salt = p.getString(KEY_SALT, "") ?: ""
        val storedHash = p.getString(KEY_HASH, "") ?: ""
        if (salt.isEmpty() || storedHash.isEmpty()) return false
        val candidateHash = sha256Hex("$salt:$password")
        return constantTimeEquals(storedHash, candidateHash)
    }

    fun isUsingDefaultPassword(context: Context): Boolean {
        val p = ensureInitialized(context)
        return p.getBoolean(KEY_IS_DEFAULT, true)
    }

    fun createSessionToken(context: Context): String {
        val p = ensureInitialized(context)
        val version = p.getInt(KEY_VERSION, 1)
        val secret = p.getString(KEY_SECRET, "") ?: ""
        val expirySec = (System.currentTimeMillis() / 1000L) + COOKIE_MAX_AGE_SECONDS
        val payload = "v$version.$expirySec"
        val sig = hmacSha256Hex(secret, payload)
        return "$payload.$sig"
    }

    fun buildSessionCookieHeader(context: Context): String {
        val token = createSessionToken(context)
        return "$COOKIE_NAME=$token; Path=/; Max-Age=$COOKIE_MAX_AGE_SECONDS; HttpOnly; SameSite=Lax"
    }

    fun buildClearCookieHeader(): String {
        return "$COOKIE_NAME=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax"
    }

    fun isRequestAuthenticated(context: Context, cookieHeader: String?): Boolean {
        if (cookieHeader.isNullOrBlank()) return false
        val token = extractCookie(cookieHeader, COOKIE_NAME) ?: return false
        return verifySessionToken(context, token)
    }

    fun verifySessionToken(context: Context, token: String): Boolean {
        val parts = token.trim().split(".")
        if (parts.size != 3) return false
        val versionStr = parts[0]
        val expirySec = parts[1].toLongOrNull() ?: return false
        val providedSig = parts[2]
        if (!versionStr.startsWith("v")) return false
        val tokenVersion = versionStr.substring(1).toIntOrNull() ?: return false

        val nowSec = System.currentTimeMillis() / 1000L
        if (expirySec < nowSec) return false

        val p = ensureInitialized(context)
        val currentVersion = p.getInt(KEY_VERSION, 1)
        if (tokenVersion != currentVersion) return false

        val secret = p.getString(KEY_SECRET, "") ?: return false
        if (secret.isEmpty()) return false
        val expectedSig = hmacSha256Hex(secret, "$versionStr.$expirySec")
        return constantTimeEquals(expectedSig, providedSig)
    }

    @Synchronized
    fun changePassword(
        context: Context,
        currentPassword: String,
        newPassword: String
    ): Pair<Boolean, String> {
        if (!verifyPassword(context, currentPassword)) {
            return false to "Current password is incorrect"
        }
        if (newPassword.isEmpty()) {
            return false to "New password cannot be empty"
        }

        val p = ensureInitialized(context)
        val newSalt = randomHex(16)
        val newHash = sha256Hex("$newSalt:$newPassword")
        val nextVersion = p.getInt(KEY_VERSION, 1) + 1

        p.edit()
            .putString(KEY_SALT, newSalt)
            .putString(KEY_HASH, newHash)
            .putInt(KEY_VERSION, nextVersion)
            .putBoolean(KEY_IS_DEFAULT, newPassword == DEFAULT_PASSWORD)
            .commit()

        return true to "Password updated successfully"
    }

    fun extractCookie(cookieHeader: String, targetName: String): String? {
        val pairs = cookieHeader.split(";")
        for (raw in pairs) {
            val idx = raw.indexOf('=')
            if (idx <= 0) continue
            val name = raw.substring(0, idx).trim()
            if (name == targetName) {
                return raw.substring(idx + 1).trim()
            }
        }
        return null
    }

    private fun randomHex(byteCount: Int): String {
        val bytes = ByteArray(byteCount)
        secureRandom.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun hmacSha256Hex(secret: String, data: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
        val raw = mac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
        return raw.joinToString("") { "%02x".format(it) }
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        return MessageDigest.isEqual(
            a.toByteArray(StandardCharsets.UTF_8),
            b.toByteArray(StandardCharsets.UTF_8)
        )
    }
}
