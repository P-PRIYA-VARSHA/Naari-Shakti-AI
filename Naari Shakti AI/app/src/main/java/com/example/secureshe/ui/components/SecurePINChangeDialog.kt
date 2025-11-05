package com.example.secureshe.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.fragment.app.FragmentActivity
import com.example.secureshe.utils.BiometricHelper
import kotlinx.coroutines.launch

@Composable
fun SecurePINChangeDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    currentPin: String? = null
) {
    val context = LocalContext.current
    val biometricHelper = remember { BiometricHelper(context) }
    val coroutineScope = rememberCoroutineScope()
    
    var showBiometricPrompt by remember { mutableStateOf(true) }
    var biometricSuccess by remember { mutableStateOf(false) }
    var biometricAvailable by remember { mutableStateOf(false) }
    var showPINDialog by remember { mutableStateOf(false) }
    var showFallbackOption by remember { mutableStateOf(false) }
    
    // Check biometric availability
    LaunchedEffect(Unit) {
        try {
            biometricAvailable = biometricHelper.isBiometricAvailable()
            if (!biometricAvailable) {
                // If biometric is not available, show fallback option immediately
                android.util.Log.w("SecurePINChangeDialog", "Biometric not available, showing fallback option")
                showFallbackOption = true
            }
        } catch (e: Exception) {
            android.util.Log.e("SecurePINChangeDialog", "Error checking biometric availability: ${e.message}")
            // If there's any error, show fallback option
            showFallbackOption = true
        }
    }
    
    // Handle biometric authentication
    LaunchedEffect(showBiometricPrompt) {
        if (showBiometricPrompt && biometricAvailable) {
            val activity = context as? FragmentActivity
            if (activity != null) {
                try {
                    val success = biometricHelper.authenticate(activity)
                    biometricSuccess = success
                    if (success) {
                        showPINDialog = true
                    } else {
                        // If biometric fails, show a message and allow fallback
                        android.util.Log.w("SecurePINChangeDialog", "Biometric authentication failed, showing fallback option")
                        showFallbackOption = true
                    }
                } catch (e: Exception) {
                    android.util.Log.e("SecurePINChangeDialog", "Error during biometric authentication: ${e.message}")
                    showFallbackOption = true
                }
            }
            showBiometricPrompt = false
        }
    }
    
    // Show biometric prompt dialog
    if (showBiometricPrompt && biometricAvailable) {
        BiometricPromptDialog(
            onDismiss = onDismiss
        )
    }
    
    // Show fallback option if biometric fails
    if (showFallbackOption) {
        BiometricFallbackDialog(
            onDismiss = onDismiss,
            onContinueWithoutBiometric = {
                showFallbackOption = false
                showPINDialog = true
            }
        )
    }
    
    // Show PIN change dialog after successful biometric authentication or fallback
    if (showPINDialog) {
        PINChangeDialog(
            onDismiss = onDismiss,
            onConfirm = onConfirm,
            currentPin = currentPin
        )
    }
}

@Composable
private fun BiometricPromptDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Fingerprint,
                    contentDescription = "Fingerprint",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Verify Your Identity",
                    style = MaterialTheme.typography.titleLarge
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Use your fingerprint to change the SOS PIN",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun BiometricFallbackDialog(
    onDismiss: () -> Unit,
    onContinueWithoutBiometric: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = "Warning",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Biometric Authentication Unavailable",
                    style = MaterialTheme.typography.titleLarge
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Fingerprint authentication is not available. You can still change your PIN, but it will be less secure.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = onContinueWithoutBiometric,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Continue")
                    }
                }
            }
        }
    }
} 