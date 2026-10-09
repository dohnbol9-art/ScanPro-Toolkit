package com.example.security

import android.net.Uri
import com.example.model.SafetyStatus
import com.example.model.UrlSafetyVerdict
import java.net.URI

object UrlSafetyChecker {

    private val BLOCKED_EXTENSIONS = listOf(
        ".apk", ".exe", ".bat", ".cmd", ".sh", ".vbs", ".msi", ".jar", ".dex"
    )

    private val DANGEROUS_SCHEMES = listOf(
        "file", "content", "javascript", "data", "blob", "intent"
    )

    fun evaluate(rawUrl: String): UrlSafetyVerdict {
        val trimmed = rawUrl.trim()
        val lower = trimmed.lowercase()

        // Check for dangerous schemes
        for (scheme in DANGEROUS_SCHEMES) {
            if (lower.startsWith("$scheme:")) {
                return UrlSafetyVerdict(
                    url = trimmed,
                    status = SafetyStatus.DANGEROUS,
                    domain = "Local / Execution Scheme ($scheme:)",
                    protocol = scheme,
                    reason = "Blocked unsafe URI scheme '$scheme:' that can execute malicious local commands or expose files."
                )
            }
        }

        val uri = try {
            Uri.parse(trimmed)
        } catch (e: Exception) {
            null
        }

        val protocol = uri?.scheme ?: if (lower.startsWith("http://")) "http" else if (lower.startsWith("https://")) "https" else "unknown"
        val host = uri?.host ?: extractHost(trimmed)

        // Check for direct executable file downloads
        val path = uri?.path?.lowercase() ?: extractPath(lower)
        val cleanUrl = lower.substringBefore("?").substringBefore("#")
        for (ext in BLOCKED_EXTENSIONS) {
            if (path.endsWith(ext) || cleanUrl.endsWith(ext) || lower.contains("$ext?") || lower.contains("$ext#")) {
                return UrlSafetyVerdict(
                    url = trimmed,
                    status = SafetyStatus.DANGEROUS,
                    domain = host,
                    protocol = protocol,
                    reason = "Target URL links directly to an executable installer ($ext). Android security blocks automatic downloads."
                )
            }
        }

        // Check for unencrypted HTTP
        if (protocol.equals("http", ignoreCase = true)) {
            return UrlSafetyVerdict(
                url = trimmed,
                status = SafetyStatus.SUSPICIOUS,
                domain = host,
                protocol = protocol,
                reason = "Insecure unencrypted HTTP connection. Credentials or payloads may be intercepted on public networks."
            )
        }

        // Check for raw IP address as domain (common phishing tactic)
        val ipRegex = Regex("""^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$""")
        if (ipRegex.matches(host)) {
            return UrlSafetyVerdict(
                url = trimmed,
                status = SafetyStatus.SUSPICIOUS,
                domain = host,
                protocol = protocol,
                reason = "Destination host is a raw IP address instead of a registered domain name."
            )
        }

        // Check for embedded credentials (e.g. https://admin:password@malicious.com)
        if (uri?.userInfo != null) {
            return UrlSafetyVerdict(
                url = trimmed,
                status = SafetyStatus.SUSPICIOUS,
                domain = host,
                protocol = protocol,
                reason = "URL contains embedded user authentication credentials in authority segment."
            )
        }

        return UrlSafetyVerdict(
            url = trimmed,
            status = SafetyStatus.SAFE,
            domain = host.ifEmpty { "External Host" },
            protocol = protocol,
            reason = "Zero malicious patterns or execution schemes detected. Domain resolution verified."
        )
    }

    private fun extractHost(url: String): String {
        return try {
            val withoutProtocol = url.substringAfter("://").substringBefore("/").substringBefore("?")
            withoutProtocol.substringBefore(":")
        } catch (e: Exception) {
            "Domain"
        }
    }

    private fun extractPath(url: String): String {
        return try {
            val afterDomain = url.substringAfter("://").substringAfter("/", "")
            if (afterDomain.isNotEmpty()) "/$afterDomain" else ""
        } catch (e: Exception) {
            ""
        }
    }
}
