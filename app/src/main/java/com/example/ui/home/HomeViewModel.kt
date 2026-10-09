package com.example.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ScanProApp
import com.example.model.ScanRecord
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ScanProApp
    private val repository = app.repository
    private val billingManager = app.billingManager

    val storedCount: StateFlow<Int> = repository.totalCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val generatedCount: StateFlow<Int> = repository.generatedCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val recentScans: StateFlow<List<ScanRecord>> = repository.getRecentScans(5)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isProUser: StateFlow<Boolean> = billingManager.isProUser

    fun toggleFavorite(record: ScanRecord) {
        viewModelScope.launch {
            repository.toggleFavorite(record.id, record.isFavorite)
        }
    }

    fun saveImportedScan(record: ScanRecord, onSaved: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repository.insert(record)
            onSaved(id)
        }
    }
}
