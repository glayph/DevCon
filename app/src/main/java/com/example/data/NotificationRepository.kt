package com.example.data

import com.example.model.CapturedNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.CopyOnWriteArrayList

object NotificationRepository {
    private val _notifications = MutableStateFlow<List<CapturedNotification>>(emptyList())
    val notifications: StateFlow<List<CapturedNotification>> = _notifications.asStateFlow()

    private val listeners = CopyOnWriteArrayList<(CapturedNotification) -> Unit>()

    fun addNotification(notification: CapturedNotification) {
        val current = _notifications.value.toMutableList()
        current.add(0, notification)
        if (current.size > 100) {
            current.removeAt(current.size - 1)
        }
        _notifications.value = current
        listeners.forEach { runCatching { it(notification) } }
    }

    fun clear() {
        _notifications.value = emptyList()
    }

    fun subscribe(listener: (CapturedNotification) -> Unit) {
        listeners.add(listener)
    }

    fun unsubscribe(listener: (CapturedNotification) -> Unit) {
        listeners.remove(listener)
    }
}
