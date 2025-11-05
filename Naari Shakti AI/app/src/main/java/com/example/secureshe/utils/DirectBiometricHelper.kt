package com.example.secureshe.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class DirectBiometricHelper(private val context: Context) {
    
    fun isBiometricAvailable(): Boolean {
        return try {
            // Check if device has fingerprint hardware
            val packageManager = context.packageManager
            val hasFingerprint = packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
            
            if (!hasFingerprint) {
                android.util.Log.d("DirectBiometricHelper", "No fingerprint hardware detected")
                return false
            }
            
            // Check biometric manager
            val biometricManager = BiometricManager.from(context)
            val canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            
            when (canAuthenticate) {
                BiometricManager.BIOMETRIC_SUCCESS -> {
                    android.util.Log.d("DirectBiometricHelper", "Biometric authentication available")
                    true
                }
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                    android.util.Log.w("DirectBiometricHelper", "No biometric hardware")
                    false
                }
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                    android.util.Log.w("DirectBiometricHelper", "Biometric hardware unavailable")
                    false
                }
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                    android.util.Log.w("DirectBiometricHelper", "No biometric enrolled")
                    false
                }
                BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                    android.util.Log.w("DirectBiometricHelper", "Security update required")
                    false
                }
                else -> {
                    android.util.Log.w("DirectBiometricHelper", "Unknown biometric status: $canAuthenticate")
                    false
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("DirectBiometricHelper", "Error checking biometric: ${e.message}")
            false
        }
    }
    
    suspend fun authenticate(activity: FragmentActivity): Boolean {
        return suspendCancellableCoroutine { continuation ->
            try {
                android.util.Log.d("DirectBiometricHelper", "Starting biometric authentication...")
                
                val executor = ContextCompat.getMainExecutor(context)
                
                val biometricPrompt = BiometricPrompt(activity, executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            super.onAuthenticationError(errorCode, errString)
                            android.util.Log.e("DirectBiometricHelper", "Auth error: $errorCode - $errString")
                            continuation.resume(false)
                        }
                        
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            super.onAuthenticationSucceeded(result)
                            android.util.Log.d("DirectBiometricHelper", "Biometric auth successful")
                            continuation.resume(true)
                        }
                        
                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                            android.util.Log.w("DirectBiometricHelper", "Biometric auth failed")
                            continuation.resume(false)
                        }
                    })
                
                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Verify Identity")
                    .setSubtitle("Use your fingerprint to change SOS PIN")
                    .setNegativeButtonText("Cancel")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                    .build()
                
                android.util.Log.d("DirectBiometricHelper", "Showing biometric prompt...")
                biometricPrompt.authenticate(promptInfo)
                
            } catch (e: Exception) {
                android.util.Log.e("DirectBiometricHelper", "Error during auth: ${e.message}")
                continuation.resume(false)
            }
        }
    }
} 