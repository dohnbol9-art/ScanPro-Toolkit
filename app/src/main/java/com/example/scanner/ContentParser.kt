package com.example.scanner

import com.example.model.BarcodeFormatType
import com.example.model.ParsedContentType
import java.util.Locale

object ContentParser {

    fun parse(raw: String, format: BarcodeFormatType): ParsedResult {
        val trimmed = raw.trim()

        // Wi-Fi QR
        if (trimmed.startsWith("WIFI:", ignoreCase = true)) {
            val ssid = extractWifiParam(trimmed, "S:")
            val auth = extractWifiParam(trimmed, "T:")
            return ParsedResult(
                contentType = ParsedContentType.WIFI,
                title = if (ssid.isNotBlank()) "SSID: $ssid" else "Wi-Fi Network",
                domain = if (auth.isNotBlank()) "Security: $auth" else "WPA/WPA2",
                metadata = "Wi-Fi Access Point"
            )
        }

        // vCard / Contact
        if (trimmed.startsWith("BEGIN:VCARD", ignoreCase = true) || trimmed.startsWith("MECARD:", ignoreCase = true)) {
            val name = extractVCardName(trimmed)
            return ParsedResult(
                contentType = ParsedContentType.CONTACT,
                title = name.ifBlank { "Contact Card" },
                domain = "vCard 4.0",
                metadata = "Contact Info"
            )
        }

        // Calendar Event
        if (trimmed.startsWith("BEGIN:VEVENT", ignoreCase = true)) {
            val summary = extractCalendarSummary(trimmed)
            return ParsedResult(
                contentType = ParsedContentType.CALENDAR,
                title = summary.ifBlank { "Calendar Event" },
                domain = "iCal Event",
                metadata = "Event Schedule"
            )
        }

        // Geo Location
        if (trimmed.startsWith("geo:", ignoreCase = true)) {
            val coords = trimmed.substringAfter("geo:").substringBefore("?")
            return ParsedResult(
                contentType = ParsedContentType.LOCATION,
                title = "Coordinates: $coords",
                domain = "Geo Location",
                metadata = "GPS Coordinates"
            )
        }

        // Phone
        if (trimmed.startsWith("tel:", ignoreCase = true)) {
            val number = trimmed.substringAfter("tel:")
            return ParsedResult(
                contentType = ParsedContentType.PHONE,
                title = number,
                domain = "Direct Dial",
                metadata = "Telephone Number"
            )
        }

        // Email
        if (trimmed.startsWith("mailto:", ignoreCase = true) || trimmed.startsWith("MATMSG:", ignoreCase = true)) {
            val email = if (trimmed.startsWith("mailto:", ignoreCase = true)) {
                trimmed.substringAfter("mailto:").substringBefore("?")
            } else {
                extractWifiParam(trimmed, "TO:")
            }
            return ParsedResult(
                contentType = ParsedContentType.EMAIL,
                title = email.ifBlank { "Email Message" },
                domain = "Email Protocol",
                metadata = "Mailto Payload"
            )
        }

        // SMS
        if (trimmed.startsWith("smsto:", ignoreCase = true) || trimmed.startsWith("sms:", ignoreCase = true)) {
            val num = trimmed.substringAfter(":").substringBefore(":")
            return ParsedResult(
                contentType = ParsedContentType.SMS,
                title = "SMS: $num",
                domain = "Cellular Messaging",
                metadata = "SMS Target"
            )
        }

        // URL
        val lower = trimmed.lowercase(Locale.ROOT)
        if (lower.startsWith("http://") || lower.startsWith("https://") || (lower.startsWith("www.") && lower.contains("."))) {
            val normalized = if (!lower.startsWith("http://") && !lower.startsWith("https://")) "https://$trimmed" else trimmed
            val host = try {
                val withoutScheme = normalized.substringAfter("://").substringBefore("/").substringBefore("?")
                withoutScheme.substringBefore(":")
            } catch (e: Exception) {
                "Web Domain"
            }
            return ParsedResult(
                contentType = ParsedContentType.URL,
                title = host,
                domain = host,
                metadata = "${trimmed.toByteArray().size} Bytes"
            )
        }

        // 1D Retail Barcode Product
        if (!format.is2D && (format == BarcodeFormatType.EAN_13 || format == BarcodeFormatType.UPC_A || format == BarcodeFormatType.EAN_8 || format == BarcodeFormatType.UPC_E)) {
            return ParsedResult(
                contentType = ParsedContentType.PRODUCT,
                title = "Product #$trimmed",
                domain = format.displayName,
                metadata = "${trimmed.length} Digits Validated"
            )
        }

        // Default Plain Text
        val previewTitle = if (trimmed.length > 32) trimmed.take(30) + "..." else trimmed
        return ParsedResult(
            contentType = ParsedContentType.TEXT,
            title = previewTitle,
            domain = format.displayName,
            metadata = "${trimmed.length} Characters"
        )
    }

    private fun extractWifiParam(data: String, key: String): String {
        return try {
            val start = data.indexOf(key)
            if (start == -1) return ""
            val end = data.indexOf(";", start + key.length)
            if (end == -1) data.substring(start + key.length)
            else data.substring(start + key.length, end)
        } catch (e: Exception) {
            ""
        }
    }

    private fun extractVCardName(data: String): String {
        return try {
            val fnLine = data.lines().firstOrNull { it.startsWith("FN:", ignoreCase = true) }
            if (fnLine != null) {
                return fnLine.substringAfter(":").trim()
            }
            val nLine = data.lines().firstOrNull { it.startsWith("N:", ignoreCase = true) }
            if (nLine != null) {
                val parts = nLine.substringAfter(":").split(";")
                return parts.reversed().filter { it.isNotBlank() }.joinToString(" ").trim()
            }
            "Contact"
        } catch (e: Exception) {
            "Contact"
        }
    }

    private fun extractCalendarSummary(data: String): String {
        return try {
            val line = data.lines().firstOrNull { it.startsWith("SUMMARY:", ignoreCase = true) }
            line?.substringAfter(":")?.trim() ?: "Event"
        } catch (e: Exception) {
            "Event"
        }
    }

    data class ParsedResult(
        val contentType: ParsedContentType,
        val title: String,
        val domain: String?,
        val metadata: String?
    )
}
