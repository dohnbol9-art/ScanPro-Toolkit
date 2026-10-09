package com.example.ui.create

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.ParsedContentType
import com.example.ui.components.ScanProTopBar
import com.example.ui.navigation.Screen

@Composable
fun CreateQrScreen(
    viewModel: CreateQrViewModel,
    initialCategory: String?,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LaunchedEffect(initialCategory) {
        if (!initialCategory.isNullOrBlank() && initialCategory != "{category}") {
            val cat = runCatching { ParsedContentType.valueOf(initialCategory.uppercase()) }.getOrNull()
            if (cat != null) {
                viewModel.setCategory(cat)
            }
        }
    }

    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val generatedBitmap by viewModel.generatedBitmap.collectAsStateWithLifecycle()
    val validationStatus by viewModel.validationStatus.collectAsStateWithLifecycle()

    val fgColor by viewModel.fgColor.collectAsStateWithLifecycle()
    val eccLevel by viewModel.eccLevel.collectAsStateWithLifecycle()
    val resolutionSize by viewModel.resolutionSize.collectAsStateWithLifecycle()

    // Form inputs
    val urlInput by viewModel.urlInput.collectAsStateWithLifecycle()
    val textInput by viewModel.textInput.collectAsStateWithLifecycle()
    val wifiSsid by viewModel.wifiSsid.collectAsStateWithLifecycle()
    val wifiPassword by viewModel.wifiPassword.collectAsStateWithLifecycle()
    val wifiAuthType by viewModel.wifiAuthType.collectAsStateWithLifecycle()
    val wifiIsHidden by viewModel.wifiIsHidden.collectAsStateWithLifecycle()

    val contactName by viewModel.contactName.collectAsStateWithLifecycle()
    val contactOrg by viewModel.contactOrg.collectAsStateWithLifecycle()
    val contactPhone by viewModel.contactPhone.collectAsStateWithLifecycle()
    val contactEmail by viewModel.contactEmail.collectAsStateWithLifecycle()
    val contactWebsite by viewModel.contactWebsite.collectAsStateWithLifecycle()

    val phoneNumber by viewModel.phoneNumber.collectAsStateWithLifecycle()
    val emailRecipient by viewModel.emailRecipient.collectAsStateWithLifecycle()
    val emailSubject by viewModel.emailSubject.collectAsStateWithLifecycle()
    val emailBody by viewModel.emailBody.collectAsStateWithLifecycle()

    val smsNumber by viewModel.smsNumber.collectAsStateWithLifecycle()
    val smsBody by viewModel.smsBody.collectAsStateWithLifecycle()
    val geoLat by viewModel.geoLat.collectAsStateWithLifecycle()
    val geoLon by viewModel.geoLon.collectAsStateWithLifecycle()
    val geoLabel by viewModel.geoLabel.collectAsStateWithLifecycle()

    val eventTitle by viewModel.eventTitle.collectAsStateWithLifecycle()
    val eventLocation by viewModel.eventLocation.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ScanProTopBar(
            subtitle = "Create QR",
            onHomeClick = { onNavigate(Screen.Home.route) },
            onBackClick = onBack,
            onFlashlightClick = { onNavigate(Screen.Scan.defaultRoute) },
            onSettingsClick = { onNavigate(Screen.Settings.route) },
            onProfileClick = { onNavigate(Screen.Settings.route) }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("create_qr_screen_scroll"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Engine Status Ribbon
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Engine Status",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "QR ENGINE READY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 10.sp
                                )
                            )
                        }

                        Text(
                            text = "ISO/IEC 18004 COMPLIANT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 9.sp
                            )
                        )
                    }
                }
            }

            // 2. Category Tab Strip
            item {
                CategoryTabsStrip(
                    selectedCategory = selectedCategory,
                    onCategorySelect = { viewModel.setCategory(it) }
                )
            }

            // 3. Live Real-time QR Code Canvas Viewport
            item {
                LiveQrCanvasCard(
                    bitmap = generatedBitmap,
                    validationStatus = validationStatus
                )
            }

            // 4. Category-Specific Input Fields
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "${selectedCategory.label.uppercase()} CONFIGURATION",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        )

                        when (selectedCategory) {
                            ParsedContentType.URL -> {
                                ScanProInputField(
                                    label = "Website URL",
                                    value = urlInput,
                                    onValueChange = {
                                        viewModel.urlInput.value = it
                                        viewModel.regenerateQr()
                                    },
                                    placeholder = "https://example.com"
                                )
                            }
                            ParsedContentType.WIFI -> {
                                ScanProInputField(
                                    label = "Network Name (SSID)",
                                    value = wifiSsid,
                                    onValueChange = {
                                        viewModel.wifiSsid.value = it
                                        viewModel.regenerateQr()
                                    }
                                )
                                ScanProInputField(
                                    label = "Password",
                                    value = wifiPassword,
                                    onValueChange = {
                                        viewModel.wifiPassword.value = it
                                        viewModel.regenerateQr()
                                    }
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Hidden Network", style = MaterialTheme.typography.bodySmall)
                                    Switch(
                                        checked = wifiIsHidden,
                                        onCheckedChange = {
                                            viewModel.wifiIsHidden.value = it
                                            viewModel.regenerateQr()
                                        },
                                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.secondaryContainer)
                                    )
                                }
                            }
                            ParsedContentType.CONTACT -> {
                                ScanProInputField(
                                    label = "Full Name",
                                    value = contactName,
                                    onValueChange = {
                                        viewModel.contactName.value = it
                                        viewModel.regenerateQr()
                                    }
                                )
                                ScanProInputField(
                                    label = "Phone",
                                    value = contactPhone,
                                    onValueChange = {
                                        viewModel.contactPhone.value = it
                                        viewModel.regenerateQr()
                                    },
                                    keyboardType = KeyboardType.Phone
                                )
                                ScanProInputField(
                                    label = "Email",
                                    value = contactEmail,
                                    onValueChange = {
                                        viewModel.contactEmail.value = it
                                        viewModel.regenerateQr()
                                    },
                                    keyboardType = KeyboardType.Email
                                )
                                ScanProInputField(
                                    label = "Organization",
                                    value = contactOrg,
                                    onValueChange = {
                                        viewModel.contactOrg.value = it
                                        viewModel.regenerateQr()
                                    }
                                )
                            }
                            ParsedContentType.PHONE -> {
                                ScanProInputField(
                                    label = "Phone Number",
                                    value = phoneNumber,
                                    onValueChange = {
                                        viewModel.phoneNumber.value = it
                                        viewModel.regenerateQr()
                                    },
                                    keyboardType = KeyboardType.Phone
                                )
                            }
                            ParsedContentType.EMAIL -> {
                                ScanProInputField(
                                    label = "Recipient Email",
                                    value = emailRecipient,
                                    onValueChange = {
                                        viewModel.emailRecipient.value = it
                                        viewModel.regenerateQr()
                                    },
                                    keyboardType = KeyboardType.Email
                                )
                                ScanProInputField(
                                    label = "Subject",
                                    value = emailSubject,
                                    onValueChange = {
                                        viewModel.emailSubject.value = it
                                        viewModel.regenerateQr()
                                    }
                                )
                                ScanProInputField(
                                    label = "Message Body",
                                    value = emailBody,
                                    onValueChange = {
                                        viewModel.emailBody.value = it
                                        viewModel.regenerateQr()
                                    }
                                )
                            }
                            ParsedContentType.SMS -> {
                                ScanProInputField(
                                    label = "Phone Number",
                                    value = smsNumber,
                                    onValueChange = {
                                        viewModel.smsNumber.value = it
                                        viewModel.regenerateQr()
                                    },
                                    keyboardType = KeyboardType.Phone
                                )
                                ScanProInputField(
                                    label = "SMS Message",
                                    value = smsBody,
                                    onValueChange = {
                                        viewModel.smsBody.value = it
                                        viewModel.regenerateQr()
                                    }
                                )
                            }
                            ParsedContentType.LOCATION -> {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ScanProInputField(
                                        label = "Latitude",
                                        value = geoLat,
                                        onValueChange = {
                                            viewModel.geoLat.value = it
                                            viewModel.regenerateQr()
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                    ScanProInputField(
                                        label = "Longitude",
                                        value = geoLon,
                                        onValueChange = {
                                            viewModel.geoLon.value = it
                                            viewModel.regenerateQr()
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                ScanProInputField(
                                    label = "Location Label",
                                    value = geoLabel,
                                    onValueChange = {
                                        viewModel.geoLabel.value = it
                                        viewModel.regenerateQr()
                                    }
                                )
                            }
                            ParsedContentType.CALENDAR -> {
                                ScanProInputField(
                                    label = "Event Title",
                                    value = eventTitle,
                                    onValueChange = {
                                        viewModel.eventTitle.value = it
                                        viewModel.regenerateQr()
                                    }
                                )
                                ScanProInputField(
                                    label = "Location",
                                    value = eventLocation,
                                    onValueChange = {
                                        viewModel.eventLocation.value = it
                                        viewModel.regenerateQr()
                                    }
                                )
                            }
                            else -> {
                                ScanProInputField(
                                    label = "Text Content",
                                    value = textInput,
                                    onValueChange = {
                                        viewModel.textInput.value = it
                                        viewModel.regenerateQr()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 5. Engine Tuning (Color Palette, ECC Level, Resolution)
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "STYLING & ENGINE TUNING",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        )

                        // Color Palette Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Foreground Color", style = MaterialTheme.typography.bodySmall)

                            val palette = listOf(
                                0xFFBAC3FF.toInt(), // Electric Indigo
                                0xFF00F2D1.toInt(), // Laser Cyan
                                0xFFFFFFFF.toInt(), // Pure White
                                0xFF4CAF50.toInt(), // Emerald
                                0xFFFFD54F.toInt()  // Amber
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                for (c in palette) {
                                    val isSelected = fgColor == c
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(Color(c))
                                            .border(
                                                2.dp,
                                                if (isSelected) Color.White else Color.Transparent,
                                                CircleShape
                                            )
                                            .clickable { viewModel.setFgColor(c) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = if (c == 0xFFFFFFFF.toInt()) Color.Black else Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ECC Level Selector (L, M, Q, H)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Error Correction (ECC)", style = MaterialTheme.typography.bodySmall)
                                Text("Higher ECC enables logo overlay", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("L" to "7%", "M" to "15%", "Q" to "25%", "H" to "30%").forEach { (level, pct) ->
                                    val isSelected = eccLevel == level
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
                                            .clickable { viewModel.setEcc(level) }
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$level ($pct)",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                                                fontSize = 10.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // Export Resolution Presets (512, 1024, 2048 UHD)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Export Resolution", style = MaterialTheme.typography.bodySmall)

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(512 to "512px", 1024 to "1024px", 2048 to "2048px (UHD)").forEach { (res, label) ->
                                    val isSelected = resolutionSize == res
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
                                            .clickable { viewModel.setResolution(res) }
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                fontSize = 10.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 6. Action Stack: Save & Share Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.saveToGallery { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                if (success) {
                                    val activity = context as? android.app.Activity
                                    com.example.ScanProApp.instance.adManager.recordScanAction()
                                    com.example.ScanProApp.instance.adManager.showInterstitial(activity)
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("create_save_gallery_btn"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Save to Gallery")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Save to Gallery",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.shareQr { success, msg ->
                                if (!success) Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("create_share_code_btn"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share Code")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Code", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryTabsStrip(
    selectedCategory: ParsedContentType,
    onCategorySelect: (ParsedContentType) -> Unit
) {
    val categories = listOf(
        ParsedContentType.URL to ("Web URL" to Icons.Default.Link),
        ParsedContentType.WIFI to ("Wi-Fi" to Icons.Default.Wifi),
        ParsedContentType.CONTACT to ("Contact" to Icons.Default.AccountBox),
        ParsedContentType.TEXT to ("Text" to Icons.Default.Notes),
        ParsedContentType.PHONE to ("Phone" to Icons.Default.Phone),
        ParsedContentType.EMAIL to ("Email" to Icons.Default.Email),
        ParsedContentType.SMS to ("SMS" to Icons.Default.Sms),
        ParsedContentType.LOCATION to ("Location" to Icons.Default.LocationOn),
        ParsedContentType.CALENDAR to ("Calendar" to Icons.Default.CalendarMonth)
    )

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { (type, pair) ->
            val (label, icon) = pair
            val isSelected = selectedCategory == type
            Surface(
                color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onCategorySelect(type) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveQrCanvasCard(
    bitmap: android.graphics.Bitmap?,
    validationStatus: String
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Viewport frame with corner accents
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF0C0E14))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(18.dp))
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Generated QR Code",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                            .testTag("create_live_qr_image")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Validation Status Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Status",
                    tint = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = validationStatus,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun ScanProInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        placeholder = if (placeholder.isNotBlank()) { { Text(placeholder) } } else null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.secondaryContainer,
            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        )
    )
}
