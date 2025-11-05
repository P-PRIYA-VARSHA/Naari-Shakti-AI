package com.example.shaktibotprobono

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.style.TextAlign
import android.view.View
import android.widget.FrameLayout
import android.view.ViewGroup
import android.graphics.Paint
import android.graphics.Canvas as AndroidCanvas
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.PermissionStatus

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun VerificationPhotoScreen(
    onPhotoTaken: (Bitmap) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val cameraPermissionState = rememberPermissionState(
        permission = android.Manifest.permission.CAMERA
    )
    
    val viewModel: VerificationPhotoViewModel = viewModel()
    
    val cameraState by viewModel.cameraState.collectAsState()
    val status by viewModel.status.collectAsState()
    val error by viewModel.error.collectAsState()
    val photo by viewModel.photo.collectAsState()
    
    LaunchedEffect(cameraPermissionState.status) {
        if (cameraPermissionState.status == PermissionStatus.Granted) {
            viewModel.startCamera(context, lifecycleOwner)
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Permission request
        if (cameraPermissionState.status != PermissionStatus.Granted) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Camera permission is required for verification",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "We need camera access to verify your identity with a live photo",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { cameraPermissionState.launchPermissionRequest() },
                    modifier = Modifier.size(width = 200.dp, height = 48.dp)
                ) {
                    Text("Grant Camera Permission")
                }
            }
        } else {
            // Camera preview with front camera indicator
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.Black)
            ) {
                CameraPreview(
                    cameraState = cameraState,
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
                
                // Face alignment overlay (higher draw order)
                FaceGuideOverlay(
                    modifier = Modifier.fillMaxSize(),
                    hint = "Align your face inside the circle"
                )

                // Front camera indicator
                Card(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.TopStart),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📱 Front Camera Only",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                
                // Error message overlay
                error?.let { errorMessage ->
                    Card(
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.TopCenter),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = errorMessage,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { viewModel.startCamera(context, lifecycleOwner) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Retry",
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
                
                // Processing indicator
                if (status == VerificationStatus.PROCESSING) {
                    Card(
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.Center),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Analyzing photo...",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
            
            // Capture button
            Button(
                onClick = { viewModel.capturePhoto() },
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .size(width = 200.dp, height = 56.dp),
                enabled = status != VerificationStatus.PROCESSING && cameraState is CameraState.READY
            ) {
                Text("📸 Capture Photo")
            }
            
            // Instructions
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "📸 Live Photo Verification",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Please take a selfie using the front camera only",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Requirements:",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• Use front camera only\n" +
                               "• Remove sunglasses and hats\n" +
                               "• Face the camera directly\n" +
                               "• Ensure good lighting\n" +
                               "• Keep your face clearly visible",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "⚠️ Photo-of-photo detection is active",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
    
    // Handle successful photo capture
    LaunchedEffect(photo) {
        if (photo != null && status == VerificationStatus.SUCCESS) {
            onPhotoTaken(photo!!)
        }
    }
}

@Composable
private fun FaceGuideOverlay(
    modifier: Modifier = Modifier,
    hint: String = "Align your face inside the circle"
) {
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val radius = minOf(w, h) * 0.35f
            val center = Offset(w / 2f, h / 2f)

            // Slightly dim the whole preview for contrast
            drawRect(Color.Black.copy(alpha = 0.25f))

            // White circular guide (thicker)
            val strokePx = 10.dp.toPx()
            drawCircle(
                color = Color.White,
                radius = radius,
                center = center,
                style = Stroke(width = strokePx)
            )

            // Soft inner glow to highlight target area
            drawCircle(
                color = Color.White.copy(alpha = 0.06f),
                radius = radius - strokePx,
                center = center
            )

            // Small side ticks to aid alignment
            val tickLen = 18.dp.toPx()
            val tickStroke = 4.dp.toPx()
            // Left tick
            drawLine(
                color = Color.White,
                start = Offset(center.x - radius, center.y - tickLen),
                end = Offset(center.x - radius, center.y + tickLen),
                strokeWidth = tickStroke
            )
            // Right tick
            drawLine(
                color = Color.White,
                start = Offset(center.x + radius, center.y - tickLen),
                end = Offset(center.x + radius, center.y + tickLen),
                strokeWidth = tickStroke
            )
        }
        Text(
            text = hint,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}

@Composable
fun CameraPreview(
    cameraState: CameraState,
    viewModel: VerificationPhotoViewModel,
    modifier: Modifier = Modifier
) {
    when (cameraState) {
        is CameraState.READY -> {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        // Fill view to make face larger and easier to detect
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        // Use TextureView-based rendering so Compose overlays (circle) are visible
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        // Prefer compatibility mode to ensure preview displays over Compose overlays
                    }
                },
                modifier = modifier,
                update = { previewView ->
                    // Connect the PreviewView to the camera preview
                    viewModel.connectPreviewView(previewView)
                }
            )
        }
        CameraState.UNINITIALIZED -> {
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Initializing front camera...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
            }
        }
    }
}
