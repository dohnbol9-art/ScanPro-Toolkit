package com.example.ui.create

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ScanProApp
import com.example.export.ExportManager
import com.example.generator.PayloadFormatters
import com.example.generator.QrCodeGenerator
import com.example.model.BarcodeFormatType
import com.example.model.ParsedContentType
import com.example.model.QrGenerationConfig
import com.example.model.ScanRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CreateQrViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ScanProApp
    private val repository = app.repository

    // Active category
    private val _selectedCategory = MutableStateFlow(ParsedContentType.URL)
    val selectedCategory: StateFlow<ParsedContentType> = _selectedCategory.asStateFlow()

    // Form inputs
    val urlInput = MutableStateFlow("https://google.com")
    val textInput = MutableStateFlow("Hello from ScanPro Toolkit")

    // Wi-Fi inputs
    val wifiSsid = MutableStateFlow("Office_Guest_5G")
    val wifiPassword = MutableStateFlow("SecurePass2026")
    val wifiAuthType = MutableStateFlow("WPA") // WPA, WEP, Open
    val wifiIsHidden = MutableStateFlow(false)

    // Contact inputs
    val contactName = MutableStateFlow("Alex Morgan")
    val contactOrg = MutableStateFlow("DeepMind")
    val contactPhone = MutableStateFlow("+14155552671")
    val contactEmail = MutableStateFlow("alex.morgan@example.com")
    val contactWebsite = MutableStateFlow("https://example.com")

    // Phone / Email / SMS / Geo
    val phoneNumber = MutableStateFlow("+14155552671")
    val emailRecipient = MutableStateFlow("support@example.com")
    val emailSubject = MutableStateFlow("Inquiry")
    val emailBody = MutableStateFlow("Hello team,")
    val smsNumber = MutableStateFlow("+14155552671")
    val smsBody = MutableStateFlow("Meeting confirmed.")
    val geoLat = MutableStateFlow("37.7749")
    val geoLon = MutableStateFlow("-122.4194")
    val geoLabel = MutableStateFlow("San Francisco HQ")
    val eventTitle = MutableStateFlow("Tech Summit 2026")
    val eventLocation = MutableStateFlow("Hall A")

    // Engine Customization
    val fgColor = MutableStateFlow(0xFFBAC3FF.toInt()) // Electric Indigo
    val bgColor = MutableStateFlow(0xFF0C0E14.toInt()) // Carbon Void
    val eccLevel = MutableStateFlow("H") // L, M, Q, H
    val resolutionSize = MutableStateFlow(1024)

    // Generated QR State
    private val _generatedBitmap = MutableStateFlow<Bitmap?>(null)
    val generatedBitmap: StateFlow<Bitmap?> = _generatedBitmap.asStateFlow()

    private val _validationStatus = MutableStateFlow("Readability: 100% (Optical Decode Verified)")
    val validationStatus: StateFlow<String> = _validationStatus.asStateFlow()

    init {
        regenerateQr()
    }

    fun setCategory(category: ParsedContentType) {
        _selectedCategory.value = category
        regenerateQr()
    }

    fun setEcc(level: String) {
        eccLevel.value = level
        regenerateQr()
    }

    fun setResolution(res: Int) {
        resolutionSize.value = res
        regenerateQr()
    }

    fun setFgColor(color: Int) {
        fgColor.value = color
        regenerateQr()
    }

    fun regenerateQr() {
        viewModelScope.launch(Dispatchers.Default) {
            val content = computeFormattedPayload()
            val config = QrGenerationConfig(
                content = content,
                contentType = _selectedCategory.value,
                size = resolutionSize.value,
                errorCorrectionLevel = eccLevel.value,
                foregroundColor = fgColor.value,
                backgroundColor = bgColor.value,
                margin = 2
            )
            val result = QrCodeGenerator.generateQrBitmap(config)
            _generatedBitmap.value = result.bitmap
            _validationStatus.value = result.validationMessage
        }
    }

    private fun computeFormattedPayload(): String {
        return when (_selectedCategory.value) {
            ParsedContentType.URL -> PayloadFormatters.formatUrl(urlInput.value)
            ParsedContentType.WIFI -> PayloadFormatters.formatWifi(
                ssid = wifiSsid.value,
                pass = wifiPassword.value,
                authType = wifiAuthType.value,
                isHidden = wifiIsHidden.value
            )
            ParsedContentType.CONTACT -> PayloadFormatters.formatVCard(
                name = contactName.value,
                organization = contactOrg.value,
                phone = contactPhone.value,
                email = contactEmail.value,
                website = contactWebsite.value
            )
            ParsedContentType.PHONE -> PayloadFormatters.formatPhone(phoneNumber.value)
            ParsedContentType.EMAIL -> PayloadFormatters.formatEmail(
                email = emailRecipient.value,
                subject = emailSubject.value,
                body = emailBody.value
            )
            ParsedContentType.SMS -> PayloadFormatters.formatSms(
                phone = smsNumber.value,
                message = smsBody.value
            )
            ParsedContentType.LOCATION -> PayloadFormatters.formatGeo(
                latitude = geoLat.value.toDoubleOrNull() ?: 0.0,
                longitude = geoLon.value.toDoubleOrNull() ?: 0.0,
                label = geoLabel.value
            )
            ParsedContentType.CALENDAR -> PayloadFormatters.formatCalendarEvent(
                title = eventTitle.value,
                location = eventLocation.value
            )
            else -> textInput.value
        }
    }

    fun saveToGallery(onComplete: (Boolean, String) -> Unit) {
        val bitmap = _generatedBitmap.value ?: return
        viewModelScope.launch {
            val res = ExportManager.saveQrToGallery(app.applicationContext, bitmap)
            res.onSuccess {
                saveToDatabase()
                onComplete(true, "Saved to Pictures/ScanPro")
            }.onFailure { err ->
                onComplete(false, err.localizedMessage ?: "Failed to save")
            }
        }
    }

    fun shareQr(onComplete: (Boolean, String) -> Unit) {
        val bitmap = _generatedBitmap.value ?: return
        viewModelScope.launch {
            val res = ExportManager.shareQrImage(app.applicationContext, bitmap)
            res.onSuccess {
                saveToDatabase()
                onComplete(true, "Sharesheet opened")
            }.onFailure { err ->
                onComplete(false, err.localizedMessage ?: "Failed to share")
            }
        }
    }

    private suspend fun saveToDatabase() {
        val content = computeFormattedPayload()
        val record = ScanRecord(
            content = content,
            format = BarcodeFormatType.QR_CODE,
            contentType = _selectedCategory.value,
            title = "Generated: ${_selectedCategory.value.label}",
            domain = if (_selectedCategory.value == ParsedContentType.URL) urlInput.value else null,
            metadata = "${resolutionSize.value}x${resolutionSize.value} • ECC ${eccLevel.value}",
            createdAt = System.currentTimeMillis(),
            isGenerated = true
        )
        repository.insert(record)
    }
}
