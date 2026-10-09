package com.example.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.BarcodeFormatType
import com.example.scanner.CameraScannerManager
import com.example.security.IntentLauncher
import com.example.ui.navigation.Screen

@Composable
fun ScannerScreen(
    viewModel: ScannerViewModel,
    initialMode: String?,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
        if (initialMode == "batch") {
            viewModel.setBatchMode(true)
        }
    }

    val isTorchOn by viewModel.isTorchOn.collectAsStateWithLifecycle()
    val zoomRatio by viewModel.zoomRatio.collectAsStateWithLifecycle()
    val isBatchMode by viewModel.isBatchMode.collectAsStateWithLifecycle()
    val batchItems by viewModel.batchItems.collectAsStateWithLifecycle()
    val isVibrationEnabled by viewModel.isVibrationEnabled.collectAsStateWithLifecycle()
    val isBeepEnabled by viewModel.isBeepEnabled.collectAsStateWithLifecycle()

    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    val scannerManager = remember {
        CameraScannerManager(context) { scannedRecord ->
            viewModel.onBarcodeDetected(scannedRecord) { savedId ->
                val activity = context as? android.app.Activity
                com.example.ScanProApp.instance.adManager.recordScanAction()
                com.example.ScanProApp.instance.adManager.showInterstitial(activity) {
                    onNavigate(Screen.ResultDetail.createRoute(savedId))
                }
            }
        }
    }

    DisposableEffect(Unit) {
        scannerManager.isVibrationEnabled = isVibrationEnabled
        scannerManager.isBeepEnabled = isBeepEnabled
        scannerManager.isBatchMode = isBatchMode

        onDispose {
            scannerManager.release()
        }
    }

    LaunchedEffect(isBatchMode) {
        scannerManager.isBatchMode = isBatchMode
    }

    // Android Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.decodeFromGallery(
                uri = uri,
                onSuccess = { id ->
                    val activity = context as? android.app.Activity
                    com.example.ScanProApp.instance.adManager.recordScanAction()
                    com.example.ScanProApp.instance.adManager.showInterstitial(activity) {
                        onNavigate(Screen.ResultDetail.createRoute(id))
                    }
                },
                onError = { err -> Toast.makeText(context, err, Toast.LENGTH_LONG).show() }
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasCameraPermission) {
            // Camera Preview
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                    }.also { pv ->
                        previewViewRef = pv
                        scannerManager.startCamera(lifecycleOwner, pv)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Reticle and Scanner Overlays
            ScannerOverlay(
                modifier = Modifier.fillMaxSize(),
                isBatchMode = isBatchMode,
                batchCount = batchItems.size
            )

            // Top Telemetry HUD Strip
            TopScannerHud(
                isTorchOn = isTorchOn,
                isBatchMode = isBatchMode,
                onTorchToggle = {
                    scannerManager.toggleTorch { newState ->
                        viewModel.setTorchState(newState)
                    }
                },
                onBatchModeToggle = { nextState ->
                    viewModel.setBatchMode(nextState)
                },
                onFlipCamera = {
                    previewViewRef?.let { pv ->
                        scannerManager.flipCamera(lifecycleOwner, pv)
                    }
                },
                onBack = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            )

            // Zoom Multiplier Pills
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ZoomPill(label = "1x", isSelected = zoomRatio == 1.0f) {
                        viewModel.setZoom(1.0f)
                        scannerManager.setZoom(1.0f)
                    }
                    ZoomPill(label = "2x", isSelected = zoomRatio == 2.0f) {
                        viewModel.setZoom(2.0f)
                        scannerManager.setZoom(2.0f)
                    }
                    ZoomPill(label = "5x", isSelected = zoomRatio == 5.0f) {
                        viewModel.setZoom(5.0f)
                        scannerManager.setZoom(5.0f)
                    }
                }
            }

            // Bottom Actions Bar
            BottomScannerControls(
                isBatchMode = isBatchMode,
                batchCount = batchItems.size,
                onPickImage = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onReviewBatch = {
                    if (batchItems.isNotEmpty()) {
                        onNavigate(Screen.History.route)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp, start = 20.dp, end = 20.dp)
            )

        } else {
            // Permission Denied Card
            CameraPermissionDeniedCard(
                onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                onOpenSettings = { IntentLauncher.openAppSettings(context) },
                onBack = onBack,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun TopScannerHud(
    isTorchOn: Boolean,
    isBatchMode: Boolean,
    onTorchToggle: () -> Unit,
    onBatchModeToggle: (Boolean) -> Unit,
    onFlipCamera: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Back Button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Mode Selector Pill (Single / Batch)
            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier.padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ModeTabItem(
                        label = "Single",
                        isSelected = !isBatchMode,
                        onClick = { onBatchModeToggle(false) }
                    )
                    ModeTabItem(
                        label = "Batch",
                        isSelected = isBatchMode,
                        onClick = { onBatchModeToggle(true) }
                    )
                }
            }

            // Action Buttons (Torch & Flip)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isTorchOn) MaterialTheme.colorScheme.secondaryContainer else Color.Black.copy(alpha = 0.5f))
                        .clickable(onClick = onTorchToggle)
                        .testTag("scanner_torch_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Torch",
                        tint = if (isTorchOn) MaterialTheme.colorScheme.onSecondaryContainer else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable(onClick = onFlipCamera),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Flip Camera",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Symbology Format Chips Carousel
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val formats = listOf(
                "QR CODE", "EAN-13", "DATA MATRIX", "CODE 128", "AZTEC", "PDF 417", "UPC-A"
            )
            items(formats) { fmt ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = fmt,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeTabItem(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else Color.White.copy(alpha = 0.75f),
                fontSize = 11.sp
            )
        )
    }
}

@Composable
private fun ZoomPill(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Black.copy(alpha = 0.6f))
            .border(1.dp, if (isSelected) Color.Transparent else Color.White.copy(alpha = 0.2f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else Color.White,
                fontSize = 11.sp
            )
        )
    }
}

@Composable
private fun ScannerOverlay(
    isBatchMode: Boolean,
    batchCount: Int,
    modifier: Modifier = Modifier
) {
    val laserTransition = rememberInfiniteTransition(label = "laser")
    val laserProgress by laserTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Central Reticle Box (280dp x 280dp)
        Box(
            modifier = Modifier
                .size(280.dp)
                .testTag("scanner_reticle_frame")
        ) {
            // Draw Reticle Corners & Laser Beam
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 5.dp.toPx()
                val cornerLength = 28.dp.toPx()
                val cyan = Color(0xFF00F2D1)

                // Top-Left Corner
                drawLine(cyan, Offset(0f, 0f), Offset(cornerLength, 0f), strokeWidth)
                drawLine(cyan, Offset(0f, 0f), Offset(0f, cornerLength), strokeWidth)

                // Top-Right Corner
                drawLine(cyan, Offset(size.width, 0f), Offset(size.width - cornerLength, 0f), strokeWidth)
                drawLine(cyan, Offset(size.width, 0f), Offset(size.width, cornerLength), strokeWidth)

                // Bottom-Left Corner
                drawLine(cyan, Offset(0f, size.height), Offset(cornerLength, size.height), strokeWidth)
                drawLine(cyan, Offset(0f, size.height), Offset(0f, size.height - cornerLength), strokeWidth)

                // Bottom-Right Corner
                drawLine(cyan, Offset(size.width, size.height), Offset(size.width - cornerLength, size.height), strokeWidth)
                drawLine(cyan, Offset(size.width, size.height), Offset(size.width, size.height - cornerLength), strokeWidth)

                // Oscillating Laser Beam
                val beamY = size.height * laserProgress
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0x0000F2D1),
                            Color(0xFF00F2D1),
                            Color(0xFFFFFFFF),
                            Color(0xFF00F2D1),
                            Color(0x0000F2D1)
                        )
                    ),
                    start = Offset(0f, beamY),
                    end = Offset(size.width, beamY),
                    strokeWidth = 3.dp.toPx()
                )
            }
        }

        // Instructional Tip Below Reticle
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 330.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (isBatchMode) "Batch Mode: Scan continuously ($batchCount queued)" else "Align QR or barcode inside frame",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun BottomScannerControls(
    isBatchMode: Boolean,
    batchCount: Int,
    onPickImage: () -> Unit,
    onReviewBatch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Import Image Button
        Surface(
            color = Color.Black.copy(alpha = 0.65f),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onPickImage)
                .testTag("scanner_pick_image_btn")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = "Scan Image",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Import Image",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    )
                )
            }
        }

        // Batch Queue Counter / Review button
        if (isBatchMode) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onReviewBatch)
                    .testTag("scanner_review_batch_btn")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ListAlt,
                        contentDescription = "Review Batch",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Review Batch ($batchCount)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraPermissionDeniedCard(
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .padding(24.dp)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Camera Permission Required",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Camera Permission Required",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "ScanPro Toolkit requires camera access to scan physical QR codes and retail barcodes in real-time. Camera frames are processed strictly on-device.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                ),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onRequestPermission,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Grant Camera Access", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                androidx.compose.material3.TextButton(onClick = onOpenSettings) {
                    Text("App Settings", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                androidx.compose.material3.TextButton(onClick = onBack) {
                    Text("Go Back", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
