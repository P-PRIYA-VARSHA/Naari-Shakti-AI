@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.shaktibotprobono

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
// no need to import Lawyer explicitly
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.colorResource
import com.example.secureshe.R

@Composable
fun LawyerProfileScreen(
    lawyerIdOrBarId: String,
    initialLawyer: Lawyer? = null,
    onBackPressed: () -> Unit = {}
) {
    val viewModel: LawyerProfileViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val state = viewModel.state.collectAsState()
    
    LaunchedEffect(lawyerIdOrBarId, initialLawyer) {
        println("🔎 LawyerProfileScreen arg='${lawyerIdOrBarId}'")
        if (initialLawyer != null) {
            viewModel.prefillLawyer(initialLawyer)
        }
        viewModel.loadLawyer(lawyerIdOrBarId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(text = state.value.lawyer?.name ?: "Lawyer Profile")
                        if (state.value.lawyer?.verified == true) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(Color(0xFF8FB3FF), Color(0xFFD6C8FF))
                                        )
                                    )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "✓",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Verified",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
        val current = state.value
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF7F5FF), // lighter lavender (muted)
                            Color(0xFFF1F6FF), // lighter soft blue
                            Color(0xFFFAFAFD)  // light neutral
                        )
                    )
                )
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            when {
                current.isLoading -> {
                    CircularProgressIndicator()
                }
                current.errorMessage != null -> {
                    Text(current.errorMessage!!, color = MaterialTheme.colorScheme.error)
                }
                current.lawyer == null -> {
                    Text("Lawyer not found", color = MaterialTheme.colorScheme.error)
                }
                else -> {
                    val lawyer = current.lawyer

                    // Profile header
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Circular avatar placeholder
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFFB8C6FF), Color(0xFF8FB3FF))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (lawyer.name?.firstOrNull()?.uppercase() ?: "L"),
                                color = Color.White,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = lawyer.name ?: "Lawyer",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        if (lawyer.verified == true) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(Color(0xFF25D366), Color(0xFF1EBE5D)) // WhatsApp-like green
                                        )
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("✓ Verified", color = Color.White, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Bar ID: ${lawyer.barId ?: "-"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF374151)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    val labelColor = Color(0xFF2E2E5E)
                    val valueColor = Color(0xFF5E6B8F)

                    // Contact Information section title
                    Text(
                        text = "Contact Information",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF2E2E5E)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Stacked input-like cards
                    FieldCard(label = "Email", value = lawyer.email ?: "-", icon = "📧", labelColor = labelColor, valueColor = valueColor)
                    FieldCard(label = "Phone Number", value = lawyer.contactNumber ?: "-", icon = "📞", labelColor = labelColor, valueColor = valueColor)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Professional Information section title
                    Text(
                        text = "Professional Information",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF2E2E5E)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    FieldCard(label = "State", value = lawyer.state ?: "-", icon = "📍", labelColor = labelColor, valueColor = valueColor)
                    FieldCard(label = "Specialization", value = lawyer.specialization ?: "-", icon = "⚖️", labelColor = labelColor, valueColor = valueColor)
                    FieldCard(label = "Languages", value = lawyer.languages.joinToString(", "), icon = "🌐", labelColor = labelColor, valueColor = valueColor)
                    FieldCard(label = "Availability", value = if (lawyer.availability) "Available" else "Not Available", icon = "📅", labelColor = labelColor, valueColor = valueColor)

                    // Cases Handled & Experience (visible only on detailed profile)
                    val cases = lawyer.casesHandled.orEmpty().trim()
                    if (cases.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Cases Handled & Experience",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF2E2E5E)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F8FF)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Text(
                                text = cases,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                color = Color(0xFF5E6B8F),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FormRow(
    label: String,
    value: String,
    icon: String? = null,
    labelColor: Color = Color.Black,
    valueColor: Color = Color.DarkGray
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        if (!icon.isNullOrBlank()) {
            Text(text = icon, modifier = Modifier.padding(end = 10.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "$label:", color = labelColor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, color = valueColor, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun FieldCard(
    label: String,
    value: String,
    icon: String,
    labelColor: Color,
    valueColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F8FF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(text = icon, modifier = Modifier.padding(end = 12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, color = labelColor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = value, color = valueColor, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
    Spacer(modifier = Modifier.height(10.dp))
}
