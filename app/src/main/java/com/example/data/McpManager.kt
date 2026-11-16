package com.example.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.io.OutputStream

object McpManager {

    private const val PREFS_NAME = "phonedeck_mcp_prefs"
    private const val KEY_ENABLED = "mcp_enabled"
    private const val KEY_TOKEN = "mcp_token"
    private const val KEY_TUNNEL_URL = "mcp_tunnel_url"

    const val CAT_DEVICE = "device"
    const val CAT_CONTROLS = "controls"
    const val CAT_FILES = "files"
    const val CAT_COMMS = "comms"
    const val CAT_APPS = "apps"
    const val CAT_SHELL = "shell"

    val CATEGORIES = listOf(CAT_DEVICE, CAT_CONTROLS, CAT_FILES, CAT_COMMS, CAT_APPS, CAT_SHELL)

    private val secureRandom = SecureRandom()

    // Registry for active SSE client streams (sessionId -> OutputStream)
    private val activeSseSessions = ConcurrentHashMap<String, OutputStream>()

    private fun prefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    @Synchronized
    private fun ensureInitialized(context: Context): SharedPreferences {
        val p = prefs(context)
        if (!p.contains(KEY_TOKEN)) {
            val token = "pd_mcp_" + randomHex(24)
            p.edit()
                .putBoolean(KEY_ENABLED, true)
                .putString(KEY_TOKEN, token)
                .putString(KEY_TUNNEL_URL, "")
                .putBoolean("cat_$CAT_DEVICE", true)
                .putBoolean("cat_$CAT_CONTROLS", true)
                .putBoolean("cat_$CAT_FILES", true)
                .putBoolean("cat_$CAT_COMMS", true)
                .putBoolean("cat_$CAT_APPS", true)
                .putBoolean("cat_$CAT_SHELL", true)
                .apply()
        }
        return p
    }

    fun isMcpEnabled(context: Context): Boolean {
        val p = ensureInitialized(context)
        return p.getBoolean(KEY_ENABLED, true)
    }

    fun setMcpEnabled(context: Context, enabled: Boolean) {
        val p = ensureInitialized(context)
        p.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun getToken(context: Context): String {
        val p = ensureInitialized(context)
        return p.getString(KEY_TOKEN, "") ?: ""
    }

    @Synchronized
    fun regenerateToken(context: Context): String {
        val p = ensureInitialized(context)
        val newToken = "pd_mcp_" + randomHex(24)
        p.edit().putString(KEY_TOKEN, newToken).apply()
        return newToken
    }

    fun getTunnelUrl(context: Context): String {
        val p = ensureInitialized(context)
        return p.getString(KEY_TUNNEL_URL, "") ?: ""
    }

    fun setTunnelUrl(context: Context, url: String) {
        val p = ensureInitialized(context)
        p.edit().putString(KEY_TUNNEL_URL, url.trim()).apply()
    }

    fun getCloudTunnelUrl(context: Context): String = getTunnelUrl(context)
    fun setCloudTunnelUrl(context: Context, url: String) = setTunnelUrl(context, url)
    fun getOrGenerateToken(context: Context): String = getToken(context)

    fun isCategoryEnabled(context: Context, category: String): Boolean {
        val p = ensureInitialized(context)
        return p.getBoolean("cat_$category", true)
    }

    fun setCategoryEnabled(context: Context, category: String, enabled: Boolean) {
        val p = ensureInitialized(context)
        p.edit().putBoolean("cat_$category", enabled).apply()
    }

    fun verifyToken(context: Context, candidate: String?): Boolean {
        return verifyToken(context, candidate, null) || verifyToken(context, null, candidate)
    }

    fun verifyToken(context: Context, authHeader: String?, queryToken: String?): Boolean {
        val storedToken = getToken(context)
        if (storedToken.isEmpty()) return false

        val candidate = extractBearerToken(authHeader) ?: queryToken?.trim() ?: ""
        if (candidate.isEmpty()) return false

        return constantTimeEquals(storedToken, candidate)
    }

    private fun extractBearerToken(authHeader: String?): String? {
        if (authHeader.isNullOrBlank()) return null
        val parts = authHeader.trim().split("\\s+".toRegex(), 2)
        if (parts.size == 2 && parts[0].equals("Bearer", ignoreCase = true)) {
            return parts[1].trim()
        }
        return null
    }

    fun registerSseSession(sessionId: String, stream: OutputStream) {
        activeSseSessions[sessionId] = stream
    }

    fun unregisterSseSession(sessionId: String) {
        activeSseSessions.remove(sessionId)
    }

    fun getSseSession(sessionId: String): OutputStream? {
        return activeSseSessions[sessionId]
    }

    fun getActiveSseSessionCount(): Int {
        return activeSseSessions.size
    }

    fun getConfigJson(context: Context): JSONObject {
        val enabled = isMcpEnabled(context)
        val token = getToken(context)
        val tunnelUrl = getTunnelUrl(context)
        val catMap = JSONObject()
        for (cat in CATEGORIES) {
            catMap.put(cat, isCategoryEnabled(context, cat))
        }

        return JSONObject().apply {
            put("enabled", enabled)
            put("token", token)
            put("maskedToken", maskToken(token))
            put("tunnelUrl", tunnelUrl)
            put("categories", catMap)
            put("activeSseSessions", getActiveSseSessionCount())
            put("serverPort", ServerStateManager.serverPort.value)
            put("localIp", ServerStateManager.serverIp.value)
        }
    }

    fun updateConfigFromJson(context: Context, json: JSONObject) {
        val p = ensureInitialized(context)
        val editor = p.edit()
        if (json.has("enabled")) {
            editor.putBoolean(KEY_ENABLED, json.optBoolean("enabled", true))
        }
        if (json.has("tunnelUrl")) {
            editor.putString(KEY_TUNNEL_URL, json.optString("tunnelUrl", "").trim())
        }
        if (json.has("categories")) {
            val cats = json.optJSONObject("categories")
            if (cats != null) {
                for (cat in CATEGORIES) {
                    if (cats.has(cat)) {
                        editor.putBoolean("cat_$cat", cats.optBoolean(cat, true))
                    }
                }
            }
        }
        editor.apply()
    }

    private fun maskToken(token: String): String {
        if (token.length <= 10) return "••••••••"
        return token.take(7) + "••••••••" + token.takeLast(4)
    }

    private fun randomHex(byteCount: Int): String {
        val bytes = ByteArray(byteCount)
        secureRandom.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        return MessageDigest.isEqual(
            a.toByteArray(StandardCharsets.UTF_8),
            b.toByteArray(StandardCharsets.UTF_8)
        )
    }
}
