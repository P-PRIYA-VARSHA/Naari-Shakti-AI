@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.shaktibotprobono

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ArrowBack

@Composable
fun EmailVerificationScreen(
    onEmailVerified: () -> Unit,
    onBackPressed: () -> Unit = {},
    viewModel: EmailVerificationViewModel = viewModel()
) {
    val verificationState by viewModel.verificationState.collectAsState()
    var navigationError by remember { mutableStateOf<String?>(null) }
    
    // Check verification status when screen loads
    LaunchedEffect(Unit) {
        viewModel.checkVerificationStatus()
    }
    
    LaunchedEffect(verificationState.isVerified) {
        if (verificationState.isVerified) {
            Log.d("EmailVerification", "✅ Email verified, navigating to document upload...")
            delay(1000) // Show success message briefly
            try {
                onEmailVerified()
                Log.d("EmailVerification", "✅ Navigation callback executed successfully")
            } catch (e: Exception) {
                Log.e("EmailVerification", "❌ Error during navigation: ${e.message}")
                e.printStackTrace()
                navigationError = e.message ?: "Navigation failed"
            }
        }
    }

    Scaffold(
        topBar = {
            // Pro Bono header: hamburger + title with gradient background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFFB8C6FF), Color(0xFFD6C8FF))
                        )
                    )
                    .padding(top = 16.dp, bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pro Bono",
                        color = Color.White,
                        fontSize = 20.sp
                    )
                }
            }
        }
    ) { paddingValues ->
        // Light, calm background consistent with platform theme
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF3E8FF), // light lavender
                            Color(0xFFE0EAFF)  // soft blue
                        )
                    )
                )
                .padding(24.dp),

            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header
            Text(
                text = "Verify Your Email",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                color = Color(0xFF111827)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "We've sent a verification email to:",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = Color(0xFF374151)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Email display
            val displayEmail = verificationState.email.ifBlank { FirebaseAuth.getInstance().currentUser?.email ?: "" }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Text(
                    text = displayEmail,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center,
                    color = Color(0xFF111827)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Instructions
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Check your email",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF111827)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "1. Open the email we sent you\n" +
                               "2. Click the verification link\n" +
                               "3. Return to this page and tap 'Check Status'",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF374151)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Status indicator
            when {
                verificationState.isLoading -> {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Checking verification status...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                verificationState.isVerified -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "✅",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Email Verified!",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Proceeding to next step...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
                verificationState.errorMessage != null -> {
                    val errorMsg = verificationState.errorMessage
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMsg ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                else -> {
                    // Not verified yet
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⏳",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Email Not Verified Yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xFF111827)
                                )
                                Text(
                                    text = "Please check your email and click the verification link",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF374151)
                                )
                            }
                        }
                    }
                }
            }
            
            // Navigation error display
            if (navigationError != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "⚠️ Navigation Error",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = navigationError ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                navigationError = null
                                try {
                                    onEmailVerified()
                                } catch (e: Exception) {
                                    navigationError = e.message ?: "Navigation failed"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onErrorContainer,
                                contentColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text("Retry Navigation")
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val context = androidx.compose.ui.platform.LocalContext.current
                OutlinedButton(
                    onClick = {
                        viewModel.resendVerificationEmail()
                        android.widget.Toast.makeText(
                            context,
                            "Verification email resent",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !verificationState.isLoading
                ) {
                    Text("Resend Email")
                }
                
                Button(
                    onClick = { viewModel.checkVerificationStatus() },
                    modifier = Modifier.weight(1f),
                    enabled = !verificationState.isLoading
                ) {
                    Text("Check Status")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Help text
            Text(
                text = "Can't find the email? Check your spam folder",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
} 