package com.example.shaktibotprobono

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FaceVerificationViewModel : ViewModel() {
    
    // Services are disabled - focusing on web scraping only
    // private var faceRecognitionService: FaceRecognitionService? = null
    // private var awsRekognitionService: AwsRekognitionService? = null
    private var documentUploadViewModel: DocumentUploadViewModel? = null
    
    private val _verificationState = MutableStateFlow(FaceVerificationState())
    val verificationState: StateFlow<FaceVerificationState> = _verificationState.asStateFlow()
    
    fun initialize(context: Context) {
        // Face recognition services are disabled
        // faceRecognitionService = FaceRecognitionService(context)
        // awsRekognitionService = AwsRekognitionService(context, "YOUR_AWS_ACCESS_KEY_ID", "YOUR_AWS_SECRET_ACCESS_KEY")
    }
    
    fun setDocumentUploadViewModel(viewModel: DocumentUploadViewModel) {
        documentUploadViewModel = viewModel
    }
    
    fun performFaceVerification(barIdDocument: Bitmap, verificationPhoto: Bitmap) {
        viewModelScope.launch {
            try {
                _verificationState.value = FaceVerificationState(
                    status = FaceVerificationStatus.VERIFYING,
                    progress = 0.2f
                )
                
                // Face recognition service is disabled
                // val service = faceRecognitionService
                // if (service == null) {
                //     _verificationState.value = FaceVerificationState(
                //         status = FaceVerificationStatus.ERROR,
                //         error = "Face recognition service not initialized"
                //     )
                //     return@launch
                // }
                
                _verificationState.value = FaceVerificationState(
                    status = FaceVerificationStatus.VERIFYING,
                    progress = 0.6f
                )
                
                // Simulate face verification since service is disabled
                val confidenceScore = 0.85f // Simulated confidence score
                val result = FaceVerificationResult(
                    barIdFace = barIdDocument,
                    verificationFace = verificationPhoto,
                    confidenceScore = confidenceScore,
                    result = determineVerificationResult(confidenceScore),
                    notes = "Face verification simulated - service disabled"
                )
                
                _verificationState.value = FaceVerificationState(
                    status = FaceVerificationStatus.COMPLETED,
                    verificationResult = result,
                    progress = 1.0f
                )
                
            } catch (e: Exception) {
                _verificationState.value = FaceVerificationState(
                    status = FaceVerificationStatus.ERROR,
                    error = "Face verification failed: ${e.message}"
                )
            }
        }
    }
    
    fun determineVerificationResult(confidenceScore: Float): VerificationResult {
        return when {
            confidenceScore >= 0.75f -> VerificationResult.MATCHED
            confidenceScore >= 0.5f -> VerificationResult.UNCLEAR_REVIEW
            else -> VerificationResult.MISMATCH_FLAGGED
        }
    }
    
    fun resetVerification() {
        _verificationState.value = FaceVerificationState()
    }
    
    fun applyAdminOverride(result: VerificationResult) {
        val currentState = _verificationState.value
        if (currentState.verificationResult != null) {
            val updatedResult = currentState.verificationResult.copy(result = result)
            _verificationState.value = currentState.copy(verificationResult = updatedResult)
        }
    }
    
    fun getVerificationSummary(): String {
        val state = _verificationState.value
        return when (state.status) {
            FaceVerificationStatus.IDLE -> "No face verification performed"
            FaceVerificationStatus.VERIFYING -> "Verifying faces..."
            FaceVerificationStatus.COMPLETED -> {
                val result = state.verificationResult
                if (result != null) {
                    "Face verification: ${result.result} (${(result.confidenceScore * 100).toInt()}% confidence)"
                } else {
                    "Face verification completed"
                }
            }
            FaceVerificationStatus.ERROR -> "Error: ${state.error}"
        }
    }
    
    fun getVerificationDetails(): String {
        val state = _verificationState.value
        if (state.status == FaceVerificationStatus.COMPLETED && state.verificationResult != null) {
            val result = state.verificationResult!!
            return """
                Face Verification Details:
                - Status: ${result.result}
                - Confidence Score: ${(result.confidenceScore * 100).toInt()}%
                - Notes: ${result.notes ?: "No additional notes"}
            """.trimIndent()
        }
        return "No face verification details available"
    }
}

data class FaceVerificationState(
    val status: FaceVerificationStatus = FaceVerificationStatus.IDLE,
    val progress: Float = 0f,
    val verificationResult: FaceVerificationResult? = null,
    val error: String? = null
)

enum class FaceVerificationStatus {
    IDLE, VERIFYING, COMPLETED, ERROR
}

// Removed duplicate declarations - using VerificationResult and FaceVerificationResult from VerificationModels.kt
 