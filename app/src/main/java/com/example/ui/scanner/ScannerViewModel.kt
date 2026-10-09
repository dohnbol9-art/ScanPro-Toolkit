package com.example.ui.scanner

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ScanProApp
import com.example.model.BarcodeFormatType
import com.example.model.ScanRecord
import com.example.scanner.ImageQrDecoder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ScannerViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ScanProApp
    private val repository = app.repository
    private val settings = app.settingsDataStore

    val isVibrationEnabled: StateFlow<Boolean> = settings.scanVibration
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isBeepEnabled: StateFlow<Boolean> = settings.scanBeep
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()

    private val _zoomRatio = MutableStateFlow(1.0f)
    val zoomRatio: StateFlow<Float> = _zoomRatio.asStateFlow()

    private val _isBatchMode = MutableStateFlow(false)
    val isBatchMode: StateFlow<Boolean> = _isBatchMode.asStateFlow()

    private val _batchItems = MutableStateFlow<List<ScanRecord>>(emptyList())
    val batchItems: StateFlow<List<ScanRecord>> = _batchItems.asStateFlow()

    private val _lastScannedRecord = MutableStateFlow<ScanRecord?>(null)
    val lastScannedRecord: StateFlow<ScanRecord?> = _lastScannedRecord.asStateFlow()

    fun setBatchMode(enabled: Boolean) {
        _isBatchMode.value = enabled
    }

    fun setTorchState(isOn: Boolean) {
        _isTorchOn.value = isOn
    }

    fun setZoom(zoom: Float) {
        _zoomRatio.value = zoom
    }

    fun onBarcodeDetected(record: ScanRecord, onSingleScanSaved: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repository.insert(record)
            val savedRecord = record.copy(id = id)
            _lastScannedRecord.value = savedRecord

            if (_isBatchMode.value) {
                _batchItems.value = _batchItems.value + savedRecord
            } else {
                onSingleScanSaved(id)
            }
        }
    }

    fun decodeFromGallery(uri: Uri, onSuccess: (Long) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = ImageQrDecoder.decodeFromUri(app.applicationContext, uri)
            result.onSuccess { record ->
                val id = repository.insert(record)
                onSuccess(id)
            }.onFailure { err ->
                onError(err.localizedMessage ?: "No barcode detected in selected image")
            }
        }
    }

    fun clearBatch() {
        _batchItems.value = emptyList()
    }
}
