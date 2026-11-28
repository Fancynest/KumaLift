package com.bearbones.kumalift.core.media

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Size as AndroidSize
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.bearbones.kumalift.core.designsystem.KumaColors
import com.bearbones.kumalift.core.designsystem.LocalKumaColors
import com.bearbones.kumalift.core.localization.LocalStrings
import com.bearbones.kumalift.core.logging.KumaLogger
import com.bearbones.kumalift.domain.model.WorkoutPlanShareCodec
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

internal actual fun platformCreateBitmapFromArgb(
    pixels: IntArray,
    width: Int,
    height: Int
): ImageBitmap {
    val bmp = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    return bmp.asImageBitmap()
}

actual fun encodeQrCodeMatrix(content: String): QrCodeMatrix? {
    if (content.isBlank()) return null
    return try {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.L,
            EncodeHintType.MARGIN to 0,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, hints)
        val size = matrix.width
        val bools = BooleanArray(size * size)
        for (y in 0 until size) {
            for (x in 0 until size) {
                bools[y * size + x] = matrix.get(x, y)
            }
        }
        QrCodeMatrix(size = size, modules = bools)
    } catch (e: Exception) {
        KumaLogger.w("QrCodeCodec", "Failed encoding QR matrix: ${e.message}")
        null
    }
}

@Composable
actual fun QrPlanScannerDialog(
    onCodeScanned: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val s = LocalStrings.current
    val colors = LocalKumaColors.current
    val scope = rememberCoroutineScope()
    val latestOnCodeScanned by rememberUpdatedState(onCodeScanned)

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var scanError by remember { mutableStateOf<String?>(null) }
    var hasHandledSuccess by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasCameraPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    fun handleDecodedText(raw: String?) {
        if (hasHandledSuccess || raw.isNullOrBlank()) return
        val parsed = WorkoutPlanShareCodec.decodePlanInput(raw)
        if (parsed != null) {
            hasHandledSuccess = true
            latestOnCodeScanned(raw)
        } else {
            scanError = s.scanBarcodeInvalidError
        }
    }

    fun decodeUriFromGallery(uri: Uri?) {
        if (uri == null) return
        scope.launch {
            val decoded = withContext(Dispatchers.IO) {
                decodeQrFromGalleryUri(context, uri)
            }
            if (decoded != null) {
                handleDecodedText(decoded)
            } else {
                scanError = s.scanBarcodeInvalidError
            }
        }
    }

    val pickVisualLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> decodeUriFromGallery(uri) }

    val getContentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> decodeUriFromGallery(uri) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 20.dp),
            shape = RoundedCornerShape(22.dp),
            color = KumaColors.SurfaceDim,
            tonalElevation = 0.dp,
            border = BorderStroke(1.dp, colors.border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = s.scanBarcodeTitle,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.textPrimary
                        )
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = s.close, tint = colors.textSecondary)
                    }
                }

                Text(
                    text = s.scanBarcodeHint,
                    style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                )

                // Camera Viewfinder Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(KumaColors.CanvasLowest)
                        .border(1.5.dp, colors.accentText.copy(alpha = 0.65f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasCameraPermission) {
                        val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
                        var cameraProviderRef by remember { mutableStateOf<ProcessCameraProvider?>(null) }
                        var isDisposed by remember { mutableStateOf(false) }
                        DisposableEffect(Unit) {
                            isDisposed = false
                            onDispose {
                                isDisposed = true
                                try { cameraProviderRef?.unbindAll() } catch (_: Exception) {}
                                cameraProviderRef = null
                                analysisExecutor.shutdownNow()
                            }
                        }
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { ctx ->
                                val previewView = PreviewView(ctx).apply {
                                    scaleType = PreviewView.ScaleType.FILL_CENTER
                                }
                                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                cameraProviderFuture.addListener({
                                    if (isDisposed) return@addListener
                                    try {
                                        val cameraProvider = cameraProviderFuture.get()
                                        if (isDisposed) return@addListener
                                        cameraProviderRef = cameraProvider
                                        val preview = Preview.Builder().build().also {
                                            it.surfaceProvider = previewView.surfaceProvider
                                        }
                                        val reader = MultiFormatReader().apply {
                                            setHints(
                                                mapOf(
                                                    DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                                                    DecodeHintType.TRY_HARDER to true,
                                                    DecodeHintType.ALSO_INVERTED to true
                                                )
                                            )
                                        }
                                        val resolutionSelector = ResolutionSelector.Builder()
                                            .setResolutionStrategy(
                                                ResolutionStrategy(
                                                    AndroidSize(1280, 960),
                                                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                                                )
                                            )
                                            .build()
                                        val analysis = ImageAnalysis.Builder()
                                            .setResolutionSelector(resolutionSelector)
                                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                            .build()
                                            .also { imageAnalysis ->
                                                imageAnalysis.setAnalyzer(analysisExecutor) { proxy ->
                                                    val text = try {
                                                        decodeQrFromImageProxy(proxy, reader)
                                                    } finally {
                                                        proxy.close()
                                                    }
                                                    if (!text.isNullOrBlank() && !isDisposed) {
                                                        previewView.post { handleDecodedText(text) }
                                                    }
                                                }
                                            }
                                        cameraProvider.unbindAll()
                                        val camera = cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            CameraSelector.DEFAULT_BACK_CAMERA,
                                            preview,
                                            analysis
                                        )
                                        val centerPoint = previewView.meteringPointFactory.createPoint(0.5f, 0.5f)
                                        camera.cameraControl.startFocusAndMetering(
                                            FocusMeteringAction.Builder(centerPoint).build()
                                        )
                                        previewView.setOnClickListener {
                                            val pt = previewView.meteringPointFactory.createPoint(0.5f, 0.5f)
                                            camera.cameraControl.startFocusAndMetering(
                                                FocusMeteringAction.Builder(pt).build()
                                            )
                                        }
                                    } catch (e: Exception) {
                                        KumaLogger.w("QrScanner", "CameraX bind failed: ${e.message}")
                                    }
                                }, ContextCompat.getMainExecutor(ctx))
                                previewView
                            }
                        )
                        // Subtle wide outer viewfinder border without crowding small QR targets
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp)
                                .border(1.5.dp, colors.accentFill.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                        )
                    } else {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = colors.accentText,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = s.cameraPermissionDeniedMessage,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                            )
                            Button(
                                onClick = { openAppPermissionSettings() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.accentFill,
                                    contentColor = colors.onAccentFill
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(s.openSettingsAction, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                scanError?.let { err ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.errorTint)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = err,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = colors.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        scanError = null
                        launchAndroidGalleryPicker(
                            context = context,
                            maxSelectCount = 1,
                            onLaunchSingle = { req -> pickVisualLauncher.launch(req) },
                            onLaunchFallback = { mime -> getContentLauncher.launch(mime) }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp), tint = colors.accentText)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(s.scanFromGalleryAction, color = colors.textPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun decodeQrFromImageProxy(image: ImageProxy, reader: MultiFormatReader): String? {
    return try {
        val plane = image.planes.firstOrNull() ?: return null
        val buffer = plane.buffer
        val data = ByteArray(buffer.remaining())
        buffer.get(data)
        val width = image.width
        val height = image.height
        val source = PlanarYUVLuminanceSource(data, plane.rowStride, height, 0, 0, width, height, false)
        try {
            reader.decodeWithState(BinaryBitmap(HybridBinarizer(source))).text
        } catch (_: Exception) {
            reader.reset()
            reader.decodeWithState(BinaryBitmap(GlobalHistogramBinarizer(source))).text
        }
    } catch (_: Exception) {
        null
    } finally {
        reader.reset()
    }
}

private fun decodeQrFromGalleryUri(context: android.content.Context, uri: Uri): String? {
    var bmp: Bitmap? = null
    var scaled: Bitmap? = null
    return try {
        val maxSide = 1280
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        }
        var sampleSize = 1
        if (bounds.outWidth > 0 && bounds.outHeight > 0) {
            while ((bounds.outWidth / sampleSize) > maxSide * 2 || (bounds.outHeight / sampleSize) > maxSide * 2) {
                sampleSize *= 2
            }
        }
        bmp = context.contentResolver.openInputStream(uri)?.use { stream ->
            val opts = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inSampleSize = sampleSize
            }
            BitmapFactory.decodeStream(stream, null, opts)
        } ?: return null

        scaled = if (bmp.width > maxSide || bmp.height > maxSide) {
            val ratio = minOf(maxSide.toFloat() / bmp.width, maxSide.toFloat() / bmp.height)
            Bitmap.createScaledBitmap(
                bmp,
                (bmp.width * ratio).toInt().coerceAtLeast(1),
                (bmp.height * ratio).toInt().coerceAtLeast(1),
                true
            )
        } else {
            bmp
        }
        val target = scaled ?: return null
        val pixels = IntArray(target.width * target.height)
        target.getPixels(pixels, 0, target.width, 0, 0, target.width, target.height)
        val source = RGBLuminanceSource(target.width, target.height, pixels)
        val reader = MultiFormatReader().apply {
            setHints(
                mapOf(
                    DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                    DecodeHintType.TRY_HARDER to true,
                    DecodeHintType.ALSO_INVERTED to true
                )
            )
        }
        try {
            reader.decode(BinaryBitmap(HybridBinarizer(source))).text
        } catch (_: Exception) {
            reader.reset()
            reader.decode(BinaryBitmap(GlobalHistogramBinarizer(source))).text
        }
    } catch (_: Throwable) {
        null
    } finally {
        if (scaled != null && scaled !== bmp) scaled.recycle()
        bmp?.recycle()
    }
}
