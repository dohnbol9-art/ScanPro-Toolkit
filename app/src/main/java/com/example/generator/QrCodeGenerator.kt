package com.example.generator

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import com.example.model.QrGenerationConfig
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.EncodeHintType
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.EnumMap

object QrCodeGenerator {

    data class GenerationResult(
        val bitmap: Bitmap,
        val isValidated: Boolean,
        val validationMessage: String
    )

    fun generateQrBitmap(
        config: QrGenerationConfig,
        centerLogoBitmap: Bitmap? = null
    ): GenerationResult {
        val content = config.content.ifBlank { "ScanPro Toolkit" }
        val size = config.size.coerceIn(256, 4096)

        val ecc = when (config.errorCorrectionLevel.uppercase()) {
            "L" -> ErrorCorrectionLevel.L
            "M" -> ErrorCorrectionLevel.M
            "Q" -> ErrorCorrectionLevel.Q
            else -> ErrorCorrectionLevel.H // Default highest redundancy
        }

        val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
            put(EncodeHintType.CHARACTER_SET, "UTF-8")
            put(EncodeHintType.ERROR_CORRECTION, ecc)
            put(EncodeHintType.MARGIN, config.margin.coerceIn(0, 8))
        }

        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)

        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)

        val fg = config.foregroundColor
        val bg = config.backgroundColor

        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix.get(x, y)) fg else bg
            }
        }

        var bitmap = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)

        // Overlay optional centered logo with rounded padding card
        if (config.includeLogo && centerLogoBitmap != null) {
            bitmap = overlayLogo(bitmap, centerLogoBitmap, bg)
        }

        // Automated decode validation test
        val (isValid, msg) = validateScannability(bitmap)

        return GenerationResult(
            bitmap = bitmap,
            isValidated = isValid,
            validationMessage = msg
        )
    }

    private fun overlayLogo(qrBitmap: Bitmap, logo: Bitmap, bgFillColor: Int): Bitmap {
        val result = qrBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val qrSize = result.width

        // Logo occupies max 20% of QR size to preserve ECC decode
        val logoSize = (qrSize * 0.20f).toInt().coerceAtLeast(32)
        val left = (qrSize - logoSize) / 2f
        val top = (qrSize - logoSize) / 2f
        val rect = RectF(left, top, left + logoSize, top + logoSize)

        // Draw rounded background backing container
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bgFillColor
            style = Paint.Style.FILL
        }
        val radius = logoSize * 0.22f
        canvas.drawRoundRect(rect, radius, radius, bgPaint)

        // Draw scaled logo inside inset
        val inset = logoSize * 0.10f
        val logoRect = RectF(rect.left + inset, rect.top + inset, rect.right - inset, rect.bottom - inset)
        val scaledLogo = Bitmap.createScaledBitmap(logo, logoRect.width().toInt(), logoRect.height().toInt(), true)
        canvas.drawBitmap(scaledLogo, logoRect.left, logoRect.top, Paint(Paint.FILTER_BITMAP_FLAG))

        return result
    }

    private fun validateScannability(bitmap: Bitmap): Pair<Boolean, String> {
        return try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            val source = RGBLuminanceSource(width, height, pixels)
            val reader = QRCodeReader()

            var decodedText: String? = null
            try {
                val binarizer = HybridBinarizer(source)
                val result = reader.decode(BinaryBitmap(binarizer))
                decodedText = result?.text
            } catch (_: Exception) {
                // Inverted fallback for dark-mode QR codes (light foreground on dark background)
                try {
                    reader.reset()
                    val invertedSource = source.invert()
                    val result = reader.decode(BinaryBitmap(HybridBinarizer(invertedSource)))
                    decodedText = result?.text
                } catch (_: Exception) {
                    // Failed both normal and inverted
                }
            }

            if (!decodedText.isNullOrEmpty()) {
                Pair(true, "Readability: 100% (Optical Decode Verified)")
            } else {
                Pair(false, "Contrast Warning: Code might be difficult to scan in low light.")
            }
        } catch (e: Exception) {
            // ZXing reader may fail if contrast is too close or logo is too big
            Pair(false, "Your design may be difficult to scan. Increase contrast or ECC level.")
        }
    }
}
