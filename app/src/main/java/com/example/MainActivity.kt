package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
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
import com.example.service.RemoteServerService
import com.example.ui.PhoneDeckScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.Zinc950
import com.example.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        runCatching {
            val port = com.example.data.ServerStateManager.getSavedPort(applicationContext)
            RemoteServerService.startService(applicationContext, port)
        }

        setContent {
            MyApplicationTheme {
                var lastRequestedPerms by remember { mutableStateOf<List<String>>(emptyList()) }

                val multiPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { resultMap ->
                    viewModel.refreshPermissions(this@MainActivity)
                    val anyDenied = resultMap.any { !it.value }
                    if (anyDenied && lastRequestedPerms.isNotEmpty()) {
                        val anyCanShowRationale = lastRequestedPerms.any { perm ->
                            ActivityCompat.shouldShowRequestPermissionRationale(this@MainActivity, perm)
                        }
                        if (!anyCanShowRationale) {
                            runCatching {
                                startActivity(PermissionHelper.appDetailsIntent(this@MainActivity))
                            }
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Zinc950
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
        viewModel.refreshPermissions(this)
    }
}
