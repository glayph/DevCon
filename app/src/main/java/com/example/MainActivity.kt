package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import com.example.controller.PermissionHelper
import com.example.ui.PhoneDeckScreen
import com.example.ui.theme.DashBgPage
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )

        setContent {
            MyApplicationTheme {
                var lastRequestedPerms by remember { mutableStateOf<List<String>>(emptyList()) }
                var isManualRequest by remember { mutableStateOf(false) }

                val multiPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { resultMap ->
                    viewModel.refreshPermissions(this@MainActivity)
                    val anyDenied = resultMap.any { !it.value }
                    if (isManualRequest && anyDenied && lastRequestedPerms.isNotEmpty()) {
                        val anyCanShowRationale = lastRequestedPerms.any { perm ->
                            ActivityCompat.shouldShowRequestPermissionRationale(this@MainActivity, perm)
                        }
                        if (!anyCanShowRationale) {
                            runCatching {
                                startActivity(PermissionHelper.appDetailsIntent(this@MainActivity))
                            }
                        }
                    }
                    isManualRequest = false
                }

                // Automatically request essential runtime permissions upon app launch
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    val ungranted = PermissionHelper.getEssentialRuntimePermissions(this@MainActivity)
                    if (ungranted.isNotEmpty()) {
                        lastRequestedPerms = ungranted
                        isManualRequest = false
                        multiPermissionLauncher.launch(ungranted.toTypedArray())
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DashBgPage
                ) {
                    PhoneDeckScreen(
                        viewModel = viewModel,
                        onRequestPermissionItem = { item ->
                            if (item.isSpecial || item.runtimePermissions.isEmpty()) {
                                val intent = PermissionHelper.buildSpecialPermissionIntent(this@MainActivity, item.id)
                                runCatching {
                                    startActivity(intent)
                                }.onFailure {
                                    runCatching {
                                        startActivity(PermissionHelper.appDetailsIntent(this@MainActivity))
                                    }
                                }
                            } else {
                                lastRequestedPerms = item.runtimePermissions
                                isManualRequest = true
                                multiPermissionLauncher.launch(item.runtimePermissions.toTypedArray())
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.ensureServiceActive(this)
        viewModel.refreshPermissions(this)
    }
}
