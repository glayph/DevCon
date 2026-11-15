package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import com.example.ui.PhoneDeckIcons
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.controller.PermissionHelper
import com.example.model.PermissionItemState
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentRose
import com.example.ui.theme.PureWhite
import com.example.ui.theme.Zinc100
import com.example.ui.theme.Zinc400
import com.example.ui.theme.Zinc500
import com.example.ui.theme.Zinc800
import com.example.ui.theme.Zinc850
import com.example.ui.theme.Zinc900
import com.example.ui.theme.Zinc950
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

    LaunchedEffect(Unit) {
        viewModel.refreshPermissions(context)
    }

    val ungrantedPermissions = permissionStates.filter { !it.isGranted }
    val localhostUrl = "http://localhost:$port"
    val lanUrl = "http://$ip:$port"
    val adbCmd = "adb reverse tcp:$port tcp:$port"

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
                                .background(if (isRunning) AccentEmerald else AccentRose)
                        )
                        Text(
                            "PhoneDeck",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Zinc100
                            )
                        )
                    }
                },
                actions = {
                    Switch(
                        checked = isRunning,
                        onCheckedChange = { viewModel.toggleServer(context) },
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .testTag("server_master_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PureWhite,
                            checkedTrackColor = AccentCyan,
                            uncheckedThumbColor = Zinc500,
                            uncheckedTrackColor = Zinc800
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Zinc950,
                    titleContentColor = Zinc100
                )
            )
        },
        containerColor = Zinc950
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(key = "localhost_card") {
                    LocalhostAddressCard(
                        isRunning = isRunning,
                        localhostUrl = localhostUrl,
                        lanUrl = lanUrl,
                        adbCmd = adbCmd,
                        onCopyLocalhost = { copyText(context, "Localhost URL", localhostUrl) },
                        onOpenLocalhost = {
                            runCatching {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(localhostUrl)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            }
                        },
                        onCopyAdb = { copyText(context, "ADB Command", adbCmd) },
                        onCopyLan = { copyText(context, "LAN URL", lanUrl) }
                    )
                }

                if (ungrantedPermissions.isNotEmpty()) {
                    item(key = "permissions_spacer") {
                        Spacer(modifier = Modifier.height(4.dp))
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
}

@Composable
private fun LocalhostAddressCard(
    isRunning: Boolean,
    localhostUrl: String,
    lanUrl: String,
    adbCmd: String,
    onCopyLocalhost: () -> Unit,
    onOpenLocalhost: () -> Unit,
    onCopyAdb: () -> Unit,
    onCopyLan: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("localhost_address_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Zinc900),
        border = BorderStroke(1.dp, Zinc800)
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
                Text(
                    "LOCALHOST",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = Zinc400
                )
                Text(
                    if (isRunning) "ONLINE" else "OFFLINE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isRunning) AccentEmerald else AccentRose
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Zinc950)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    localhostUrl,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AccentCyan,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onCopyLocalhost,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("copy_localhost_button")
                    ) {
                        Icon(
                            PhoneDeckIcons.ContentCopy,
                            contentDescription = "Copy Localhost URL",
                            tint = Zinc100,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onOpenLocalhost,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("open_localhost_button")
                    ) {
                        Icon(
                            PhoneDeckIcons.OpenInBrowser,
                            contentDescription = "Open Localhost URL",
                            tint = AccentCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Zinc850)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    adbCmd,
                    fontSize = 12.sp,
                    color = Zinc400,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = onCopyAdb,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, Zinc800),
                    modifier = Modifier.testTag("copy_adb_button")
                ) {
                    Text("Copy", fontSize = 11.sp, color = Zinc100)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Zinc850)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    lanUrl,
                    fontSize = 12.sp,
                    color = Zinc400,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = onCopyLan,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, Zinc800),
                    modifier = Modifier.testTag("copy_lan_button")
                ) {
                    Text("Copy", fontSize = 11.sp, color = Zinc100)
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Zinc900),
        border = BorderStroke(1.dp, Zinc800)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
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
                    tint = AccentCyan,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Zinc100,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onClick,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentCyan,
                    contentColor = Color.Black
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag(testTag)
            ) {
                Text("Enable", fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
        PermissionHelper.ID_NOTIFICATION_LISTENER -> PhoneDeckIcons.NotificationsActive
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

private fun copyText(context: Context, label: String, text: String) {
    val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cb.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "Copied: $text", Toast.LENGTH_SHORT).show()
}
