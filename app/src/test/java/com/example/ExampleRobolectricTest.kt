package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.ScanRecordEntity
import com.example.model.BarcodeFormatType
import com.example.model.ParsedContentType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var database: AppDatabase

    @Before
    fun initDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun verifyAppNameStringResource() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ScanPro Toolkit", appName)
    }

    @Test
    fun verifyRoomDatabaseInsertAndQuery() = runBlocking {
        val dao = database.scanRecordDao()
        val entity = ScanRecordEntity(
            content = "https://github.com",
            format = BarcodeFormatType.QR_CODE.name,
            contentType = ParsedContentType.URL.name,
            title = "github.com",
            domain = "github.com",
            metadata = "18 Bytes",
            createdAt = System.currentTimeMillis(),
            isFavorite = false,
            isGenerated = false
        )

        val id = dao.insertRecord(entity)
        assertTrue(id > 0)

        val retrieved = dao.getRecordById(id)
        assertNotNull(retrieved)
        assertEquals("https://github.com", retrieved?.content)
        assertEquals(BarcodeFormatType.QR_CODE, retrieved?.toDomain()?.format)

        // Test favorite toggle
        dao.setFavorite(id, true)
        val favs = dao.getFavoriteRecords().first()
        assertEquals(1, favs.size)
        assertEquals(id, favs[0].id)
    }
}
