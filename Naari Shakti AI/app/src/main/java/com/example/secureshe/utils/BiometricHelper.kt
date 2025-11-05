package com.example.secureshe.utils

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.common.GoogleApiAvailability
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class BiometricHelper(private val context: Context) {
    
    fun isBiometricAvailable(): Boolean {
        return try {
            // First check if Google Play Services is available
            val googleApiAvailability = GoogleApiAvailability.getInstance()
            val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context)
            
            if (resultCode != com.google.android.gms.common.ConnectionResult.SUCCESS) {
                android.util.Log.w("BiometricHelper", "Google Play Services not available: $resultCode")
                return false
            }
            
            // Additional check to prevent the specific error
            try {
                val biometricManager = BiometricManager.from(context)
                when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
                    BiometricManager.BIOMETRIC_SUCCESS -> true
                    BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> false
                    BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> false
                    BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> false
                    BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> false
                    BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> false
                    else -> false
                }
            } catch (e: SecurityException) {
                android.util.Log.w("BiometricHelper", "Security exception in biometric check: ${e.message}")
                false
            } catch (e: Exception) {
                android.util.Log.w("BiometricHelper", "General exception in biometric check: ${e.message}")
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("BiometricHelper", "Error checking biometric availability: ${e.message}")
            false
        }
    }
    
    suspend fun authenticate(activity: FragmentActivity): Boolean {
        return suspendCancellableCoroutine { continuation ->
            try {
                val executor = ContextCompat.getMainExecutor(context)
                
                val biometricPrompt = BiometricPrompt(activity, executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            super.onAuthenticationError(errorCode, errString)
                            android.util.Log.e("BiometricHelper", "Authentication error: $errorCode - $errString")
                            
                            // Handle specific error codes
                            when (errorCode) {
                                BiometricPrompt.ERROR_HW_NOT_PRESENT,
                                BiometricPrompt.ERROR_HW_UNAVAILABLE,
                                BiometricPrompt.ERROR_NO_BIOMETRICS,
                                BiometricPrompt.ERROR_SECURITY_UPDATE_REQUIRED -> {
                                    // These are permanent errors, don't retry
                                    continuation.resume(false)
                                }
                                BiometricPrompt.ERROR_CANCELED,
                                BiometricPrompt.ERROR_NEGATIVE_BUTTON -> {
                                    // User canceled, don't retry
                                    continuation.resume(false)
                                }
                                else -> {
                                    // Other errors, try to continue without biometric
                                    android.util.Log.w("BiometricHelper", "Biometric error, falling back to regular PIN change")
                                    continuation.resume(false)
                                }
                            }
                        }
                        
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            super.onAuthenticationSucceeded(result)
                            android.util.Log.d("BiometricHelper", "Biometric authentication successful")
                            continuation.resume(true)
                        }
                        
                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                            android.util.Log.w("BiometricHelper", "Biometric authentication failed")
                            continuation.resume(false)
                        }
                    })
                
                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Verify Identity")
                    .setSubtitle("Use your fingerprint to change SOS PIN")
                    .setNegativeButtonText("Cancel")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                    .build()
                
                biometricPrompt.authenticate(promptInfo)
                
            } catch (e: SecurityException) {
                android.util.Log.e("BiometricHelper", "Security exception during biometric authentication: ${e.message}")
                // Security exception means we can't use biometric, continue without it
                continuation.resume(false)
            } catch (e: Exception) {
                android.util.Log.e("BiometricHelper", "Error during biometric authentication: ${e.message}")
                e.printStackTrace()
                // If biometric fails, allow the user to continue without it
                continuation.resume(false)
            }
        }
    }
} 