package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanRecordDao {

    @Query("SELECT * FROM scan_records ORDER BY createdAt DESC")
    fun getAllRecords(): Flow<List<ScanRecordEntity>>

    @Query("SELECT * FROM scan_records WHERE isGenerated = 0 ORDER BY createdAt DESC")
    fun getScannedRecords(): Flow<List<ScanRecordEntity>>

    @Query("SELECT * FROM scan_records WHERE isGenerated = 1 ORDER BY createdAt DESC")
    fun getGeneratedRecords(): Flow<List<ScanRecordEntity>>

    @Query("SELECT * FROM scan_records WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteRecords(): Flow<List<ScanRecordEntity>>

    @Query("SELECT * FROM scan_records ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentRecords(limit: Int = 10): Flow<List<ScanRecordEntity>>

    @Query("SELECT * FROM scan_records WHERE id = :id LIMIT 1")
    suspend fun getRecordById(id: Long): ScanRecordEntity?

    @Query("""
        SELECT * FROM scan_records 
        WHERE content LIKE '%' || :query || '%' 
           OR title LIKE '%' || :query || '%'
           OR (domain IS NOT NULL AND domain LIKE '%' || :query || '%')
        ORDER BY createdAt DESC
    """)
    fun searchRecords(query: String): Flow<List<ScanRecordEntity>>

    @Query("SELECT COUNT(*) FROM scan_records")
    fun getRecordCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM scan_records WHERE isGenerated = 0")
    fun getScannedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM scan_records WHERE isGenerated = 1")
    fun getGeneratedCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: ScanRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<ScanRecordEntity>)

    @Update
    suspend fun updateRecord(record: ScanRecordEntity)

    @Query("UPDATE scan_records SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    @Query("DELETE FROM scan_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM scan_records WHERE id IN (:ids)")
    suspend fun deleteMultiple(ids: List<Long>)

    @Query("DELETE FROM scan_records")
    suspend fun clearAll()

    @Query("DELETE FROM scan_records WHERE createdAt < :cutoffTimestamp AND isFavorite = 0")
    suspend fun purgeOldRecords(cutoffTimestamp: Long)
}
