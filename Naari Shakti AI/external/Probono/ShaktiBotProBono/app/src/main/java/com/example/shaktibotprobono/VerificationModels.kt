package com.example.shaktibotprobono

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.ImageCaptureException
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import android.graphics.BitmapFactory

sealed class CameraState {
    object UNINITIALIZED : CameraState()
    data class READY(val imageCapture: androidx.camera.core.ImageCapture) : CameraState()
}

data class LivePhotoResult(
    val bitmap: Bitmap,
    val isLive: Boolean,
    val confidence: Float,
    val reasons: List<String>
)

data class FaceAnalysis(
    private val scores: MutableMap<String, Float> = mutableMapOf(),
    val reasons: MutableList<String> = mutableListOf()
) {
    fun addScore(type: String, value: Float) {
        scores[type] = value
        if (value < 0.5f) {
            reasons.add("Low $type score")
        }
    }
    
    fun getTotalScore(): Float {
        return if (scores.isNotEmpty()) {
            scores.values.average().toFloat()
        } else {
            0f
        }
    }
    
    fun getScore(type: String): Float {
        return scores[type] ?: 0f
    }
}

// New models for facial recognition verification
enum class VerificationResult {
    MATCHED,           // ✅ Above 90% confidence
    UNCLEAR_REVIEW,    // ⚠️ 70-90% confidence
    MISMATCH_FLAGGED   // ❌ Below 70% confidence
}

data class FaceVerificationResult(
    val barIdFace: Bitmap?,
    val verificationFace: Bitmap?,
    val confidenceScore: Float,
    val result: VerificationResult,
    val timestamp: Long = System.currentTimeMillis(),
    val adminOverride: AdminOverride? = null,
    val notes: String? = null
)

data class AdminOverride(
    val adminId: String,
    val action: OverrideAction,
    val notes: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class OverrideAction {
    APPROVE,
    REJECT
}

data class FaceComparisonRequest(
    val barIdDocumentId: String,
    val verificationPhotoId: String,
    val barIdFace: Bitmap,
    val verificationFace: Bitmap
)

data class FaceComparisonResponse(
    val success: Boolean,
    val confidenceScore: Float?,
    val error: String? = null
)

fun ImageProxy.toBitmap(): Bitmap {
    val yBuffer = planes[0].buffer
    val uBuffer = planes[1].buffer
    val vBuffer = planes[2].buffer

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)
    
    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)

    val yuvImage = YuvImage(
        nv21,
        ImageFormat.NV21,
        width,
        height,
        null
    )

    val outputStream = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, outputStream)
    
    val imageBytes = outputStream.toByteArray()
    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}
