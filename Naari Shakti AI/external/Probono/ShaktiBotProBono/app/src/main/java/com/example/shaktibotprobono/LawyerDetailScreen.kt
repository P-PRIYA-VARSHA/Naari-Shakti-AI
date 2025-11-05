@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.shaktibotprobono

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import android.content.Intent
import android.net.Uri
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.clip
import com.example.secureshe.R
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.res.colorResource

@Composable
fun LawyerDetailScreen(
    lawyerId: String,
    onBackPressed: () -> Unit,
    onRefreshRequested: () -> Unit = {},
    viewModel: LawyerDetailViewModel = viewModel()
) {
    val lawyerDetailState by viewModel.lawyerDetailState.collectAsState()
    val context = LocalContext.current
    
    LaunchedEffect(lawyerId) {
        viewModel.loadLawyerDetails(lawyerId)
    }

    val isDark = isSystemInDarkTheme()
    val headerColor = colorResource(id = R.color.header_color)
    val footerStripColor = if (isDark) Color(0xFF2E2436) else Color(0xFF9B7AE0)

    Scaffold(
        topBar = {
            // Header aligned with Dashboard theme
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerColor)
                    .padding(top = 16.dp, bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = if (isDark) Color.White else Color(0xFF3A3152)
                        )
                    }
                    Text(
                        text = "Pro Bono",
                        color = if (isDark) Color.White else Color(0xFF3A3152),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "App logo",
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
            }
        },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(footerStripColor)
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "\"You're Never Alone\"",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
        }
    ) { paddingValues ->
        if (lawyerDetailState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (lawyerDetailState.errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = lawyerDetailState.errorMessage ?: "Error loading lawyer details",
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else {
            // Calm lavender-blue page background
            val pageBackground = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFEDEBFF), // light lavender
                    Color(0xFFDDE9FF), // soft blue
                    Color(0xFFF3F4F6)  // pastel grey
                )
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(pageBackground)
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Lawyer Basic Info
                item {
                    // Header: Name + Verified pill on the same line
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = lawyerDetailState.lawyer?.name ?: "Not available",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2D3142),
                            modifier = Modifier.weight(1f)
                        )
                        if (lawyerDetailState.lawyer?.verified == true) {
                            val badgeBrush = Brush.horizontalGradient(listOf(Color(0xFF8EA1FF), Color(0xFFC4A0FF)))
                            Box(
                                modifier = Modifier
                                    .background(badgeBrush, shape = RoundedCornerShape(16.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(text = "Verified", color = Color.White, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Contact Information Card
                    val cardGradient = Brush.horizontalGradient(listOf(Color(0xFF9AA6FF), Color(0xFFB89CFF)))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(cardGradient, shape = RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "Contact Information",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                InfoRow(icon = Icons.Filled.Email, label = "Email", value = lawyerDetailState.lawyer?.email ?: "Not available")
                                InfoRow(icon = Icons.Filled.Phone, label = "Contact", value = lawyerDetailState.lawyer?.contactNumber ?: "Not available")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Professional Information Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(cardGradient, shape = RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "Professional Information",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                InfoRow(icon = Icons.Filled.Gavel, label = "Bar ID", value = lawyerDetailState.lawyer?.barId ?: "Not available")
                                InfoRow(icon = Icons.Filled.LocationOn, label = "State", value = lawyerDetailState.lawyer?.state ?: "Not available")
                                InfoRow(icon = Icons.Filled.Language, label = "Specialization", value = lawyerDetailState.lawyer?.specialization ?: "Not available")
                                InfoRow(icon = Icons.Filled.Language, label = "Languages", value = lawyerDetailState.lawyer?.languages?.joinToString(", ") ?: "Not available")

                                // Cases Handled Section (kept, styled for readability)
                                if (!lawyerDetailState.lawyer?.casesHandled.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Cases Handled & Experience",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = lawyerDetailState.lawyer?.casesHandled ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFEDF0FF)
                                    )
                                }

                                InfoRow(icon = Icons.Filled.CalendarToday, label = "Status", value = if (lawyerDetailState.lawyer?.verified == true) "Verified" else "Pending")
                                InfoRow(icon = Icons.Filled.CalendarToday, label = "Availability", value = if (lawyerDetailState.lawyer?.availability == true) "Available" else "Not Available")
                            }
                        }
                    }
                    // Admin-only: Database check section (unchanged functionality; kept after info cards)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "\ud83d\udcca Database Check (Admin)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    val excelStatus = when {
                        lawyerDetailState.excelIsVerified == true -> "Verified"
                        lawyerDetailState.excelConfidence != null -> {
                            val confidence = lawyerDetailState.excelConfidence!!
                            if (confidence >= 0.6f) "Pending manual review" else "No match"
                        }
                        else -> "Not available"
                    }
                    DetailRow("Status", excelStatus)
                    lawyerDetailState.excelConfidence?.let { conf ->
                        DetailRow("Confidence", "${(conf * 100).toInt()}%")
                    }
                    lawyerDetailState.excelMatchedName?.let { mn ->
                        if (mn.isNotBlank()) DetailRow("Matched Name", mn)
                    }
                    lawyerDetailState.excelMatchedEnrollment?.let { me ->
                        if (me.isNotBlank()) DetailRow("Matched Enrollment", me)
                    }
                    lawyerDetailState.excelReason?.let { r ->
                        if (r.isNotBlank()) {
                            val sanitized = r
                                .replace("in Excel database", "in database", ignoreCase = true)
                                .replace("Excel ", "", ignoreCase = true)
                            DetailRow("Reason", sanitized)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val scope = rememberCoroutineScope()
                    var isRechecking by remember { mutableStateOf(false) }
                    OutlinedButton(
                        onClick = {
                            if (!isRechecking) {
                                isRechecking = true
                                scope.launch {
                                    try {
                                        val svc = ExcelVerificationService(context)
                                        svc.loadAdvocatesFromEmbeddedData()
                                        val lawyerName = lawyerDetailState.lawyer?.name ?: ""
                                        val enrollment = lawyerDetailState.lawyer?.barId
                                        val result = svc.verifyLawyer(lawyerName, enrollment)
                                        svc.saveVerificationResultToFirestore(lawyerId, result)
                                        viewModel.loadLawyerDetails(lawyerId)
                                    } catch (_: Exception) {
                                    } finally {
                                        isRechecking = false
                                    }
                                }
                            }
                        },
                        enabled = !isRechecking
                    ) {
                        if (isRechecking) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Rechecking...")
                        } else {
                            Text("Re-run Database Check")
                        }
                    }
                }
                
                // Uploaded Documents
                if (lawyerDetailState.documents.isNotEmpty()) {
                    item {
                        Text(
                            text = "📄 Uploaded Documents",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    items(lawyerDetailState.documents) { document ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    enabled = document.googleDriveFileId != null
                                ) {
                                    document.googleDriveFileId?.let { fileId ->
                                        val driveUrl = "https://drive.google.com/file/d/$fileId/view"
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(driveUrl))
                                        try {
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            // Handle case where no browser is available
                                        }
                                    }
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
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
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "Uploaded: ${formatDate(document.uploadTime)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (document.googleDriveFileId != null) {
                                            Text(
                                                text = "✅ Backed up to Google Drive (Click to open)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Uploaded Photos
                if (lawyerDetailState.photos.isNotEmpty()) {
                    item {
                        Text(
                            text = "📸 Uploaded Photos",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    items(lawyerDetailState.photos) { photo ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    enabled = photo.downloadUrl.isNotEmpty() && photo.downloadUrl != "photo_uploaded"
                                ) {
                                    if (photo.downloadUrl.isNotEmpty() && photo.downloadUrl != "photo_uploaded") {
                                        val driveUrl = "https://drive.google.com/file/d/${photo.downloadUrl}/view"
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(driveUrl))
                                        try {
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            // Handle case where no browser is available
                                        }
                                    }
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "📸",
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "${photo.type.capitalize()} Photo",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "Uploaded: ${formatDate(photo.uploadTime)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (photo.downloadUrl.isNotEmpty() && photo.downloadUrl != "photo_uploaded") {
                                            Text(
                                                text = "✅ Backed up to Google Drive (Click to open)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Action Buttons
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Actions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            var showReject by remember { mutableStateOf(false) }
                            var reason by remember { mutableStateOf("") }

                            if (showReject) {
                                AlertDialog(
                                    onDismissRequest = { showReject = false },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            viewModel.rejectLawyer(lawyerId, reason, onRefreshRequested)
                                            showReject = false
                                        }) { Text("Reject") }
                                    },
                                    dismissButton = { TextButton(onClick = { showReject = false }) { Text("Cancel") } },
                                    title = { Text("Reject application") },
                                    text = {
                                        OutlinedTextField(
                                            value = reason,
                                            onValueChange = { reason = it },
                                            label = { Text("Reason") },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                )
                            }

                            val actionButtonShape = RoundedCornerShape(24.dp)
                            val buttonModifier = Modifier
                                .height(48.dp)

                            // Use equal weights so labels never wrap; center within a fixed row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.verifyLawyer(lawyerId, onRefreshRequested) },
                                    modifier = Modifier.weight(1f),
                                    shape = actionButtonShape,
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                    enabled = lawyerDetailState.lawyer?.verified != true
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Verify")
                                }

                                OutlinedButton(
                                    onClick = { showReject = true },
                                    modifier = Modifier.weight(1f),
                                    shape = actionButtonShape,
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                    enabled = lawyerDetailState.lawyer?.verified != true
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Reject")
                                }

                                OutlinedButton(
                                    onClick = { viewModel.deleteLawyer(lawyerId, onRefreshRequested) },
                                    modifier = Modifier.weight(1f),
                                    shape = actionButtonShape,
                                    contentPadding = PaddingValues(horizontal = 20.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Delete")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.White)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "$label:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFEDF0FF)
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    val formatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return formatter.format(date)
}