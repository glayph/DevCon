package com.example.server

/**
 * Single source of truth for the stable per-device Cloudflare relay address.
 * The URL is derived from the persistent device ID, not from the live socket state.
 */
object CloudflareRelayConfig {
    const val WORKER_ORIGIN = "https://glayph.phonedeck.workers.dev"
    const val DEVICE_SOCKET_URL = "wss://glayph.phonedeck.workers.dev/ws/device"

    fun publicUrl(deviceId: String): String = "$WORKER_ORIGIN/d/$deviceId/"
}
