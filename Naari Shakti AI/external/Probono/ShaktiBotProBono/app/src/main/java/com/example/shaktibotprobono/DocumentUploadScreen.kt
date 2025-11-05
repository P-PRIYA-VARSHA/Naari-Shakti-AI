@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.shaktibotprobono

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import androidx.compose.ui.res.colorResource
import java.text.SimpleDateFormat
import java.util.*
import java.util.Date
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.example.secureshe.R
import com.example.shaktibotprobono.FaceVerificationViewModel

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DocumentUploadScreen(
    onUploadComplete: () -> Unit,
    onBackPressed: () -> Unit = {},
    viewModel: DocumentUploadViewModel = viewModel()
) {
    val uploadState by viewModel.uploadState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val verificationViewModel: VerificationPhotoViewModel = viewModel()
    
    // Face Verification
    val faceVerificationViewModel: FaceVerificationViewModel = viewModel()
    val faceVerificationState by faceVerificationViewModel.verificationState.collectAsState()
    
    // Excel Verification
    val excelVerificationViewModel: ExcelVerificationViewModel = viewModel()
    val excelVerificationState by excelVerificationViewModel.verificationState.collectAsState()
    val excelDataState by excelVerificationViewModel.excelDataState.collectAsState()
    var showExcelVerificationDialog by remember { mutableStateOf(false) }
    
    // Google Sign-In state
    // Backend uploads no longer need user Google Sign-In. Keep false by default.
    var isGoogleSignInRequired by remember { mutableStateOf(false) }
    var showGoogleSignInDialog by remember { mutableStateOf(false) }
    
    // Initialize Excel verification service
    LaunchedEffect(Unit) {
        excelVerificationViewModel.initializeService(context)
        excelVerificationViewModel.loadAdvocatesFromEmbeddedData()
        faceVerificationViewModel.setDocumentUploadViewModel(viewModel)
    }
    
    val cameraState by verificationViewModel.cameraState.collectAsState()
    val status by verificationViewModel.status.collectAsState()
    val error by verificationViewModel.error.collectAsState()
    val photo by verificationViewModel.photo.collectAsState()
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)
    var showVerificationDialog by remember { mutableStateOf(false) }
    var barIdBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Document picker launcher
    val documentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = getFileName(context, it) ?: "document_${System.currentTimeMillis()}"
            if (!uploadState.isLoading) viewModel.uploadDocument(it, fileName)
            
            // Capture bitmap for face verification
            try {
                val bitmap = uriToBitmap(context, it)
                if (bitmap != null) {
                    barIdBitmap = bitmap
                    println("✅ Bar ID document bitmap captured: ${bitmap.width}x${bitmap.height}")
                } else {
                    println("❌ Failed to capture Bar ID document bitmap")
                }
            } catch (e: Exception) {
                println("❌ Error capturing Bar ID document bitmap: ${e.message}")
            }
        }
    }
    
    // Photo picker launcher for profile photo
    val profilePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = getFileName(context, it) ?: "profile_photo_${System.currentTimeMillis()}.jpg"
            if (!uploadState.isLoading) viewModel.uploadPhoto(it, fileName, "profile")
        }
    }
    
    // Load embedded data automatically
    LaunchedEffect(Unit) {
        excelVerificationViewModel.loadAdvocatesFromEmbeddedData()
        // Background: run database check once registration data is available
        try {
            val reg = getCurrentLawyerRegistrationData()
            reg?.let { (name, enrollment) ->
                excelVerificationViewModel.verifyLawyer(name = name, enrollmentNumber = enrollment.ifBlank { null })
            }
        } catch (_: Exception) { }
    }

    // Google Sign-In helper and launcher
    val googleSignInHelper = remember { GoogleSignInHelper(context) }
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        println("🔍 Google Sign-In launcher received result!")
        println("🔍 Result code: ${result.resultCode}")
        println("🔍 Result data: ${result.data}")
        println("🔍 Result data extras: ${result.data?.extras}")
        
        if (result.resultCode == Activity.RESULT_OK) {
            println("✅ Google Sign-In result is OK, processing account...")
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                println("✅ Google Sign-In successful: ${account.email}")
                println("✅ Account ID: ${account.id}")
                println("✅ ID Token available: ${!account.idToken.isNullOrEmpty()}")
                println("✅ Server Auth Code available: ${!account.serverAuthCode.isNullOrEmpty()}")
                
                viewModel.setGoogleAccount(account)
                isGoogleSignInRequired = false
                showGoogleSignInDialog = false
                
                // Debug the sign-in status
                googleSignInHelper.debugSignInStatus()
                
            } catch (e: Exception) {
                println("❌ Google Sign-In failed: ${e.message}")
                println("❌ Error details: ${e}")
                println("❌ Error type: ${e.javaClass.simpleName}")
                // Show error to user
                viewModel.clearError()
                viewModel.setGoogleSignInError("Google Sign-In failed: ${e.message}. Please try again.")
            }
        } else {
            println("❌ Google Sign-In failed with result code: ${result.resultCode}")
            println("❌ Result data: ${result.data}")
            // Show error to user
            viewModel.clearError()
            viewModel.setGoogleSignInError("Google Sign-In was cancelled or failed. Please try again.")
        }
    }
    
    // Check if user is already signed in
    LaunchedEffect(Unit) {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account != null) {
            println("✅ User already signed in: ${account.email}")
            viewModel.setGoogleAccount(account)
            isGoogleSignInRequired = false
            // Debug the sign-in status
            googleSignInHelper.debugSignInStatus()
        } else {
            println("❌ No user signed in, showing Google Sign-In dialog")
            // Show Google Sign-In dialog if not signed in
            showGoogleSignInDialog = true
        }
    }
    
    // Load existing uploads when screen is first displayed
    LaunchedEffect(Unit) {
        viewModel.loadExistingUploads()
        viewModel.initializeGoogleDrive(context)
        
        // Test Google Sign-In configuration
        googleSignInHelper.testConfiguration()
        googleSignInHelper.debugSignInIssues()
    }
    
    // Handle successful verification photo
    LaunchedEffect(photo) {
        if (photo != null && status == VerificationStatus.SUCCESS) {
            println("✅ Verification photo captured successfully!")
            val uri = bitmapToUri(context, photo!!)
            val fileName = "verification_photo_${System.currentTimeMillis()}.jpg"
            viewModel.uploadPhoto(uri, fileName, "verification")
            showVerificationDialog = false
        }
    }
    
    // Debug camera state changes
    LaunchedEffect(cameraState) {
        println("📷 Camera state changed: $cameraState")
    }
    
    // Debug status changes
    LaunchedEffect(status) {
        println("📊 Verification status changed: $status")
    }
    
    // Debug error changes
    LaunchedEffect(error) {
        error?.let { errorMsg ->
            println("❌ Verification error: $errorMsg")
        }
    }
    
    // Initialize camera when permission is granted
    LaunchedEffect(cameraPermissionState.status) {
        if (cameraPermissionState.status == PermissionStatus.Granted) {
            verificationViewModel.startCamera(context, lifecycleOwner)
        }
    }
    
    // Initialize camera when dialog opens
    LaunchedEffect(showVerificationDialog) {
        if (showVerificationDialog && cameraPermissionState.status == PermissionStatus.Granted) {
            println("🔧 Starting camera for verification dialog...")
            verificationViewModel.startCamera(context, lifecycleOwner)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pro Bono") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorResource(R.color.header_color),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF3E8FF), // light lavender
                            Color(0xFFE0EAFF)  // soft blue
                        )
                    )
                )
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // (Removed Google Drive box)
                
                // Progress Checklist Section (Show after Google Sign-In)
                if (!isGoogleSignInRequired) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                // Policy note (highlighted)
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("⚠️", style = MaterialTheme.typography.titleMedium)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Upload only authentic, unaltered documents. Submitting fake or forged documents may lead to strict legal action.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF92400E)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "📋",
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Upload Checklist",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF111827)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                val hasDocuments = uploadState.uploadedDocuments.isNotEmpty()
                                val hasProfilePhoto = uploadState.uploadedPhotos.any { it.type == "profile" }
                                val hasVerificationPhoto = uploadState.uploadedPhotos.any { it.type == "verification" }
                                
                                // Bar Council Certificate
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = if (hasDocuments) Icons.Default.CheckCircle else Icons.Default.Close,
                                        contentDescription = null,
                                        tint = if (hasDocuments) Color(0xFF4CAF50) else Color(0xFFF44336),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "Bar Council Certificate",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF111827)
                                        )
                                        Text(
                                            text = if (hasDocuments) "✅ Uploaded" else "❌ Required",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (hasDocuments) Color(0xFF4CAF50) else Color(0xFFF44336)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                // Profile Photo
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = if (hasProfilePhoto) Icons.Default.CheckCircle else Icons.Default.Close,
                                        contentDescription = null,
                                        tint = if (hasProfilePhoto) Color(0xFF4CAF50) else Color(0xFFF44336),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "Profile Photo",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White
                                        )
                                        Text(
                                            text = if (hasProfilePhoto) "✅ Uploaded" else "❌ Required",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (hasProfilePhoto) Color(0xFF4CAF50) else Color(0xFFF44336)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                // Verification Photo
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = if (hasVerificationPhoto) Icons.Default.CheckCircle else Icons.Default.Close,
                                        contentDescription = null,
                                        tint = if (hasVerificationPhoto) Color(0xFF4CAF50) else Color(0xFFF44336),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "Verification Photo",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White
                                        )
                                        Text(
                                            text = if (hasVerificationPhoto) "✅ Uploaded" else "❌ Required",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (hasVerificationPhoto) Color(0xFF4CAF50) else Color(0xFFF44336)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                // Overall Progress
                                val allRequiredUploads = hasDocuments && hasProfilePhoto && hasVerificationPhoto
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (allRequiredUploads) "🎉 All Documents Ready!" else "📝 ${listOf(hasDocuments, hasProfilePhoto, hasVerificationPhoto).count { it }}/3 Complete",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (allRequiredUploads) Color(0xFF4CAF50) else Color(0xFFFF9800)
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Bar Council Certificate Section
                if (!isGoogleSignInRequired) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "📄",
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Bar Council Certificate",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF111827)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Upload your Bar Council Certificate (PDF, JPG, JPEG, PNG)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF374151)
                                )
                                Text(
                                    text = "Max size: 5MB",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF6B7280)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { documentLauncher.launch("*/*") },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !uploadState.isLoading,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF2196F3), // Light blue color
                                        contentColor = Color.White,
                                        disabledContainerColor = Color(0xFF2196F3),
                                        disabledContentColor = Color.White
                                    )
                                ) {
                                    Text("📄")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Upload Certificate")
                                }
                            }
                        }
                    }
                }
                
                // Photos Section
                if (!isGoogleSignInRequired) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "📷",
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Photos",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF111827)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                // Profile Photo Sub-section
                                Text(
                                    text = "Profile Photo",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF111827)
                                )
                                Text(
                                    text = "Upload a professional photo for your profile (Gallery only)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF374151)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { profilePhotoLauncher.launch("image/*") },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !uploadState.isLoading,
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color(0xFF111827),
                                        disabledContentColor = Color(0xFF111827)
                                    )
                                ) {
                                    Text("📁")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Select from Gallery")
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                // Verification Photo Sub-section
                                Text(
                                    text = "Verification Photo",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF111827)
                                )
                                Text(
                                    text = "Take a live photo for identity verification (Camera only)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF374151)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        if (cameraPermissionState.status == PermissionStatus.Granted) {
                                            showVerificationDialog = true
                                        } else {
                                            cameraPermissionState.launchPermissionRequest()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !uploadState.isLoading,
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color(0xFF111827),
                                        disabledContentColor = Color(0xFF111827)
                                    )
                                ) {
                                    Text("📷")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Take Live Photo")
                                }
                            }
                        }
                    }
                }
                
                // Progress indicator
                if (uploadState.isLoading) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF2A2A2A)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "📤 Uploading...",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                        color = Color(0xFF2196F3)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = Color(0xFF2196F3),
                                    trackColor = Color(0xFF2196F3).copy(alpha = 0.2f)
                                )
                            }
                        }
                    }
                }
                
                // Error message
                if (uploadState.errorMessage != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFD32F2F)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⚠️",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = uploadState.errorMessage!!,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                IconButton(onClick = { viewModel.clearError() }) {
                                    Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color.White)
                                }
                            }
                        }
                    }
                }
                
                // Uploaded Documents
                if (uploadState.uploadedDocuments.isNotEmpty()) {
                    item {
                        Text(
                            text = "Uploaded Documents",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827)
                        )
                    }
                    items(uploadState.uploadedDocuments) { document ->
                        DocumentCard(
                            document = document,
                            onDelete = { viewModel.deleteDocument(document.id) }
                        )
                    }
                }
                
                // Uploaded Photos
                if (uploadState.uploadedPhotos.isNotEmpty()) {
                    item {
                        Text(
                            text = "Uploaded Photos",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827)
                        )
                    }
                    items(uploadState.uploadedPhotos) { photo ->
                        PhotoCard(
                            photo = photo,
                            onDelete = { viewModel.deletePhoto(photo.id) }
                        )
                    }
                }
                
                // Submit for Review Button (Only show when all required uploads are complete)
                if (!isGoogleSignInRequired) {
                    val hasDocuments = uploadState.uploadedDocuments.isNotEmpty()
                    val hasProfilePhoto = uploadState.uploadedPhotos.any { it.type == "profile" }
                    val hasVerificationPhoto = uploadState.uploadedPhotos.any { it.type == "verification" }
                    val allRequiredUploads = hasDocuments && hasProfilePhoto && hasVerificationPhoto
                    
                    if (allRequiredUploads) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF4CAF50) // Green background for success
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "🎉",
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "All Documents Ready!",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "You have successfully uploaded all required documents. Click below to submit for review.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = onUploadComplete,
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = !uploadState.isLoading,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White,
                                            contentColor = Color(0xFF4CAF50)
                                        )
                                    ) {
                                        Icon(Icons.Default.Send, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Submit Documents for Review")
                                    }
                                    if (uploadState.isLoading) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Google Sign-In Dialog (disabled when not required)
    if (isGoogleSignInRequired && showGoogleSignInDialog) {
        println("🔍 Google Sign-In dialog is showing")
        AlertDialog(
            onDismissRequest = { 
                println("🔍 Google Sign-In dialog dismissed")
                showGoogleSignInDialog = false 
            },
            title = { Text("🔐 Google Drive Sign-In Required") },
            text = {
                Column {
                    Text(
                        text = "To upload your documents securely, you need to sign in with your Google account.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This will allow us to automatically backup your documents to Google Drive.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { 
                        println("🔍 Sign in with Google button clicked")
                        try {
                            val signInIntent = googleSignInHelper.getSignInIntent()
                            println("🔍 Launching Google Sign-In intent...")
                            println("🔍 Intent action: ${signInIntent.action}")
                            println("🔍 Intent package: ${signInIntent.`package`}")
                            println("🔍 Intent component: ${signInIntent.component}")
                            println("🔍 Intent data: ${signInIntent.data}")
                            
                            // Launch the intent and immediately check if it's still active
                            googleSignInLauncher.launch(signInIntent)
                            println("🔍 Google Sign-In intent launched successfully")
                            
                            // Add a delay and check if the activity is still running
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                kotlinx.coroutines.delay(2000) // Wait 2 seconds
                                println("🔍 Checking if Google Sign-In activity completed...")
                                val account = GoogleSignIn.getLastSignedInAccount(context)
                                if (account != null) {
                                    println("✅ Account found after sign-in attempt: ${account.email}")
                                    googleSignInHelper.debugSignInStatus()
                                } else {
                                    println("❌ No account found after sign-in attempt")
                                }
                            }
                            
                        } catch (e: Exception) {
                            println("❌ Error launching Google Sign-In: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                ) {
                    Text("Sign in with Google")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        println("🔍 Cancel button clicked")
                        showGoogleSignInDialog = false
                        // Allow user to go back if they don't want to sign in
                        onBackPressed()
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Verification Photo Dialog
    if (showVerificationDialog) {
        AlertDialog(
            onDismissRequest = { showVerificationDialog = false },
            title = { Text("🔐 Live Photo Verification") },
            text = {
                Column {
                    if (cameraPermissionState.status != PermissionStatus.Granted) {
                        Text("Camera permission is required for verification")
                        Button(
                            onClick = { cameraPermissionState.launchPermissionRequest() }
                        ) {
                            Text("Grant Permission")
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .background(Color.Black)
                        ) {
                            CameraPreview(
                                cameraState = cameraState,
                                viewModel = verificationViewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                            Card(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .align(Alignment.TopStart),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                                )
                            ) {
                                Text(
                                    text = "📱 Front Camera Only",
                                    modifier = Modifier.padding(4.dp),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            error?.let { errorMessage ->
                                Card(
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .align(Alignment.TopCenter),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                    )
                                ) {
                                    Text(
                                        text = errorMessage,
                                        modifier = Modifier.padding(8.dp),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            if (status == VerificationStatus.PROCESSING) {
                                Card(
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .align(Alignment.Center),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Analyzing...",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                        Button(
                            onClick = { verificationViewModel.capturePhoto() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            enabled = status != VerificationStatus.PROCESSING && cameraState is CameraState.READY
                        ) {
                            Text("📸 Capture Photo")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showVerificationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Excel Verification Dialog
    if (showExcelVerificationDialog) {
        AlertDialog(
            onDismissRequest = { showExcelVerificationDialog = false },
            title = { Text("📊 Excel Database Verification") },
            text = {
                Column {
                    when (excelVerificationState) {
                        is ExcelVerificationState.Idle -> {
                            Text("Ready to verify lawyer against Advocate Database.")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "This will check if the lawyer is registered in the advocate database.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    // Use registration data instead of document extraction
                                    // Note: This functionality is disabled for now
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Start Verification")
                            }
                        }
                        is ExcelVerificationState.Verifying -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Text("Verifying lawyer against advocate database...")
                            }
                        }
                        is ExcelVerificationState.Complete -> {
                            val state = excelVerificationState as ExcelVerificationState.Complete
                            val result = state.result
                            // Show verification result
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (result.isVerified) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.errorContainer
                                    }
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (result.isVerified) "✅" else "❌",
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (result.isVerified) "Advocate Verification: VERIFIED" else "Advocate Verification: NOT VERIFIED",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Confidence: ${(result.confidence * 100).toInt()}%",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Reason: ${result.reason}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    
                                    result.matchedAdvocate?.let { advocate ->
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Matched: ${advocate.name} (${advocate.enrollmentNumber ?: "No Enrollment Number"})",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Total advocates in database: ${result.totalAdvocatesInDatabase}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        is ExcelVerificationState.Error -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = "❌ Advocate Verification Error",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = (excelVerificationState as ExcelVerificationState.Error).message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { 
                        showExcelVerificationDialog = false
                        val state = excelVerificationState
                        if (state is ExcelVerificationState.Complete) {
                            excelVerificationViewModel.clearVerification()
                        }
                    }
                ) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun DocumentCard(
    document: DocumentInfo,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📄",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = document.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF111827)
                )
                Text(
                    text = formatDate(document.uploadTime),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6B7280)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.Red
                )
            }
        }
    }
}

@Composable
private fun PhotoCard(
    photo: PhotoInfo,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (photo.type == "profile") "👤" else "📷",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "${photo.type.capitalize()} Photo",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF111827)
                )
                Text(
                    text = formatDate(photo.uploadTime),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6B7280)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.Red
                )
            }
        }
    }
}



// Helper functions
private fun getFileName(context: Context, uri: Uri): String? {
    return try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            cursor.getString(nameIndex)
        }
    } catch (e: Exception) {
        null
    }
}

private fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        else -> "${size / (1024 * 1024)} MB"
    }
}

private fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    val formatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return formatter.format(date)
}

private fun createImageUri(context: android.content.Context): Uri {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val imageFileName = "JPEG_${timeStamp}_"
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, imageFileName)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
    }
    return context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
} 

private fun bitmapToUri(context: Context, bitmap: Bitmap): Uri {
    val filename = "verification_photo_${System.currentTimeMillis()}.jpg"
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
    }
    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    uri?.let {
        context.contentResolver.openOutputStream(it)?.use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
        }
    }
    return uri ?: throw IllegalStateException("Failed to create URI for bitmap")
}

private fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream)
        }
    } catch (e: Exception) {
        println("❌ Error converting URI to bitmap: ${e.message}")
        null
    }
}

// Data class for extracted lawyer information
data class LawyerInfo(
    val name: String,
    val enrollmentNumber: String?
)

// Helper function to extract lawyer information from uploaded documents
private fun extractLawyerInfoFromDocuments(documents: List<DocumentInfo>): LawyerInfo {
    // For now, we'll use a simple approach based on document names
    // In a real implementation, you'd use OCR to extract text from documents
    
    var extractedName = ""
    var extractedEnrollmentNumber = ""
    
    for (document in documents) {
        val fileName = document.name.lowercase()
        
        // Look for enrollment number patterns in filename
        if (fileName.contains("bar") || fileName.contains("id") || fileName.contains("license") || fileName.contains("enrollment")) {
            // Try to extract enrollment number from filename
            val enrollmentPattern = Regex("[A-Z0-9]{3,}")
            val enrollmentMatch = enrollmentPattern.find(document.name)
            if (enrollmentMatch != null) {
                extractedEnrollmentNumber = enrollmentMatch.value
            }
        }
        
        // Look for name patterns in filename
        if (fileName.contains("name") || fileName.contains("profile") || fileName.contains("certificate")) {
            // Try to extract name from filename
            val namePattern = Regex("([A-Z][a-z]+\\s+[A-Z][a-z]+)")
            val nameMatch = namePattern.find(document.name)
            if (nameMatch != null) {
                extractedName = nameMatch.value
            }
        }
    }
    
    // If no name extracted, use a default
    if (extractedName.isEmpty()) {
        extractedName = "Lawyer Name" // This should be replaced with actual OCR extraction
    }
    
    // If no enrollment number extracted, use null
    val enrollmentNumber = if (extractedEnrollmentNumber.isNotEmpty()) extractedEnrollmentNumber else null
    
    println("🔍 Extracted lawyer info: Name='$extractedName', Enrollment Number='$enrollmentNumber'")
    
    return LawyerInfo(extractedName, enrollmentNumber)
}

// Helper function to get current lawyer's registration data from Firebase
private suspend fun getCurrentLawyerRegistrationData(): Pair<String, String>? {
    return try {
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        
        if (currentUser != null) {
            val db = FirebaseFirestore.getInstance()
            val lawyerDoc = db.collection("lawyers").document(currentUser.uid).get().await()
            
            if (lawyerDoc.exists()) {
                val name = lawyerDoc.getString("name") ?: ""
                val enrollmentNumber = lawyerDoc.getString("barId") ?: "" // barId field contains enrollment number
                Pair(name, enrollmentNumber)
            } else {
                null
            }
        } else {
            null
        }
    } catch (e: Exception) {
        println("❌ Error getting lawyer registration data: ${e.message}")
        null
    }
}