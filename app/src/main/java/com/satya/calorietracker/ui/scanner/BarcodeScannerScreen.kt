package com.satya.calorietracker.ui.scanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.satya.calorietracker.domain.model.Food
import com.satya.calorietracker.ui.components.EmptyState
import com.satya.calorietracker.util.Format
import java.util.concurrent.Executors

/**
 * Point-and-shoot barcode scanning.
 *
 * Camera permission is requested here and nowhere else, on the first scan, with a
 * plain explanation of why. Denying it leaves every other feature untouched.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScannerScreen(
    state: ScanState,
    onBarcodeDetected: (String) -> Unit,
    onUseFood: (Food) -> Unit,
    onCreateManually: (barcode: String) -> Unit,
    onRetry: () -> Unit,
    onResume: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionRequested by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        permissionRequested = true
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) launcher.launch(Manifest.permission.CAMERA)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan barcode") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                hasPermission -> CameraLayer(onBarcodeDetected = onBarcodeDetected, paused = state !is ScanState.Scanning)
                permissionRequested -> PermissionDenied(
                    onOpenSettings = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)
                            )
                        )
                    },
                    onManual = { onCreateManually("") }
                )
                else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            if (hasPermission) {
                ScannerOverlay(state = state)
            }
        }
    }

    // ------------------------------------------------------------ result sheet
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    if (state !is ScanState.Scanning && state !is ScanState.LookingUp) {
        ModalBottomSheet(onDismissRequest = onResume, sheetState = sheetState) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 30.dp)
            ) {
                when (state) {
                    is ScanState.Found -> {
                        Text(
                            state.food.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        state.food.brand?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                            NutrientPill("Calories", "${Format.kcal(state.food.nutrients.calories)}")
                            NutrientPill("Protein", "${Format.grams(state.food.nutrients.protein)} g")
                            NutrientPill("Carbs", "${Format.grams(state.food.nutrients.carbs)} g")
                            NutrientPill("Fat", "${Format.grams(state.food.nutrients.fat)} g")
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "per ${state.food.per.toInt()} ${state.food.perUnit.label}" +
                                if (state.fromCache) " · saved on this phone" else "",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = { onUseFood(state.food) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(18.dp)
                        ) { Text("Choose serving and add") }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = onResume, modifier = Modifier.fillMaxWidth()) {
                            Text("Scan another")
                        }
                    }

                    is ScanState.NotFound -> ResultMessage(
                        title = "Product not found",
                        message = "We couldn't find ${state.barcode} online. You can add it manually — it'll be saved with this barcode so the next scan is instant.",
                        primaryLabel = "Add it manually",
                        onPrimary = { onCreateManually(state.barcode) },
                        secondaryLabel = "Scan another",
                        onSecondary = onResume
                    )

                    is ScanState.Offline -> ResultMessage(
                        title = "You're offline",
                        message = "Barcode ${state.barcode} isn't saved on this phone yet and we can't reach the food database. Add it manually now, or try again once you're connected.",
                        primaryLabel = "Add it manually",
                        onPrimary = { onCreateManually(state.barcode) },
                        secondaryLabel = "Try again",
                        onSecondary = onRetry
                    )

                    is ScanState.Error -> ResultMessage(
                        title = "Lookup failed",
                        message = state.message,
                        primaryLabel = "Try again",
                        onPrimary = onRetry,
                        secondaryLabel = "Add it manually",
                        onSecondary = { onCreateManually(state.barcode) }
                    )

                    else -> Unit
                }
            }
        }
    }
}

@Composable
private fun CameraLayer(onBarcodeDetected: (String) -> Unit, paused: Boolean) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val analyzer = remember { BarcodeAnalyzer(onBarcodeDetected) }

    DisposableEffect(Unit) {
        onDispose {
            analyzer.release()
            executor.shutdown()
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            val providerFuture = ProcessCameraProvider.getInstance(ctx)
            providerFuture.addListener({
                runCatching {
                    val provider = providerFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { it.setAnalyzer(executor, analyzer) }

                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis
                    )
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun ScannerOverlay(state: ScanState) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxWidth(0.78f)
                .height(190.dp)
                .border(
                    width = 3.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(22.dp)
                )
        )
    }
    Column(
        Modifier
            .fillMaxSize()
            .padding(bottom = 60.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state is ScanState.LookingUp) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Looking up ${state.barcode}…", color = Color.White)
                } else {
                    Icon(
                        Icons.Outlined.QrCodeScanner,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Point at the barcode", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun PermissionDenied(onOpenSettings: () -> Unit, onManual: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        EmptyState(
            icon = Icons.Outlined.CameraAlt,
            title = "Camera access is off",
            message = "The camera is only used to read barcodes. Nothing is recorded, stored or sent anywhere. You can still add foods by searching or typing them in.",
            actionLabel = "Open app settings",
            onAction = onOpenSettings
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onManual, modifier = Modifier.fillMaxWidth()) {
            Text("Add a food manually instead")
        }
    }
}

@Composable
private fun NutrientPill(label: String, value: String) {
    Column {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ResultMessage(
    title: String,
    message: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String,
    onSecondary: () -> Unit
) {
    Column {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(18.dp)
        ) { Text(primaryLabel) }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onSecondary, modifier = Modifier.fillMaxWidth()) {
            Text(secondaryLabel)
        }
    }
}
