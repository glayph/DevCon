package com.example.data

import java.util.concurrent.ConcurrentHashMap

/**
 * Sliding-window rate limiter to protect auth, OTP dispatch, OTP verification, and recovery
 * endpoints against brute-force or SMS-flooding abuse.
 */
object RateLimiter {

    data class LimitResult(
        val allowed: Boolean,
        val retryAfterSeconds: Long = 0L,
        val remaining: Int = 0
    )

    private data class BucketKey(val scope: String, val clientKey: String)

    private val windows = ConcurrentHashMap<BucketKey, ArrayDeque<Long>>()

    fun checkAndRecord(
        scope: String,
        clientKey: String,
        maxRequests: Int,
        windowMs: Long,
        nowMs: Long = System.currentTimeMillis()
    ): LimitResult {
        val normalizedClient = clientKey.trim().ifEmpty { "unknown" }
        val key = BucketKey(scope, normalizedClient)
        val queue = windows.getOrPut(key) { ArrayDeque() }
        synchronized(queue) {
            val cutoff = nowMs - windowMs
            while (queue.isNotEmpty() && queue.first() <= cutoff) {
                queue.removeFirst()
            }
            if (queue.size >= maxRequests) {
                val oldest = queue.first()
                val retryMs = (oldest + windowMs - nowMs).coerceAtLeast(1000L)
                return LimitResult(
                    allowed = false,
                    retryAfterSeconds = (retryMs + 999L) / 1000L,
                    remaining = 0
                )
            }
            queue.addLast(nowMs)
            return LimitResult(
                allowed = true,
                retryAfterSeconds = 0L,
                remaining = (maxRequests - queue.size).coerceAtLeast(0)
            )
        }
    }

    fun peekBlocked(
        scope: String,
        clientKey: String,
        maxRequests: Int,
        windowMs: Long,
        nowMs: Long = System.currentTimeMillis()
    ): LimitResult {
        val normalizedClient = clientKey.trim().ifEmpty { "unknown" }
        val key = BucketKey(scope, normalizedClient)
        val queue = windows[key] ?: return LimitResult(true, 0L, maxRequests)
        synchronized(queue) {
            val cutoff = nowMs - windowMs
            while (queue.isNotEmpty() && queue.first() <= cutoff) {
                queue.removeFirst()
            }
            if (queue.size >= maxRequests) {
                val oldest = queue.first()
                val retryMs = (oldest + windowMs - nowMs).coerceAtLeast(1000L)
                return LimitResult(false, (retryMs + 999L) / 1000L, 0)
            }
            return LimitResult(true, 0L, (maxRequests - queue.size).coerceAtLeast(0))
        }
    }

    fun recordFailure(
        scope: String,
        clientKey: String,
        windowMs: Long,
        nowMs: Long = System.currentTimeMillis()
    ) {
        val normalizedClient = clientKey.trim().ifEmpty { "unknown" }
        val key = BucketKey(scope, normalizedClient)
        val queue = windows.getOrPut(key) { ArrayDeque() }
        synchronized(queue) {
            val cutoff = nowMs - windowMs
            while (queue.isNotEmpty() && queue.first() <= cutoff) {
                queue.removeFirst()
            }
            queue.addLast(nowMs)
        }
    }

    fun reset(scope: String, clientKey: String) {
        val normalizedClient = clientKey.trim().ifEmpty { "unknown" }
        windows.remove(BucketKey(scope, normalizedClient))
    }

    fun clearAll() {
        windows.clear()
    }
}
