package com.example.secureshe.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.secureshe.ui.viewmodels.AuthViewModel
import com.example.secureshe.ui.viewmodels.AuthState
import com.example.secureshe.ui.components.SecurePINChangeDialog
import com.example.secureshe.ui.components.SimpleSecurePINChangeDialog
import com.example.secureshe.ui.components.EncryptedPINChangeDialog
import com.example.secureshe.ui.components.FingerprintPINChangeDialog
import com.example.secureshe.ui.components.NativeBiometricPINChangeDialog
import com.example.secureshe.ui.components.SimplePINChangeDialog
import com.example.secureshe.ui.components.BasicBiometricPINChangeDialog
import com.example.secureshe.ui.components.DirectBiometricPINChangeDialog
import com.example.secureshe.ui.components.WorkingBiometricPINChangeDialog
import com.example.secureshe.ui.components.SimpleBiometricPINChangeDialog
import com.example.secureshe.R
import com.example.secureshe.ui.components.LocalDrawerController
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.BorderStroke

@Composable
fun ProfileScreen(
    navController: NavController,
    authViewModel: AuthViewModel = hiltViewModel(),
    userPreferencesViewModel: com.example.secureshe.ui.viewmodels.UserPreferencesViewModel = hiltViewModel()
) {
    val authState by authViewModel.authState.collectAsState()

    val userName by userPreferencesViewModel.userName.collectAsState(initial = null)
    val userEmail by userPreferencesViewModel.userEmail.collectAsState(initial = null)
    val userPhone by userPreferencesViewModel.userPhone.collectAsState(initial = null)
    val userPin by userPreferencesViewModel.userPin.collectAsState(initial = null)
    val contactEmail by userPreferencesViewModel.contactEmail.collectAsState(initial = null)
    val context = androidx.compose.ui.platform.LocalContext.current
    
    var showPINChangeDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showReauthDialog by remember { mutableStateOf(false) }
    var reauthPassword by remember { mutableStateOf("") }
    var reauthPasswordVisible by remember { mutableStateOf(false) }

    // Refresh user data when screen is first displayed
    LaunchedEffect(Unit) {
        android.util.Log.d("ProfileScreen", "Profile screen launched, refreshing user data...")
        userPreferencesViewModel.refreshUserData()

        // Also try to load from Firestore if we have a user ID
        val currentUserId = userPreferencesViewModel.userId.value
        if (currentUserId != null) {
            android.util.Log.d("ProfileScreen", "Attempting to load user data from Firestore...")
            userPreferencesViewModel.loadUserDataFromFirestore(currentUserId)
        }
    }

    val headerColor = colorResource(id = R.color.header_color)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isSystemInDarkTheme()) {
                        listOf(Color(0xFF1A2A3D), Color(0xFF2A3D57))
                    } else {
                        listOf(Color(0xFFF8D7E8), Color(0xFFE3C7FF))
                    }
                )
            )
            .verticalScroll(rememberScrollState())
    ) {
        // Top bar styled like other screens
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .background(headerColor)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hamburger opens drawer in MainActivity
            val drawer = LocalDrawerController.current
            IconButton(onClick = { drawer.open() }) {
                Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = Color.White)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(id = R.string.ns_header_title),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "App logo",
                tint = Color.Unspecified,
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        // Page padding after header
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {

        // Profile information
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF1E4FF),
                contentColor = Color(0xFF1A1A1A)
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "User Information",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Display user information from preferences (regardless of auth state)
                if (userName != null || userEmail != null || userPhone != null) {
                    // Show user information if any data is available
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Person, contentDescription = "Name")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Name: ${userName ?: "Not set"}")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Email, contentDescription = "Email")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Email: ${userEmail ?: "Not set"}")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = "Phone")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Phone: ${userPhone ?: "Not set"}")
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                } else {
                    // Show loading or no data message
                    when (authState) {
                        is AuthState.Loading -> {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                        else -> {
                            Text("User information not available")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Settings section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF1E4FF),
                contentColor = Color(0xFF1A1A1A)
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Set or update your SOS PIN used to quickly trigger emergency actions.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showPINChangeDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = headerColor),
                    border = BorderStroke(1.dp, headerColor)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = "PIN")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Change SOS PIN")
                }

                Text(
                    text = "Add or update trusted contacts who will be notified in emergencies.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { navController.navigate("emergency_contacts") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = headerColor),
                    border = BorderStroke(1.dp, headerColor)
                ) {
                    Icon(Icons.Default.Contacts, contentDescription = "Contacts")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Manage Emergency Contacts")
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Trusted contact Gmail (for sending evidence backups like recordings/screenshots)",
                    style = MaterialTheme.typography.bodySmall
                )

                var tempContact by remember { mutableStateOf(contactEmail ?: "") }
                LaunchedEffect(contactEmail) {
                    tempContact = contactEmail ?: ""
                }

                OutlinedTextField(
                    value = tempContact,
                    onValueChange = { tempContact = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF1A1A1A),
                        unfocusedTextColor = Color(0xFF1A1A1A),
                        disabledTextColor = Color(0xFF7A7A7A),
                        cursorColor = headerColor,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = headerColor,
                        unfocusedBorderColor = Color(0xFFC8B6FF),
                        focusedLabelColor = headerColor,
                        unfocusedLabelColor = headerColor
                    ),
                    label = { Text("Trusted contact Gmail") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        if (tempContact.isNotBlank()) {
                            userPreferencesViewModel.saveContactEmail(tempContact)
                            android.widget.Toast.makeText(context, "Saved", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = headerColor),
                    border = BorderStroke(1.dp, headerColor)
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Save")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Email")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Danger zone: Delete account
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "Danger zone",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFFB00020)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB00020))
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Account", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Account", color = Color.White)
                }
            }
        }

        // End padding space
        Spacer(modifier = Modifier.height(16.dp))
        }

        // Footer band
        Text(
            text = "\"The Safety You Can Count On\"",
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .background(headerColor)
                .padding(vertical = 64.dp),
            style = MaterialTheme.typography.titleMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
    }
    
    // Simple Biometric PIN Change Dialog (shows dialog first, then fingerprint)
    if (showPINChangeDialog) {
        SimpleBiometricPINChangeDialog(
            onDismiss = { showPINChangeDialog = false },
            onConfirm = { newPin ->
                android.util.Log.d("ProfileScreen", "Changing PIN to: $newPin")
                userPreferencesViewModel.updateUserPin(newPin)
                showPINChangeDialog = false
            }
        )
    }

    // Delete Account Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFB00020)) },
            title = { Text("Delete Account") },
            text = { Text("This will permanently delete your account and profile data. This action cannot be undone. Continue?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Trigger deletion
                        authViewModel.deleteAccount { success, error ->
                            if (success) {
                                showDeleteDialog = false
                                // Navigate to auth, clear back stack to dashboard
                                navController.navigate("auth") {
                                    popUpTo("dashboard") { inclusive = true }
                                }
                            } else {
                                val msg = error ?: "Failed to delete account"
                                // Detect recent login requirement and prompt for reauth
                                if (msg.contains("recent", ignoreCase = true) || msg.contains("auth", ignoreCase = true)) {
                                    showDeleteDialog = false
                                    showReauthDialog = true
                                } else {
                                    android.widget.Toast.makeText(
                                        context,
                                        msg,
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    }
                ) {
                    Text("Delete", color = Color(0xFFB00020))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Re-authentication Dialog (asks for password then retries deletion)
    if (showReauthDialog) {
        AlertDialog(
            onDismissRequest = { showReauthDialog = false },
            icon = { Icon(Icons.Default.Lock, contentDescription = null) },
            title = { Text("Confirm your password") },
            text = {
                Column {
                    Text("For security, please re-enter your password to delete your account.")
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = reauthPassword,
                        onValueChange = { reauthPassword = it },
                        singleLine = true,
                        label = { Text("Password") },
                        visualTransformation = if (reauthPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        trailingIcon = {
                            val icon = if (reauthPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            val tint = if (reauthPasswordVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            IconButton(onClick = { reauthPasswordVisible = !reauthPasswordVisible }) {
                                Icon(icon, contentDescription = if (reauthPasswordVisible) "Hide password" else "Show password", tint = tint)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (reauthPassword.isBlank()) {
                            android.widget.Toast.makeText(context, "Password required", android.widget.Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }
                        authViewModel.reauthenticateAndDelete(reauthPassword) { success, error ->
                            if (success) {
                                showReauthDialog = false
                                reauthPassword = ""
                                // Navigate to auth, clear back stack to dashboard
                                navController.navigate("auth") {
                                    popUpTo("dashboard") { inclusive = true }
                                }
                            } else {
                                android.widget.Toast.makeText(
                                    context,
                                    error ?: "Re-authentication failed",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReauthDialog = false }) { Text("Cancel") }
            }
        )
    }
}