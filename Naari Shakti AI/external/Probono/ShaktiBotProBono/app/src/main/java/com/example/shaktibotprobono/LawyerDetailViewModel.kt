package com.example.shaktibotprobono

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.Source

data class LawyerDetailState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val lawyer: Lawyer? = null,
    val documents: List<DocumentInfo> = emptyList(),
    val photos: List<PhotoInfo> = emptyList(),
    val excelIsVerified: Boolean? = null,
    val excelConfidence: Float? = null,
    val excelMatchedName: String? = null,
    val excelMatchedEnrollment: String? = null,
    val excelReason: String? = null
)

class LawyerDetailViewModel : ViewModel() {
    
    private val db = FirebaseFirestore.getInstance(FirebaseApp.getInstance("probono"))
    
    private val _lawyerDetailState = MutableStateFlow(LawyerDetailState())
    val lawyerDetailState: StateFlow<LawyerDetailState> = _lawyerDetailState.asStateFlow()
    
    fun loadLawyerDetails(lawyerId: String) {
        viewModelScope.launch {
            try {
                _lawyerDetailState.value = _lawyerDetailState.value.copy(isLoading = true, errorMessage = null)
                
                println("🔍 Loading lawyer details for ID: $lawyerId")
                
                // Load lawyer basic info
                val lawyerDoc = db.collection("lawyers")
                    .document(lawyerId)
                    .get(Source.SERVER)
                    .await()
                
                if (!lawyerDoc.exists()) {
                    println("❌ Lawyer document not found for ID: $lawyerId")
                    _lawyerDetailState.value = _lawyerDetailState.value.copy(
                        isLoading = false,
                        errorMessage = "Lawyer not found"
                    )
                    return@launch
                }
                
                // Map Firestore document to Lawyer object
                val lawyerData = lawyerDoc.data ?: emptyMap()
                val lawyer = Lawyer(
                    id = lawyerId,
                    userId = lawyerData["userId"] as? String,
                    name = lawyerData["name"] as? String ?: "",
                    barId = lawyerData["barId"] as? String ?: "",
                    email = lawyerData["email"] as? String ?: "",
                    contactNumber = lawyerData["contactNumber"] as? String ?: "",
                    state = lawyerData["state"] as? String ?: "",
                    specialization = lawyerData["specialization"] as? String ?: "",
                    languages = (lawyerData["languages"] as? List<String>)?.toList() ?: listOf(),
                    casesHandled = lawyerData["casesHandled"] as? String ?: "",
                    verified = lawyerData["verified"] as? Boolean ?: false,
                    availability = lawyerData["availability"] as? Boolean ?: true,
                    emailVerified = lawyerData["emailVerified"] as? Boolean ?: false,
                    registrationDate = lawyerData["registrationDate"] as? String ?: ""
                )
                
                println("✅ Loaded lawyer details:")
                println("  - Name: ${lawyer.name}")
                println("  - Email: ${lawyer.email}")
                println("  - Contact: ${lawyer.contactNumber}")
                println("  - State: ${lawyer.state}")
                println("  - Specialization: ${lawyer.specialization}")
                println("  - Languages: ${lawyer.languages.joinToString(", ")}")
                
                // Admin-only excel verification fields (optional, if stored alongside lawyer)
                val excelIsVerified = lawyerData["excelIsVerified"] as? Boolean
                val excelConfidence = (lawyerData["excelConfidence"] as? Number)?.toFloat()
                val excelMatchedName = lawyerData["excelMatchedName"] as? String
                val excelMatchedEnrollment = lawyerData["excelMatchedEnrollment"] as? String
                val excelReason = lawyerData["excelReason"] as? String

                // Update state with mapped lawyer data
                _lawyerDetailState.value = _lawyerDetailState.value.copy(
                    isLoading = false,
                    lawyer = lawyer,
                    documents = emptyList(),
                    photos = emptyList(),
                    excelIsVerified = excelIsVerified,
                    excelConfidence = excelConfidence,
                    excelMatchedName = excelMatchedName,
                    excelMatchedEnrollment = excelMatchedEnrollment,
                    excelReason = excelReason
                )
                
                println("✅ Successfully loaded lawyer details")
                
            } catch (e: Exception) {
                println("❌ Error loading lawyer details: ${e.message}")
                e.printStackTrace()
                _lawyerDetailState.value = _lawyerDetailState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load lawyer details: ${e.message}"
                )
            }
        }
    }
    
    fun verifyLawyer(lawyerId: String, onRefreshRequested: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                _lawyerDetailState.value = _lawyerDetailState.value.copy(isLoading = true, errorMessage = null)
                
                // Update verification status in Firestore
                db.collection("lawyers")
                    .document(lawyerId)
                    .update(
                        mapOf(
                            "verified" to true,
                            "verificationStatus" to "approved",
                            "updatedAt" to com.google.firebase.Timestamp.now()
                        )
                    )
                    .await()
                
                // Update local state
                val updatedLawyer = _lawyerDetailState.value.lawyer?.copy(verified = true)
                _lawyerDetailState.value = _lawyerDetailState.value.copy(
                    isLoading = false,
                    lawyer = updatedLawyer,
                    errorMessage = "✅ Lawyer verified successfully!"
                )
                
                // Trigger refresh of admin dashboard
                onRefreshRequested()
                
            } catch (e: Exception) {
                _lawyerDetailState.value = _lawyerDetailState.value.copy(
                    isLoading = false,
                    errorMessage = "❌ Failed to verify lawyer: ${e.message}"
                )
            }
        }
    }
    
    fun deleteLawyer(lawyerId: String, onRefreshRequested: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                _lawyerDetailState.value = _lawyerDetailState.value.copy(isLoading = true, errorMessage = null)
                
                // Delete the main lawyer document
                db.collection("lawyers")
                    .document(lawyerId)
                    .delete()
                    .await()
                
                _lawyerDetailState.value = _lawyerDetailState.value.copy(
                    isLoading = false,
                    errorMessage = "✅ Lawyer deleted successfully!"
                )
                
                // Trigger refresh of admin dashboard
                onRefreshRequested()
                
            } catch (e: Exception) {
                _lawyerDetailState.value = _lawyerDetailState.value.copy(
                    isLoading = false,
                    errorMessage = "❌ Failed to delete lawyer: ${e.message}"
                )
            }
        }
    }
    
    fun rejectLawyer(lawyerId: String, reason: String, onRefreshRequested: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                _lawyerDetailState.value = _lawyerDetailState.value.copy(isLoading = true, errorMessage = null)
                db.collection("lawyers").document(lawyerId)
                    .update(
                        mapOf(
                            "verified" to false,
                            "verificationStatus" to "rejected",
                            "rejectionReason" to reason,
                            "updatedAt" to com.google.firebase.Timestamp.now()
                        )
                    )
                    .await()
                _lawyerDetailState.value = _lawyerDetailState.value.copy(
                    isLoading = false,
                    errorMessage = "✅ Lawyer rejected"
                )
                onRefreshRequested()
            } catch (e: Exception) {
                _lawyerDetailState.value = _lawyerDetailState.value.copy(
                    isLoading = false,
                    errorMessage = "❌ Failed to reject: ${e.message}"
                )
            }
        }
    }
    
    fun clearError() {
        _lawyerDetailState.value = _lawyerDetailState.value.copy(errorMessage = null)
    }
    

} 