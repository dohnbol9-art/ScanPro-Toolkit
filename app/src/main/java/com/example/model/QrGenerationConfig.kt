package com.example.model

data class QrGenerationConfig(
    val content: String,
    val contentType: ParsedContentType,
    val size: Int = 1024,
    val errorCorrectionLevel: String = "H", // L (7%), M (15%), Q (25%), H (30%)
    val foregroundColor: Int = 0xFFBAC3FF.toInt(), // Default Electric Indigo or custom
    val backgroundColor: Int = 0xFF0C0E14.toInt(), // Dark Carbon Void or transparent/white
    val margin: Int = 2,
    val includeLogo: Boolean = false
)

enum class SafetyStatus {
    SAFE,
    SUSPICIOUS,
    DANGEROUS
}

data class UrlSafetyVerdict(
    val url: String,
    val status: SafetyStatus,
    val domain: String,
    val protocol: String,
    val reason: String,
    val isHttps: Boolean = protocol.equals("https", ignoreCase = true)
)
