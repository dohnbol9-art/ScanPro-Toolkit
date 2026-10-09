package com.example.generator

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object PayloadFormatters {

    fun formatUrl(input: String): String {
        val trimmed = input.trim()
        return when {
            trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            trimmed.startsWith("ftp://", ignoreCase = true) -> trimmed
            else -> "https://$trimmed"
        }
    }

    fun formatWifi(ssid: String, pass: String, authType: String = "WPA", isHidden: Boolean = false): String {
        val cleanSsid = escapeSpecial(ssid.trim())
        val cleanPass = escapeSpecial(pass)
        val cleanAuth = when (authType.uppercase()) {
            "WPA", "WPA2", "WPA3" -> "WPA"
            "WEP" -> "WEP"
            else -> "nopass"
        }
        val hiddenStr = if (isHidden) "H:true;" else ""
        return "WIFI:T:$cleanAuth;S:$cleanSsid;P:$cleanPass;$hiddenStr;"
    }

    fun formatVCard(
        name: String,
        organization: String = "",
        phone: String = "",
        email: String = "",
        website: String = "",
        address: String = ""
    ): String {
        val sb = StringBuilder()
        sb.appendLine("BEGIN:VCARD")
        sb.appendLine("VERSION:4.0")
        sb.appendLine("FN:${name.trim()}")
        if (organization.isNotBlank()) sb.appendLine("ORG:${organization.trim()}")
        if (phone.isNotBlank()) sb.appendLine("TEL;TYPE=cell,voice:${phone.trim()}")
        if (email.isNotBlank()) sb.appendLine("EMAIL:${email.trim()}")
        if (website.isNotBlank()) sb.appendLine("URL:${formatUrl(website)}")
        if (address.isNotBlank()) sb.appendLine("ADR;TYPE=home:;;${address.trim()};;;;")
        sb.append("END:VCARD")
        return sb.toString()
    }

    fun formatPhone(phone: String): String {
        val clean = phone.trim().replace(" ", "")
        return "tel:$clean"
    }

    fun formatEmail(email: String, subject: String = "", body: String = ""): String {
        val cleanEmail = email.trim()
        if (subject.isBlank() && body.isBlank()) {
            return "mailto:$cleanEmail"
        }
        val encSub = urlEncode(subject)
        val encBody = urlEncode(body)
        val params = mutableListOf<String>()
        if (encSub.isNotBlank()) params.add("subject=$encSub")
        if (encBody.isNotBlank()) params.add("body=$encBody")
        return "mailto:$cleanEmail?${params.joinToString("&")}"
    }

    fun formatSms(phone: String, message: String = ""): String {
        val clean = phone.trim().replace(" ", "")
        return if (message.isBlank()) {
            "smsto:$clean"
        } else {
            "smsto:$clean:$message"
        }
    }

    fun formatGeo(latitude: Double, longitude: Double, label: String = ""): String {
        return if (label.isBlank()) {
            "geo:$latitude,$longitude"
        } else {
            "geo:$latitude,$longitude?q=${urlEncode(label)}"
        }
    }

    fun formatCalendarEvent(
        title: String,
        location: String = "",
        description: String = "",
        dtStartUtcFormatted: String = "",
        dtEndUtcFormatted: String = ""
    ): String {
        val sb = StringBuilder()
        sb.appendLine("BEGIN:VEVENT")
        sb.appendLine("SUMMARY:${title.trim()}")
        if (location.isNotBlank()) sb.appendLine("LOCATION:${location.trim()}")
        if (description.isNotBlank()) sb.appendLine("DESCRIPTION:${description.trim()}")
        if (dtStartUtcFormatted.isNotBlank()) sb.appendLine("DTSTART:$dtStartUtcFormatted")
        if (dtEndUtcFormatted.isNotBlank()) sb.appendLine("DTEND:$dtEndUtcFormatted")
        sb.append("END:VEVENT")
        return sb.toString()
    }

    private fun escapeSpecial(text: String): String {
        return text.replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace(":", "\\:")
    }

    private fun urlEncode(text: String): String {
        return try {
            URLEncoder.encode(text, StandardCharsets.UTF_8.name())
        } catch (e: Exception) {
            text
        }
    }
}
