package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.controller.ContentDataProvider
import com.example.controller.DeviceController
import com.example.controller.PermissionHelper
import com.example.controller.ScreenCastManager
import com.example.data.AuthManager
import com.example.data.CommandLogRepository
import com.example.data.ServerStateManager
import com.example.server.AuthHtml
import com.example.server.DashboardHtml
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `verify app name matches PhoneDeck`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("PhoneDeck", appName)
  }

  @Test
  fun `verify device status telemetry reading`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val status = DeviceController.getDeviceStatus(context)
    assertNotNull(status)
    assertTrue(status.batteryLevel >= 0)
    assertNotNull(status.localIp)
  }

  @Test
  fun `verify all permissions state list includes cast and app installer`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val states = PermissionHelper.getAllPermissionStates(context)
    assertTrue(states.size >= 18)
    assertTrue(states.any { it.id == PermissionHelper.ID_ALL_FILES })
    assertTrue(states.any { it.id == PermissionHelper.ID_CONTACTS })
    assertTrue(states.any { it.id == PermissionHelper.ID_SMS })
    assertTrue(states.any { it.id == PermissionHelper.ID_LOCATION })
    assertTrue(states.any { it.id == PermissionHelper.ID_SCREEN_CAST })
    assertTrue(states.any { it.id == PermissionHelper.ID_INSTALL_PACKAGES })
    assertTrue(states.any { it.id == PermissionHelper.ID_ACCESSIBILITY })
  }

  @Test
  fun `verify web dashboard contains screen cast, upload dropzone, and app installer`() {
    val html = DashboardHtml.get()
    assertTrue(html.contains("sidebar-nav"))
    assertTrue(html.contains("page-screen"))
    assertTrue(html.contains("/api/screen/stream"))
    assertTrue(html.contains("file-dropzone"))
    assertTrue(html.contains("apk-dropzone"))
    assertTrue(html.contains("/api/apps/upload-install"))
    assertTrue(html.contains("/api/apps/uninstall"))
    assertTrue(html.contains("page-contacts"))
    assertTrue(html.contains("page-files"))
  }

  @Test
  fun `verify screen cast config clamping and persistence flag`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    ScreenCastManager.updateConfig(fps = 99, quality = 5, scale = 200)
    assertEquals(20, ScreenCastManager.targetFps.get())
    assertEquals(20, ScreenCastManager.jpegQuality.get())
    assertEquals(100, ScreenCastManager.scalePercent.get())

    ServerStateManager.setManuallyStopped(context, true)
    assertTrue(ServerStateManager.isManuallyStopped(context))
    ServerStateManager.setManuallyStopped(context, false)
    assertFalse(ServerStateManager.isManuallyStopped(context))
  }

  @Test
  fun `verify file browse result returns safely`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
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
  fun `verify silent foreground notification channel and boot receiver`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val service = org.robolectric.Robolectric.buildService(com.example.service.RemoteServerService::class.java)
      .create()
      .get()
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
    val channel = nm.getNotificationChannel(com.example.service.RemoteServerService.CHANNEL_ID)
    assertNotNull(channel)
    assertEquals(android.app.NotificationManager.IMPORTANCE_MIN, channel.importance)
    assertFalse(channel.canShowBadge())

    val receiver = com.example.service.BootAndWatchdogReceiver()
    com.example.data.ServerStateManager.setManuallyStopped(context, false)
    receiver.onReceive(context, android.content.Intent(android.content.Intent.ACTION_BOOT_COMPLETED))
    val started = org.robolectric.Shadows.shadowOf(context as android.app.Application).nextStartedService
    assertNotNull(started)
    assertEquals(com.example.service.RemoteServerService.ACTION_START, started.action)
    service.stopSelf()
  }

  @Test
  fun `verify localhost auth default admin, persistent cookie, and password change`() {
    val context = ApplicationProvider.getApplicationContext<Context>()

    assertTrue(AuthManager.verifyPassword(context, "admin"))
    assertFalse(AuthManager.verifyPassword(context, "wrongpass"))
    assertTrue(AuthManager.isUsingDefaultPassword(context))

    val oldCookieHeader = AuthManager.buildSessionCookieHeader(context)
    val oldCookieValue = oldCookieHeader.substringBefore(";")
    assertTrue(AuthManager.isRequestAuthenticated(context, oldCookieValue))
    assertFalse(AuthManager.isRequestAuthenticated(context, "${AuthManager.COOKIE_NAME}=tampered.123.abc"))
    assertFalse(AuthManager.isRequestAuthenticated(context, null))

    val (badOk, _) = AuthManager.changePassword(context, "wrong", "newPass123")
    assertFalse(badOk)

    val (ok, _) = AuthManager.changePassword(context, "admin", "newPass123")
    assertTrue(ok)
    assertFalse(AuthManager.isUsingDefaultPassword(context))
    assertFalse(AuthManager.verifyPassword(context, "admin"))
    assertTrue(AuthManager.verifyPassword(context, "newPass123"))

    // Old session cookie is invalidated on password change; new cookie succeeds
    assertFalse(AuthManager.isRequestAuthenticated(context, oldCookieValue))
    val newCookieValue = AuthManager.buildSessionCookieHeader(context).substringBefore(";")
    assertTrue(AuthManager.isRequestAuthenticated(context, newCookieValue))

    val loginHtml = AuthHtml.get()
    assertTrue(loginHtml.contains("/api/auth/login"))
    assertFalse(loginHtml.contains("admin"))

    val dashHtml = DashboardHtml.get()
    assertTrue(dashHtml.contains("page-settings"))
    assertTrue(dashHtml.contains("/api/auth/change-password"))
    assertTrue(dashHtml.contains("/api/auth/logout"))
  }

  @Test
  fun `verify mcp manager token, category controls, and json-rpc engine dispatch`() {
    val context = ApplicationProvider.getApplicationContext<Context>()

    // Token generation & verification
    val token = com.example.data.McpManager.getOrGenerateToken(context)
    assertTrue(token.startsWith("pd_mcp_"))
    assertTrue(com.example.data.McpManager.verifyToken(context, token))
    assertTrue(com.example.data.McpManager.verifyToken(context, "Bearer $token"))
    assertFalse(com.example.data.McpManager.verifyToken(context, "invalid_token"))
    assertFalse(com.example.data.McpManager.verifyToken(context, null))

    // Token regeneration
    val newToken = com.example.data.McpManager.regenerateToken(context)
    assertTrue(newToken.startsWith("pd_mcp_"))
    assertTrue(com.example.data.McpManager.verifyToken(context, newToken))
    assertFalse(com.example.data.McpManager.verifyToken(context, token))

    // Categories
    assertTrue(com.example.data.McpManager.isCategoryEnabled(context, com.example.data.McpManager.CAT_DEVICE))
    com.example.data.McpManager.setCategoryEnabled(context, com.example.data.McpManager.CAT_DEVICE, false)
    assertFalse(com.example.data.McpManager.isCategoryEnabled(context, com.example.data.McpManager.CAT_DEVICE))
    com.example.data.McpManager.setCategoryEnabled(context, com.example.data.McpManager.CAT_DEVICE, true)
    assertTrue(com.example.data.McpManager.isCategoryEnabled(context, com.example.data.McpManager.CAT_DEVICE))

    // Cloud tunnel URL
    com.example.data.McpManager.setCloudTunnelUrl(context, "https://agent.tunnel.example.com")
    assertEquals("https://agent.tunnel.example.com", com.example.data.McpManager.getCloudTunnelUrl(context))

    // JSON-RPC Ping, Initialize, Tools List, and Tools Call in runBlocking
    kotlinx.coroutines.runBlocking {
      val pingReq = """{"jsonrpc":"2.0","id":1,"method":"ping"}"""
      val pingRes = com.example.mcp.McpEngine.processJsonRpc(context, pingReq)
      val pingJson = org.json.JSONObject(pingRes)
      assertEquals(1, pingJson.getInt("id"))
      assertNotNull(pingJson.optJSONObject("result"))

      // JSON-RPC Initialize
      val initReq = """{"jsonrpc":"2.0","id":2,"method":"initialize","params":{"protocolVersion":"2024-11-05"}}"""
      val initRes = com.example.mcp.McpEngine.processJsonRpc(context, initReq)
      val initJson = org.json.JSONObject(initRes)
      val result = initJson.getJSONObject("result")
      assertEquals("2024-11-05", result.getString("protocolVersion"))
      assertEquals("PhoneDeck MCP Server", result.getJSONObject("serverInfo").getString("name"))

      // JSON-RPC Tools List
      val listReq = """{"jsonrpc":"2.0","id":3,"method":"tools/list"}"""
      val listRes = com.example.mcp.McpEngine.processJsonRpc(context, listReq)
      val listJson = org.json.JSONObject(listRes)
      val toolsArr = listJson.getJSONObject("result").getJSONArray("tools")
      assertTrue(toolsArr.length() >= 20)

      // JSON-RPC Tool Call - get_device_status
      val callReq = """{"jsonrpc":"2.0","id":4,"method":"tools/call","params":{"name":"get_device_status","arguments":{}}}"""
      val callRes = com.example.mcp.McpEngine.processJsonRpc(context, callReq)
      val callJson = org.json.JSONObject(callRes)
      val callResult = callJson.getJSONObject("result")
      assertFalse(callResult.optBoolean("isError", false))
      val content = callResult.getJSONArray("content").getJSONObject(0).getString("text")
      assertTrue(content.contains("batteryLevel"))
    }

    // Web UI Dashboard integration
    val dashHtml = DashboardHtml.get()
    assertTrue(dashHtml.contains("page-mcp"))
    assertTrue(dashHtml.contains("/api/mcp/config"))
    assertTrue(dashHtml.contains("/api/mcp/regenerate-token"))
    assertTrue(dashHtml.contains("/api/mcp/tools"))
    assertTrue(dashHtml.contains("/api/mcp/test"))
  }
}
