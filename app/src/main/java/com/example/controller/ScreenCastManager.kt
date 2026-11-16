package com.example.controller

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.PowerManager
import android.util.DisplayMetrics
import android.view.WindowManager
import com.example.service.RemoteServerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

object ScreenCastManager {

    private const val VIEWER_PAUSE_THRESHOLD_MS = 8_000L
    private const val VIEWER_AUTO_STOP_MS = 35_000L

    private val _isCasting = MutableStateFlow(false)
    val isCasting: StateFlow<Boolean> = _isCasting.asStateFlow()

    private val _lastError = MutableStateFlow("")
    val lastError: StateFlow<String> = _lastError.asStateFlow()

    val targetFps = AtomicInteger(8)
    val jpegQuality = AtomicInteger(65)
    val scalePercent = AtomicInteger(65)

    val streamWidth = AtomicInteger(0)
    val streamHeight = AtomicInteger(0)
    val frameCount = AtomicLong(0L)

    private val latestFrame = AtomicReference<ByteArray?>(null)
    private val lastFrameTimeMs = AtomicLong(0L)
    private val lastViewerPollMs = AtomicLong(0L)
    private val isProcessing = AtomicBoolean(false)

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null
    private var appContext: Context? = null
    private var powerManager: PowerManager? = null
    private var reusableRawBitmap: Bitmap? = null
    private var reusableBaos: ByteArrayOutputStream? = null
    private var baseWidth: Int = 1080
    private var baseHeight: Int = 1920
    private var screenDensity: Int = 320

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            stopCast()
        }

        override fun onCapturedContentResize(width: Int, height: Int) {
            if (width > 0 && height > 0) {
                baseWidth = width
                baseHeight = height
                recreateVirtualDisplay()
            }
        }
    }

    fun markViewerActive() {
        lastViewerPollMs.set(System.currentTimeMillis())
    }

    @Synchronized
    fun startCast(context: Context, resultCode: Int, resultData: Intent): Boolean {
        stopCast()
        val appCtx = context.applicationContext
        appContext = appCtx
        powerManager = appCtx.getSystemService(Context.POWER_SERVICE) as? PowerManager
        _lastError.value = ""
        frameCount.set(0L)
        markViewerActive()

        return try {
            val mpm = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val projection = mpm.getMediaProjection(resultCode, resultData)
            if (projection == null) {
                _lastError.value = "MediaProjection denied or unavailable"
                return false
            }

            mediaProjection = projection
            val metrics = resolveDisplayMetrics(context)
            baseWidth = metrics.first
            baseHeight = metrics.second
            screenDensity = metrics.third

            val thread = HandlerThread("PhoneDeckScreenCast").apply { start() }
            handlerThread = thread
            val bgHandler = Handler(thread.looper)
            handler = bgHandler

            projection.registerCallback(projectionCallback, bgHandler)
            setupVirtualDisplay(projection, bgHandler)
            _isCasting.value = true
            true
        } catch (e: Exception) {
            _lastError.value = e.message ?: "Failed to start screen cast"
            stopCast()
            false
        }
    }

    @Synchronized
    fun updateConfig(fps: Int?, quality: Int?, scale: Int?) {
        fps?.let { targetFps.set(it.coerceIn(1, 20)) }
        quality?.let { jpegQuality.set(it.coerceIn(20, 95)) }
        val oldScale = scalePercent.get()
        scale?.let { scalePercent.set(it.coerceIn(25, 100)) }
        if (_isCasting.value && scale != null && scalePercent.get() != oldScale) {
            recreateVirtualDisplay()
        }
    }

    @Synchronized
    private fun recreateVirtualDisplay() {
        val projection = mediaProjection ?: return
        val bgHandler = handler ?: return
        runCatching { virtualDisplay?.release() }
        virtualDisplay = null
        runCatching { imageReader?.close() }
        imageReader = null
        releaseReusableBuffers()
        runCatching { setupVirtualDisplay(projection, bgHandler) }
    }

    private fun setupVirtualDisplay(projection: MediaProjection, bgHandler: Handler) {
        val scale = scalePercent.get().coerceIn(25, 100) / 100f
        val w = ((baseWidth * scale).toInt() and 1.inv()).coerceAtLeast(240)
        val h = ((baseHeight * scale).toInt() and 1.inv()).coerceAtLeast(320)
        streamWidth.set(w)
        streamHeight.set(h)

        val reader = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 2)
        imageReader = reader
        reader.setOnImageAvailableListener({ r ->
            processAvailableImage(r)
        }, bgHandler)

        virtualDisplay = projection.createVirtualDisplay(
            "PhoneDeckCast",
            w,
            h,
            screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface,
            null,
            bgHandler
        )
    }

    private fun processAvailableImage(reader: ImageReader) {
        val image = runCatching { reader.acquireLatestImage() }.getOrNull() ?: return
        try {
            val now = System.currentTimeMillis()
            val idleSinceViewerMs = now - lastViewerPollMs.get()
            if (idleSinceViewerMs > VIEWER_AUTO_STOP_MS && frameCount.get() > 0L) {
                val ctx = appContext
                stopCast()
                if (ctx != null) {
                    runCatching { RemoteServerService.stopScreenCast(ctx) }
                }
                return
            }

            if (powerManager?.isInteractive == false && frameCount.get() > 0L) {
                return
            }

            if (idleSinceViewerMs > VIEWER_PAUSE_THRESHOLD_MS && frameCount.get() > 0L) {
                return
            }

            val minIntervalMs = 1000L / targetFps.get().coerceIn(1, 20)
            if (now - lastFrameTimeMs.get() < minIntervalMs || !isProcessing.compareAndSet(false, true)) {
                return
            }

            var croppedBitmap: Bitmap? = null
            try {
                val plane = image.planes.firstOrNull() ?: return
                val buffer = plane.buffer
                val pixelStride = plane.pixelStride
                val rowStride = plane.rowStride
                val width = image.width
                val height = image.height
                if (width <= 0 || height <= 0 || pixelStride <= 0) return

                val rowPadding = rowStride - pixelStride * width
                val bitmapWidth = width + rowPadding / pixelStride
                var rawBitmap = reusableRawBitmap
                if (rawBitmap == null || rawBitmap.isRecycled || rawBitmap.width != bitmapWidth || rawBitmap.height != height) {
                    runCatching { rawBitmap?.recycle() }
                    rawBitmap = Bitmap.createBitmap(bitmapWidth, height, Bitmap.Config.ARGB_8888)
                    reusableRawBitmap = rawBitmap
                }
                rawBitmap.copyPixelsFromBuffer(buffer)

                val outBitmap = if (bitmapWidth != width) {
                    croppedBitmap = Bitmap.createBitmap(rawBitmap, 0, 0, width, height)
                    croppedBitmap
                } else {
                    rawBitmap
                }

                val baos = reusableBaos ?: ByteArrayOutputStream(64 * 1024).also { reusableBaos = it }
                baos.reset()
                outBitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality.get().coerceIn(20, 95), baos)
                latestFrame.set(baos.toByteArray())
                lastFrameTimeMs.set(now)
                frameCount.incrementAndGet()
            } catch (_: Throwable) {
                // Protect against transient surface/OOM errors
            } finally {
                runCatching { croppedBitmap?.recycle() }
                isProcessing.set(false)
            }
        } finally {
            runCatching { image.close() }
        }
    }

    fun getLatestFrame(): ByteArray? {
        markViewerActive()
        return latestFrame.get()
    }

    @Synchronized
    private fun releaseReusableBuffers() {
        runCatching {
            if (reusableRawBitmap?.isRecycled == false) {
                reusableRawBitmap?.recycle()
            }
        }
        reusableRawBitmap = null
        reusableBaos = null
    }

    @Synchronized
    fun stopCast() {
        _isCasting.value = false
        runCatching { virtualDisplay?.release() }
        virtualDisplay = null
        runCatching { imageReader?.setOnImageAvailableListener(null, null) }
        runCatching { imageReader?.close() }
        imageReader = null
        runCatching { mediaProjection?.unregisterCallback(projectionCallback) }
        runCatching { mediaProjection?.stop() }
        mediaProjection = null
        runCatching { handlerThread?.quitSafely() }
        handlerThread = null
        handler = null
        latestFrame.set(null)
        releaseReusableBuffers()
    }

    private fun resolveDisplayMetrics(context: Context): Triple<Int, Int, Int> {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val density = context.resources.displayMetrics.densityDpi.coerceAtLeast(160)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = wm.maximumWindowMetrics.bounds
            Triple(bounds.width().coerceAtLeast(720), bounds.height().coerceAtLeast(1280), density)
        } else {
            val dm = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(dm)
            Triple(dm.widthPixels.coerceAtLeast(720), dm.heightPixels.coerceAtLeast(1280), dm.densityDpi.coerceAtLeast(160))
        }
    }
}
