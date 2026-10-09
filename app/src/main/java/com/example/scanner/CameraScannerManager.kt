package com.example.scanner

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.model.BarcodeFormatType
import com.example.model.ScanRecord
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class CameraScannerManager(
    private val context: Context,
    private val onBarcodeScanned: (ScanRecord) -> Unit
) {

    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var cameraControl: CameraControl? = null
    private var cameraInfo: CameraInfo? = null

    private var isTorchOn: Boolean = false
    private var currentLensFacing: Int = CameraSelector.LENS_FACING_BACK
    private var isPaused: AtomicBoolean = AtomicBoolean(false)
    private var lastScannedValue: String = ""
    private var lastScannedTimestamp: Long = 0L

    var isVibrationEnabled: Boolean = true
    var isBeepEnabled: Boolean = false
    var isBatchMode: Boolean = false

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private var toneGenerator: ToneGenerator? = null

    init {
        runCatching {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
        }
    }

    fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        onCameraReady: () -> Unit = {}
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases(lifecycleOwner, previewView)
                onCameraReady()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindCameraUseCases(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView
    ) {
        val provider = cameraProvider ?: return

        val preview = Preview.Builder()
            .build()
            .also {
                it.surfaceProvider = previewView.surfaceProvider
            }

        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_AZTEC,
                Barcode.FORMAT_DATA_MATRIX,
                Barcode.FORMAT_PDF417,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_CODE_93,
                Barcode.FORMAT_ITF
            )
            .build()

        val barcodeScanner = BarcodeScanning.getClient(options)

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
            processImageProxy(barcodeScanner, imageProxy)
        }

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(currentLensFacing)
            .build()

        try {
            provider.unbindAll()
            camera = provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            )
            cameraControl = camera?.cameraControl
            cameraInfo = camera?.cameraInfo
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun processImageProxy(
        scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
        imageProxy: ImageProxy
    ) {
        val mediaImage = imageProxy.image
        if (mediaImage == null || isPaused.get()) {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                val now = System.currentTimeMillis()
                for (barcode in barcodes) {
                    val rawValue = barcode.rawValue
                    if (!rawValue.isNullOrBlank()) {
                        // Prevent duplicate frame processing
                        val isSameRecentCode = (rawValue == lastScannedValue && (now - lastScannedTimestamp) < 2500L)
                        if (!isSameRecentCode) {
                            lastScannedValue = rawValue
                            lastScannedTimestamp = now

                            triggerHapticAndSound()

                            val formatType = BarcodeFormatType.fromMlKitFormat(barcode.format)
                            val parsed = ContentParser.parse(rawValue, formatType)
                            val record = ScanRecord(
                                content = rawValue,
                                format = formatType,
                                contentType = parsed.contentType,
                                title = parsed.title,
                                domain = parsed.domain,
                                metadata = parsed.metadata,
                                createdAt = now
                            )

                            if (!isBatchMode) {
                                isPaused.set(true)
                            }

                            ContextCompat.getMainExecutor(context).execute {
                                onBarcodeScanned(record)
                            }
                            break
                        }
                    }
                }
            }
            .addOnFailureListener {
                // Ignore frame failure
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun triggerHapticAndSound() {
        if (isVibrationEnabled) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(45L, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(45L)
                }
            } catch (e: Exception) {
                // Vibration fallback
            }
        }

        if (isBeepEnabled) {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 80)
            } catch (e: Exception) {
                // Tone fallback
            }
        }
    }

    fun toggleTorch(onTorchToggled: (Boolean) -> Unit = {}) {
        val info = cameraInfo ?: return
        if (info.hasFlashUnit()) {
            val nextState = !isTorchOn
            cameraControl?.enableTorch(nextState)?.addListener({
                isTorchOn = nextState
                onTorchToggled(isTorchOn)
            }, ContextCompat.getMainExecutor(context))
        }
    }

    fun setZoom(ratio: Float) {
        cameraControl?.setZoomRatio(ratio.coerceIn(1.0f, 5.0f))
    }

    fun flipCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        currentLensFacing = if (currentLensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        isTorchOn = false
        bindCameraUseCases(lifecycleOwner, previewView)
    }

    fun pause() {
        isPaused.set(true)
    }

    fun resume() {
        isPaused.set(false)
        lastScannedValue = ""
    }

    fun release() {
        try {
            cameraProvider?.unbindAll()
            cameraExecutor.shutdown()
            toneGenerator?.release()
            toneGenerator = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
