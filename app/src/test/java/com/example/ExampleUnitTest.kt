package com.example

import com.example.generator.PayloadFormatters
import com.example.generator.QrCodeGenerator
import com.example.model.BarcodeFormatType
import com.example.model.ParsedContentType
import com.example.model.QrGenerationConfig
import com.example.model.SafetyStatus
import com.example.scanner.ContentParser
import com.example.security.UrlSafetyChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleUnitTest {

    @Test
    fun urlSafetyChecker_detectsSafeHttpsUrl() {
        val verdict = UrlSafetyChecker.evaluate("https://github.com/android/camera-samples")
        assertEquals(SafetyStatus.SAFE, verdict.status)
        assertEquals("github.com", verdict.domain)
        assertTrue(verdict.isHttps)
    }

    @Test
    fun urlSafetyChecker_detectsInsecureHttpUrl() {
        val verdict = UrlSafetyChecker.evaluate("http://example.com")
        assertEquals(SafetyStatus.SUSPICIOUS, verdict.status)
        assertEquals("example.com", verdict.domain)
    }

    @Test
    fun urlSafetyChecker_blocksDangerousApkExtension() {
        val verdict = UrlSafetyChecker.evaluate("https://untrusted-server.com/malicious_payload.apk")
        assertEquals(SafetyStatus.DANGEROUS, verdict.status)
        assertTrue(verdict.reason.contains("executable installer"))
    }

    @Test
    fun urlSafetyChecker_blocksDangerousScheme() {
        val verdict = UrlSafetyChecker.evaluate("javascript:alert(1)")
        assertEquals(SafetyStatus.DANGEROUS, verdict.status)
        assertTrue(verdict.reason.contains("unsafe URI scheme"))
    }

    @Test
    fun contentParser_correctlyClassifiesWifi() {
        val raw = "WIFI:T:WPA;S:TestLab_5G;P:Password123;;"
        val result = ContentParser.parse(raw, BarcodeFormatType.QR_CODE)
        assertEquals(ParsedContentType.WIFI, result.contentType)
        assertTrue(result.title.contains("TestLab_5G"))
    }

    @Test
    fun contentParser_correctlyClassifiesUrl() {
        val raw = "https://developer.android.com"
        val result = ContentParser.parse(raw, BarcodeFormatType.QR_CODE)
        assertEquals(ParsedContentType.URL, result.contentType)
        assertEquals("developer.android.com", result.domain)
    }

    @Test
    fun payloadFormatters_buildsValidWifiString() {
        val wifiStr = PayloadFormatters.formatWifi("MySsid", "SecretPass", "WPA", false)
        assertEquals("WIFI:T:WPA;S:MySsid;P:SecretPass;;", wifiStr)
    }

    @Test
    fun payloadFormatters_normalizesUrlWithoutScheme() {
        val formatted = PayloadFormatters.formatUrl("example.org/docs")
        assertEquals("https://example.org/docs", formatted)
    }

    @Test
    fun qrCodeGenerator_createsScannableBitmap() {
        val config = QrGenerationConfig(
            content = "https://ai.google.dev",
            contentType = ParsedContentType.URL,
            size = 512,
            errorCorrectionLevel = "H"
        )
        val result = QrCodeGenerator.generateQrBitmap(config)
        assertNotNull(result.bitmap)
        assertEquals(512, result.bitmap.width)
        assertEquals(512, result.bitmap.height)
        assertTrue(result.isValidated)
    }
}
