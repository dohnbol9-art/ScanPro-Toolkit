package com.example.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.model.BarcodeFormatType
import com.example.model.ScanRecord
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.EnumMap
import kotlin.coroutines.resume

object ImageQrDecoder {

    class NoBarcodeDetectedException(message: String = "No QR code or supported barcode was detected in this image.") : Exception(message)

    suspend fun decodeFromUri(context: Context, imageUri: Uri): Result<ScanRecord> = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = decodeSampledBitmapFromUri(context, imageUri, reqWidth = 1600, reqHeight = 1600)
                ?: throw IllegalArgumentException("Could not load image file from selected source")

            // First attempt: Google ML Kit Barcode Scanner
            val mlKitResult = scanWithMlKit(bitmap)
            if (mlKitResult != null) {
                return@runCatching mlKitResult
            }

            // Fallback attempt: ZXing MultiFormatReader
            val zxingResult = scanWithZXing(bitmap)
            if (zxingResult != null) {
                return@runCatching zxingResult
            }

            throw NoBarcodeDetectedException()
        }
    }

    private suspend fun scanWithMlKit(bitmap: Bitmap): ScanRecord? = suspendCancellableCoroutine { continuation ->
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .build()
        val scanner = BarcodeScanning.getClient(options)
        val image = InputImage.fromBitmap(bitmap, 0)

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                val first = barcodes.firstOrNull()
                if (first != null && !first.rawValue.isNullOrBlank()) {
                    val rawValue = first.rawValue!!
                    val formatType = BarcodeFormatType.fromMlKitFormat(first.format)
                    val parsed = ContentParser.parse(rawValue, formatType)
                    val record = ScanRecord(
                        content = rawValue,
                        format = formatType,
                        contentType = parsed.contentType,
                        title = parsed.title,
                        domain = parsed.domain,
                        metadata = parsed.metadata,
                        createdAt = System.currentTimeMillis()
                    )
                    continuation.resume(record)
                } else {
                    continuation.resume(null)
                }
            }
            .addOnFailureListener {
                continuation.resume(null)
            }
    }

    private fun scanWithZXing(bitmap: Bitmap): ScanRecord? {
        return try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            val source = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

            val hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java).apply {
                put(DecodeHintType.TRY_HARDER, true)
                put(DecodeHintType.CHARACTER_SET, "UTF-8")
            }

            val reader = MultiFormatReader()
            val result = reader.decode(binaryBitmap, hints)
            if (result != null && result.text.isNotBlank()) {
                val format = when (result.barcodeFormat) {
                    com.google.zxing.BarcodeFormat.QR_CODE -> BarcodeFormatType.QR_CODE
                    com.google.zxing.BarcodeFormat.DATA_MATRIX -> BarcodeFormatType.DATA_MATRIX
                    com.google.zxing.BarcodeFormat.AZTEC -> BarcodeFormatType.AZTEC
                    com.google.zxing.BarcodeFormat.PDF_417 -> BarcodeFormatType.PDF_417
                    com.google.zxing.BarcodeFormat.EAN_13 -> BarcodeFormatType.EAN_13
                    com.google.zxing.BarcodeFormat.EAN_8 -> BarcodeFormatType.EAN_8
                    com.google.zxing.BarcodeFormat.UPC_A -> BarcodeFormatType.UPC_A
                    com.google.zxing.BarcodeFormat.UPC_E -> BarcodeFormatType.UPC_E
                    com.google.zxing.BarcodeFormat.CODE_128 -> BarcodeFormatType.CODE_128
                    com.google.zxing.BarcodeFormat.CODE_39 -> BarcodeFormatType.CODE_39
                    com.google.zxing.BarcodeFormat.CODE_93 -> BarcodeFormatType.CODE_93
                    com.google.zxing.BarcodeFormat.ITF -> BarcodeFormatType.ITF
                    else -> BarcodeFormatType.UNKNOWN
                }
                val parsed = ContentParser.parse(result.text, format)
                ScanRecord(
                    content = result.text,
                    format = format,
                    contentType = parsed.contentType,
                    title = parsed.title,
                    domain = parsed.domain,
                    metadata = parsed.metadata,
                    createdAt = System.currentTimeMillis()
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun decodeSampledBitmapFromUri(context: Context, uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap? {
        var input: InputStream? = null
        return try {
            input = context.contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(input, null, options)
            input?.close()

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false

            input = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(input, null, options)
        } catch (e: Exception) {
            null
        } finally {
            input?.close()
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
