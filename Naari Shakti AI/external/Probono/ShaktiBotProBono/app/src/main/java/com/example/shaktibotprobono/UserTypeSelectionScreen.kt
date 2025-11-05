@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.shaktibotprobono

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.secureshe.R
import com.example.secureshe.ui.components.LocalDrawerController

enum class UserType {
    VICTIM,
    LAWYER
}

@Composable
fun UserTypeSelectionScreen(
    onUserTypeSelected: (UserType) -> Unit,
    onAdminLogin: () -> Unit = {},
    onBackPressed: () -> Unit = {}
) {
    // Align header/footer with main app (Dashboard) theme
    val isDark = isSystemInDarkTheme()
    val headerColor = if (isDark) Color(0xFF2E2436) else Color(0xFFC7A4FF)
    val footerStripColor = if (isDark) Color(0xFF2E2436) else Color(0xFF9B7AE0)
    val pageBackground = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFEDEBFF), // light lavender
            Color(0xFFDDE9FF), // soft blue
            Color(0xFFF3F4F6)  // pastel grey
        )
    )

    Scaffold(
        topBar = {
            // Header aligned with Dashboard top app bar colors
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
                    val drawer = LocalDrawerController.current
                    IconButton(onClick = { drawer.open() }) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = if (isDark) Color.White else Color(0xFF3A3152)
                        )
                    }
                    // Title sits right next to hamburger
                    Text(
                        text = "Pro Bono",
                        color = if (isDark) Color.White else Color(0xFF3A3152),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    // Right logo for visual balance
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
            // Simple lavender footer strip with centered quote and nav bar patch
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(footerStripColor)
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "\"You're Never Alone\"",
                        color = Color.White,
                        fontSize = 20.sp,
                        letterSpacing = 0.6.sp,
                        textAlign = TextAlign.Center
                    )
                }
                // Patch to fit device navigation bar height
                Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(pageBackground)
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Info card about the platform
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F4FF))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "About Pro Bono",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFF2D3142),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Pro bono means ‘for the public good’. We connect individuals with verified pro bono lawyers. Each lawyer is vetted for trust and safety, making support accessible and reliable.",
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                        color = Color(0xFF3A3F58),
                        textAlign = TextAlign.Start
                    )
                }
            }

            Text(
                text = "Choose your role to get started",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = Color(0xFF2D3142)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Role rows with arrow navigation — gradient, rounded, with subtle press animation
            val btnShape = RoundedCornerShape(20.dp)
            val gradient = Brush.horizontalGradient(listOf(Color(0xFF8EA1FF), Color(0xFFC4A0FF)))
            var victimPressed by remember { mutableStateOf(false) }
            val victimScale by animateFloatAsState(targetValue = if (victimPressed) 0.98f else 1f, label = "victimScale")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer(scaleX = victimScale, scaleY = victimScale)
                    .shadow(14.dp, btnShape)
                    .background(brush = gradient, shape = btnShape)
                    .heightIn(min = 72.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                victimPressed = true
                                val released = tryAwaitRelease()
                                victimPressed = false
                                if (released) {
                                    onUserTypeSelected(UserType.VICTIM)
                                }
                            }
                        )
                    }
                    .padding(horizontal = 22.dp, vertical = 24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "I need legal help",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Default.ArrowForward, contentDescription = "Go", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            var lawyerPressed by remember { mutableStateOf(false) }
            val lawyerScale by animateFloatAsState(targetValue = if (lawyerPressed) 0.98f else 1f, label = "lawyerScale")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer(scaleX = lawyerScale, scaleY = lawyerScale)
                    .shadow(14.dp, btnShape)
                    .background(brush = gradient, shape = btnShape)
                    .heightIn(min = 72.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                lawyerPressed = true
                                val released = tryAwaitRelease()
                                lawyerPressed = false
                                if (released) {
                                    onUserTypeSelected(UserType.LAWYER)
                                }
                            }
                        )
                    }
                    .padding(horizontal = 20.dp, vertical = 22.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "I'm a lawyer offering help",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Default.ArrowForward, contentDescription = "Go", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            // Admin Option - distinct outlined button
            OutlinedButton(
                onClick = onAdminLogin,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, Color(0xFF5C6BC0)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF5C6BC0)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Admin"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Admin Login", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(16.dp))
            // Info text
            Text(
                text = "Tap an option to continue",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF2D3142),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}