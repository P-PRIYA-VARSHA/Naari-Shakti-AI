@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.shaktibotprobono

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.ui.res.painterResource
import com.example.secureshe.R
import androidx.compose.foundation.isSystemInDarkTheme

@Composable
fun LawyerRegistrationScreen(
    onRegistrationComplete: () -> Unit,
    onBackPressed: () -> Unit = {},
    viewModel: LawyerRegistrationViewModel = viewModel()
) {
    val registrationState by viewModel.registrationState.collectAsState()
    
    LaunchedEffect(registrationState.isSuccess) {
        if (registrationState.isSuccess) {
            onRegistrationComplete()
        }
    }

    val isDark = isSystemInDarkTheme()
    val headerColor = if (isDark) Color(0xFF2E2436) else Color(0xFFC7A4FF)

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
        }
    ) { paddingValues ->
        val pageBackground = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFEDEBFF), // light lavender
                Color(0xFFDDE9FF)  // soft blue
            )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(pageBackground)
                .padding(paddingValues)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = "Join Our Pro Bono Network",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF111827),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Help those in need by offering your legal expertise",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF6B7280),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Registration Form
            RegistrationForm(
                viewModel = viewModel,
                isLoading = registrationState.isLoading,
                errorMessage = registrationState.errorMessage
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Register Button
            val isFormValid by viewModel.isFormValid.collectAsState()
            val gradient = Brush.horizontalGradient(listOf(Color(0xFFB8C6FF), Color(0xFF7FB3FF)))
            val btnShape = RoundedCornerShape(12.dp)
            Button(
                onClick = { viewModel.registerLawyer() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(btnShape)
                    .background(gradient, btnShape),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                enabled = !registrationState.isLoading && isFormValid,
                contentPadding = PaddingValues(0.dp)
            ) {
                if (registrationState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Create Account", color = Color.White, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Bottom spacing only (tagline removed per request)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RegistrationForm(
    viewModel: LawyerRegistrationViewModel,
    isLoading: Boolean,
    errorMessage: String?
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Name
        val name by viewModel.name.collectAsState()
        val nameError by viewModel.nameError.collectAsState()
        Text("Full Name", color = Color(0xFF374151), style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(
            value = name,
            onValueChange = { viewModel.updateName(it) },
            placeholder = { Text("Enter your full name") },
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp)),
            enabled = !isLoading,
            isError = nameError.isNotEmpty(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color(0xFF111827),
                unfocusedTextColor = Color(0xFF111827),
                focusedBorderColor = Color(0xFFB39DFF),
                unfocusedBorderColor = Color(0xFFE5E7EB),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                cursorColor = Color(0xFF6D28D9),
                focusedPlaceholderColor = Color(0xFF6B7280),
                unfocusedPlaceholderColor = Color(0xFF6B7280)
            ),
            shape = RoundedCornerShape(12.dp)
        )
        if (nameError.isNotEmpty()) {
            Text(
                text = nameError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        // Email
        val email by viewModel.email.collectAsState()
        val emailError by viewModel.emailError.collectAsState()
        Text("Email Address", color = Color(0xFF374151), style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(
            value = email,
            onValueChange = { viewModel.updateEmail(it) },
            placeholder = { Text("name@example.com") },
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp)),
            enabled = !isLoading,
            isError = emailError.isNotEmpty(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color(0xFF111827),
                unfocusedTextColor = Color(0xFF111827),
                focusedBorderColor = Color(0xFFB39DFF),
                unfocusedBorderColor = Color(0xFFE5E7EB),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                cursorColor = Color(0xFF6D28D9),
                focusedPlaceholderColor = Color(0xFF6B7280),
                unfocusedPlaceholderColor = Color(0xFF6B7280)
            ),
            shape = RoundedCornerShape(12.dp)
        )
        if (emailError.isNotEmpty()) {
            Text(
                text = emailError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        // Phone
        val phone by viewModel.phone.collectAsState()
        val phoneError by viewModel.phoneError.collectAsState()
        Text("Phone Number", color = Color(0xFF374151), style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(
            value = phone,
            onValueChange = { viewModel.updatePhone(it) },
            placeholder = { Text("+91-XXXXXXXXXX") },
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp)),
            enabled = !isLoading,
            isError = phoneError.isNotEmpty(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color(0xFF111827),
                unfocusedTextColor = Color(0xFF111827),
                focusedBorderColor = Color(0xFFB39DFF),
                unfocusedBorderColor = Color(0xFFE5E7EB),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                cursorColor = Color(0xFF6D28D9),
                focusedPlaceholderColor = Color(0xFF6B7280),
                unfocusedPlaceholderColor = Color(0xFF6B7280)
            ),
            shape = RoundedCornerShape(12.dp)
        )
        if (phoneError.isNotEmpty()) {
            Text(
                text = phoneError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        // Bar ID
        val barId by viewModel.barId.collectAsState()
        val barIdError by viewModel.barIdError.collectAsState()
        Text("Enrollment Number", color = Color(0xFF374151), style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(
            value = barId,
            onValueChange = { viewModel.updateBarId(it) },
            placeholder = { Text("Bar Council Enrollment No.") },
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp)),
            enabled = !isLoading,
            isError = barIdError.isNotEmpty(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color(0xFF111827),
                unfocusedTextColor = Color(0xFF111827),
                focusedBorderColor = Color(0xFFB39DFF),
                unfocusedBorderColor = Color(0xFFE5E7EB),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                cursorColor = Color(0xFF6D28D9),
                focusedPlaceholderColor = Color(0xFF6B7280),
                unfocusedPlaceholderColor = Color(0xFF6B7280)
            ),
            shape = RoundedCornerShape(12.dp)
        )
        if (barIdError.isNotEmpty()) {
            Text(
                text = barIdError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        // State
        val state by viewModel.state.collectAsState()
        var stateExpanded by remember { mutableStateOf(false) }
        Text("State", color = Color(0xFF374151), style = MaterialTheme.typography.labelLarge)
        ExposedDropdownMenuBox(
            expanded = stateExpanded,
            onExpandedChange = { stateExpanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = state,
                onValueChange = {},
                readOnly = true,
                placeholder = { Text("Select your state") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stateExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                enabled = !isLoading,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFF111827),
                    unfocusedTextColor = Color(0xFF6B7280),
                    focusedBorderColor = Color(0xFFB39DFF),
                    unfocusedBorderColor = Color(0xFFE5E7EB),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    cursorColor = Color(0xFF6D28D9),
                    focusedPlaceholderColor = Color(0xFF6B7280),
                    unfocusedPlaceholderColor = Color(0xFF6B7280)
                ),
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(
                expanded = stateExpanded,
                onDismissRequest = { stateExpanded = false },
                containerColor = Color.White,
                tonalElevation = 6.dp,
                shadowElevation = 6.dp
            ) {
                Utils.getAllIndianStates().filter { it != "All" }.forEach { state ->
                    DropdownMenuItem(
                        text = { Text(state, color = Color(0xFF111827)) },
                        onClick = {
                            viewModel.updateState(state)
                            stateExpanded = false
                        }
                    )
                }
            }
        }
        
        // Specialization
        val specialization by viewModel.specialization.collectAsState()
        var specializationExpanded by remember { mutableStateOf(false) }
        Text("Specialization", color = Color(0xFF374151), style = MaterialTheme.typography.labelLarge)
        ExposedDropdownMenuBox(
            expanded = specializationExpanded,
            onExpandedChange = { specializationExpanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = specialization,
                onValueChange = {},
                readOnly = true,
                placeholder = { Text("Select specialization", color = Color(0xFF6B7280)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = specializationExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                enabled = !isLoading,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFF111827),
                    unfocusedTextColor = Color(0xFF6B7280),
                    focusedBorderColor = Color(0xFFB39DFF),
                    unfocusedBorderColor = Color(0xFFE5E7EB),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    cursorColor = Color(0xFF6D28D9),
                    focusedPlaceholderColor = Color(0xFF6B7280),
                    unfocusedPlaceholderColor = Color(0xFF6B7280)
                ),
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(
                expanded = specializationExpanded,
                onDismissRequest = { specializationExpanded = false },
                containerColor = Color.White,
                tonalElevation = 6.dp,
                shadowElevation = 6.dp
            ) {
                Utils.getAllSpecializations().filter { it != "All" }.forEach { specialization ->
                    DropdownMenuItem(
                        text = { Text(specialization, color = Color(0xFF111827)) },
                        onClick = {
                            viewModel.updateSpecialization(specialization)
                            specializationExpanded = false
                        }
                    )
                }
            }
        }
        
        // Languages (multi-select with tags)
        val selectedLanguages by viewModel.selectedLanguages.collectAsState()
        var languageExpanded by remember { mutableStateOf(false) }
        Text("Languages", color = Color(0xFF374151), style = MaterialTheme.typography.labelLarge)
        ExposedDropdownMenuBox(
            expanded = languageExpanded,
            onExpandedChange = { languageExpanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedLanguages.joinToString(", "),
                onValueChange = {},
                readOnly = true,
                placeholder = { Text("Choose languages") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = languageExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                enabled = !isLoading,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFF111827),
                    unfocusedTextColor = Color(0xFF111827),
                    focusedBorderColor = Color(0xFFB39DFF),
                    unfocusedBorderColor = Color(0xFFE5E7EB),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    cursorColor = Color(0xFF6D28D9),
                    focusedPlaceholderColor = Color(0xFF6B7280),
                    unfocusedPlaceholderColor = Color(0xFF6B7280)
                ),
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(
                expanded = languageExpanded,
                onDismissRequest = { languageExpanded = false },
                containerColor = Color.White,
                tonalElevation = 6.dp,
                shadowElevation = 6.dp
            ) {
                Utils.getAllLanguages().filter { it != "All" }.forEach { language ->
                    DropdownMenuItem(
                        text = { 
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = selectedLanguages.contains(language),
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            viewModel.addLanguage(language)
                                        } else {
                                            viewModel.removeLanguage(language)
                                        }
                                    }
                                )
                                Text(language, color = Color(0xFF111827))
                            }
                        },
                        onClick = {
                            if (selectedLanguages.contains(language)) {
                                viewModel.removeLanguage(language)
                            } else {
                                viewModel.addLanguage(language)
                            }
                        }
                    )
                }
            }
        }
        if (selectedLanguages.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                selectedLanguages.forEach { lang ->
                    AssistChip(
                        onClick = { viewModel.removeLanguage(lang) },
                        label = { Text(lang) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color(0xFFF3F4F6),
                            labelColor = Color(0xFF374151)
                        )
                    )
                }
            }
        }

        // Gender
        val gender by viewModel.gender.collectAsState()
        var genderExpanded by remember { mutableStateOf(false) }
        Text("Gender", color = Color(0xFF374151), style = MaterialTheme.typography.labelLarge)
        ExposedDropdownMenuBox(
            expanded = genderExpanded,
            onExpandedChange = { genderExpanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = gender,
                onValueChange = {},
                readOnly = true,
                placeholder = { Text("Select gender") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                enabled = !isLoading,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFF111827),
                    unfocusedTextColor = Color(0xFF111827),
                    focusedBorderColor = Color(0xFFB39DFF),
                    unfocusedBorderColor = Color(0xFFE5E7EB),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    cursorColor = Color(0xFF6D28D9),
                    focusedPlaceholderColor = Color(0xFF6B7280),
                    unfocusedPlaceholderColor = Color(0xFF6B7280)
                ),
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(
                expanded = genderExpanded,
                onDismissRequest = { genderExpanded = false },
                containerColor = Color.White,
                tonalElevation = 6.dp,
                shadowElevation = 6.dp
            ) {
                listOf("Male", "Female", "Other", "Prefer not to say").forEach { genderOption ->
                    DropdownMenuItem(
                        text = { Text(genderOption, color = Color(0xFF111827)) },
                        onClick = {
                            viewModel.updateGender(genderOption)
                            genderExpanded = false
                        }
                    )
                }
            }
        }

        // Password
        val password by viewModel.password.collectAsState()
        val confirmPassword by viewModel.confirmPassword.collectAsState()
        val passwordError by viewModel.passwordError.collectAsState()
        Text("Password", color = Color(0xFF374151), style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(
            value = password,
            onValueChange = { viewModel.updatePassword(it) },
            placeholder = { Text("Min 8 chars, mixed case, number") },
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp)),
            enabled = !isLoading,
            isError = passwordError.isNotEmpty(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color(0xFF111827),
                unfocusedTextColor = Color(0xFF111827),
                focusedBorderColor = Color(0xFFB39DFF),
                unfocusedBorderColor = Color(0xFFE5E7EB),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                cursorColor = Color(0xFF6D28D9),
                focusedPlaceholderColor = Color(0xFF6B7280),
                unfocusedPlaceholderColor = Color(0xFF6B7280)
            ),
            shape = RoundedCornerShape(12.dp)
        )
        Text("Confirm Password", color = Color(0xFF374151), style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { viewModel.updateConfirmPassword(it) },
            placeholder = { Text("Re-enter your password") },
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp)),
            enabled = !isLoading,
            isError = passwordError.isNotEmpty(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color(0xFF111827),
                unfocusedTextColor = Color(0xFF111827),
                focusedBorderColor = Color(0xFFB39DFF),
                unfocusedBorderColor = Color(0xFFE5E7EB),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                cursorColor = Color(0xFF6D28D9),
                focusedPlaceholderColor = Color(0xFF6B7280),
                unfocusedPlaceholderColor = Color(0xFF6B7280)
            ),
            shape = RoundedCornerShape(12.dp)
        )
        if (passwordError.isNotEmpty()) {
            Text(
                text = passwordError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        // Error message
        if (errorMessage != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
} 