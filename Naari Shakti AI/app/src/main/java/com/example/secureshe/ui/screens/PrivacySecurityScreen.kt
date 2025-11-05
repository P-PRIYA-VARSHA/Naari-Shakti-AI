package com.example.secureshe.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.secureshe.R
import com.example.secureshe.ui.components.LocalDrawerController

@Composable
fun PrivacySecurityScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F7FF))
            .verticalScroll(rememberScrollState())
    ) {
        val drawer = LocalDrawerController.current
        TopHeader(
            title = "Privacy & Security",
            onMenuClick = { drawer.open() }
        )

        Spacer(Modifier.height(12.dp))

        // App Permissions Control
        SectionCard(title = "App Permissions Control") {
            PermissionRow(
                label = "Location access",
                note = "Needed for SOS & Safe Path"
            )
            Divider(Modifier.padding(vertical = 8.dp), color = Color(0xFFE1E5FF))
            PermissionRow(
                label = "Camera & microphone access",
                note = "For legal evidence recording"
            )
            Divider(Modifier.padding(vertical = 8.dp), color = Color(0xFFE1E5FF))
            PermissionRow(label = "Notifications access")
        }

        // Data Privacy Settings
        SectionCard(title = "Data Privacy Settings") {
            ReadonlyRow(
                label = "Who can access your location?",
                value = "Only emergency contacts"
            )
            Spacer(Modifier.height(8.dp))
            ToggleRow(label = "Enable auto-location sharing during SOS")
            Spacer(Modifier.height(8.dp))
            ToggleRow(label = "Auto-delete stored legal evidence after 45 days")
        }

        // Security Settings
        SectionCard(title = "Security Settings") {
            ToggleRow(label = "SOS Lock (Biometric)")
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Change Password / Reset PIN",
                color = Color(0xFF1A1A1A),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            BulletText("AI Chat won't be accessed by others.")
            BulletText("Lawyers are verified and information about them is accurate.")
        }

        // Data Management
        SectionCard(title = "Data Management") {
            ActionChipRow("Download My Data (compliance with data protection laws)")
            Spacer(Modifier.height(8.dp))
            ActionChipRow("Delete My Account (and all associated data)")
        }

        // Transparency Section
        SectionCard(title = "Transparency") {
            Text(
                text = "“We do not sell or misuse your data. Your safety and privacy are our top priority.”",
                color = Color(0xFF1A1A1A),
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        Spacer(Modifier.height(24.dp))
        FooterBand(text = "\"Strength In Your Hands\"")
    }
}

@Composable
private fun TopHeader(title: String, onMenuClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF5B6EAE), Color(0xFF6E7FC0))
                )
            )
            .height(80.dp)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            Icon(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "App Logo",
                tint = Color.Unspecified,
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF1FF)),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF1A1A1A),
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun PermissionRow(label: String, note: String? = null) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = Color(0xFF1A1A1A), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        note?.let {
            Spacer(Modifier.height(2.dp))
            Text(it, color = Color(0xFF475569), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ReadonlyRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = Color(0xFF1A1A1A), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(2.dp))
        Text(value, color = Color(0xFF1A1A1A), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ToggleRow(label: String) {
    var checked by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color(0xFF1A1A1A), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = { checked = it })
    }
}

@Composable
private fun ActionChipRow(text: String) {
    AssistChip(
        onClick = { /* TODO: wire actions */ },
        label = { Text(text, color = Color.Black) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = Color(0xFFEFF3FF),
            labelColor = Color.Black
        )
    )
}

@Composable
private fun BulletText(text: String) {
    Text("• $text", color = Color(0xFF1A1A1A), style = MaterialTheme.typography.bodyLarge)
}

@Composable
private fun FooterBand(text: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF3B3E63))
                .padding(vertical = 28.dp)
                .heightIn(min = 140.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                letterSpacing = 0.6.sp
            )
        }
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .background(Color.White)
        )
    }
}
