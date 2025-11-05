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

class SimpleBiometricHelper(private val context: Context) {
    
    fun isBiometricAvailable(): Boolean {
        return try {
            // Check if device has fingerprint hardware
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val packageManager = context.packageManager
                if (packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)) {
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
                } else {
                    false
                }
            } else {
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("SimpleBiometricHelper", "Error checking biometric: ${e.message}")
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
                            android.util.Log.e("SimpleBiometricHelper", "Auth error: $errorCode - $errString")
                            continuation.resume(false)
                        }
                        
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            super.onAuthenticationSucceeded(result)
                            android.util.Log.d("SimpleBiometricHelper", "Biometric auth successful")
                            continuation.resume(true)
                        }
                        
                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                            android.util.Log.w("SimpleBiometricHelper", "Biometric auth failed")
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
                
            } catch (e: Exception) {
                android.util.Log.e("SimpleBiometricHelper", "Error during auth: ${e.message}")
                continuation.resume(false)
            }
        }
    }
} 