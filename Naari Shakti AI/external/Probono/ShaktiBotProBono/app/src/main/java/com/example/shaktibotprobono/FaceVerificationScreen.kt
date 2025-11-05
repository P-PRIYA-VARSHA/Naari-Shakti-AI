/*
package com.example.shaktibotprobono

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceVerificationScreen(
    onNavigateToDocumentUpload: () -> Unit,
    onNavigateToVerificationComplete: () -> Unit,
    viewModel: FaceVerificationViewModel = viewModel(),
) {
    val verificationState by viewModel.verificationState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Face Verification") },
                navigationIcon = {
                    IconButton(onClick = { /* Navigate back */ }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Face verification content would go here
            Text("Face verification screen - DISABLED")
        }
    }
}
*/

// Face Verification Screen - DISABLED (Focusing on web scraping only)
// This screen is commented out since we're not using face matching functionality 