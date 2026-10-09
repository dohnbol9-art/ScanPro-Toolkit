package com.example.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ScanProApp
import com.example.export.ExportManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ScanProApp
    private val settings = app.settingsDataStore
    private val repository = app.repository
    private val billing = app.billingManager

    val isProUser: StateFlow<Boolean> = billing.isProUser

    val themeMode: StateFlow<String> = settings.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "SYSTEM")

    val scanVibration: StateFlow<Boolean> = settings.scanVibration
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val scanBeep: StateFlow<Boolean> = settings.scanBeep
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val autoCopy: StateFlow<Boolean> = settings.autoCopy
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val batchMode: StateFlow<Boolean> = settings.batchMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val safeUrlPreview: StateFlow<Boolean> = settings.safeUrlPreview
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val blockDangerousSchemes: StateFlow<Boolean> = settings.blockDangerousSchemes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val defaultEcc: StateFlow<String> = settings.defaultEcc
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "H")

    val defaultResolution: StateFlow<Int> = settings.defaultResolution
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1024)

    fun toggleVibration(enabled: Boolean) = viewModelScope.launch { settings.setScanVibration(enabled) }
    fun toggleBeep(enabled: Boolean) = viewModelScope.launch { settings.setScanBeep(enabled) }
    fun toggleAutoCopy(enabled: Boolean) = viewModelScope.launch { settings.setAutoCopy(enabled) }
    fun toggleBatchMode(enabled: Boolean) = viewModelScope.launch { settings.setBatchMode(enabled) }
    fun toggleSafeUrlPreview(enabled: Boolean) = viewModelScope.launch { settings.setSafeUrlPreview(enabled) }
    fun toggleBlockDangerous(enabled: Boolean) = viewModelScope.launch { settings.setBlockDangerousSchemes(enabled) }
    fun setDefaultEcc(ecc: String) = viewModelScope.launch { settings.setDefaultEcc(ecc) }
    fun setDefaultResolution(res: Int) = viewModelScope.launch { settings.setDefaultResolution(res) }

    fun upgradeToPro(onResult: (Boolean, String) -> Unit) {
        billing.purchaseProduct("scanpro_pro_lifetime", onResult)
    }

    fun restorePurchases(onResult: (Boolean, String) -> Unit) {
        billing.restorePurchases(onResult)
    }

    fun clearHistoryVault(onCleared: () -> Unit) {
        viewModelScope.launch {
            repository.clearAll()
            onCleared()
        }
    }

    fun exportDatabaseCsv(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val allRecords = repository.allRecords.first()
            val res = ExportManager.exportHistoryToCsv(app.applicationContext, allRecords)
            res.onSuccess {
                onResult(true, "Database exported (${allRecords.size} records)")
            }.onFailure { err ->
                onResult(false, err.localizedMessage ?: "Export failed")
            }
        }
    }
}
