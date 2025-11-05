package com.example.secureshe.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.secureshe.utils.BiometricHelper

@Composable
fun SimpleSecurePINChangeDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    currentPin: String? = null
) {
    val context = LocalContext.current
    val biometricHelper = remember { BiometricHelper(context) }
    
    var biometricAvailable by remember { mutableStateOf(false) }
    var showPINDialog by remember { mutableStateOf(false) }
    var showFallbackDialog by remember { mutableStateOf(false) }
    
    // Check biometric availability
    LaunchedEffect(Unit) {
        try {
            biometricAvailable = biometricHelper.isBiometricAvailable()
            if (biometricAvailable) {
                // Try biometric authentication
                showFallbackDialog = true
            } else {
                // Biometric not available, show PIN dialog directly
                showPINDialog = true
            }
        } catch (e: Exception) {
            android.util.Log.e("SimpleSecurePINChangeDialog", "Error checking biometric: ${e.message}")
            // If there's any error, show PIN dialog directly
            showPINDialog = true
        }
    }
    
    // Show fallback dialog if biometric is available
    if (showFallbackDialog) {
        BiometricFallbackDialog(
            onDismiss = onDismiss,
            onContinueWithoutBiometric = {
                showFallbackDialog = false
                showPINDialog = true
            }
        )
    }
    
    // Show PIN change dialog
    if (showPINDialog) {
        PINChangeDialog(
            onDismiss = onDismiss,
            onConfirm = onConfirm,
            currentPin = currentPin
        )
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
                    Icons.Default.Security,
                    contentDescription = "Security",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Change SOS PIN",
                    style = MaterialTheme.typography.titleLarge
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "For security, fingerprint authentication is recommended but not required. You can proceed to change your PIN.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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