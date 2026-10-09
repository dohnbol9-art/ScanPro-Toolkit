package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.local.SettingsDataStore
import com.example.data.repository.ScanHistoryRepository
import com.example.monetization.AdManager
import com.example.monetization.BillingManager

class ScanProApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: ScanHistoryRepository
        private set

    lateinit var settingsDataStore: SettingsDataStore
        private set

    lateinit var adManager: AdManager
        private set

    lateinit var billingManager: BillingManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.getInstance(this)
        repository = ScanHistoryRepository(database.scanRecordDao())
        settingsDataStore = SettingsDataStore(this)
        adManager = AdManager(this)
        billingManager = BillingManager(this)
    }

    companion object {
        lateinit var instance: ScanProApp
            private set
    }
}
