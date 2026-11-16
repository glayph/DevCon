package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.controller.PermissionHelper
import com.example.data.TunnelStatus
import com.example.model.PermissionItemState
import com.example.ui.theme.DashAccent
import com.example.ui.theme.DashAmber
import com.example.ui.theme.DashBgPage
import com.example.ui.theme.DashBorder
import com.example.ui.theme.DashEmerald
import com.example.ui.theme.DashInk
import com.example.ui.theme.DashInkMuted
import com.example.ui.theme.DashInkSecondary
import com.example.ui.theme.DashRose
import com.example.ui.theme.DashSubtle
import com.example.ui.theme.DashSurface
import com.example.ui.theme.PureWhite
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneDeckScreen(
    viewModel: MainViewModel,
    onRequestPermissionItem: (PermissionItemState) -> Unit
) {
    val context = LocalContext.current
    val isRunning by viewModel.isServerRunning.collectAsStateWithLifecycle()
    val port by viewModel.serverPort.collectAsStateWithLifecycle()
    val ip by viewModel.serverIp.collectAsStateWithLifecycle()
    val permissionStates by viewModel.permissionStates.collectAsStateWithLifecycle()

    val tunnelStatus by viewModel.tunnelStatus.collectAsStateWithLifecycle()
    val publicUrl by viewModel.publicUrl.collectAsStateWithLifecycle()
    val tunnelError by viewModel.tunnelError.collectAsStateWithLifecycle()
    val isRegistered by viewModel.isRegistered.collectAsStateWithLifecycle()
    val registeredUserId by viewModel.registeredUserId.collectAsStateWithLifecycle()

    var showInAppConsole by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.ensureServiceActive(context)
        viewModel.refreshPermissions(context)
    }

    val ungrantedPermissions = permissionStates.filter { !it.isGranted }
    val localhostUrl = "http://localhost:$port"
    val lanUrl = "http://$ip:$port"
    val adbForwardCmd = "adb forward tcp:$port tcp:$port"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isRunning) DashEmerald else DashRose)
                        )
                        Column {
                            Text(
                                "PhoneDeck",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = DashInk
                                )
                            )
                            Text(
                                text = "24/7 Background Service Active",
                                fontSize = 11.sp,
                                color = DashEmerald,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = DashSubtle,
                        border = BorderStroke(1.dp, DashBorder),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .testTag("always_active_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (isRunning) DashEmerald else DashAmber)
                            )
                            Text(
                                text = if (isRunning) "ONLINE" else "STARTING",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp,
                                color = if (isRunning) DashEmerald else DashAmber
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DashBgPage,
                    titleContentColor = DashInk
                )
            )
        },
        containerColor = DashBgPage
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Localhost & LAN Dashboard Card (PRIMARY)
                item(key = "localhost_card") {
                    LocalhostCard(
                        isRunning = isRunning,
                        port = port,
                        localhostUrl = localhostUrl,
                        lanUrl = lanUrl,
                        adbCommand = adbForwardCmd,
                        onCopyLocalhost = { copyText(context, "Localhost URL", localhostUrl) },
                        onOpenLocalhost = { openUrl(context, localhostUrl) },
                        onOpenInAppConsole = { showInAppConsole = true },
                        onCopyLan = { copyText(context, "LAN URL", lanUrl) },
                        onCopyAdb = { copyText(context, "ADB Command", adbForwardCmd) }
                    )
                }

                // Public Tunnel Card
                item(key = "remote_access_card") {
                    RemoteAccessCard(
                        isServerRunning = isRunning,
                        tunnelStatus = tunnelStatus,
                        publicUrl = publicUrl,
                        tunnelError = tunnelError,
                        isRegistered = isRegistered,
                        registeredUserId = registeredUserId,
                        onCopyPublicUrl = {
                            if (publicUrl.isNotBlank()) copyText(context, "Public URL", publicUrl)
                        },
                        onOpenPublicUrl = {
                            if (publicUrl.isNotBlank()) {
                                openUrl(context, publicUrl)
                            }
                        },
                        onRetryTunnel = {
                            viewModel.reconnectTunnel(context)
                        }
                    )
                }

                if (ungrantedPermissions.isNotEmpty()) {
                    item(key = "permissions_spacer") {
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                    items(ungrantedPermissions, key = { it.id }) { item ->
                        PermissionActionCard(
                            icon = iconForPermission(item.id),
                            title = item.title,
                            testTag = "perm_${item.id}_btn",
                            onClick = { onRequestPermissionItem(item) }
                        )
                    }
                }
            }
        }
    }

    // Fullscreen In-App Web Console Modal
    if (showInAppConsole) {
        Dialog(
            onDismissRequest = { showInAppConsole = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = DashBgPage
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DashSurface)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "PhoneDeck In-App Web Console",
                                fontWeight = FontWeight.Bold,
                                color = DashInk,
                                fontSize = 15.sp
                            )
                            Text(
                                localhostUrl,
                                fontSize = 12.sp,
                                color = DashAccent,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        IconButton(onClick = { showInAppConsole = false }) {
                            Icon(
                                PhoneDeckIcons.Close,
                                contentDescription = "Close",
                                tint = DashInkSecondary
                            )
                        }
                    }
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.databaseEnabled = true
                                settings.useWideViewPort = true
                                settings.loadWithOverviewMode = true
                                webViewClient = WebViewClient()
                                loadUrl(localhostUrl)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LocalhostCard(
    isRunning: Boolean,
    port: Int,
    localhostUrl: String,
    lanUrl: String,
    adbCommand: String,
    onCopyLocalhost: () -> Unit,
    onOpenLocalhost: () -> Unit,
    onOpenInAppConsole: () -> Unit,
    onCopyLan: () -> Unit,
    onCopyAdb: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("localhost_address_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DashSurface),
        border = BorderStroke(1.dp, DashBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "DESKTOP & LOCALHOST",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = DashInkMuted
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = DashEmerald.copy(alpha = 0.12f)
                    ) {
                        Text(
                            "24/7 BACKEND",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = DashEmerald,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    if (isRunning) "ONLINE" else "OFFLINE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isRunning) DashEmerald else DashRose
                )
            }

            // Localhost URL row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DashSubtle)
                    .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Local URL",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = DashInkMuted
                    )
                    Text(
                        localhostUrl,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DashInk,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row {
                    IconButton(
                        onClick = onCopyLocalhost,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("copy_localhost_button")
                    ) {
                        Icon(
                            PhoneDeckIcons.ContentCopy,
                            contentDescription = "Copy Localhost URL",
                            tint = DashInkSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onOpenLocalhost,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("open_localhost_button")
                    ) {
                        Icon(
                            PhoneDeckIcons.OpenInBrowser,
                            contentDescription = "Open Localhost URL",
                            tint = DashAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Quick In-App Console button
            Button(
                onClick = onOpenInAppConsole,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DashAccent),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    PhoneDeckIcons.OpenInBrowser,
                    contentDescription = null,
                    tint = PureWhite,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Open In-App Web Console",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PureWhite
                )
            }

            // ADB Forward command helper
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DashSubtle)
                    .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Desktop ADB Forward Command (Run in PC Terminal)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = DashInkMuted
                    )
                    Text(
                        adbCommand,
                        fontSize = 12.5.sp,
                        color = DashInkSecondary,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = onCopyAdb,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        PhoneDeckIcons.ContentCopy,
                        contentDescription = "Copy ADB Command",
                        tint = DashInkSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // LAN Wi-Fi URL row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DashSubtle)
                    .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Wi-Fi LAN URL (For PCs on Same Network)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = DashInkMuted
                    )
                    Text(
                        lanUrl,
                        fontSize = 12.5.sp,
                        color = DashInkSecondary,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = onCopyLan,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("copy_lan_button")
                ) {
                    Icon(
                        PhoneDeckIcons.ContentCopy,
                        contentDescription = "Copy LAN URL",
                        tint = DashInkSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RemoteAccessCard(
    isServerRunning: Boolean,
    tunnelStatus: TunnelStatus,
    publicUrl: String,
    tunnelError: String,
    isRegistered: Boolean,
    registeredUserId: String,
    onCopyPublicUrl: () -> Unit,
    onOpenPublicUrl: () -> Unit,
    onRetryTunnel: () -> Unit
) {
    val (statusLabel, statusColor) = when {
        !isServerRunning -> "OFFLINE" to DashRose
        tunnelStatus == TunnelStatus.CONNECTED -> "ONLINE" to DashEmerald
        tunnelStatus == TunnelStatus.CONNECTING -> "CONNECTING" to DashAmber
        tunnelStatus == TunnelStatus.RECONNECTING -> "STANDBY" to DashAmber
        else -> "STANDBY" to DashInkMuted
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("remote_access_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DashSurface),
        border = BorderStroke(1.dp, DashBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "PUBLIC TUNNEL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = DashInkMuted
                    )
                    if (isRegistered && registeredUserId.isNotBlank()) {
                        Text(
                            "@$registeredUserId",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = DashInkSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Text(
                    statusLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor,
                    modifier = Modifier.testTag("remote_status_label")
                )
            }

            // The URL belongs to the persistent device identity, not to the current
            // WebSocket session. Keep showing/copying it while the phone reconnects.
            if (publicUrl.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DashSubtle)
                        .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        publicUrl,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DashAccent,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("remote_public_url")
                    )
                    Row {
                        IconButton(
                            onClick = onCopyPublicUrl,
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("copy_public_url_button")
                        ) {
                            Icon(
                                PhoneDeckIcons.ContentCopy,
                                contentDescription = "Copy Public URL",
                                tint = DashInkSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onOpenPublicUrl,
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("open_public_url_button")
                        ) {
                            Icon(
                                PhoneDeckIcons.OpenInBrowser,
                                contentDescription = "Open Public URL",
                                tint = DashAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            if (!isServerRunning || tunnelStatus != TunnelStatus.CONNECTED) {
                val statusText = when {
                    !isServerRunning -> "Public URL is reserved; the phone server is offline"
                    tunnelError.isNotBlank() -> tunnelError
                    tunnelStatus == TunnelStatus.CONNECTING -> "Connecting to the public relay"
                    tunnelStatus == TunnelStatus.RECONNECTING -> "Connection interrupted; URL unchanged, reconnecting"
                    else -> "Localhost & LAN active (Public relay on standby)"
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DashSubtle)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = statusText,
                        fontSize = 12.5.sp,
                        color = DashInkSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("remote_offline_notice")
                    )
                    OutlinedButton(
                        onClick = onRetryTunnel,
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            PhoneDeckIcons.Refresh,
                            contentDescription = "Retry",
                            tint = DashAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Retry", fontSize = 11.sp, color = DashAccent)
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionActionCard(
    icon: ImageVector,
    title: String,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = DashSurface),
        border = BorderStroke(1.dp, DashBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = DashInkSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = DashInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Button(
                onClick = onClick,
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DashSubtle,
                    contentColor = DashInk
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag(testTag)
            ) {
                Text(
                    text = "Grant",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private fun iconForPermission(id: String): ImageVector {
    return when (id) {
        PermissionHelper.ID_ALL_FILES -> PhoneDeckIcons.Folder
        PermissionHelper.ID_MEDIA -> PhoneDeckIcons.PermMedia
        PermissionHelper.ID_CONTACTS -> PhoneDeckIcons.Contacts
        PermissionHelper.ID_SMS -> PhoneDeckIcons.Sms
        PermissionHelper.ID_CALLS_PHONE -> PhoneDeckIcons.Call
        PermissionHelper.ID_CALENDAR -> PhoneDeckIcons.CalendarToday
        PermissionHelper.ID_LOCATION -> PhoneDeckIcons.LocationOn
        PermissionHelper.ID_CAMERA -> PhoneDeckIcons.CameraAlt
        PermissionHelper.ID_MICROPHONE -> PhoneDeckIcons.Mic
        PermissionHelper.ID_BLUETOOTH -> PhoneDeckIcons.Bluetooth
        PermissionHelper.ID_NOTIFICATION_LISTENER,
        PermissionHelper.ID_POST_NOTIFICATIONS -> PhoneDeckIcons.NotificationsActive
        PermissionHelper.ID_BATTERY_OPTIMIZATION -> PhoneDeckIcons.BatteryChargingFull
        PermissionHelper.ID_OVERLAY -> PhoneDeckIcons.Layers
        PermissionHelper.ID_WRITE_SETTINGS -> PhoneDeckIcons.Settings
        PermissionHelper.ID_USAGE_STATS -> PhoneDeckIcons.QueryStats
        PermissionHelper.ID_INSTALL_PACKAGES -> PhoneDeckIcons.SystemUpdateAlt
        PermissionHelper.ID_ACCESSIBILITY -> PhoneDeckIcons.AccessibilityNew
        PermissionHelper.ID_SCREEN_CAST -> PhoneDeckIcons.Cast
        else -> PhoneDeckIcons.Settings
    }
}

private fun openUrl(context: Context, url: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

private fun copyText(context: Context, label: String, text: String) {
    val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cb.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
}
