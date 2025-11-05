package com.example.shaktibotprobono

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ExcelVerificationViewModel : ViewModel() {
    
    private val _verificationState = MutableStateFlow<ExcelVerificationState>(ExcelVerificationState.Idle)
    val verificationState: StateFlow<ExcelVerificationState> = _verificationState.asStateFlow()
    
    private val _excelDataState = MutableStateFlow<ExcelDataState>(ExcelDataState.NotLoaded)
    val excelDataState: StateFlow<ExcelDataState> = _excelDataState.asStateFlow()
    
    private var excelVerificationService: ExcelVerificationService? = null
    
    /**
     * Initialize the Excel verification service
     */
    fun initializeService(context: Context) {
        excelVerificationService = ExcelVerificationService(context)
    }
    
    /**
     * Load advocates from embedded data
     */
    fun loadAdvocatesFromEmbeddedData() {
        viewModelScope.launch {
            try {
                _excelDataState.value = ExcelDataState.Loading
                
                val service = excelVerificationService
                if (service == null) {
                    _excelDataState.value = ExcelDataState.Error("Service not initialized")
                    return@launch
                }
                
                val advocates = service.loadAdvocatesFromEmbeddedData()
                
                if (advocates.isNotEmpty()) {
                    _excelDataState.value = ExcelDataState.Loaded(advocates.size)
                } else {
                    _excelDataState.value = ExcelDataState.Error("No advocates found in embedded data")
                }
                
            } catch (e: Exception) {
                _excelDataState.value = ExcelDataState.Error("Failed to load embedded data: ${e.message}")
            }
        }
    }
    
    /**
     * Verify a lawyer against the Excel database
     */
    fun verifyLawyer(name: String, enrollmentNumber: String?) {
        viewModelScope.launch {
            try {
                _verificationState.value = ExcelVerificationState.Verifying
                
                val service = excelVerificationService
                if (service == null) {
                    _verificationState.value = ExcelVerificationState.Error("Service not initialized")
                    return@launch
                }
                
                val result = service.verifyLawyer(name, enrollmentNumber)
                _verificationState.value = ExcelVerificationState.Complete(result)
                
                // Save verification result to Firestore for admin viewing
                try {
                    val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                    if (currentUser != null) {
                        service.saveVerificationResultToFirestore(currentUser.uid, result)
                    }
                } catch (e: Exception) {
                    // Log error but don't fail the verification
                    android.util.Log.e("ExcelVerificationViewModel", "Failed to save verification result to Firestore: ${e.message}")
                }
                
            } catch (e: Exception) {
                _verificationState.value = ExcelVerificationState.Error("Verification failed: ${e.message}")
            }
        }
    }
    
    /**
     * Clear verification state
     */
    fun clearVerification() {
        _verificationState.value = ExcelVerificationState.Idle
    }
    
    /**
     * Clear Excel data cache
     */
    fun clearCache() {
        excelVerificationService?.clearCache()
        _excelDataState.value = ExcelDataState.NotLoaded
    }
    
    /**
     * Get cache status
     */
    fun getCacheStatus(): String {
        return excelVerificationService?.getCacheStatus() ?: "Service not initialized"
    }
}

sealed class ExcelVerificationState {
    object Idle : ExcelVerificationState()
    object Verifying : ExcelVerificationState()
    data class Complete(val result: ExcelVerificationResult) : ExcelVerificationState()
    data class Error(val message: String) : ExcelVerificationState()
}

sealed class ExcelDataState {
    object NotLoaded : ExcelDataState()
    object Loading : ExcelDataState()
    data class Loaded(val advocateCount: Int) : ExcelDataState()
    data class Error(val message: String) : ExcelDataState()
} 