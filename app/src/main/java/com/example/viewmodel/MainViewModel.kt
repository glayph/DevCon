package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.controller.PermissionHelper
import com.example.data.ServerStateManager
import com.example.model.PermissionItemState
import com.example.service.RemoteServerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    val isServerRunning: StateFlow<Boolean> = ServerStateManager.isServerRunning
    val serverPort: StateFlow<Int> = ServerStateManager.serverPort
    val serverIp: StateFlow<String> = ServerStateManager.serverIp

    private val _permissionStates = MutableStateFlow<List<PermissionItemState>>(emptyList())
    val permissionStates: StateFlow<List<PermissionItemState>> = _permissionStates.asStateFlow()

    fun refreshPermissions(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _permissionStates.value = PermissionHelper.getAllPermissionStates(context)
        }
    }

    fun toggleServer(context: Context) {
        runCatching {
            if (isServerRunning.value) {
                RemoteServerService.stopService(context)
            } else {
                RemoteServerService.startService(context, serverPort.value)
            }
        }
    }
}
