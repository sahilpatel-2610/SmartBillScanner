package com.example.smartbillscanner.ui.screens.camera

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.smartbillscanner.domain.model.BillItem
import com.example.smartbillscanner.domain.model.BillReceipt
import com.example.smartbillscanner.ocr.BillOcr
import com.example.smartbillscanner.ocr.CameraAnalyzer
import com.example.smartbillscanner.ocr.MlKitTextParser
import com.example.smartbillscanner.ocr.ParsedReceipt
import com.example.smartbillscanner.ui.components.GlassIconButton
import com.example.smartbillscanner.ui.components.NeonPrimaryButton
import com.example.smartbillscanner.ui.theme.*
import java.io.File
import java.util.concurrent.Executors

@Composable
fun ScannerScreen(
    onBillScanned: (BillReceipt) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    var isTorchOn by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var liveItemCount by remember { mutableStateOf(0) }
    var isProcessing by remember { mutableStateOf(false) }

    LaunchedEffect(isTorchOn, camera) {
        camera?.cameraControl?.enableTorch(isTorchOn)
    }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()
    }

    fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    // ---- everything below uses REAL data read from the photo, nothing is hard-coded ----

    fun handleParsed(parsed: ParsedReceipt) {
        isProcessing = false
        if (parsed.isEmpty) {
            toast("No bill items found in this photo. Use a clear, flat photo of a bill, or add items manually.")
            return
        }
        val store = parsed.storeName
        onBillScanned(
            BillReceipt(
                title = if (store.isNotBlank()) "Bill at $store" else "Scanned bill",
                storeName = store,
                date = parsed.dateMillis ?: System.currentTimeMillis(),
                items = parsed.items.mapIndexed { index, item ->
                    BillItem(
                        id = (index + 1).toLong(),
                        name = item.name,
                        price = item.price,
                        quantity = item.quantity
                    )
                },
                tax = parsed.tax,
                discount = parsed.discount,
                tip = parsed.tip,
                totalAmount = parsed.total
            )
        )
    }

    // tempFile = the photo we just took; it is deleted right after reading so no bill photo stays on the phone
    fun processUri(uri: Uri, tempFile: File? = null) {
        isProcessing = true
        BillOcr.read(
            context = context,
            uri = uri,
            onResult = {
                tempFile?.delete()
                handleParsed(it)
            },
            onError = {
                tempFile?.delete()
                isProcessing = false
                toast("Couldn't read this image. Please try again.")
            }
        )
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> if (uri != null) processUri(uri) }
    )

    fun capture() {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
            return
        }
        if (isProcessing) return
        isProcessing = true
        try {
            val file = File.createTempFile("bill_", ".jpg", context.cacheDir)
            imageCapture.takePicture(
                ImageCapture.OutputFileOptions.Builder(file).build(),
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        processUri(Uri.fromFile(file), file)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        file.delete()
                        isProcessing = false
                        toast("Couldn't capture the photo. Please try again.")
                    }
                }
            )
        } catch (e: Exception) {
            isProcessing = false
            toast("Camera is not ready yet. Please try again.")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                camera?.cameraControl?.enableTorch(false)
            } catch (e: Exception) {
                // camera already released
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    val executor = ContextCompat.getMainExecutor(ctx)

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val analyzer = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also {
                                it.setAnalyzer(
                                    Executors.newSingleThreadExecutor(),
                                    CameraAnalyzer(
                                        onTextDetected = { visionText ->
                                            liveItemCount = MlKitTextParser.countItems(visionText)
                                        }
                                    )
                                )
                            }

                        try {
                            cameraProvider.unbindAll()
                            camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageCapture,
                                analyzer
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, executor)

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Animated neon targeting overlay
            CameraOverlay()
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .background(AccentGold.copy(alpha = 0.10f), CircleShape)
                            .border(1.dp, AccentGold.copy(alpha = 0.28f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = AccentGold,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Camera access needed",
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Allow the camera to scan bills, or pick a bill photo from your gallery below.",
                        color = TextMuted,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    NeonPrimaryButton(
                        text = "Allow camera",
                        icon = Icons.Default.CameraAlt,
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                    )
                }
            }
        }

        // Top bar (below the status bar)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 8.dp, start = 20.dp, end = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassIconButton(
                icon = Icons.Default.ArrowBack,
                contentDescription = "Back",
                onClick = onNavigateBack,
                size = 44.dp
            )

            Text(
                text = "Scan bill",
                color = TextWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.2).sp
            )

            GlassIconButton(
                icon = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                contentDescription = "Flash",
                onClick = { isTorchOn = !isTorchOn },
                tint = if (isTorchOn) AccentGold else TextWhite,
                size = 44.dp
            )
        }

        // Live hint: how many item lines the camera currently sees
        if (hasCameraPermission && liveItemCount > 0 && !isProcessing) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 160.dp)
                    .background(SuccessGreen, RoundedCornerShape(50))
                    .padding(horizontal = 16.dp, vertical = 7.dp)
            ) {
                Text(
                    text = "✔  Found $liveItemCount items in bill",
                    color = BgDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        // Bottom controls on a soft scrim so they stay readable over any receipt
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE60A0A0C))))
                .navigationBarsPadding()
                .padding(top = 48.dp, bottom = 28.dp, start = 32.dp, end = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pick an existing bill photo
                SideAction(
                    icon = Icons.Default.PhotoLibrary,
                    label = "Gallery",
                    tint = AccentChampagne,
                    onClick = { if (!isProcessing) galleryLauncher.launch("image/*") }
                )

                // Shutter: takes a full-quality photo and reads the bill
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .border(
                            3.dp,
                            Brush.verticalGradient(listOf(Color.White, Color.White.copy(alpha = 0.35f))),
                            CircleShape
                        )
                        .padding(7.dp)
                        .shadow(
                            elevation = 16.dp,
                            shape = CircleShape,
                            clip = false,
                            ambientColor = AccentGold.copy(alpha = 0.22f),
                            spotColor = AccentGold.copy(alpha = 0.38f)
                        )
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(AccentGold, AccentBronze)))
                        .clickable(enabled = !isProcessing) { capture() }
                )

                // Type the bill yourself
                SideAction(
                    icon = Icons.Default.EditNote,
                    label = "Manual",
                    tint = AccentChampagne,
                    onClick = {
                        if (!isProcessing) {
                            onBillScanned(BillReceipt(title = "Manual bill"))
                        }
                    }
                )
            }
        }

        // Reading overlay
        if (isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC0A0A0C))
                    .clickable(onClick = {}),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = AccentGold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Reading your bill…",
                        color = TextWhite,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SideAction(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(64.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(Color(0x990A0A0C), CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, fontSize = 11.sp, color = TextWhite.copy(alpha = 0.8f), maxLines = 1)
    }
}
