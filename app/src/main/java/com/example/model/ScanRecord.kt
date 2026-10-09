package com.example.model

data class ScanRecord(
    val id: Long = 0L,
    val content: String,
    val format: BarcodeFormatType,
    val contentType: ParsedContentType,
    val title: String,
    val domain: String? = null,
    val metadata: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val isGenerated: Boolean = false
)

data class BatchScanItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val content: String,
    val format: BarcodeFormatType,
    val timestamp: Long = System.currentTimeMillis()
)
