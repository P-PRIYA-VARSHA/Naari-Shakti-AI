@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.shaktibotprobono

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.example.secureshe.R

@Composable
fun AdminDashboardScreen(
    onLogout: () -> Unit,
    onLawyerClick: (String) -> Unit,
    viewModel: AdminViewModel = viewModel()
) {
    val adminState by viewModel.adminState.collectAsState()
    
    // Refresh lawyers list when screen becomes visible
    LaunchedEffect(Unit) {
        viewModel.clearError() // Clear any lingering error messages
        viewModel.loadLawyers()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard") },
                actions = {
                    IconButton(onClick = { viewModel.logout(); onLogout() }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Admin Dashboard",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Welcome, ${adminState.currentAdmin?.name ?: "Admin"}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            // Stats
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "📊 Statistics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${adminState.lawyers.filter { it.verificationStatus != "rejected" }.size}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Active Lawyers",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${adminState.lawyers.count { it.verificationStatus == "approved" }}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Verified",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${adminState.lawyers.count { it.verificationStatus == "pending" }}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Pending",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${adminState.lawyers.count { it.verificationStatus == "rejected" }}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Rejected",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            // Lawyer List
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Lawyer Management",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Filter out rejected lawyers for the main list
            val activeLawyers = adminState.lawyers.filter { it.verificationStatus != "rejected" }
            
            if (activeLawyers.isEmpty()) {
                item {
                    Text(
                        text = "No active lawyers found.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                items(activeLawyers) { lawyer ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = when (lawyer.verificationStatus) {
                                "approved" -> MaterialTheme.colorScheme.secondaryContainer
                                "pending" -> MaterialTheme.colorScheme.surfaceVariant
                                else -> MaterialTheme.colorScheme.errorContainer
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onLawyerClick(lawyer.id) }
                            ) {
                                Text(
                                    text = lawyer.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Bar ID: ${lawyer.barId}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "Email: ${lawyer.email}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = when (lawyer.verificationStatus) {
                                        "approved" -> "✅ Approved"
                                        "pending" -> "⏳ Pending Review"
                                        "rejected" -> "❌ Rejected"
                                        else -> "❓ Unknown Status"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = when (lawyer.verificationStatus) {
                                        "approved" -> MaterialTheme.colorScheme.primary
                                        "pending" -> MaterialTheme.colorScheme.tertiary
                                        "rejected" -> MaterialTheme.colorScheme.error
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            IconButton(
                                onClick = { viewModel.deleteLawyer(lawyer.id) }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Lawyer")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            
            // Rejected Lawyers Section (Optional - show if there are any)
            val rejectedLawyers = adminState.lawyers.filter { it.verificationStatus == "rejected" }
            if (rejectedLawyers.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Rejected Applications (${rejectedLawyers.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "These applications have been rejected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                items(rejectedLawyers) { lawyer ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onLawyerClick(lawyer.id) }
                            ) {
                                Text(
                                    text = lawyer.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Bar ID: ${lawyer.barId}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "❌ Rejected",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Medium
                                )
                                if (!lawyer.rejectionReason.isNullOrBlank()) {
                                    Text(
                                        text = "Reason: ${lawyer.rejectionReason}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                            IconButton(
                                onClick = { viewModel.deleteLawyer(lawyer.id) }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Lawyer")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}