package com.example.ui.result

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.ParsedContentType
import com.example.model.SafetyStatus
import com.example.security.IntentLauncher
import com.example.ui.components.BannerAdView
import com.example.ui.navigation.Screen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ScanResultDetailScreen(
    recordId: Long,
    viewModel: ScanResultViewModel,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LaunchedEffect(recordId) {
        viewModel.loadRecord(recordId)
    }

    val record by viewModel.record.collectAsStateWithLifecycle()
    val safetyVerdict by viewModel.safetyVerdict.collectAsStateWithLifecycle()
    var showRawBytes by remember { mutableStateOf(false) }

    if (record == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Text("Loading scan result...", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val rec = record!!

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Detail Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding(),
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Scan Result",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onNavigate(Screen.Home.route) },
                        modifier = Modifier.testTag("result_home_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Go Home",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = { viewModel.toggleFavorite() },
                        modifier = Modifier.testTag("result_fav_btn")
                    ) {
                        Icon(
                            imageVector = if (rec.isFavorite) Icons.Default.Star else Icons.Outlined.StarOutline,
                            contentDescription = "Favorite",
                            tint = if (rec.isFavorite) Color(0xFFFFD54F) else MaterialTheme.colorScheme.outline
                        )
                    }

                    IconButton(
                        onClick = {
                            viewModel.deleteRecord { onBack() }
                        },
                        modifier = Modifier.testTag("result_delete_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("scan_result_detail_scroll"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Live Decode Sub-Bar Status
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "DECODED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            )
                        }
                        Text(
                            text = rec.format.displayName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    Text(
                        text = SimpleDateFormat("MMM d, yyyy • HH:mm:ss", Locale.US).format(Date(rec.createdAt)),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            // 2. Security Assessment Card (Crucial for safe scanning of URLs / QR payloads)
            item {
                SecurityAssessmentCard(verdict = safetyVerdict, contentType = rec.contentType)
            }

            // 3. Decoded Value & Bitstream Card
            item {
                DecodedValueCard(
                    record = rec,
                    showRawBytes = showRawBytes,
                    onToggleRawBytes = { showRawBytes = !showRawBytes },
                    onCopy = { IntentLauncher.copyToClipboard(context, rec.content) }
                )
            }

            // 4. Primary Context Action Button (Always safe and user-initiated)
            item {
                PrimaryActionSection(
                    record = rec,
                    onOpenUrl = { IntentLauncher.openUrlSafely(context, rec.content) },
                    onConnectWifi = { IntentLauncher.openWifiSettings(context) },
                    onPromptCall = { IntentLauncher.promptCall(context, rec.content) },
                    onSendEmail = { IntentLauncher.sendEmail(context, rec.content) },
                    onSendSms = { IntentLauncher.sendSms(context, rec.content) },
                    onSearchProduct = { IntentLauncher.openUrlSafely(context, "https://www.google.com/search?q=${rec.content}") }
                )
            }

            // 5. Secondary Action Tools Grid
            item {
                SecondaryActionsGrid(
                    onCopy = { IntentLauncher.copyToClipboard(context, rec.content) },
                    onShare = { IntentLauncher.shareText(context, rec.content) },
                    onGenerateQr = { onNavigate(Screen.Create.createRoute(rec.contentType.name)) },
                    onRescan = {
                        val activity = context as? android.app.Activity
                        com.example.ScanProApp.instance.adManager.showInterstitial(activity) {
                            onNavigate(Screen.Scan.route)
                        }
                    }
                )
            }

            // 6. Technical Telemetry Details Card
            item {
                TelemetryTechnicalCard(record = rec)
            }
        }

        BannerAdView(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun SecurityAssessmentCard(verdict: com.example.model.UrlSafetyVerdict?, contentType: ParsedContentType) {
    val (status, title, reason, icon, color) = when {
        verdict != null -> {
            when (verdict.status) {
                SafetyStatus.SAFE -> Quintuple(
                    "SAFE PROTOCOL VERIFIED",
                    "Resolved: ${verdict.domain}",
                    verdict.reason,
                    Icons.Default.CheckCircle,
                    Color(0xFF00F2D1)
                )
                SafetyStatus.SUSPICIOUS -> Quintuple(
                    "SUSPICIOUS PROTOCOL / UNENCRYPTED",
                    verdict.domain,
                    verdict.reason,
                    Icons.Default.Warning,
                    Color(0xFFFFB74D)
                )
                SafetyStatus.DANGEROUS -> Quintuple(
                    "MALICIOUS / BLOCKED SCHEME",
                    verdict.domain,
                    verdict.reason,
                    Icons.Default.Security,
                    Color(0xFFFF5252)
                )
            }
        }
        contentType == ParsedContentType.WIFI -> Quintuple(
            "LOCAL ACCESS POINT VERIFIED",
            "Wi-Fi Network Configuration",
            "Contains network credentials. Never connect to untrusted networks.",
            Icons.Default.Wifi,
            MaterialTheme.colorScheme.secondary
        )
        else -> Quintuple(
            "CLEAN PAYLOAD INSPECTION",
            "Safe Client-Side Parsing",
            "Payload has no executable scripts. Automatic execution is disabled.",
            Icons.Default.CheckCircle,
            Color(0xFF00F2D1)
        )
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = status,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = status,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = color,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            Text(
                text = reason,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                ),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun DecodedValueCard(
    record: com.example.model.ScanRecord,
    showRawBytes: Boolean,
    onToggleRawBytes: () -> Unit,
    onCopy: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DECODED PAYLOAD",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                )

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onCopy)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Copy",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Code Display Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = record.content,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 20.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Raw bitstream accordion toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleRawBytes)
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (showRawBytes) "Hide Raw Bytes" else "Show Raw Byte Stream & Hex Dump",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Info",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }

            AnimatedVisibility(visible = showRawBytes) {
                val hexDump = record.content.toByteArray().joinToString(" ") { String.format("%02X", it) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = hexDump,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun PrimaryActionSection(
    record: com.example.model.ScanRecord,
    onOpenUrl: () -> Unit,
    onConnectWifi: () -> Unit,
    onPromptCall: () -> Unit,
    onSendEmail: () -> Unit,
    onSendSms: () -> Unit,
    onSearchProduct: () -> Unit
) {
    when (record.contentType) {
        ParsedContentType.URL -> {
            Button(
                onClick = onOpenUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("action_open_browser_btn"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.OpenInBrowser, contentDescription = "Open in Browser")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open in Browser", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
        }
        ParsedContentType.WIFI -> {
            Button(
                onClick = onConnectWifi,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("action_connect_wifi_btn"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Icon(Icons.Default.Wifi, contentDescription = "Connect to Wi-Fi", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Connect to Network", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer))
            }
        }
        ParsedContentType.PHONE -> {
            Button(
                onClick = onPromptCall,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("action_dial_phone_btn"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Call, contentDescription = "Dial")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Dial Number", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
        }
        ParsedContentType.EMAIL -> {
            Button(
                onClick = onSendEmail,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("action_send_email_btn"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Email, contentDescription = "Email")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Compose Email", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
        }
        ParsedContentType.SMS -> {
            Button(
                onClick = onSendSms,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Sms, contentDescription = "SMS")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Send SMS", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
        }
        ParsedContentType.PRODUCT -> {
            Button(
                onClick = onSearchProduct,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("action_search_product_btn"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Icon(Icons.Default.Search, contentDescription = "Product Search", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Search Product Lookup", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer))
            }
        }
        else -> {
            // Text or Generic
            Button(
                onClick = onOpenUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                Icon(Icons.Default.Search, contentDescription = "Search Web")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Search on Google", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface))
            }
        }
    }
}

@Composable
private fun SecondaryActionsGrid(
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onGenerateQr: () -> Unit,
    onRescan: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SecondaryActionButton(
            icon = Icons.Default.ContentCopy,
            label = "Copy",
            onClick = onCopy,
            modifier = Modifier.weight(1f)
        )
        SecondaryActionButton(
            icon = Icons.Default.Share,
            label = "Share",
            onClick = onShare,
            modifier = Modifier.weight(1f)
        )
        SecondaryActionButton(
            icon = Icons.Default.QrCode,
            label = "Create QR",
            onClick = onGenerateQr,
            modifier = Modifier.weight(1f)
        )
        SecondaryActionButton(
            icon = Icons.Default.Replay,
            label = "Re-Scan",
            onClick = onRescan,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SecondaryActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}

@Composable
private fun TelemetryTechnicalCard(record: com.example.model.ScanRecord) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "FORMAT & METADATA",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Barcode Format", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                Text(record.format.displayName, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurface))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Payload Length", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                Text("${record.content.length} characters (${record.content.toByteArray().size} Bytes)", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurface))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Engine", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                Text("ML Kit Vision", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary))
            }
        }
    }
}

private data class Quintuple<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)
