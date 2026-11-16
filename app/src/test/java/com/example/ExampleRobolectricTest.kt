package com.example

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.controller.ContentDataProvider
import com.example.controller.DeviceController
import com.example.controller.PermissionHelper
import com.example.controller.ScreenCastManager
import com.example.data.AuthManager
import com.example.data.CommandLogRepository
import com.example.data.OtpNotificationHelper
import com.example.data.RateLimiter
import com.example.data.SecureCredentialStore
import com.example.data.ServerStateManager
import com.example.data.TunnelStatus
import com.example.server.AuthHtml
import com.example.server.DashboardHtml
import com.example.server.HttpBridgeServer
import com.example.server.RemoteTunnelClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.net.ServerSocket
import java.util.concurrent.TimeUnit
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    Shadows.shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    SecureCredentialStore.clearAll(context)
    AuthManager.resetTransientStateForTesting()
    ServerStateManager.setTunnelState(TunnelStatus.DISCONNECTED)
  }

  @Test
  fun `verify app name matches PhoneDeck`() {
    val appName = context.getString(R.string.app_name)
    assertEquals("PhoneDeck", appName)
  }

  @Test
  fun `verify device status telemetry reading`() {
    val status = DeviceController.getDeviceStatus(context)
    assertNotNull(status)
    assertTrue(status.batteryLevel >= 0)
    assertNotNull(status.localIp)
  }

  @Test
  fun `verify all permissions state list includes cast, app installer, and notifications`() {
    val states = PermissionHelper.getAllPermissionStates(context)
    assertTrue(states.size >= 18)
    assertTrue(states.any { it.id == PermissionHelper.ID_ALL_FILES })
    assertTrue(states.any { it.id == PermissionHelper.ID_CONTACTS })
    assertTrue(states.any { it.id == PermissionHelper.ID_SMS })
    assertTrue(states.any { it.id == PermissionHelper.ID_LOCATION })
    assertTrue(states.any { it.id == PermissionHelper.ID_SCREEN_CAST })
    assertTrue(states.any { it.id == PermissionHelper.ID_INSTALL_PACKAGES })
    assertTrue(states.any { it.id == PermissionHelper.ID_ACCESSIBILITY })
    assertTrue(states.any { it.id == PermissionHelper.ID_POST_NOTIFICATIONS })
  }

  @Test
  fun `verify essential runtime permissions can be queried for auto-request on app launch`() {
    val needed = PermissionHelper.getEssentialRuntimePermissions(context)
    assertTrue(needed.isNotEmpty())
    assertTrue(needed.contains(Manifest.permission.CAMERA))
    assertTrue(needed.contains(Manifest.permission.READ_CONTACTS))
    assertTrue(needed.contains(Manifest.permission.READ_SMS))
    assertTrue(needed.contains(Manifest.permission.ACCESS_FINE_LOCATION))
  }

  @Test
  fun `verify web dashboard and auth html follow unified light theme and grouped sidebar navigation`() {
    val dashHtml = DashboardHtml.get()
    assertTrue(dashHtml.contains("sidebar-nav"))
    assertFalse(dashHtml.contains("top-nav"))
    assertTrue(dashHtml.contains("Control"))
    assertTrue(dashHtml.contains("Communications"))
    assertTrue(dashHtml.contains("Storage"))
    assertTrue(dashHtml.contains("System &amp; Agent"))
    assertTrue(dashHtml.contains("page-screen"))
    assertTrue(dashHtml.contains("/api/screen/stream"))
    assertTrue(dashHtml.contains("/api/screen/input"))
    assertTrue(dashHtml.contains("/api/camera/capture"))
    assertTrue(dashHtml.contains("/api/files/read"))
    assertTrue(dashHtml.contains("/api/files/write"))
    assertTrue(dashHtml.contains("/api/files/rename"))
    assertTrue(dashHtml.contains("sms-thread-list"))
    assertTrue(dashHtml.contains("sendMediaKey"))
    assertTrue(dashHtml.contains("file-dropzone"))
    assertTrue(dashHtml.contains("apk-dropzone"))
    assertTrue(dashHtml.contains("/api/apps/upload-install"))
    assertTrue(dashHtml.contains("/api/apps/uninstall"))
    assertTrue(dashHtml.contains("page-settings"))
    assertTrue(dashHtml.contains("owner-device-id"))
    assertTrue(dashHtml.contains("On-Device OTP"))
    assertFalse(dashHtml.contains("pushNotif"))

    val authHtml = AuthHtml.get()
    assertTrue(authHtml.contains("#f8fafc"))
    assertTrue(authHtml.contains("#2563eb"))
    assertTrue(authHtml.contains("view-register"))
    assertTrue(authHtml.contains("view-login"))
    assertTrue(authHtml.contains("Confirm Password"))
    assertTrue(authHtml.contains("/api/auth/register/start"))
    assertTrue(authHtml.contains("/api/auth/register/verify-otp"))
    assertTrue(authHtml.contains("/api/auth/login"))
    assertTrue(authHtml.contains("/api/auth/recover/start"))
  }

  @Test
  fun `verify screen cast config clamping, viewer tracking, app cache invalidation, and always-active behavior`() {
    ScreenCastManager.updateConfig(fps = 99, quality = 5, scale = 200)
    assertEquals(20, ScreenCastManager.targetFps.get())
    assertEquals(20, ScreenCastManager.jpegQuality.get())
    assertEquals(100, ScreenCastManager.scalePercent.get())
    ScreenCastManager.markViewerActive()

    DeviceController.invalidateAppsCache()
    val apps = DeviceController.getInstalledApps(context)
    assertNotNull(apps)

    ServerStateManager.setManuallyStopped(context, true)
    assertFalse(ServerStateManager.isManuallyStopped(context))
    ServerStateManager.setManuallyStopped(context, false)
    assertFalse(ServerStateManager.isManuallyStopped(context))
  }

  @Test
  fun `verify file browse result returns safely`() {
    val res = ContentDataProvider.browseFiles(null, context)
    assertNotNull(res.currentPath)
  }

  @Test
  fun `verify command log repository`() {
    CommandLogRepository.clear()
    CommandLogRepository.log("POST", "/api/action", "127.0.0.1", 200, "Action: test")
    val logs = CommandLogRepository.logs.value
    assertEquals(1, logs.size)
    assertEquals("POST", logs[0].method)
  }

  @Test
  fun `verify zero non-OTP notifications on service start, boot receiver, and alert action`() {
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val shadowNm = Shadows.shadowOf(nm)

    // Pre-create legacy foreground channel to confirm cleanup deletes it
    val legacyChannel = android.app.NotificationChannel(
      com.example.service.RemoteServerService.CHANNEL_ID,
      "Legacy",
      NotificationManager.IMPORTANCE_MIN
    )
    nm.createNotificationChannel(legacyChannel)

    val service = org.robolectric.Robolectric.buildService(com.example.service.RemoteServerService::class.java)
      .create()
      .get()
    assertNull(nm.getNotificationChannel(com.example.service.RemoteServerService.CHANNEL_ID))
    assertTrue(shadowNm.allNotifications.isEmpty())

    // Boot receiver ensures silent runtime without foreground service or notifications
    val freePort = ServerSocket(0).use { it.localPort }
    ServerStateManager.savePort(context, freePort)
    ServerStateManager.setManuallyStopped(context, false)
    val receiver = com.example.service.BootAndWatchdogReceiver()
    receiver.onReceive(context, android.content.Intent(android.content.Intent.ACTION_BOOT_COMPLETED))

    assertTrue(ServerStateManager.isServerRunning.value)
    assertNull(Shadows.shadowOf(context as Application).nextStartedService)
    assertTrue(shadowNm.allNotifications.isEmpty())

    // Persisted JobScheduler keeps backend active across reboots
    val js = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as android.app.job.JobScheduler
    val pending = js.allPendingJobs
    assertTrue(pending.any { it.id == com.example.service.KeepAliveJobService.JOB_ID_PERIODIC && it.isPersisted })
    assertTrue(pending.any { it.id == com.example.service.KeepAliveJobService.JOB_ID_NETWORK && it.isPersisted })

    // USER_UNLOCKED also guarantees backend active after device reboot + unlock
    com.example.server.ServerRuntime.stop(context)
    receiver.onReceive(context, android.content.Intent(android.content.Intent.ACTION_USER_UNLOCKED))
    var unlockedActive = false
    for (i in 0 until 20) {
      if (ServerStateManager.isServerRunning.value) {
        unlockedActive = true
        break
      }
      Thread.sleep(50)
    }
    assertTrue(unlockedActive)
    assertTrue(shadowNm.allNotifications.isEmpty())

    // Stray non-OTP notifications are canceled by cleanupNonOtpNotifications while OTP notification survives
    val strayNotif = android.app.Notification.Builder(context, "stray_ch")
      .setSmallIcon(R.drawable.ic_stat_phonedeck)
      .setContentTitle("PhoneDeck")
      .setContentText("localhost:8080")
      .build()
    nm.notify(com.example.service.RemoteServerService.NOTIFICATION_ID, strayNotif)
    OtpNotificationHelper.postOneTimeOtpNotification(context, "owner", "1234", false)
    com.example.server.ServerRuntime.cleanupNonOtpNotifications(context)
    assertNull(shadowNm.getNotification(com.example.service.RemoteServerService.NOTIFICATION_ID))
    assertNotNull(shadowNm.getNotification(OtpNotificationHelper.NOTIFICATION_ID))
    OtpNotificationHelper.cancelOtpNotification(context)

    // Non-OTP test notifications are strictly suppressed
    assertFalse(DeviceController.postTestNotification(context, "Test", "Ignored"))
    assertTrue(shadowNm.allNotifications.isEmpty())

    com.example.server.ServerRuntime.stop(context)
    service.stopSelf()
  }

  @Test
  fun `verify unique device id, honest tunnel URL gating, and anonymous SSH tunnel URL parsing`() {
    val id1 = AuthManager.getOrCreateDeviceId(context)
    val id2 = AuthManager.getOrCreateDeviceId(context)
    assertTrue(id1.startsWith("pd_"))
    assertEquals(id1, id2)

    // Never expose public URL unless tunnel status is CONNECTED
    ServerStateManager.setTunnelState(TunnelStatus.CONNECTING, "https://demo123.a.pinggy.link/")
    assertEquals("", ServerStateManager.publicUrl.value)

    ServerStateManager.setTunnelState(TunnelStatus.RECONNECTING, "https://demo123.a.pinggy.link/")
    assertEquals("", ServerStateManager.publicUrl.value)

    ServerStateManager.setTunnelState(TunnelStatus.CONNECTED, "https://demo123.a.pinggy.link/")
    assertEquals("https://demo123.a.pinggy.link/", ServerStateManager.publicUrl.value)

    // Verify anonymous SSH tunnel output parsing (Pinggy + localhost.run, ignoring dashboard/admin URLs)
    assertNull(RemoteTunnelClient.extractPublicHttpsUrl("Dashboard: https://dashboard.pinggy.io"))
    assertNull(RemoteTunnelClient.extractPublicHttpsUrl("Docs: https://admin.localhost.run/help"))
    assertEquals(
      "https://rnpdg-192-0-2-1.a.pinggy.link/",
      RemoteTunnelClient.extractPublicHttpsUrl("\u001B[32mhttps://rnpdg-192-0-2-1.a.pinggy.link\u001B[0m")
    )
    assertEquals(
      "https://ezvhg-34-96-48-79.free.pinggy.net/",
      RemoteTunnelClient.extractPublicHttpsUrl("https://ezvhg-34-96-48-79.free.pinggy.net")
    )
    assertEquals(
      "https://mfxtg-34-96-48-79.run.pinggy-free.link/",
      RemoteTunnelClient.extractPublicHttpsUrl("https://mfxtg-34-96-48-79.run.pinggy-free.link")
    )
    assertEquals(
      "https://abc9876543.lhr.life/",
      RemoteTunnelClient.extractPublicHttpsUrl("yoururl.lhr.life tunneled with tls termination, https://abc9876543.lhr.life")
    )
  }

  private fun readPostedOtpFromNotification(): String {
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val shadowNm = Shadows.shadowOf(nm)
    val notif = shadowNm.getNotification(OtpNotificationHelper.NOTIFICATION_ID)
    assertNotNull("Expected one-time OTP OS notification on device", notif)
    val text = notif!!.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
    val match = Regex("\\b(\\d{4})\\b").find(text)
    assertNotNull("Expected 4-digit OTP inside notification text: $text", match)
    return match!!.groupValues[1]
  }

  @Test
  fun `verify User+Password registration with one-time Android notification OTP, login persistence, rate limit, and recovery`() {
    assertFalse(AuthManager.isRegistered(context))
    assertFalse(AuthManager.isRequestAuthenticated(context, null, "127.0.0.1"))
    assertFalse(AuthManager.isRequestAuthenticated(context, null, "192.168.1.20"))
    assertFalse("Local requests must not create a default account", AuthManager.isRegistered(context))

    // Reject mismatched password & confirm password
    val mismatch = AuthManager.startRegistration(context, "owner_dev", "StrongPass!2026", "MismatchPass!", "127.0.0.1")
    assertFalse(mismatch.success)
    assertEquals(400, mismatch.statusCode)

    // 1. Start registration with User + Password + Confirm Password -> fires OS notification OTP
    val startRes = AuthManager.startRegistration(context, "owner_dev", "StrongPass!2026", "StrongPass!2026", "203.0.113.10")
    assertTrue(startRes.success)
    assertTrue(AuthManager.isOtpPrivacyWindowActive())
    val pendingId = startRes.data["pendingId"] as String
    val otpCode = readPostedOtpFromNotification()

    // 2. Reject wrong 4-digit OTP
    val wrongCode = if (otpCode == "0000") "9999" else "0000"
    val badVerify = AuthManager.verifyRegistrationOtp(context, pendingId, wrongCode, "203.0.113.10")
    assertFalse(badVerify.success)
    assertEquals(401, badVerify.statusCode)

    // 3. Accept real 4-digit notification OTP -> creates owner account & cancels OS notification
    val okVerify = AuthManager.verifyRegistrationOtp(context, pendingId, otpCode, "203.0.113.10")
    assertTrue(okVerify.success)
    assertTrue(AuthManager.isRegistered(context))
    assertEquals("owner_dev", AuthManager.getRegisteredUserId(context))
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    assertNull(Shadows.shadowOf(nm).getNotification(OtpNotificationHelper.NOTIFICATION_ID))

    // 4. Further initial registration is permanently disabled
    val secondReg = AuthManager.startRegistration(context, "attacker", "HackPass!2026", "HackPass!2026", "203.0.113.11")
    assertFalse(secondReg.success)
    assertEquals(403, secondReg.statusCode)

    // 5. Login with User and Password + 1-year persistent device-bound session cookie
    val loginOk = AuthManager.login(context, "owner_dev", "StrongPass!2026", "203.0.113.10")
    assertTrue(loginOk.success)
    val cookieHeader = AuthManager.buildSessionCookieHeader(context, isSecureTransport = true)
    assertTrue(cookieHeader.contains("Secure"))
    assertTrue(cookieHeader.contains("Max-Age=31536000"))
    val cookiePair = cookieHeader.substringBefore(";")
    assertTrue(AuthManager.isRequestAuthenticated(context, cookiePair))

    // 6. Device isolation: cookie bound to a different Device ID is rejected
    val parts = cookiePair.substringAfter("=").split(".")
    val foreignToken = "${parts[0]}.pd_foreign_device_999.${parts[2]}.${parts[3]}.${parts[4]}"
    assertFalse(AuthManager.isRequestAuthenticated(context, "${AuthManager.COOKIE_NAME}=$foreignToken"))

    // 7. Rate limiting on repeated bad logins
    RateLimiter.clearAll()
    repeat(5) {
      val fail = AuthManager.login(context, "owner_dev", "WrongPass!", "198.51.100.77")
      assertFalse(fail.success)
    }
    val locked = AuthManager.login(context, "owner_dev", "StrongPass!2026", "198.51.100.77")
    assertFalse(locked.success)
    assertEquals(429, locked.statusCode)

    // 8. Password change invalidates old cookie
    val (chgOk, _) = AuthManager.changePassword(context, "StrongPass!2026", "UpdatedPass!2026")
    assertTrue(chgOk)
    assertFalse(AuthManager.isRequestAuthenticated(context, cookiePair))
    assertTrue(AuthManager.login(context, "owner_dev", "UpdatedPass!2026", "203.0.113.12").success)

    // 9. Password reset via one-time notification OTP resets password & revokes old sessions
    val recStart = AuthManager.startRecovery(context, "owner_dev", "203.0.113.13")
    assertTrue(recStart.success)
    val recId = recStart.data["recoveryId"] as String
    val recCode = readPostedOtpFromNotification()
    val recVerify = AuthManager.verifyRecoveryOtp(context, recId, recCode, "203.0.113.13")
    assertTrue(recVerify.success)
    val recTicket = recVerify.data["recoveryTicket"] as String
    val recComplete = AuthManager.completeRecovery(context, recTicket, "RecoveredPass!2026")
    assertTrue(recComplete.success)
    assertTrue(AuthManager.login(context, "owner_dev", "RecoveredPass!2026", "203.0.113.14").success)
  }

  @Test
  fun `migrate only the old auto-created admin account to allow OTP signup`() {
    val salt = "legacy-admin-salt"
    val spec = PBEKeySpec("admin123".toCharArray(), salt.toByteArray(Charsets.UTF_8), 120_000, 256)
    val legacyHash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
      .generateSecret(spec).encoded.joinToString("") { "%02x".format(it) }
    SecureCredentialStore.putString(context, "auth_owner_user_id", "admin")
    SecureCredentialStore.putString(context, "auth_bound_device_id", "legacy-device")
    SecureCredentialStore.putString(context, "auth_pwd_salt", salt)
    SecureCredentialStore.putString(context, "auth_pwd_hash", legacyHash)
    SecureCredentialStore.putBoolean(context, "auth_is_registered", true)
    SecureCredentialStore.putInt(context, "auth_version", 1)

    assertFalse(AuthManager.isRegistered(context))
    assertEquals("", AuthManager.getRegisteredUserId(context))
    val signup = AuthManager.startRegistration(
      context,
      "new_owner",
      "FreshPass!2026",
      "FreshPass!2026",
      "203.0.113.44"
    )
    assertTrue(signup.success)
  }

  @Test
  fun `verify HttpBridgeServer enforces auth gates and completes desktop Sign Up OTP flow over HTTP`() {
    val freePort = ServerSocket(0).use { it.localPort }
    val bridge = HttpBridgeServer(context, freePort)
    bridge.start()

    val client = OkHttpClient.Builder()
      .connectTimeout(3, TimeUnit.SECONDS)
      .readTimeout(5, TimeUnit.SECONDS)
      .build()

    try {
      var started = false
      for (i in 0 until 20) {
        val ok = runCatching {
          client.newCall(Request.Builder().url("http://127.0.0.1:$freePort/api/auth/status").build()).execute().use {
            it.code == 200
          }
        }.getOrDefault(false)
        if (ok) {
          started = true
          break
        }
        Thread.sleep(100)
      }
      assertTrue(started)

      // Unauthenticated GET / serves AuthHtml
      client.newCall(Request.Builder().url("http://127.0.0.1:$freePort/").build()).execute().use { resp ->
        assertEquals(200, resp.code)
        val html = resp.body?.string().orEmpty()
        assertTrue(html.contains("view-register"))
        assertTrue(html.contains("Confirm Password"))
      }
      assertFalse("Opening the auth page must not create a default account", AuthManager.isRegistered(context))

      val legacyAutoLoginBody = "{}".toRequestBody("application/json".toMediaType())
      client.newCall(
        Request.Builder()
          .url("http://127.0.0.1:$freePort/api/auth/auto-login")
          .post(legacyAutoLoginBody)
          .build()
      ).execute().use { resp ->
        assertEquals(403, resp.code)
      }
      assertFalse("OTP-free auto-login must not create an account", AuthManager.isRegistered(context))

      // Unauthenticated GET /api/status is rejected with 401
      client.newCall(Request.Builder().url("http://127.0.0.1:$freePort/api/status").build()).execute().use { resp ->
        assertEquals(401, resp.code)
      }

      // Unauthenticated POST /mcp is rejected with 403 while unregistered
      val mcpBody = """{"jsonrpc":"2.0","id":1,"method":"ping"}""".toRequestBody("application/json".toMediaType())
      client.newCall(Request.Builder().url("http://127.0.0.1:$freePort/mcp").post(mcpBody).build()).execute().use { resp ->
        assertEquals(403, resp.code)
      }

      // HTTP Step 1: POST /api/auth/register/start with User + Password + Confirm Password
      val startBody = """{"userId":"bridge_owner","password":"BridgePass!99","confirmPassword":"BridgePass!99"}"""
        .toRequestBody("application/json".toMediaType())
      var pendingId = ""
      client.newCall(Request.Builder().url("http://127.0.0.1:$freePort/api/auth/register/start").post(startBody).build()).execute().use { resp ->
        assertEquals(200, resp.code)
        val json = JSONObject(resp.body?.string().orEmpty())
        assertTrue(json.getBoolean("success"))
        pendingId = json.getString("pendingId")
      }

      // Read 4-digit OTP from Android notification shade & submit Step 2 -> receives persistent cookie immediately
      val otp = readPostedOtpFromNotification()
      val verifyBody = """{"pendingId":"$pendingId","code":"$otp"}""".toRequestBody("application/json".toMediaType())
      var regCookie = ""
      client.newCall(Request.Builder().url("http://127.0.0.1:$freePort/api/auth/register/verify-otp").post(verifyBody).build()).execute().use { resp ->
        assertEquals(200, resp.code)
        regCookie = resp.header("Set-Cookie")?.substringBefore(";").orEmpty()
        assertTrue(regCookie.startsWith("${AuthManager.COOKIE_NAME}="))
      }

      // Authenticated GET /api/status succeeds immediately with the registration cookie
      client.newCall(
        Request.Builder()
          .url("http://127.0.0.1:$freePort/api/status")
          .header("Cookie", regCookie)
          .build()
      ).execute().use { resp ->
        assertEquals(200, resp.code)
        val obj = JSONObject(resp.body?.string().orEmpty())
        assertTrue(obj.has("batteryLevel"))
      }

      // HTTP POST /api/auth/login on another browser issues persistent cookie
      val loginJson = """{"userId":"bridge_owner","password":"BridgePass!99"}""".toRequestBody("application/json".toMediaType())
      client.newCall(Request.Builder().url("http://127.0.0.1:$freePort/api/auth/login").post(loginJson).build()).execute().use { resp ->
        assertEquals(200, resp.code)
        val rawSetCookie = resp.header("Set-Cookie").orEmpty()
        assertTrue(rawSetCookie.contains("Max-Age=31536000"))
      }

      // POST /api/auth/register/start is permanently blocked (403) after registration
      client.newCall(Request.Builder().url("http://127.0.0.1:$freePort/api/auth/register/start").post(startBody).build()).execute().use { resp ->
        assertEquals(403, resp.code)
      }
    } finally {
      bridge.stop()
    }
  }

  @Test
  fun `verify mcp manager token, category controls, and json-rpc engine dispatch`() {
    val token = com.example.data.McpManager.getOrGenerateToken(context)
    assertTrue(token.startsWith("pd_mcp_"))
    assertTrue(com.example.data.McpManager.verifyToken(context, token))
    assertTrue(com.example.data.McpManager.verifyToken(context, "Bearer $token"))
    assertFalse(com.example.data.McpManager.verifyToken(context, "invalid_token"))
    assertFalse(com.example.data.McpManager.verifyToken(context, null))

    val newToken = com.example.data.McpManager.regenerateToken(context)
    assertTrue(newToken.startsWith("pd_mcp_"))
    assertTrue(com.example.data.McpManager.verifyToken(context, newToken))
    assertFalse(com.example.data.McpManager.verifyToken(context, token))
    assertNotEquals(token, newToken)

    assertTrue(com.example.data.McpManager.isCategoryEnabled(context, com.example.data.McpManager.CAT_DEVICE))
    com.example.data.McpManager.setCategoryEnabled(context, com.example.data.McpManager.CAT_DEVICE, false)
    assertFalse(com.example.data.McpManager.isCategoryEnabled(context, com.example.data.McpManager.CAT_DEVICE))
    com.example.data.McpManager.setCategoryEnabled(context, com.example.data.McpManager.CAT_DEVICE, true)
    assertTrue(com.example.data.McpManager.isCategoryEnabled(context, com.example.data.McpManager.CAT_DEVICE))

    com.example.data.McpManager.setCloudTunnelUrl(context, "https://agent.tunnel.example.com")
    assertEquals("https://agent.tunnel.example.com", com.example.data.McpManager.getCloudTunnelUrl(context))

    kotlinx.coroutines.runBlocking {
      val pingReq = """{"jsonrpc":"2.0","id":1,"method":"ping"}"""
      val pingRes = com.example.mcp.McpEngine.processJsonRpc(context, pingReq)
      val pingJson = JSONObject(pingRes)
      assertEquals(1, pingJson.getInt("id"))
      assertNotNull(pingJson.optJSONObject("result"))

      val initReq = """{"jsonrpc":"2.0","id":2,"method":"initialize","params":{"protocolVersion":"2024-11-05"}}"""
      val initRes = com.example.mcp.McpEngine.processJsonRpc(context, initReq)
      val initJson = JSONObject(initRes)
      val result = initJson.getJSONObject("result")
      assertEquals("2024-11-05", result.getString("protocolVersion"))
      assertEquals("PhoneDeck MCP Server", result.getJSONObject("serverInfo").getString("name"))

      val listReq = """{"jsonrpc":"2.0","id":3,"method":"tools/list"}"""
      val listRes = com.example.mcp.McpEngine.processJsonRpc(context, listReq)
      val listJson = JSONObject(listRes)
      val toolsArr = listJson.getJSONObject("result").getJSONArray("tools")
      assertTrue(toolsArr.length() >= 20)

      val callReq = """{"jsonrpc":"2.0","id":4,"method":"tools/call","params":{"name":"get_device_status","arguments":{}}}"""
      val callRes = com.example.mcp.McpEngine.processJsonRpc(context, callReq)
      val callJson = JSONObject(callRes)
      val callResult = callJson.getJSONObject("result")
      assertFalse(callResult.optBoolean("isError", false))
      val content = callResult.getJSONArray("content").getJSONObject(0).getString("text")
      assertTrue(content.contains("batteryLevel"))
    }
  }

  @Test
  fun `verify new file editor, Range 206 download, stateful shell cwd, media key, and screen input HTTP APIs`() {
    val freePort = ServerSocket(0).use { it.localPort }
    val bridge = HttpBridgeServer(context, freePort)
    bridge.start()

    val client = OkHttpClient.Builder()
      .connectTimeout(3, TimeUnit.SECONDS)
      .readTimeout(5, TimeUnit.SECONDS)
      .build()

    try {
      var started = false
      for (i in 0 until 20) {
        val ok = runCatching {
          client.newCall(Request.Builder().url("http://127.0.0.1:$freePort/api/auth/status").build()).execute().use {
            it.code == 200
          }
        }.getOrDefault(false)
        if (ok) {
          started = true
          break
        }
        Thread.sleep(100)
      }
      assertTrue(started)

      // Complete owner registration to obtain session cookie
      val startBody = """{"userId":"test_owner","password":"TestPass!99","confirmPassword":"TestPass!99"}"""
        .toRequestBody("application/json".toMediaType())
      var pendingId = ""
      client.newCall(Request.Builder().url("http://127.0.0.1:$freePort/api/auth/register/start").post(startBody).build()).execute().use { resp ->
        pendingId = JSONObject(resp.body?.string().orEmpty()).getString("pendingId")
      }
      val otp = readPostedOtpFromNotification()
      val verifyBody = """{"pendingId":"$pendingId","code":"$otp"}""".toRequestBody("application/json".toMediaType())
      var cookie = ""
      client.newCall(Request.Builder().url("http://127.0.0.1:$freePort/api/auth/register/verify-otp").post(verifyBody).build()).execute().use { resp ->
        cookie = resp.header("Set-Cookie")?.substringBefore(";").orEmpty()
      }

      // 1. Test /api/files/write, /api/files/read, /api/download HTTP 206 Range, and /api/files/rename
      val tempDir = java.io.File(context.filesDir, "editor_test").apply { mkdirs() }
      val targetFile = java.io.File(tempDir, "notes.txt")
      targetFile.writeText("hello")

      val writeJson = JSONObject().put("path", targetFile.absolutePath).put("content", "PhoneDeck-Range-Content").toString()
      client.newCall(
        Request.Builder()
          .url("http://127.0.0.1:$freePort/api/files/write")
          .header("Cookie", cookie)
          .post(writeJson.toRequestBody("application/json".toMediaType()))
          .build()
      ).execute().use { resp ->
        assertEquals(200, resp.code)
        assertTrue(JSONObject(resp.body?.string().orEmpty()).getBoolean("success"))
      }

      val encPath = java.net.URLEncoder.encode(targetFile.absolutePath, "UTF-8")
      client.newCall(
        Request.Builder()
          .url("http://127.0.0.1:$freePort/api/files/read?path=$encPath")
          .header("Cookie", cookie)
          .build()
      ).execute().use { resp ->
        assertEquals(200, resp.code)
        val j = JSONObject(resp.body?.string().orEmpty())
        assertTrue(j.getBoolean("success"))
        assertEquals("PhoneDeck-Range-Content", j.getString("content"))
      }

      client.newCall(
        Request.Builder()
          .url("http://127.0.0.1:$freePort/api/download?inline=1&path=$encPath")
          .header("Cookie", cookie)
          .header("Range", "bytes=0-8")
          .build()
      ).execute().use { resp ->
        assertEquals(206, resp.code)
        assertEquals("bytes", resp.header("Accept-Ranges"))
        assertEquals("PhoneDeck", resp.body?.string())
      }

      val renameJson = JSONObject().put("path", targetFile.absolutePath).put("newName", "renamed.txt").toString()
      client.newCall(
        Request.Builder()
          .url("http://127.0.0.1:$freePort/api/files/rename")
          .header("Cookie", cookie)
          .post(renameJson.toRequestBody("application/json".toMediaType()))
          .build()
      ).execute().use { resp ->
        assertEquals(200, resp.code)
        assertTrue(JSONObject(resp.body?.string().orEmpty()).getBoolean("success"))
      }
      assertTrue(java.io.File(tempDir, "renamed.txt").exists())

      // 2. Test /api/shell stateful cwd tracking
      val shellBody = JSONObject().put("command", "pwd").put("cwd", tempDir.absolutePath).toString()
      client.newCall(
        Request.Builder()
          .url("http://127.0.0.1:$freePort/api/shell")
          .header("Cookie", cookie)
          .post(shellBody.toRequestBody("application/json".toMediaType()))
          .build()
      ).execute().use { resp ->
        assertEquals(200, resp.code)
        val j = JSONObject(resp.body?.string().orEmpty())
        assertTrue(j.getString("cwd").contains("editor_test"))
      }

      // 3. Test /api/action media_key & OTP-only notification suppression
      val mediaJson = """{"action":"media_key","key":"play_pause"}""".toRequestBody("application/json".toMediaType())
      client.newCall(
        Request.Builder().url("http://127.0.0.1:$freePort/api/action").header("Cookie", cookie).post(mediaJson).build()
      ).execute().use { resp ->
        assertEquals(200, resp.code)
        assertTrue(JSONObject(resp.body?.string().orEmpty()).getBoolean("success"))
      }

      val blockedNotif = """{"action":"show_notification","title":"Hi","message":"Test"}""".toRequestBody("application/json".toMediaType())
      client.newCall(
        Request.Builder().url("http://127.0.0.1:$freePort/api/action").header("Cookie", cookie).post(blockedNotif).build()
      ).execute().use { resp ->
        assertEquals(200, resp.code)
        assertFalse(JSONObject(resp.body?.string().orEmpty()).getBoolean("success"))
      }

      // 4. Test /api/screen/status and /api/screen/input
      client.newCall(
        Request.Builder().url("http://127.0.0.1:$freePort/api/screen/status").header("Cookie", cookie).build()
      ).execute().use { resp ->
        assertEquals(200, resp.code)
        assertTrue(JSONObject(resp.body?.string().orEmpty()).has("accessibilityEnabled"))
      }
    } finally {
      bridge.stop()
    }
  }
}
