@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.shaktibotprobono

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.tasks.await
import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.res.colorResource

// Data class to hold all my lawyer profile state
data class MyLawyerProfileState(
    val loading: Boolean = true,
    val error: String? = null,
    val name: String = "",
    val email: String = "",
    val barId: String = "",
    val contact: String = "",
    val state: String = "",
    val specialization: String = "",
    val languages: List<String> = emptyList(),
    val casesHandled: String = "",
    val verified: Boolean = false,
    val availability: Boolean = true,
    val verificationStatus: String = "pending",
    val rejectionReason: String? = null,
    val lawyerDocId: String? = null
)

@Composable
fun MyLawyerProfileScreen(onBackPressed: () -> Unit) {
    // Log only once when the composable is first created
    LaunchedEffect(Unit) {
        Log.e("MyLawyerProfile", "📱 MyLawyerProfileScreen LOADED (LaunchedEffect)")
    }
    val probonoApp = remember { FirebaseApp.getInstance("probono") }
    val auth = remember { FirebaseAuth.getInstance(probonoApp) }
    val db = remember { FirebaseFirestore.getInstance(probonoApp) }
    val uid = auth.currentUser?.uid
    val coroutineScope = rememberCoroutineScope()
    
    // Use a single state object to reduce recompositions
    var profileState by remember { mutableStateOf(MyLawyerProfileState()) }
    
    // Add debouncing for back button
    var isBackButtonClickable by remember { mutableStateOf(true) }

    DisposableEffect(uid) {
        if (uid == null) {
            profileState = profileState.copy(
                error = "Please sign in to view your profile.",
                loading = false
            )
            return@DisposableEffect onDispose { }
        }
        // Listen by userId query (works even if document ID is not the uid)
        val queryReg = db.collection("lawyers")
            .whereEqualTo("userId", uid)
            .limit(1)
            .addSnapshotListener { snaps, e ->
                if (e != null) { 
                    profileState = profileState.copy(error = e.message, loading = false)
                    return@addSnapshotListener 
                }
                val doc = snaps?.documents?.firstOrNull()
                if (doc != null && doc.exists()) {
                     val casesHandled = doc.getString("casesHandled") ?: ""
                     Log.d("MyLawyerProfile", "📥 Loading profile - casesHandled: '$casesHandled'")
                     profileState = profileState.copy(
                         lawyerDocId = doc.id,
                         name = doc.getString("name") ?: "",
                         email = doc.getString("email") ?: "",
                         barId = doc.getString("barId") ?: "",
                         contact = doc.getString("contactNumber") ?: "",
                         state = doc.getString("state") ?: "",
                         specialization = doc.getString("specialization") ?: "",
                         languages = (doc.get("languages") as? List<String>) ?: emptyList(),
                         casesHandled = casesHandled,
                         verified = doc.getBoolean("verified") ?: false,
                         availability = doc.getBoolean("availability") ?: true,
                         verificationStatus = doc.getString("verificationStatus") ?: "pending",
                         rejectionReason = doc.getString("rejectionReason"),
                         loading = false,
                         error = null
                     )
                }
            }
        // Also attach a direct doc(uid) listener as fallback for older records
        val directReg = db.collection("lawyers").document(uid)
            .addSnapshotListener { snap, e ->
                if (e != null) { return@addSnapshotListener }
                if (snap != null && snap.exists() && profileState.lawyerDocId == null) {
                    profileState = profileState.copy(
                         lawyerDocId = snap.id,
                         name = snap.getString("name") ?: "",
                         email = snap.getString("email") ?: "",
                         barId = snap.getString("barId") ?: "",
                         contact = snap.getString("contactNumber") ?: "",
                         state = snap.getString("state") ?: "",
                         specialization = snap.getString("specialization") ?: "",
                         languages = (snap.get("languages") as? List<String>) ?: emptyList(),
                         casesHandled = snap.getString("casesHandled") ?: "",
                         verified = snap.getBoolean("verified") ?: false,
                         availability = snap.getBoolean("availability") ?: true,
                         verificationStatus = snap.getString("verificationStatus") ?: "pending",
                         rejectionReason = snap.getString("rejectionReason"),
                         loading = false,
                         error = null
                     )
                }
            }
        onDispose { 
            queryReg.remove(); 
            directReg.remove()
        }
    }

    val isDark = isSystemInDarkTheme()
    val headerColor = colorResource(id = com.example.secureshe.R.color.header_color)

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
                    IconButton(
                        onClick = {
                            if (isBackButtonClickable) {
                                isBackButtonClickable = false
                                onBackPressed()
                                coroutineScope.launch {
                                    delay(1000)
                                    isBackButtonClickable = true
                                }
                            }
                        },
                        enabled = isBackButtonClickable
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = if (isDark) Color.White else Color(0xFF3A3152)
                        )
                    }
                    Text(
                        text = "My Profile",
                        color = if (isDark) Color.White else Color(0xFF3A3152),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }
    ) { pv ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFF7F5FF), Color(0xFFF1F6FF), Color(0xFFFAFAFD))
                    )
                )
                .padding(pv)
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when {
                profileState.loading -> CircularProgressIndicator()
                profileState.error != null -> Text(profileState.error!!, color = MaterialTheme.colorScheme.error)
                else -> {
                    // Profile header (avatar, name, verified, bar id)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Simple avatar with initial
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(Color(0xFFDCE6FF), RoundedCornerShape(36.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                val initial = (profileState.name.firstOrNull()?.uppercase() ?: "L").toString()
                                Text(initial, color = Color(0xFF4F46E5), style = MaterialTheme.typography.headlineMedium)
                            }
                            Text(profileState.name.ifBlank { "Lawyer" }, style = MaterialTheme.typography.titleLarge, color = Color(0xFF111827))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (profileState.verificationStatus == "approved") {
                                    AssistChip(onClick = {}, label = { Text("Verified", color = Color.White) }, colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFF22C55E)))
                                } else if (profileState.verificationStatus == "pending") {
                                    AssistChip(onClick = {}, label = { Text("Pending", color = Color.White) }, colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFFF59E0B)))
                                } else {
                                    AssistChip(onClick = {}, label = { Text("Rejected", color = Color.White) }, colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFFEF4444)))
                                }
                            }
                            Text("Bar ID: ${profileState.barId.ifBlank { "-" }}", color = Color(0xFF374151), style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    // Rejection banner with reason (if any)
                    if (profileState.verificationStatus == "rejected") {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE5E5)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Application Rejected",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xFFB00020)
                                )
                                val reason = profileState.rejectionReason
                                if (!reason.isNullOrBlank()) {
                                    Text(
                                        text = "Reason: $reason",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF7A1C1C)
                                    )
                                } else {
                                    Text(
                                        text = "Your application was rejected by the admin.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF7A1C1C)
                                    )
                                }
                            }
                        }
                    }

                    // Availability (place near top for visibility)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Column {
                                Text("Availability", color = Color(0xFF111827), style = MaterialTheme.typography.titleMedium)
                                Text("Toggle to show if you are available to take cases", color = Color(0xFF6B7280), style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(
                                checked = profileState.availability,
                                onCheckedChange = { checked ->
                                    profileState = profileState.copy(availability = checked)
                                    val targetId = profileState.lawyerDocId ?: uid
                                    if (targetId != null) {
                                        db.collection("lawyers").document(targetId)
                                            .update(
                                                mapOf(
                                                    "availability" to checked,
                                                    "updatedAt" to com.google.firebase.Timestamp.now()
                                                )
                                            )
                                    }
                                }
                            )
                        }
                    }

                    // Contact Information
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            ReadOnlyRow("Email", profileState.email)
                            ReadOnlyRow("Phone Number", profileState.contact)
                        }
                    }

                    // Professional Information
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            ReadOnlyRow("State", profileState.state)
                            ReadOnlyRow("Specialization", profileState.specialization)
                            ReadOnlyRow("Languages", profileState.languages.joinToString(", "))
                        }
                    }

                    // Cases Handled Section (Editable)
                    Spacer(modifier = Modifier.height(8.dp))
                    var casesText by remember { mutableStateOf(profileState.casesHandled) }
                    var isEditingCases by remember { mutableStateOf(false) }
                    var isSavingCases by remember { mutableStateOf(false) }
                    
                                         // Update casesText when profileState.casesHandled changes
                     LaunchedEffect(profileState.casesHandled) {
                         if (!isEditingCases) {
                             casesText = profileState.casesHandled
                         }
                     }
                    
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Cases Handled (Optional)",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF111827)
                            )
                            Row {
                                if (isEditingCases) {
                                    TextButton(
                                        onClick = { 
                                            // Cancel editing - restore original text
                                            casesText = profileState.casesHandled
                                            isEditingCases = false
                                        },
                                        enabled = !isSavingCases
                                    ) {
                                        Text("Cancel")
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                TextButton(
                                onClick = { 
                                                                         if (isEditingCases) {
                                         // Save the cases asynchronously
                                         Log.d("MyLawyerProfile", "🔄 Saving cases: '$casesText'")
                                         isSavingCases = true
                                         val targetId = profileState.lawyerDocId ?: uid
                                         Log.d("MyLawyerProfile", "🔄 Target ID: $targetId")
                                         if (targetId != null) {
                                             coroutineScope.launch {
                                                 try {
                                                     Log.d("MyLawyerProfile", "🔄 Starting Firestore update...")
                                                     db.collection("lawyers").document(targetId)
                                                         .update(
                                                             mapOf(
                                                                 "casesHandled" to casesText,
                                                                 "updatedAt" to com.google.firebase.Timestamp.now()
                                                             )
                                                         )
                                                         .await()
                                                     
                                                     Log.d("MyLawyerProfile", "✅ Firestore update successful")
                                                     // Update local state after successful save
                                                     profileState = profileState.copy(casesHandled = casesText)
                                                     isEditingCases = false
                                                     isSavingCases = false
                                                     Log.d("MyLawyerProfile", "✅ Local state updated")
                                                 } catch (e: Exception) {
                                                     Log.e("MyLawyerProfile", "❌ Failed to save cases: ${e.message}")
                                                     isSavingCases = false
                                                 }
                                             }
                                        } else {
                                            // If no targetId, just update local state
                                            profileState = profileState.copy(casesHandled = casesText)
                                            isEditingCases = false
                                            isSavingCases = false
                                        }
                                    } else {
                                        isEditingCases = true
                                    }
                                },
                                enabled = !isSavingCases
                            ) {
                                if (isSavingCases) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Saving...")
                                } else {
                                    Text(if (isEditingCases) "Save" else "Edit")
                                }
                            }
                        }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (isEditingCases) {
                            OutlinedTextField(
                                value = casesText,
                                onValueChange = { casesText = it },
                                placeholder = { Text("Describe the cases you've handled, your experience, and notable achievements...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 120.dp),
                                maxLines = 10,
                                minLines = 4,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color(0xFF111827),
                                    unfocusedTextColor = Color(0xFF111827),
                                    cursorColor = MaterialTheme.colorScheme.primary,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color(0xFF9CA3AF),
                                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                                    unfocusedLabelColor = Color(0xFF6B7280)
                                )
                            )
                        } else {
                            Text(
                                text = casesText.ifBlank { "No cases added yet. Click 'Edit' to describe your experience and cases handled." },
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (casesText.isBlank()) Color(0xFF6B7280) else Color(0xFF111827),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                    }
                    // Availability moved to top; no duplicate here

                    // Bottom spacer to avoid nav-bar overlap
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
        }
    }
}

@Composable
private fun ReadOnlyRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color(0xFF6B7280))
        Text(value.ifBlank { "-" }, style = MaterialTheme.typography.bodyLarge, color = Color(0xFF111827))
    }
}


