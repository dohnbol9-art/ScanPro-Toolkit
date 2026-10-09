package com.example.data.repository

import com.example.data.local.ScanRecordDao
import com.example.data.local.ScanRecordEntity
import com.example.model.ScanRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ScanHistoryRepository(private val dao: ScanRecordDao) {

    val allRecords: Flow<List<ScanRecord>> = dao.getAllRecords().map { list -> list.map { it.toDomain() } }
    val scannedRecords: Flow<List<ScanRecord>> = dao.getScannedRecords().map { list -> list.map { it.toDomain() } }
    val generatedRecords: Flow<List<ScanRecord>> = dao.getGeneratedRecords().map { list -> list.map { it.toDomain() } }
    val favoriteRecords: Flow<List<ScanRecord>> = dao.getFavoriteRecords().map { list -> list.map { it.toDomain() } }
    
    val totalCount: Flow<Int> = dao.getRecordCount()
    val scannedCount: Flow<Int> = dao.getScannedCount()
    val generatedCount: Flow<Int> = dao.getGeneratedCount()

    fun getRecentScans(limit: Int = 5): Flow<List<ScanRecord>> {
        return dao.getRecentRecords(limit).map { list -> list.map { it.toDomain() } }
    }

    suspend fun getRecordById(id: Long): ScanRecord? {
        return dao.getRecordById(id)?.toDomain()
    }

    fun search(query: String): Flow<List<ScanRecord>> {
        return dao.searchRecords(query).map { list -> list.map { it.toDomain() } }
    }

    suspend fun insert(record: ScanRecord): Long {
        return dao.insertRecord(ScanRecordEntity.fromDomain(record))
    }

    suspend fun insertBatch(records: List<ScanRecord>) {
        dao.insertAll(records.map { ScanRecordEntity.fromDomain(it) })
    }

    suspend fun toggleFavorite(id: Long, currentIsFav: Boolean) {
        dao.setFavorite(id, !currentIsFav)
    }

    suspend fun delete(id: Long) {
        dao.deleteById(id)
    }

    suspend fun deleteMultiple(ids: List<Long>) {
        dao.deleteMultiple(ids)
    }

    suspend fun clearAll() {
        dao.clearAll()
    }

    suspend fun purgeOld(days: Int) {
        if (days > 0) {
            val cutoff = System.currentTimeMillis() - (days.toLong() * 24L * 60L * 60L * 1000L)
            dao.purgeOldRecords(cutoff)
        }
    }
}
