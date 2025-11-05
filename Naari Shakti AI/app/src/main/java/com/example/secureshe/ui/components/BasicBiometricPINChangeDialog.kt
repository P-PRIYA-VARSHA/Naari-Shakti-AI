package com.example.secureshe.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.fragment.app.FragmentActivity
import com.example.secureshe.utils.BasicBiometricHelper

@Composable
fun BasicBiometricPINChangeDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val context = LocalContext.current
    val biometricHelper = remember { BasicBiometricHelper(context) }
    
    var showFingerprintPrompt by remember { mutableStateOf(true) }
    var showPINDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var biometricAvailable by remember { mutableStateOf(false) }
    
    // Check biometric availability
    LaunchedEffect(Unit) {
        biometricAvailable = biometricHelper.isBiometricAvailable()
        if (!biometricAvailable) {
            showErrorDialog = true
        }
    }
    
    // Handle fingerprint authentication
    LaunchedEffect(showFingerprintPrompt) {
        if (showFingerprintPrompt && biometricAvailable) {
            val activity = context as? FragmentActivity
            if (activity != null) {
                try {
                    val success = biometricHelper.authenticate(activity)
                    if (success) {
                        showPINDialog = true
                    } else {
                        // Authentication failed, dismiss
                        onDismiss()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("BasicBiometricPINChangeDialog", "Error: ${e.message}")
                    onDismiss()
                }
            }
            showFingerprintPrompt = false
        }
    }
    
    // Show fingerprint prompt
    if (showFingerprintPrompt && biometricAvailable) {
        BasicFingerprintPromptDialog(
            onDismiss = onDismiss
        )
    }
    
    // Show error dialog if biometric not available
    if (showErrorDialog) {
        BasicBiometricErrorDialog(
            onDismiss = onDismiss
        )
    }
    
    // Show PIN change dialog after successful fingerprint authentication
    if (showPINDialog) {
        BasicPINChangeDialogWithoutCurrent(
            onDismiss = onDismiss,
            onConfirm = onConfirm
        )
    }
}

@Composable
private fun BasicFingerprintPromptDialog(
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
private fun BasicBiometricErrorDialog(
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
                    Icons.Default.Error,
                    contentDescription = "Error",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Fingerprint Not Available",
                    style = MaterialTheme.typography.titleLarge
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Fingerprint authentication is required to change the SOS PIN. Please set up fingerprint in your device settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("OK")
                }
            }
        }
    }
}

@Composable
private fun BasicPINChangeDialogWithoutCurrent(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newPinInput by remember { mutableStateOf("") }
    var confirmPinInput by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

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
                    Icons.Default.Lock,
                    contentDescription = "Security",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Set New SOS PIN",
                    style = MaterialTheme.typography.titleLarge
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Your PIN will be encrypted for security",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // New PIN
                OutlinedTextField(
                    value = newPinInput,
                    onValueChange = { 
                        newPinInput = it.take(4) // Limit to 4 digits
                        showError = false
                    },
                    label = { Text("New PIN") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Confirm New PIN
                OutlinedTextField(
                    value = confirmPinInput,
                    onValueChange = { 
                        confirmPinInput = it.take(4) // Limit to 4 digits
                        showError = false
                    },
                    label = { Text("Confirm New PIN") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (showError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
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
                        onClick = {
                            // Validate inputs
                            when {
                                newPinInput.length != 4 -> {
                                    showError = true
                                    errorMessage = "PIN must be 4 digits"
                                }
                                newPinInput != confirmPinInput -> {
                                    showError = true
                                    errorMessage = "PINs do not match"
                                }
                                else -> {
                                    onConfirm(newPinInput)
                                    onDismiss()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Set PIN")
                    }
                }
            }
        }
    }
} 