package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.controller.PermissionHelper
import com.example.data.ServerStateManager
import com.example.data.TunnelStatus
import com.example.model.PermissionItemState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    val isServerRunning: StateFlow<Boolean> = ServerStateManager.isServerRunning
    val serverPort: StateFlow<Int> = ServerStateManager.serverPort
    val serverIp: StateFlow<String> = ServerStateManager.serverIp

    val tunnelStatus: StateFlow<TunnelStatus> = ServerStateManager.tunnelStatus
    val publicUrl: StateFlow<String> = ServerStateManager.publicUrl
    val tunnelError: StateFlow<String> = ServerStateManager.tunnelError
    val deviceId: StateFlow<String> = ServerStateManager.deviceId
    val isRegistered: StateFlow<Boolean> = ServerStateManager.isRegistered
    val registeredUserId: StateFlow<String> = ServerStateManager.registeredUserId

    private val _permissionStates = MutableStateFlow<List<PermissionItemState>>(emptyList())
    val permissionStates: StateFlow<List<PermissionItemState>> = _permissionStates.asStateFlow()

    fun refreshPermissions(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            ServerStateManager.refreshAuthInfo(context)
            _permissionStates.value = PermissionHelper.getAllPermissionStates(context)
        }
    }

    fun ensureServiceActive(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            com.example.service.RemoteServerService.startService(context.applicationContext, serverPort.value)
        }
    }

    fun reconnectTunnel(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            com.example.service.RemoteServerService.reconnectTunnel(
                context.applicationContext,
                serverPort.value
            )
        }
    }
}
