package com.example.data

import android.content.Context
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Authoritative on-device authentication, device-bound owner registration via on-device
 * one-time OTP notification, PBKDF2 password hashing, and 1-year persistent session cookies.
 */
object AuthManager {

    private const val KEY_DEVICE_ID = "auth_device_id"
    private const val KEY_TUNNEL_SECRET = "auth_tunnel_secret"
    private const val KEY_IS_REGISTERED = "auth_is_registered"
    private const val KEY_USER_ID = "auth_owner_user_id"
    private const val KEY_BOUND_DEVICE_ID = "auth_bound_device_id"
    private const val KEY_PWD_SALT = "auth_pwd_salt"
    private const val KEY_PWD_HASH = "auth_pwd_hash"
    private const val KEY_HMAC_SECRET = "auth_hmac_secret"
    private const val KEY_VERSION = "auth_version"
    private const val KEY_LEGACY_ADMIN_MIGRATION_DONE = "auth_legacy_admin_migration_done"

    const val COOKIE_NAME = "phonedeck_session"
    private const val COOKIE_MAX_AGE_SECONDS = 365L * 24L * 60L * 60L // 1 year persistent per browser
    private const val PBKDF2_ITERATIONS = 120_000
    private const val PBKDF2_KEY_BITS = 256
    private const val OTP_TTL_MS = 5L * 60L * 1000L
    private const val OTP_COOLDOWN_MS = 60L * 1000L
    private const val OTP_MAX_ATTEMPTS = 5
    private const val TICKET_TTL_MS = 10L * 60L * 1000L

    private const val RL_LOGIN_MAX = 5
    private const val RL_LOGIN_WINDOW_MS = 15L * 60L * 1000L
    private const val RL_OTP_SEND_MAX = 5
    private const val RL_OTP_SEND_WINDOW_MS = 15L * 60L * 1000L
    private const val RL_OTP_VERIFY_MAX = 5
    private const val RL_OTP_VERIFY_WINDOW_MS = 15L * 60L * 1000L

    private val secureRandom = SecureRandom()
    private val userIdRegex = Regex("^[a-zA-Z0-9_.-]{3,32}$")
    private val otpRegex = Regex("^\\d{4}$")

    private data class PendingRegistration(
        val pendingId: String,
        val userId: String,
        val pwdSalt: String,
        val pwdHash: String,
        var otpHash: String,
        var expiresAtMs: Long,
        var sentAtMs: Long,
        var attempts: Int = 0
    )

    private data class PendingRecoveryOtp(
        val recoveryId: String,
        val userId: String,
        val otpHash: String,
        val expiresAtMs: Long,
        val sentAtMs: Long,
        var attempts: Int = 0
    )

    private data class PendingTicket(
        val purpose: String,
        val token: String,
        val userId: String,
        val expiresAtMs: Long
    )

    @Volatile
    private var activePendingReg: PendingRegistration? = null

    @Volatile
    private var activeRecoveryOtp: PendingRecoveryOtp? = null

    @Volatile
    private var activeTicket: PendingTicket? = null

    @Volatile
    private var lastOtpPrivacyWindowUntilMs: Long = 0L

    data class AuthResult(
        val success: Boolean,
        val statusCode: Int,
        val message: String,
        val data: Map<String, Any?> = emptyMap()
    )

    @Synchronized
    fun getOrCreateDeviceId(context: Context): String {
        val existing = SecureCredentialStore.getString(context, KEY_DEVICE_ID, null)
        if (!existing.isNullOrBlank()) return existing
        val created = "pd_" + randomHex(10)
        SecureCredentialStore.putString(context, KEY_DEVICE_ID, created)
        return created
    }

    @Synchronized
    fun getOrCreateTunnelSecret(context: Context): String {
        val existing = SecureCredentialStore.getString(context, KEY_TUNNEL_SECRET, null)
        if (!existing.isNullOrBlank()) return existing
        val created = randomHex(32)
        SecureCredentialStore.putString(context, KEY_TUNNEL_SECRET, created)
        return created
    }

    @Synchronized
    private fun getOrCreateHmacSecret(context: Context): String {
        val existing = SecureCredentialStore.getString(context, KEY_HMAC_SECRET, null)
        if (!existing.isNullOrBlank()) return existing
        val created = randomHex(32)
        SecureCredentialStore.putString(context, KEY_HMAC_SECRET, created)
        return created
    }

    @Synchronized
    fun isRegistered(context: Context): Boolean {
        if (!SecureCredentialStore.getBoolean(context, KEY_LEGACY_ADMIN_MIGRATION_DONE, false)) {
            val registered = SecureCredentialStore.getBoolean(context, KEY_IS_REGISTERED, false)
            val userId = SecureCredentialStore.getString(context, KEY_USER_ID, "") ?: ""
            val salt = SecureCredentialStore.getString(context, KEY_PWD_SALT, "") ?: ""
            val storedHash = SecureCredentialStore.getString(context, KEY_PWD_HASH, "") ?: ""
            val isLegacyDefault = registered && userId == "admin" && salt.isNotEmpty() &&
                constantTimeEquals(storedHash, pbkdf2HashHex("admin123", salt))

            if (isLegacyDefault) {
                listOf(
                    KEY_IS_REGISTERED,
                    KEY_USER_ID,
                    KEY_BOUND_DEVICE_ID,
                    KEY_PWD_SALT,
                    KEY_PWD_HASH,
                    KEY_VERSION
                ).forEach { SecureCredentialStore.remove(context, it) }
            }
            SecureCredentialStore.putBoolean(context, KEY_LEGACY_ADMIN_MIGRATION_DONE, true)
            if (isLegacyDefault) {
                runCatching { ServerStateManager.refreshAuthInfo(context) }
            }
        }
        return SecureCredentialStore.getBoolean(context, KEY_IS_REGISTERED, false)
    }

    fun getRegisteredUserId(context: Context): String {
        if (!isRegistered(context)) return ""
        return SecureCredentialStore.getString(context, KEY_USER_ID, "") ?: ""
    }

    fun isOtpPrivacyWindowActive(nowMs: Long = System.currentTimeMillis()): Boolean {
        return nowMs < lastOtpPrivacyWindowUntilMs
    }

    /**
     * Sign Up Page 1 -> Page 2:
     * Validates User ID + Password + Confirm Password, stores pending PBKDF2 hash,
     * and dispatches a one-time 4-digit OS notification to the PhoneDeck Android phone.
     */
    @Synchronized
    fun startRegistration(
        context: Context,
        rawUserId: String,
        password: String,
        confirmPassword: String?,
        clientIp: String
    ): AuthResult {
        if (isRegistered(context)) {
            return AuthResult(false, 403, "Device is already registered. Sign in instead.")
        }

        val rl = RateLimiter.checkAndRecord("reg_otp_send", clientIp, RL_OTP_SEND_MAX, RL_OTP_SEND_WINDOW_MS)
        if (!rl.allowed) {
            return AuthResult(
                false,
                429,
                "Too many attempts. Try again in ${rl.retryAfterSeconds}s.",
                mapOf("retryAfter" to rl.retryAfterSeconds)
            )
        }

        val userId = rawUserId.trim()
        if (!userIdRegex.matches(userId)) {
            return AuthResult(false, 400, "User must be 3-32 characters (letters, numbers, ., _, -).")
        }
        if (password.length < 8 || password.length > 128) {
            return AuthResult(false, 400, "Password must be at least 8 characters.")
        }
        if (confirmPassword != null && password != confirmPassword) {
            return AuthResult(false, 400, "Passwords do not match.")
        }
        if (!OtpNotificationHelper.canPostNotifications(context)) {
            return AuthResult(
                false,
                503,
                "Allow Notifications on the PhoneDeck phone first to receive the OTP."
            )
        }

        val now = System.currentTimeMillis()
        val salt = randomHex(16)
        val pwdHash = pbkdf2HashHex(password, salt)
        val pendingId = randomHex(16)
        val code = "%04d".format(secureRandom.nextInt(10_000))
        val secret = getOrCreateHmacSecret(context)
        val otpHash = hmacSha256Hex(secret, "register:$pendingId:$userId:$code")

        lastOtpPrivacyWindowUntilMs = now + OTP_TTL_MS
        val posted = OtpNotificationHelper.postOneTimeOtpNotification(
            context = context,
            userId = userId,
            code = code,
            isRecovery = false
        )
        if (!posted) {
            return AuthResult(false, 503, "Could not show OTP notification on the phone.")
        }

        activePendingReg = PendingRegistration(
            pendingId = pendingId,
            userId = userId,
            pwdSalt = salt,
            pwdHash = pwdHash,
            otpHash = otpHash,
            expiresAtMs = now + OTP_TTL_MS,
            sentAtMs = now,
            attempts = 0
        )

        return AuthResult(
            true,
            200,
            "One-time OTP sent to PhoneDeck notification.",
            mapOf(
                "pendingId" to pendingId,
                "userId" to userId,
                "expiresInSeconds" to (OTP_TTL_MS / 1000L)
            )
        )
    }

    /**
     * Sign Up Page 2 (Resend): sends a fresh 4-digit OTP notification after cooldown.
     */
    @Synchronized
    fun resendRegistrationOtp(context: Context, pendingId: String, clientIp: String): AuthResult {
        if (isRegistered(context)) {
            return AuthResult(false, 403, "Device is already registered.")
        }
        val pending = activePendingReg
        val now = System.currentTimeMillis()
        if (pending == null || now > pending.expiresAtMs || !constantTimeEquals(pending.pendingId, pendingId.trim())) {
            return AuthResult(false, 400, "Session expired. Please go back and continue again.")
        }
        if ((now - pending.sentAtMs) < OTP_COOLDOWN_MS) {
            val waitSec = ((OTP_COOLDOWN_MS - (now - pending.sentAtMs) + 999L) / 1000L).coerceAtLeast(1L)
            return AuthResult(false, 429, "Wait ${waitSec}s before resending.", mapOf("retryAfter" to waitSec))
        }
        val rl = RateLimiter.checkAndRecord("reg_otp_send", clientIp, RL_OTP_SEND_MAX, RL_OTP_SEND_WINDOW_MS)
        if (!rl.allowed) {
            return AuthResult(false, 429, "Too many requests. Try again in ${rl.retryAfterSeconds}s.")
        }
        if (!OtpNotificationHelper.canPostNotifications(context)) {
            return AuthResult(false, 503, "Allow Notifications on the PhoneDeck phone first.")
        }

        val code = "%04d".format(secureRandom.nextInt(10_000))
        val secret = getOrCreateHmacSecret(context)
        val otpHash = hmacSha256Hex(secret, "register:${pending.pendingId}:${pending.userId}:$code")

        lastOtpPrivacyWindowUntilMs = now + OTP_TTL_MS
        val posted = OtpNotificationHelper.postOneTimeOtpNotification(
            context = context,
            userId = pending.userId,
            code = code,
            isRecovery = false
        )
        if (!posted) {
            return AuthResult(false, 503, "Could not show OTP notification on the phone.")
        }

        pending.otpHash = otpHash
        pending.expiresAtMs = now + OTP_TTL_MS
        pending.sentAtMs = now
        pending.attempts = 0

        return AuthResult(true, 200, "New OTP sent to PhoneDeck notification.")
    }

    /**
     * Sign Up Page 2 (Verify OTP & Complete Account Creation):
     * Confirms the 4-digit OTP from the Android device notification, persists owner credentials,
     * cancels the notification, and permanently disables future initial registration.
     */
    @Synchronized
    fun verifyRegistrationOtp(
        context: Context,
        pendingId: String,
        rawCode: String,
        clientIp: String
    ): AuthResult {
        if (isRegistered(context)) {
            return AuthResult(false, 403, "Device is already registered.")
        }

        val rl = RateLimiter.checkAndRecord("reg_otp_verify", clientIp, RL_OTP_VERIFY_MAX, RL_OTP_VERIFY_WINDOW_MS)
        if (!rl.allowed) {
            return AuthResult(
                false,
                429,
                "Too many failed attempts. Try again in ${rl.retryAfterSeconds}s.",
                mapOf("retryAfter" to rl.retryAfterSeconds)
            )
        }

        val code = rawCode.trim()
        if (!otpRegex.matches(code)) {
            return AuthResult(false, 400, "Enter the 4-digit OTP code.")
        }

        val pending = activePendingReg
        val now = System.currentTimeMillis()
        if (pending == null || !constantTimeEquals(pending.pendingId, pendingId.trim())) {
            return AuthResult(false, 400, "No pending sign up. Go back and enter User & Password.")
        }
        if (now > pending.expiresAtMs) {
            activePendingReg = null
            OtpNotificationHelper.cancelOtpNotification(context)
            return AuthResult(false, 400, "OTP expired. Request a new code.")
        }

        val secret = getOrCreateHmacSecret(context)
        val candidateHash = hmacSha256Hex(secret, "register:${pending.pendingId}:${pending.userId}:$code")
        if (!constantTimeEquals(pending.otpHash, candidateHash)) {
            pending.attempts += 1
            if (pending.attempts >= OTP_MAX_ATTEMPTS) {
                activePendingReg = null
                OtpNotificationHelper.cancelOtpNotification(context)
                return AuthResult(false, 429, "Too many wrong codes. Start registration again.")
            }
            val left = OTP_MAX_ATTEMPTS - pending.attempts
            return AuthResult(false, 401, "Invalid code ($left attempts left).")
        }

        val deviceId = getOrCreateDeviceId(context)
        SecureCredentialStore.putString(context, KEY_USER_ID, pending.userId)
        SecureCredentialStore.putString(context, KEY_BOUND_DEVICE_ID, deviceId)
        SecureCredentialStore.putString(context, KEY_PWD_SALT, pending.pwdSalt)
        SecureCredentialStore.putString(context, KEY_PWD_HASH, pending.pwdHash)
        SecureCredentialStore.putInt(context, KEY_VERSION, 1)
        SecureCredentialStore.putBoolean(context, KEY_IS_REGISTERED, true)

        val registeredUser = pending.userId
        activePendingReg = null
        lastOtpPrivacyWindowUntilMs = 0L
        RateLimiter.reset("reg_otp_verify", clientIp)
        OtpNotificationHelper.cancelOtpNotification(context)
        ServerStateManager.refreshAuthInfo(context)

        return AuthResult(
            true,
            200,
            "Account created",
            mapOf("userId" to registeredUser, "deviceId" to deviceId)
        )
    }

    /**
     * Subsequent login on any browser: User + Password only (persists 1 year via signed cookie).
     */
    @Synchronized
    fun login(context: Context, rawUserId: String, password: String, clientIp: String): AuthResult {
        if (!isRegistered(context)) {
            return AuthResult(false, 403, "Account has not been created yet.")
        }

        val blocked = RateLimiter.peekBlocked("login", clientIp, RL_LOGIN_MAX, RL_LOGIN_WINDOW_MS)
        if (!blocked.allowed) {
            return AuthResult(
                false,
                429,
                "Too many failed attempts. Try again in ${blocked.retryAfterSeconds}s.",
                mapOf("retryAfter" to blocked.retryAfterSeconds)
            )
        }

        val deviceId = getOrCreateDeviceId(context)
        val boundDeviceId = SecureCredentialStore.getString(context, KEY_BOUND_DEVICE_ID, "") ?: ""
        val storedUserId = SecureCredentialStore.getString(context, KEY_USER_ID, "") ?: ""
        val salt = SecureCredentialStore.getString(context, KEY_PWD_SALT, "") ?: ""
        val storedHash = SecureCredentialStore.getString(context, KEY_PWD_HASH, "") ?: ""

        if (boundDeviceId != deviceId || storedUserId.isEmpty() || salt.isEmpty() || storedHash.isEmpty()) {
            return AuthResult(false, 403, "Device credential binding mismatch.")
        }

        val candidateUser = rawUserId.trim()
        val userMatch = constantTimeEquals(storedUserId, candidateUser)
        val candidateHash = pbkdf2HashHex(password, salt)
        val pwdMatch = constantTimeEquals(storedHash, candidateHash)

        if (!userMatch || !pwdMatch) {
            RateLimiter.recordFailure("login", clientIp, RL_LOGIN_WINDOW_MS)
            val after = RateLimiter.peekBlocked("login", clientIp, RL_LOGIN_MAX, RL_LOGIN_WINDOW_MS)
            val msg = if (!after.allowed) {
                "Invalid User or Password. Locked for ${after.retryAfterSeconds}s."
            } else {
                "Invalid User or Password."
            }
            return AuthResult(false, if (!after.allowed) 429 else 401, msg)
        }

        RateLimiter.reset("login", clientIp)
        return AuthResult(
            true,
            200,
            "Signed in",
            mapOf("userId" to storedUserId, "deviceId" to deviceId)
        )
    }

    /**
     * Password Reset Step 1: validates registered User, posts one-time 4-digit OTP notification to the phone.
     */
    @Synchronized
    fun startRecovery(context: Context, rawUserId: String, clientIp: String): AuthResult {
        if (!isRegistered(context)) {
            return AuthResult(false, 403, "No account exists on this device.")
        }

        val rl = RateLimiter.checkAndRecord("rec_otp_send", clientIp, RL_OTP_SEND_MAX, RL_OTP_SEND_WINDOW_MS)
        if (!rl.allowed) {
            return AuthResult(
                false,
                429,
                "Too many reset requests. Try again in ${rl.retryAfterSeconds}s.",
                mapOf("retryAfter" to rl.retryAfterSeconds)
            )
        }

        val userId = rawUserId.trim()
        val storedUser = SecureCredentialStore.getString(context, KEY_USER_ID, "") ?: ""
        if (!constantTimeEquals(storedUser, userId)) {
            return AuthResult(false, 401, "User does not match this device.")
        }

        val now = System.currentTimeMillis()
        val existing = activeRecoveryOtp
        if (existing != null && (now - existing.sentAtMs) < OTP_COOLDOWN_MS) {
            val waitSec = ((OTP_COOLDOWN_MS - (now - existing.sentAtMs) + 999L) / 1000L).coerceAtLeast(1L)
            return AuthResult(false, 429, "Wait ${waitSec}s before requesting another code.")
        }

        if (!OtpNotificationHelper.canPostNotifications(context)) {
            return AuthResult(false, 503, "Allow Notifications on the PhoneDeck phone first.")
        }

        val recoveryId = randomHex(16)
        val code = "%04d".format(secureRandom.nextInt(10_000))
        val secret = getOrCreateHmacSecret(context)
        val otpHash = hmacSha256Hex(secret, "recover:$recoveryId:$userId:$code")

        lastOtpPrivacyWindowUntilMs = now + OTP_TTL_MS
        val posted = OtpNotificationHelper.postOneTimeOtpNotification(
            context = context,
            userId = userId,
            code = code,
            isRecovery = true
        )
        if (!posted) {
            return AuthResult(false, 503, "Could not show OTP notification on the phone.")
        }

        activeRecoveryOtp = PendingRecoveryOtp(
            recoveryId = recoveryId,
            userId = userId,
            otpHash = otpHash,
            expiresAtMs = now + OTP_TTL_MS,
            sentAtMs = now,
            attempts = 0
        )

        return AuthResult(
            true,
            200,
            "One-time reset OTP sent to PhoneDeck notification.",
            mapOf("recoveryId" to recoveryId, "userId" to userId)
        )
    }

    @Synchronized
    fun verifyRecoveryOtp(
        context: Context,
        recoveryId: String,
        rawCode: String,
        clientIp: String
    ): AuthResult {
        if (!isRegistered(context)) {
            return AuthResult(false, 403, "Device is not registered.")
        }

        val rl = RateLimiter.checkAndRecord("rec_otp_verify", clientIp, RL_OTP_VERIFY_MAX, RL_OTP_VERIFY_WINDOW_MS)
        if (!rl.allowed) {
            return AuthResult(false, 429, "Too many failed attempts. Try again in ${rl.retryAfterSeconds}s.")
        }

        val code = rawCode.trim()
        if (!otpRegex.matches(code)) {
            return AuthResult(false, 400, "Enter the 4-digit OTP code.")
        }

        val pending = activeRecoveryOtp
        val now = System.currentTimeMillis()
        if (pending == null || !constantTimeEquals(pending.recoveryId, recoveryId.trim())) {
            return AuthResult(false, 400, "No active reset request.")
        }
        if (now > pending.expiresAtMs) {
            activeRecoveryOtp = null
            OtpNotificationHelper.cancelOtpNotification(context)
            return AuthResult(false, 400, "Reset code expired.")
        }

        val secret = getOrCreateHmacSecret(context)
        val candidateHash = hmacSha256Hex(secret, "recover:${pending.recoveryId}:${pending.userId}:$code")
        if (!constantTimeEquals(pending.otpHash, candidateHash)) {
            pending.attempts += 1
            if (pending.attempts >= OTP_MAX_ATTEMPTS) {
                activeRecoveryOtp = null
                OtpNotificationHelper.cancelOtpNotification(context)
                return AuthResult(false, 429, "Maximum attempts exceeded. Request a new code.")
            }
            return AuthResult(false, 401, "Invalid 4-digit code.")
        }

        activeRecoveryOtp = null
        RateLimiter.reset("rec_otp_verify", clientIp)
        OtpNotificationHelper.cancelOtpNotification(context)
        val ticketToken = randomHex(24)
        activeTicket = PendingTicket(
            purpose = "recover",
            token = ticketToken,
            userId = pending.userId,
            expiresAtMs = now + TICKET_TTL_MS
        )

        return AuthResult(true, 200, "Verified", mapOf("recoveryTicket" to ticketToken))
    }

    @Synchronized
    fun completeRecovery(context: Context, recoveryTicket: String, newPassword: String): AuthResult {
        if (!isRegistered(context)) {
            return AuthResult(false, 403, "Device is not registered.")
        }
        val now = System.currentTimeMillis()
        val ticket = activeTicket
        if (ticket == null || ticket.purpose != "recover" || now > ticket.expiresAtMs) {
            activeTicket = null
            return AuthResult(false, 401, "Reset session expired.")
        }
        if (!constantTimeEquals(ticket.token, recoveryTicket.trim())) {
            return AuthResult(false, 401, "Invalid reset token.")
        }
        if (newPassword.length < 8 || newPassword.length > 128) {
            return AuthResult(false, 400, "Password must be at least 8 characters.")
        }

        val newSalt = randomHex(16)
        val newHash = pbkdf2HashHex(newPassword, newSalt)
        val nextVersion = SecureCredentialStore.getInt(context, KEY_VERSION, 1) + 1

        SecureCredentialStore.putString(context, KEY_PWD_SALT, newSalt)
        SecureCredentialStore.putString(context, KEY_PWD_HASH, newHash)
        SecureCredentialStore.putInt(context, KEY_VERSION, nextVersion)

        activeTicket = null
        activeRecoveryOtp = null
        lastOtpPrivacyWindowUntilMs = 0L

        return AuthResult(true, 200, "Password updated.")
    }

    @Synchronized
    fun changePassword(context: Context, currentPassword: String, newPassword: String): Pair<Boolean, String> {
        if (!isRegistered(context)) {
            return false to "Device is not registered"
        }
        val salt = SecureCredentialStore.getString(context, KEY_PWD_SALT, "") ?: ""
        val storedHash = SecureCredentialStore.getString(context, KEY_PWD_HASH, "") ?: ""
        if (salt.isEmpty() || storedHash.isEmpty()) {
            return false to "Owner credentials missing"
        }
        val curHash = pbkdf2HashHex(currentPassword, salt)
        if (!constantTimeEquals(storedHash, curHash)) {
            return false to "Current password is incorrect"
        }
        if (newPassword.length < 8 || newPassword.length > 128) {
            return false to "New password must be at least 8 characters"
        }

        val newSalt = randomHex(16)
        val newHash = pbkdf2HashHex(newPassword, newSalt)
        val nextVersion = SecureCredentialStore.getInt(context, KEY_VERSION, 1) + 1

        SecureCredentialStore.putString(context, KEY_PWD_SALT, newSalt)
        SecureCredentialStore.putString(context, KEY_PWD_HASH, newHash)
        SecureCredentialStore.putInt(context, KEY_VERSION, nextVersion)

        return true to "Password updated successfully"
    }

    @Synchronized
    fun revokeAllSessions(context: Context) {
        if (!isRegistered(context)) return
        val nextVersion = SecureCredentialStore.getInt(context, KEY_VERSION, 1) + 1
        SecureCredentialStore.putInt(context, KEY_VERSION, nextVersion)
    }

    fun createSessionToken(context: Context): String {
        val deviceId = getOrCreateDeviceId(context)
        val version = SecureCredentialStore.getInt(context, KEY_VERSION, 1)
        val secret = getOrCreateHmacSecret(context)
        val expirySec = (System.currentTimeMillis() / 1000L) + COOKIE_MAX_AGE_SECONDS
        val jti = randomHex(8)
        val payload = "v$version.$deviceId.$expirySec.$jti"
        val sig = hmacSha256Hex(secret, payload)
        return "$payload.$sig"
    }

    fun buildSessionCookieHeader(context: Context, isSecureTransport: Boolean = false): String {
        val token = createSessionToken(context)
        val secureAttr = if (isSecureTransport) "; Secure" else ""
        return "$COOKIE_NAME=$token; Path=/; Max-Age=$COOKIE_MAX_AGE_SECONDS; HttpOnly; SameSite=Lax$secureAttr"
    }

    fun buildClearCookieHeader(isSecureTransport: Boolean = false): String {
        val secureAttr = if (isSecureTransport) "; Secure" else ""
        return "$COOKIE_NAME=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax$secureAttr"
    }

    fun isRequestAuthenticated(context: Context, cookieHeader: String?, clientIp: String? = null): Boolean {
        if (cookieHeader.isNullOrBlank()) return false
        val token = extractCookie(cookieHeader, COOKIE_NAME) ?: return false
        return verifySessionToken(context, token)
    }


    fun verifySessionToken(context: Context, token: String): Boolean {
        if (!isRegistered(context)) return false
        val parts = token.trim().split(".")
        if (parts.size != 5) return false

        val versionStr = parts[0]
        val tokenDeviceId = parts[1]
        val expirySec = parts[2].toLongOrNull() ?: return false
        val jti = parts[3]
        val providedSig = parts[4]

        if (!versionStr.startsWith("v") || jti.isBlank()) return false
        val tokenVersion = versionStr.substring(1).toIntOrNull() ?: return false

        val currentDeviceId = getOrCreateDeviceId(context)
        val boundDeviceId = SecureCredentialStore.getString(context, KEY_BOUND_DEVICE_ID, "") ?: ""
        if (tokenDeviceId != currentDeviceId || boundDeviceId != currentDeviceId) return false

        val nowSec = System.currentTimeMillis() / 1000L
        if (expirySec < nowSec) return false

        val currentVersion = SecureCredentialStore.getInt(context, KEY_VERSION, 1)
        if (tokenVersion != currentVersion) return false

        val secret = getOrCreateHmacSecret(context)
        val expectedSig = hmacSha256Hex(secret, "$versionStr.$tokenDeviceId.$expirySec.$jti")
        return constantTimeEquals(expectedSig, providedSig)
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

    private fun pbkdf2HashHex(password: String, saltHex: String): String {
        val spec = PBEKeySpec(
            password.toCharArray(),
            saltHex.toByteArray(StandardCharsets.UTF_8),
            PBKDF2_ITERATIONS,
            PBKDF2_KEY_BITS
        )
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val bytes = factory.generateSecret(spec).encoded
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun randomHex(byteCount: Int): String {
        val bytes = ByteArray(byteCount)
        secureRandom.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
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

    internal fun resetTransientStateForTesting() {
        activePendingReg = null
        activeRecoveryOtp = null
        activeTicket = null
        lastOtpPrivacyWindowUntilMs = 0L
        RateLimiter.clearAll()
    }
}
