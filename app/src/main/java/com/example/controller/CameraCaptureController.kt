package com.example.controller

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Headless Camera2 still-photo capture controller supporting back and front cameras
 * with optional flash without requiring an active camera preview Activity.
 */
object CameraCaptureController {

    data class CaptureResult(
        val success: Boolean,
        val jpegBytes: ByteArray? = null,
        val error: String = ""
    )

    @SuppressLint("MissingPermission")
    suspend fun captureJpeg(
        context: Context,
        lens: String = "back",
        useFlash: Boolean = false
    ): CaptureResult {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            return CaptureResult(false, null, "Camera permission is not granted on the phone.")
        }

        val cm = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            ?: return CaptureResult(false, null, "CameraManager unavailable.")

        val desiredFacing = if (lens.equals("front", ignoreCase = true)) {
            CameraCharacteristics.LENS_FACING_FRONT
        } else {
            CameraCharacteristics.LENS_FACING_BACK
        }

        val cameraId = cm.cameraIdList.firstOrNull { id ->
            runCatching {
                cm.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING) == desiredFacing
            }.getOrDefault(false)
        } ?: cm.cameraIdList.firstOrNull()
            ?: return CaptureResult(false, null, "No camera sensor found.")

        val chars = cm.getCameraCharacteristics(cameraId)
        val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        val map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val jpegSizes = map?.getOutputSizes(ImageFormat.JPEG)?.toList().orEmpty()
        val chosen = jpegSizes
            .filter { it.width in 640..1920 && it.height in 480..1440 }
            .maxByOrNull { it.width * it.height }
            ?: jpegSizes.minByOrNull { it.width * it.height }
            ?: android.util.Size(1280, 720)

        val thread = HandlerThread("PhoneDeckCamCapture").apply { start() }
        val handler = Handler(thread.looper)
        val imageReader = ImageReader.newInstance(chosen.width, chosen.height, ImageFormat.JPEG, 2)
        val deferred = CompletableDeferred<CaptureResult>()

        var openedDevice: CameraDevice? = null
        var activeSession: CameraCaptureSession? = null

        imageReader.setOnImageAvailableListener({ reader ->
            runCatching {
                val img = reader.acquireLatestImage() ?: return@runCatching
                img.use {
                    val buf = it.planes[0].buffer
                    val bytes = ByteArray(buf.remaining())
                    buf.get(bytes)
                    deferred.complete(CaptureResult(true, bytes))
                }
            }.onFailure { ex ->
                deferred.complete(CaptureResult(false, null, ex.message ?: "Failed reading JPEG"))
            }
        }, handler)

        try {
            cm.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    openedDevice = camera
                    runCatching {
                        @Suppress("DEPRECATION")
                        camera.createCaptureSession(
                            listOf(imageReader.surface),
                            object : CameraCaptureSession.StateCallback() {
                                override fun onConfigured(session: CameraCaptureSession) {
                                    activeSession = session
                                    runCatching {
                                        val req = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                                            addTarget(imageReader.surface)
                                            set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
                                            set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                                            if (useFlash && hasFlash) {
                                                set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON_ALWAYS_FLASH)
                                                set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH)
                                            } else {
                                                set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON)
                                            }
                                            val orient = chars.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
                                            set(CaptureRequest.JPEG_ORIENTATION, orient)
                                            set(CaptureRequest.JPEG_QUALITY, 85.toByte())
                                        }.build()
                                        session.capture(req, null, handler)
                                    }.onFailure { ex ->
                                        deferred.complete(CaptureResult(false, null, ex.message ?: "Capture failed"))
                                    }
                                }

                                override fun onConfigureFailed(session: CameraCaptureSession) {
                                    deferred.complete(CaptureResult(false, null, "Camera session configuration failed"))
                                }
                            },
                            handler
                        )
                    }.onFailure { ex ->
                        deferred.complete(CaptureResult(false, null, ex.message ?: "Session failed"))
                    }
                }

                override fun onDisconnected(camera: CameraDevice) {
                    deferred.complete(CaptureResult(false, null, "Camera disconnected"))
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    deferred.complete(CaptureResult(false, null, "Camera error code $error"))
                }
            }, handler)

            return withTimeoutOrNull(8_000L) { deferred.await() }
                ?: CaptureResult(false, null, "Camera capture timed out")
        } catch (ex: Exception) {
            return CaptureResult(false, null, ex.message ?: "Camera error")
        } finally {
            runCatching { activeSession?.close() }
            runCatching { openedDevice?.close() }
            runCatching { imageReader.close() }
            runCatching { thread.quitSafely() }
        }
    }
}
