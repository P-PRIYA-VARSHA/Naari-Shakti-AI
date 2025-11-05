package com.example.shaktibotprobono

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.util.Log

data class EmailVerificationState(
    val email: String = "",
    val isLoading: Boolean = false,
    val isVerified: Boolean = false,
    val errorMessage: String? = null
)

class EmailVerificationViewModel : ViewModel() {
    
    companion object {
        private const val TAG = "EmailVerificationViewModel"
    }
    
    private val probonoApp = FirebaseApp.getInstance("probono")
    private val auth = FirebaseAuth.getInstance(probonoApp)
    private val db = FirebaseFirestore.getInstance(probonoApp)
    
    private val _verificationState = MutableStateFlow(EmailVerificationState())
    val verificationState: StateFlow<EmailVerificationState> = _verificationState
    
    init {
        try {
            // Get current user's email
            val currentUser = auth.currentUser
            if (currentUser != null) {
                Log.d(TAG, "✅ User found in init: ${currentUser.email}")
                _verificationState.value = _verificationState.value.copy(
                    email = currentUser.email ?: "",
                    isVerified = currentUser.isEmailVerified
                )
            } else {
                Log.w(TAG, "⚠️ No user found in init")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in init: ${e.message}")
            e.printStackTrace()
        }
    }
    
    fun checkVerificationStatus() {
        Log.d(TAG, "🔍 Checking email verification status...")
        viewModelScope.launch {
            try {
                _verificationState.value = _verificationState.value.copy(isLoading = true)
                
                // Reload user to get latest verification status
                Log.d(TAG, "🔄 Reloading user...")
                auth.currentUser?.reload()?.await()
                
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    Log.d(TAG, "👤 User found: ${currentUser.email}")
                    Log.d(TAG, "✅ Email verified: ${currentUser.isEmailVerified}")
                    
                    _verificationState.value = _verificationState.value.copy(
                        email = currentUser.email ?: "",
                        isVerified = currentUser.isEmailVerified,
                        isLoading = false,
                        errorMessage = null
                    )
                    
                    if (currentUser.isEmailVerified) {
                        Log.d(TAG, "🎉 Email verification successful!")
                        // Update the lawyer's emailVerified field in Firestore
                        updateEmailVerifiedInFirestore(currentUser.uid)
                    }
                } else {
                    Log.e(TAG, "❌ No user found")
                    _verificationState.value = _verificationState.value.copy(
                        isLoading = false,
                        errorMessage = "No user found. Please try logging in again."
                    )
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error checking verification status: ${e.message}")
                e.printStackTrace()
                _verificationState.value = _verificationState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to check verification status"
                )
            }
        }
    }
    
    fun resendVerificationEmail() {
        Log.d(TAG, "📧 Resending verification email...")
        viewModelScope.launch {
            try {
                _verificationState.value = _verificationState.value.copy(isLoading = true)
                
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    Log.d(TAG, "📧 Sending verification email to: ${currentUser.email}")
                    currentUser.sendEmailVerification().await()
                    Log.d(TAG, "✅ Verification email sent successfully")
                    
                    _verificationState.value = _verificationState.value.copy(
                        isLoading = false,
                        errorMessage = null
                    )
                } else {
                    Log.e(TAG, "❌ No user found for resending email")
                    _verificationState.value = _verificationState.value.copy(
                        isLoading = false,
                        errorMessage = "No user found. Please try logging in again."
                    )
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error resending verification email: ${e.message}")
                e.printStackTrace()
                _verificationState.value = _verificationState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to send verification email"
                )
            }
        }
    }
    
    private fun updateEmailVerifiedInFirestore(userId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "📝 Updating emailVerified field in Firestore for user: $userId")
                
                // First try to find the lawyer document by userId
                val querySnapshot = db.collection("lawyers")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()
                
                if (querySnapshot.documents.isNotEmpty()) {
                    // Update using the query result
                    val lawyerDoc = querySnapshot.documents.first()
                    db.collection("lawyers").document(lawyerDoc.id)
                        .update(
                            mapOf(
                                "emailVerified" to true,
                                "updatedAt" to com.google.firebase.Timestamp.now()
                            )
                        )
                        .await()
                    Log.d(TAG, "✅ Email verified status updated in Firestore via query")
                } else {
                    // Fallback: try direct document access with userId as document ID
                    db.collection("lawyers").document(userId)
                        .update(
                            mapOf(
                                "emailVerified" to true,
                                "updatedAt" to com.google.firebase.Timestamp.now()
                            )
                        )
                        .await()
                    Log.d(TAG, "✅ Email verified status updated in Firestore via direct access")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to update emailVerified in Firestore: ${e.message}")
                // Don't fail the verification process if Firestore update fails
            }
        }
    }
} 