package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.model.BarcodeFormatType
import com.example.model.ParsedContentType
import com.example.model.ScanRecord

@Entity(
    tableName = "scan_records",
    indices = [
        Index(value = ["createdAt"]),
        Index(value = ["isFavorite"]),
        Index(value = ["isGenerated"]),
        Index(value = ["contentType"])
    ]
)
data class ScanRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val content: String,
    val format: String, // BarcodeFormatType name
    val contentType: String, // ParsedContentType name
    val title: String,
    val domain: String? = null,
    val metadata: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val isGenerated: Boolean = false
) {
    fun toDomain(): ScanRecord {
        val parsedFormat = runCatching { BarcodeFormatType.valueOf(format) }.getOrDefault(BarcodeFormatType.UNKNOWN)
        val parsedType = runCatching { ParsedContentType.valueOf(contentType) }.getOrDefault(ParsedContentType.UNKNOWN)
        return ScanRecord(
            id = id,
            content = content,
            format = parsedFormat,
            contentType = parsedType,
            title = title,
            domain = domain,
            metadata = metadata,
            createdAt = createdAt,
            isFavorite = isFavorite,
            isGenerated = isGenerated
        )
    }

    companion object {
        fun fromDomain(record: ScanRecord): ScanRecordEntity {
            return ScanRecordEntity(
                id = record.id,
                content = record.content,
                format = record.format.name,
                contentType = record.contentType.name,
                title = record.title,
                domain = record.domain,
                metadata = record.metadata,
                createdAt = record.createdAt,
                isFavorite = record.isFavorite,
                isGenerated = record.isGenerated
            )
        }
    }
}
