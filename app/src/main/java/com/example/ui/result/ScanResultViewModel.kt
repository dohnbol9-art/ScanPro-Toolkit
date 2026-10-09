package com.example.ui.result

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ScanProApp
import com.example.model.ParsedContentType
import com.example.model.ScanRecord
import com.example.model.UrlSafetyVerdict
import com.example.security.UrlSafetyChecker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ScanResultViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ScanProApp
    private val repository = app.repository

    private val _record = MutableStateFlow<ScanRecord?>(null)
    val record: StateFlow<ScanRecord?> = _record.asStateFlow()

    private val _safetyVerdict = MutableStateFlow<UrlSafetyVerdict?>(null)
    val safetyVerdict: StateFlow<UrlSafetyVerdict?> = _safetyVerdict.asStateFlow()

    fun loadRecord(id: Long) {
        viewModelScope.launch {
            val loaded = repository.getRecordById(id)
            _record.value = loaded
            if (loaded != null && loaded.contentType == ParsedContentType.URL) {
                _safetyVerdict.value = UrlSafetyChecker.evaluate(loaded.content)
            } else {
                _safetyVerdict.value = null
            }
        }
    }

    fun toggleFavorite() {
        val current = _record.value ?: return
        viewModelScope.launch {
            repository.toggleFavorite(current.id, current.isFavorite)
            _record.value = current.copy(isFavorite = !current.isFavorite)
        }
    }

    fun deleteRecord(onDeleted: () -> Unit) {
        val current = _record.value ?: return
        viewModelScope.launch {
            repository.delete(current.id)
            onDeleted()
        }
    }
}
