package com.example.data

import com.example.model.ServerLogEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong

object CommandLogRepository {
    private val counter = AtomicLong(1)
    private val _logs = MutableStateFlow<List<ServerLogEntry>>(emptyList())
    val logs: StateFlow<List<ServerLogEntry>> = _logs.asStateFlow()

    fun log(method: String, path: String, clientIp: String, statusCode: Int, details: String) {
        val entry = ServerLogEntry(
            id = counter.getAndIncrement(),
            timestamp = System.currentTimeMillis(),
            method = method,
            path = path,
            clientIp = clientIp,
            statusCode = statusCode,
            details = details
        )
        val current = _logs.value.toMutableList()
        current.add(0, entry)
        if (current.size > 200) {
            current.removeAt(current.size - 1)
        }
        _logs.value = current
    }

    fun clear() {
        _logs.value = emptyList()
    }
}
