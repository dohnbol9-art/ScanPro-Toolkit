package com.example.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ScanProApp
import com.example.export.ExportManager
import com.example.model.ScanRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ScanProApp
    private val repository = app.repository

    val searchQuery = MutableStateFlow("")
    val activeFilter = MutableStateFlow("all") // all, scanned, generated, favorites

    val isMultiSelectMode = MutableStateFlow(false)
    val selectedIds = MutableStateFlow<Set<Long>>(emptySet())

    val records: StateFlow<List<ScanRecord>> = combine(
        searchQuery,
        activeFilter
    ) { query, filter ->
        query to filter
    }.flatMapLatest { (query, filter) ->
        if (query.isNotBlank()) {
            repository.search(query)
        } else {
            when (filter) {
                "scanned" -> repository.scannedRecords
                "generated" -> repository.generatedRecords
                "favorites" -> repository.favoriteRecords
                else -> repository.allRecords
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalVaultCount: StateFlow<Int> = repository.totalCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun setFilter(filter: String) {
        activeFilter.value = filter
    }

    fun setSearch(query: String) {
        searchQuery.value = query
    }

    fun toggleFavorite(record: ScanRecord) {
        viewModelScope.launch {
            repository.toggleFavorite(record.id, record.isFavorite)
        }
    }

    fun deleteRecord(id: Long) {
        viewModelScope.launch {
            repository.delete(id)
        }
    }

    fun toggleSelection(id: Long) {
        val current = selectedIds.value
        if (current.contains(id)) {
            selectedIds.value = current - id
        } else {
            selectedIds.value = current + id
        }
        if (selectedIds.value.isEmpty()) {
            isMultiSelectMode.value = false
        }
    }

    fun deleteSelected() {
        viewModelScope.launch {
            repository.deleteMultiple(selectedIds.value.toList())
            selectedIds.value = emptySet()
            isMultiSelectMode.value = false
        }
    }

    fun clearVault() {
        viewModelScope.launch {
            repository.clearAll()
            selectedIds.value = emptySet()
            isMultiSelectMode.value = false
        }
    }

    fun exportToCsv(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val list = records.value
            if (list.isEmpty()) {
                onResult(false, "Vault is empty. Nothing to export.")
                return@launch
            }
            val res = ExportManager.exportHistoryToCsv(app.applicationContext, list)
            res.onSuccess {
                onResult(true, "Exported ${list.size} records to CSV")
            }.onFailure { err ->
                onResult(false, err.localizedMessage ?: "Export failed")
            }
        }
    }
}
