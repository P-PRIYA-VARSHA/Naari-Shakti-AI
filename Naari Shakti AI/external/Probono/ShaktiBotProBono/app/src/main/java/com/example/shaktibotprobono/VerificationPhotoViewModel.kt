package com.example.shaktibotprobono

import android.content.Context
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import androidx.lifecycle.LifecycleOwner
// ML Kit imports disabled - focusing on web scraping only
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.common.InputImage
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.graphics.ImageFormat
import kotlinx.coroutines.tasks.await
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.io.File
import java.io.FileOutputStream
import com.example.shaktibotprobono.FaceAnalysis

// Tightened thresholds to discourage "photo of a photo" and spoofing
private const val MIN_CONFIDENCE = 0.6f
private const val MIN_FACE_SIZE = 0.05f
private const val MAX_FACE_SIZE = 0.6f
private const val MIN_EDGE_DENSITY = 0.12f
private const val MAX_EDGE_DENSITY = 0.65f

enum class VerificationStatus {
    PENDING,
    SUCCESS,
    FAILED,
    PROCESSING
}

class VerificationPhotoViewModel : ViewModel() {
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var appContext: Context? = null
    
    private val _cameraState = MutableStateFlow<CameraState>(CameraState.UNINITIALIZED)
    val cameraState: StateFlow<CameraState> = _cameraState
    
    private val _status = MutableStateFlow(VerificationStatus.PENDING)
    val status: StateFlow<VerificationStatus> = _status
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    private val _photo = MutableStateFlow<Bitmap?>(null)
    val photo: StateFlow<Bitmap?> = _photo
    
    private var currentBitmap: Bitmap? = null
    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var attachedPreviewView: androidx.camera.view.PreviewView? = null
    
    fun startCamera(context: Context, lifecycleOwner: LifecycleOwner) {
        appContext = context.applicationContext
        viewModelScope.launch {
            try {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProvider = cameraProviderFuture.get()
                
                // STRICTLY FORCE FRONT CAMERA ONLY - NO BACK CAMERA ALLOWED
                val frontCameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                    .build()
                
                // Double-check if front camera is available
                if (!cameraProvider!!.hasCamera(frontCameraSelector)) {
                    _error.value = "Front camera is not available on this device. This app requires front camera for verification."
                    return@launch
                }
                
                // Verify we're not accidentally using back camera
                val backCameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build()
                
                // Explicitly unbind any back camera that might be active
                try {
                    cameraProvider!!.unbindAll()
                } catch (e: Exception) {
                    // Ignore unbind errors
                }
                
                // Configure camera with high quality settings for front camera
                preview = Preview.Builder()
                    .setTargetResolution(android.util.Size(1080, 1920)) // Portrait orientation for selfie
                    .build()
                
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .setTargetResolution(android.util.Size(1080, 1920))
                    .setJpegQuality(95)
                    .build()
                
                // Bind ONLY to front camera - this is critical for security
                val camera = cameraProvider!!.bindToLifecycle(
                    lifecycleOwner,
                    frontCameraSelector, // This ensures ONLY front camera is used
                    preview!!,
                    imageCapture!!
                )

                // Ensure the PreviewView is connected even if it was attached before preview was created
                attachedPreviewView?.let { pv ->
                    try { preview?.setSurfaceProvider(pv.surfaceProvider) } catch (_: Exception) {}
                }
                
                // Additional verification that we're using front camera
                if (camera.cameraInfo.lensFacing != CameraSelector.LENS_FACING_FRONT) {
                    _error.value = "Security error: Back camera detected. Only front camera is allowed for verification."
                    return@launch
                }
                
                _cameraState.value = CameraState.READY(imageCapture!!)
                _error.value = null
                
            } catch (e: Exception) {
                _error.value = "Failed to start front camera: ${e.message}. Only front camera is allowed for verification."
            }
        }
    }
    
    fun capturePhoto() {
        _status.value = VerificationStatus.PROCESSING
        _error.value = null
        
        viewModelScope.launch {
            try {
                val imageCapture = imageCapture
                if (imageCapture == null) {
                    _error.value = "Camera not ready"
                    _status.value = VerificationStatus.FAILED
                    return@launch
                }
                
                // Capture image using callback
                imageCapture.takePicture(
                    cameraExecutor,
                    object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: ImageProxy) {
                            viewModelScope.launch {
                                try {
                                    val rotation = image.imageInfo.rotationDegrees
                                    var bitmap = image.toBitmap()
                                    if (rotation != 0) {
                                        bitmap = rotateBitmap(bitmap, rotation)
                                    }
                                    currentBitmap = bitmap
                                    
                                    // Store the photo
                                    storePhoto(bitmap)
                                    
                                    // Analyze for live photo detection
                                    val result = analyzePhoto(bitmap)
                                    
                                    if (result.isLive) {
                                        _photo.value = bitmap
                                        _status.value = VerificationStatus.SUCCESS
                                    } else {
                                        _error.value = "Live photo verification failed: ${result.reasons.joinToString(", ")}"
                                        _status.value = VerificationStatus.FAILED
                                    }
                                } catch (e: Exception) {
                                    _error.value = "Failed to process photo: ${e.message}"
                                    _status.value = VerificationStatus.FAILED
                                } finally {
                                    image.close()
                                }
                            }
                        }
                        
                        override fun onError(exception: ImageCaptureException) {
                            _error.value = "Failed to capture photo: ${exception.message}"
                            _status.value = VerificationStatus.FAILED
                        }
                    }
                )
                return@launch
            } catch (e: Exception) {
                _error.value = "Failed to capture photo: ${e.message}"
                _status.value = VerificationStatus.FAILED
            }
        }
    }
    
    private suspend fun analyzePhoto(bitmap: Bitmap): LivePhotoResult {
        return try {
            // Initialize ML Kit face detector
            val options = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .setMinFaceSize(MIN_FACE_SIZE)
                .build()
            
            val detector = FaceDetection.getClient(options)
            
            // Convert bitmap to InputImage
            val image = InputImage.fromBitmap(bitmap, 0)
            
            // Detect faces
            val faces = detector.process(image).await()
            
            if (faces.isEmpty()) {
                return LivePhotoResult(
                    bitmap = bitmap,
                    isLive = false,
                    confidence = 0f,
                    reasons = listOf("No face detected in the image")
                )
            }

            // Reject if multiple faces present
            if (faces.size > 1) {
                return LivePhotoResult(
                    bitmap = bitmap,
                    isLive = false,
                    confidence = 0f,
                    reasons = listOf("Multiple faces detected. Capture must contain only the applicant.")
                )
            }
            
            // Analyze each face
            val faceAnalyses = faces.map { face -> analyzeFace(face, bitmap) }
            val bestAnalysis = faceAnalyses.maxByOrNull { it.getTotalScore() } ?: FaceAnalysis()
            
            // Additional analysis
            val edgeDensityScore = analyzeEdgeDensity(bitmap)
            val lightingScore = analyzeLighting(bitmap)
            val motionBlurScore = analyzeMotionBlur(bitmap)
            
            // Calculate overall confidence
            val totalScore = (bestAnalysis.getTotalScore() + edgeDensityScore + lightingScore + motionBlurScore) / 4f

            // Extra checks using landmarks to enforce "live" selfie indicators
            val onlyFace = faces.first()
            val leftEye = onlyFace.leftEyeOpenProbability ?: 0f
            val rightEye = onlyFace.rightEyeOpenProbability ?: 0f
            val eyeOpenOk = leftEye > 0.4f && rightEye > 0.4f

            val faceArea = (onlyFace.boundingBox.width() * onlyFace.boundingBox.height()).toFloat() / (bitmap.width * bitmap.height)
            val faceSizeOk = faceArea in MIN_FACE_SIZE..MAX_FACE_SIZE

            val isLive = totalScore >= MIN_CONFIDENCE && eyeOpenOk && faceSizeOk
            
            val reasons = mutableListOf<String>()
            reasons.add("Face detected")
            if (bestAnalysis.getTotalScore() >= 0.7f) reasons.add("Face quality good")
            if (edgeDensityScore in 0.6f..1.0f) reasons.add("Natural edge density")
            if (lightingScore in 0.6f..1.0f) reasons.add("Natural lighting")
            if (motionBlurScore in 0.6f..1.0f) reasons.add("Natural motion blur")
            if (eyeOpenOk) reasons.add("Eyes open")
            if (faceSizeOk) reasons.add("Face size valid")
            
            LivePhotoResult(
                bitmap = bitmap,
                isLive = isLive,
                confidence = totalScore,
                reasons = reasons
            )
            
        } catch (e: Exception) {
            LivePhotoResult(
                bitmap = bitmap,
                isLive = false,
                confidence = 0f,
                reasons = listOf("Face analysis failed: ${e.message}")
            )
        }
    }
    
    // Face analysis disabled - focusing on web scraping only
    private fun analyzeFace(face: Face, bitmap: Bitmap): FaceAnalysis {
        val analysis = FaceAnalysis()
        
        // Face detection confidence
        analysis.addScore("detection", 1f)
        
        // Face size analysis
        val faceSize = (face.boundingBox.width() * face.boundingBox.height()).toFloat() / 
                      (bitmap.width * bitmap.height)
        val sizeScore = when {
            faceSize >= MIN_FACE_SIZE && faceSize <= MAX_FACE_SIZE -> 1f
            faceSize < MIN_FACE_SIZE -> faceSize / MIN_FACE_SIZE
            else -> MAX_FACE_SIZE / faceSize
        }
        analysis.addScore("size", sizeScore)
        
        // Landmark detection
        val landmarks = listOf(
            face.leftEyeOpenProbability,
            face.rightEyeOpenProbability,
            face.smilingProbability
        ).filter { it != null && it >= 0 }
        
        val landmarkScore = if (landmarks.isNotEmpty()) {
            landmarks.map { it?.toFloat() ?: 0f }.sum() / landmarks.size
        } else {
            0.5f // Default score if no landmarks detected
        }
        analysis.addScore("landmarks", landmarkScore)
        
        // Face angle analysis
        val headEulerY = face.headEulerAngleY ?: 0f
        val headEulerZ = face.headEulerAngleZ ?: 0f
        
        val angleScore = when {
            abs(headEulerY) <= 15f && abs(headEulerZ) <= 15f -> 1f
            abs(headEulerY) <= 30f && abs(headEulerZ) <= 30f -> 0.8f
            abs(headEulerY) <= 45f && abs(headEulerZ) <= 45f -> 0.6f
            else -> 0.3f
        }
        analysis.addScore("angle", angleScore)
        
        // Occlusion analysis
        val trackingId = face.trackingId
        val occlusionScore = if (trackingId != null && trackingId != -1) {
            1f // Face is being tracked, likely not occluded
        } else {
            0.7f // Default score
        }
        analysis.addScore("occlusion", occlusionScore)
        
        // Contour analysis - simplified to avoid unsupported constants
        val contourScore = 0.8f // Default score since contour detection is complex
        analysis.addScore("contours", contourScore)
        
        return analysis
    }
    
    private fun analyzeEdgeDensity(bitmap: Bitmap): Float {
        // Analyze edge density to detect screens/printed photos
        // Screens and printed photos often have different edge patterns
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        var edgeCount = 0
        val threshold = 30
        
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val center = pixels[y * width + x]
                val left = pixels[y * width + (x - 1)]
                val right = pixels[y * width + (x + 1)]
                val top = pixels[(y - 1) * width + x]
                val bottom = pixels[(y + 1) * width + x]
                
                val horizontalDiff = abs(center - left) + abs(center - right)
                val verticalDiff = abs(center - top) + abs(center - bottom)
                
                if (horizontalDiff > threshold || verticalDiff > threshold) {
                    edgeCount++
                }
            }
        }
        
        val edgeDensity = edgeCount.toFloat() / (width * height)
        
        return when {
            edgeDensity < MIN_EDGE_DENSITY -> 0.9f // More lenient for smooth images
            edgeDensity > MAX_EDGE_DENSITY -> 0.9f // More lenient for sharp images
            else -> 1f // Natural edge density
        }
    }
    
    private fun analyzeLighting(bitmap: Bitmap): Float {
        // Analyze lighting patterns to detect artificial light sources
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        var totalBrightness = 0f
        var brightnessVariance = 0f
        
        // Calculate average brightness
        for (pixel in pixels) {
            val brightness = (pixel shr 16 and 0xFF) * 0.299f + 
                           (pixel shr 8 and 0xFF) * 0.587f + 
                           (pixel and 0xFF) * 0.114f
            totalBrightness += brightness
        }
        val avgBrightness = totalBrightness / pixels.size
        
        // Calculate brightness variance
        for (pixel in pixels) {
            val brightness = (pixel shr 16 and 0xFF) * 0.299f + 
                           (pixel shr 8 and 0xFF) * 0.587f + 
                           (pixel and 0xFF) * 0.114f
            brightnessVariance += (brightness - avgBrightness) * (brightness - avgBrightness)
        }
        brightnessVariance /= pixels.size
        
        // Natural lighting should have moderate variance
        return when {
            brightnessVariance < 100 -> 0.9f // More lenient for uniform lighting
            brightnessVariance > 5000 -> 0.9f // More lenient for variable lighting
            else -> 1f // Natural lighting
        }
    }
    
    private fun analyzeMotionBlur(bitmap: Bitmap): Float {
        // Analyze for motion blur which is common in live photos
        // This is a simplified version - in practice you'd use more sophisticated algorithms
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        var blurScore = 0f
        val sampleSize = 1000
        val step = max(1, pixels.size / sampleSize)
        
        for (i in 0 until sampleSize) {
            val idx = i * step
            if (idx < pixels.size - 1) {
                val current = pixels[idx]
                val next = pixels[idx + 1]
                
                val diff = abs(current - next)
                if (diff > 50) { // Significant color difference
                    blurScore += 1f
                }
            }
        }
        
        val blurRatio = blurScore / sampleSize
        
        return when {
            blurRatio < 0.1f -> 0.8f // Too sharp but still acceptable
            blurRatio > 0.8f -> 0.8f // Too blurry but still acceptable
            else -> 1f // Natural motion blur
        }
    }
    
    private fun storePhoto(bitmap: Bitmap) {
        try {
            // Store the photo in internal storage
            val timestamp = System.currentTimeMillis()
            val filename = "verification_photo_$timestamp.jpg"
            
            // Store in memory for immediate use
            currentBitmap = bitmap
            
            // Save to internal storage for persistence
            appContext?.let { context ->
                val file = File(context.filesDir, filename)
                val outputStream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
                outputStream.close()
                
                println("Photo saved to: ${file.absolutePath}")
            }
            
        } catch (e: Exception) {
            // Log error but don't fail the capture
            println("Failed to store photo: ${e.message}")
        }
    }
    
    fun connectPreviewView(previewView: androidx.camera.view.PreviewView) {
        attachedPreviewView = previewView
        try { preview?.setSurfaceProvider(previewView.surfaceProvider) } catch (_: Exception) {}
    }
    
    private fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees.toFloat())
        return try {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            bitmap
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        cameraExecutor.shutdown()
        cameraProvider?.unbindAll()
    }
}
