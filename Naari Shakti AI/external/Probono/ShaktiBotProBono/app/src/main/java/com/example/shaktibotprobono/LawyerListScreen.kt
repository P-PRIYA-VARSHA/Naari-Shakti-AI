@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.shaktibotprobono

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.toArgb
import android.app.Activity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.clip
import com.example.secureshe.R
import androidx.compose.foundation.isSystemInDarkTheme

@Composable
fun LawyerListScreen(
    viewModel: LawyerViewModel = viewModel(),
    onLawyerClick: (Lawyer) -> Unit,
    onBackPressed: () -> Unit = {}
) {
    val lawyers by viewModel.lawyers.collectAsState()

    // Ensure system nav bar matches our light background (avoid black strip at bottom)
    val activity = LocalContext.current as? Activity
    SideEffect {
        activity?.window?.navigationBarColor = Color(0xFFF3F4F6).toArgb()
    }

    var selectedState by remember { mutableStateOf("All") }
    var selectedSpecialization by remember { mutableStateOf("All") }
    var selectedLanguage by remember { mutableStateOf("All") }
    var showAvailableOnly by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }

    // Trigger filter whenever these change
    LaunchedEffect(Unit) {
        // Initial fetch to populate when entering from UserTypeSelection
        viewModel.fetchLawyers()
    }
    LaunchedEffect(selectedState, selectedSpecialization, selectedLanguage, showAvailableOnly, searchQuery) {
        viewModel.applyFilters(
            state = selectedState,
            specialization = selectedSpecialization,
            language = selectedLanguage,
            availability = if (showAvailableOnly) true else null,
            query = searchQuery.text
        )
    }

    val isDark = isSystemInDarkTheme()
    val headerColor = if (isDark) Color(0xFF2E2436) else Color(0xFFC7A4FF)
    val pageBackground = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFEDEBFF), // light lavender
            Color(0xFFDDE9FF), // soft blue
            Color(0xFFF3F4F6)  // pastel grey
        )
    )

    Scaffold(
        topBar = {
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
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            text = " Pro Bono",
                            color = if (isDark) Color.White else Color(0xFF3A3152),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "App logo",
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .background(pageBackground)
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // 🔍 Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search by Name or Bar ID") },
                placeholder = { Text("Search by Name or Bar ID", color = Color(0xFF4B5563)) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(color = Color(0xFF111827)),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF8EA1FF),
                    unfocusedBorderColor = Color(0xFFB39DFF),
                    cursorColor = Color(0xFF8EA1FF),
                    focusedLabelColor = Color(0xFF5C6BC0),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 🏷 Dropdowns - First Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DropdownFilter(
                    label = "State",
                    options = Utils.getAllIndianStates(),
                    selected = selectedState,
                    onSelectedChange = { selectedState = it },
                    modifier = Modifier.weight(1f)
                )

                DropdownFilter(
                    label = "Specialization",
                    options = Utils.getAllSpecializations(),
                    selected = selectedSpecialization,
                    onSelectedChange = { selectedSpecialization = it },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 🏷 Dropdowns - Second Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DropdownFilter(
                    label = "Language",
                    options = Utils.getAllLanguages(),
                    selected = selectedLanguage,
                    onSelectedChange = { selectedLanguage = it },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ✅ Availability Checkbox (more visible)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                border = BorderStroke(1.dp, Color(0xFFB39DFF)),
                color = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .clickable { showAvailableOnly = !showAvailableOnly },
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = showAvailableOnly,
                        onCheckedChange = { showAvailableOnly = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF8EA1FF),
                            uncheckedColor = Color(0xFF8EA1FF),
                            checkmarkColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Only show available lawyers",
                        color = Color(0xFF111827),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 🧾 Lawyers List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(lawyers) { lawyer ->
                    LawyerCard(lawyer = lawyer, onClick = { onLawyerClick(lawyer) })
                }
            }
        }
    }
}



@Composable
fun DropdownFilter(
    label: String,
    options: List<String>,
    selected: String,
    onSelectedChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFB39DFF))
        ) {
            Text("$label: $selected")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(color = Color.White, shape = RoundedCornerShape(12.dp))
                .border(BorderStroke(1.dp, Color(0xFFB39DFF)), shape = RoundedCornerShape(12.dp))
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, color = Color(0xFF111827)) },
                    onClick = {
                        onSelectedChange(option)
                        expanded = false
                    },
                    colors = MenuDefaults.itemColors(textColor = Color(0xFF111827))
                )
            }
        }
    }
}

@Composable
fun LawyerCard(lawyer: Lawyer, onClick: () -> Unit) {
    val cardGradient = Brush.horizontalGradient(listOf(Color(0xFF6D5DD3), Color(0xFF2D5BFF))) // deep lavender -> royal blue
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        // Gradient background inside card
        Box(
            modifier = Modifier
                .background(cardGradient, shape = RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                // Header with name and verification badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        text = lawyer.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFFF9FAFB),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    // Verified Badge
                    if (lawyer.verified) {
                        Box(
                            modifier = Modifier
                                .background(
                                    Brush.horizontalGradient(listOf(Color(0xFF34C759), Color(0xFF2E865F))),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(text = "✓ Verified", color = Color.White, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Bar ID: ${lawyer.barId}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFD1D5DB)
                )
            }
        }
    }
}

