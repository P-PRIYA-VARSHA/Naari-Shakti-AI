package com.example.secureshe.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import android.content.Intent
import android.app.Activity
import android.widget.Toast
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.navigation.NavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.secureshe.ui.viewmodels.AuthViewModel
import com.example.secureshe.ui.viewmodels.SOSViewModel
import com.example.secureshe.ui.viewmodels.SOSState
import com.example.secureshe.ui.components.PINConfirmationDialog
import kotlinx.coroutines.launch
import com.example.secureshe.R
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs
import com.example.secureshe.legal.EnhancedChatbotActivity
import com.example.secureshe.ui.components.LocalDrawerController
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api

// --------- UI Helpers & Theme (UI-only) ---------

private data class FeatureItem(val title: String, val description: String, val bgRes: Int)

// HeaderBar removed in favor of Material 3 LargeTopAppBar within Scaffold

@Composable
private fun ShieldSOSButton(onClick: () -> Unit) {
    // Standalone red shield with ripple (no square card)
    val pulse = remember { androidx.compose.animation.core.Animatable(0.85f) }
    val glowScale = remember { androidx.compose.animation.core.Animatable(0f) }
    val glowAlpha = remember { androidx.compose.animation.core.Animatable(0f) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        while (true) {
            pulse.animateTo(1.05f, animationSpec = androidx.compose.animation.core.tween(900))
            pulse.animateTo(0.85f, animationSpec = androidx.compose.animation.core.tween(900))
        }
    }
    Box(
        modifier = Modifier
            .size(160.dp)
            .graphicsLayer { scaleX = pulse.value; scaleY = pulse.value }
            // Use default Material3 ripple; trigger red glow on tap
            .clickable {
                onClick()
                scope.launch {
                    glowScale.snapTo(0.8f)
                    glowAlpha.snapTo(0.4f)
                    // animate outward and fade
                    launch { glowScale.animateTo(1.6f, animationSpec = androidx.compose.animation.core.tween(450)) }
                    glowAlpha.animateTo(0f, animationSpec = androidx.compose.animation.core.tween(450))
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Continuous red wave glow (three waves)
        // Manually drive three waves using LaunchedEffect for broad compatibility
        val wave1 = remember { androidx.compose.animation.core.Animatable(0f) }
        val wave2 = remember { androidx.compose.animation.core.Animatable(0.33f) }
        val wave3 = remember { androidx.compose.animation.core.Animatable(0.66f) }
        LaunchedEffect(Unit) {
            while (true) {
                wave1.animateTo(1f, animationSpec = androidx.compose.animation.core.tween(1800, easing = androidx.compose.animation.core.LinearEasing))
                wave1.snapTo(0f)
            }
        }
        LaunchedEffect(Unit) {
            while (true) {
                wave2.animateTo(1f, animationSpec = androidx.compose.animation.core.tween(1800, easing = androidx.compose.animation.core.LinearEasing))
                wave2.snapTo(0f)
            }
        }
        LaunchedEffect(Unit) {
            while (true) {
                wave3.animateTo(1f, animationSpec = androidx.compose.animation.core.tween(1800, easing = androidx.compose.animation.core.LinearEasing))
                wave3.snapTo(0f)
            }
        }
        listOf(wave1.value, wave2.value, wave3.value).forEach { v ->
            val scale = 1f + v * 1.7f
            val alpha = (0.45f * (1f - v)).coerceIn(0f, 0.45f)
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(MaterialTheme.colorScheme.error.copy(alpha = 0.75f), Color.Transparent),
                            radius = 360f
                        )
                    )
            )
        }
        // Tap burst red glow
        Box(
            modifier = Modifier
                .size(160.dp)
                .graphicsLayer { scaleX = glowScale.value; scaleY = glowScale.value; alpha = glowAlpha.value }
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(MaterialTheme.colorScheme.error.copy(alpha = 0.8f), Color.Transparent),
                        radius = 360f
                    )
                )
        )
        Image(
            painter = painterResource(id = R.drawable.ic_shield_provided),
            contentDescription = "Start SOS",
            modifier = Modifier.size(140.dp)
        )
    }
}

@Composable
private fun ActiveShield() {
    // Pulsing active red shield
    val pulse = remember { androidx.compose.animation.core.Animatable(1.0f) }
    val wave1 = remember { androidx.compose.animation.core.Animatable(0f) }
    val wave2 = remember { androidx.compose.animation.core.Animatable(0.33f) }
    val wave3 = remember { androidx.compose.animation.core.Animatable(0.66f) }
    LaunchedEffect(Unit) {
        while (true) {
            pulse.animateTo(1.15f, animationSpec = androidx.compose.animation.core.tween(700))
            pulse.animateTo(1.0f, animationSpec = androidx.compose.animation.core.tween(700))
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            wave1.animateTo(1f, animationSpec = androidx.compose.animation.core.tween(1800, easing = androidx.compose.animation.core.LinearEasing))
            wave1.snapTo(0f)
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            wave2.animateTo(1f, animationSpec = androidx.compose.animation.core.tween(1800, easing = androidx.compose.animation.core.LinearEasing))
            wave2.snapTo(0f)
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            wave3.animateTo(1f, animationSpec = androidx.compose.animation.core.tween(1800, easing = androidx.compose.animation.core.LinearEasing))
            wave3.snapTo(0f)
        }
    }
    Box(contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(180.dp)
            .graphicsLayer { scaleX = pulse.value; scaleY = pulse.value }) {
        // Continuous waves in active state
        listOf(wave1.value, wave2.value, wave3.value).forEach { v ->
            val scale = 1f + v * 1.7f
            val alpha = (0.45f * (1f - v)).coerceIn(0f, 0.45f)
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(MaterialTheme.colorScheme.error.copy(alpha = 0.75f), Color.Transparent),
                            radius = 360f
                        )
                    )
            )
        }
        Image(
            painter = painterResource(id = R.drawable.ic_shield_provided),
            contentDescription = "SOS Active",
            modifier = Modifier.size(160.dp)
        )
    }
}

@Composable
private fun FeatureFlipCard(
    title: String,
    description: String,
    bgRes: Int,
    flipped: Boolean,
    onToggle: () -> Unit,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val rotation = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(flipped) {
        rotation.animateTo(if (flipped) 180f else 0f, animationSpec = androidx.compose.animation.core.tween(400))
    }
    val density = LocalDensity.current.density
    val frontVisible = abs(rotation.value) <= 90f
    Card(
        modifier = Modifier
            .fillMaxWidth(0.80f)
            .height(180.dp)
            .graphicsLayer {
                rotationY = rotation.value
                cameraDistance = 12f * density
            }
            .clickable(enabled = frontVisible) { onToggle() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (frontVisible) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Background image cover
                    Image(
                        painter = painterResource(id = bgRes),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Dark overlay for readability
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f))
                    )
                    // Centered content
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onToggle,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text("View Details", color = Color.White)
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationY = 180f }
                        .background(brush = Brush.verticalGradient(colors = listOf(Color(0xFF2D2A6A), Color(0xFF4C46B8))))
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                        .pointerInput(Unit) { detectTapGestures(onTap = { onToggle() }) },
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = description,
                        color = Color.White,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    if (actionLabel != null && onAction != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onAction,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text(actionLabel, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppFooter() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isSystemInDarkTheme()) Color(0xFF2E2436) else Color(0xFF9B7AE0)
                ) // footer aligned to App Bar pastel
                .padding(vertical = 44.dp)
                .heightIn(min = 220.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "\"Your Safety, Our Priority\"",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                letterSpacing = 0.8.sp
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun DashboardScreen(
    navController: NavController,
    authViewModel: AuthViewModel = hiltViewModel(),
    sosViewModel: SOSViewModel = hiltViewModel(),
    openDrawerInitially: Boolean = false
) {
    val context = LocalContext.current
    val sosState by sosViewModel.sosState.collectAsState()
    val emergencyContacts by sosViewModel.emergencyContacts.collectAsState()

    var showPINDialog by remember { mutableStateOf(false) }
    val drawer = LocalDrawerController.current

    // Close app on back press when Dashboard is the root
    val activity = (context as? Activity)
    BackHandler(enabled = navController.previousBackStackEntry == null) {
        activity?.finish()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "Naari Shakti AI", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { drawer.open() }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    // App logo at top right, 32dp
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "App Logo",
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = colorResource(id = R.color.header_color),
                    titleContentColor = if (isSystemInDarkTheme()) Color.White else Color(0xFF3A3152),
                    navigationIconContentColor = if (isSystemInDarkTheme()) Color.White else Color(0xFF3A3152),
                    actionIconContentColor = if (isSystemInDarkTheme()) Color.White else Color(0xFF3A3152)
                )
            )
        }
    ) { innerPadding ->
    Column(
        modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isSystemInDarkTheme()) {
                        // Dark mode Midnight Blue (lighter hues for more air)
                        listOf(Color(0xFF1A2A3D), Color(0xFF2A3D57))
                    } else {
                        // Light mode Rose palette (deeper pastels for contrast)
                        listOf(Color(0xFFF8D7E8), Color(0xFFE3C7FF))
                    }
                )
            )
            .verticalScroll(rememberScrollState())
    ) {

            // Removed emergency contacts status card (button below already provides entry)

            // Centered Shield SOS Button area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                when (sosState) {
                    is SOSState.Idle -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(id = R.string.sos_press_note),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 14.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier
                                    .padding(horizontal = 24.dp)
                                    .fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            ShieldSOSButton(
                                onClick = {
                                    if (emergencyContacts.isEmpty()) {
                                        navController.navigate("emergency_contacts")
                                    } else {
                                        sosViewModel.startSOS(context)
                                    }
                                }
                            )
                        }
                    }
                    is SOSState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(160.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 6.dp
                        )
                    }
                    is SOSState.Active -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // Active state uses the same shield visual, in alert color with pulse
                            ActiveShield()
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showPINDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = "Stop")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Stop SOS")
                            }
                        }
                    }
                    is SOSState.Error -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Error,
                                        contentDescription = "Error",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Error",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = (sosState as SOSState.Error).message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(onClick = { sosViewModel.clearError() }) { Text("Dismiss") }
                            }
                        }
                    }
                }
            }

            // Set Emergency Contacts button
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { navController.navigate("emergency_contacts") },
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Contacts, contentDescription = "Contacts", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Set Emergency Contacts", color = Color.White)
            }

            // Feature Cards with flip animation (stacked vertically)
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Explore Features",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            val features = listOf(
                FeatureItem(
                    title = "SOS",
                    description = "With one tap, SOS instantly alerts your emergency contacts by sending urgent messages to three people at once. It also shares your live GPS location and starts priority-based calling — if the first contact doesn’t respond, the call automatically goes to the next.",
                    bgRes = R.drawable.sos_bg
                ),
                FeatureItem(
                    title = "Legal Evidence",
                    description = "In emergencies, this feature allows you to quickly capture video and audio as proof. The recordings can later support legal action, helping ensure your rights and safety are protected.",
                    bgRes = R.drawable.legal_evidence_bg
                ),
                FeatureItem(
                    title = "AI Legal Chatbot",
                    description = "The AI-powered legal chatbot guides you with instant legal assistance during emergencies. It includes a legal research assistant, form filling, document generation, know your rights, and filing guidance — making legal help accessible anytime.",
                    bgRes = R.drawable.ai_chatbot_bg
                ),
                FeatureItem(
                    title = "ProBono – Lawyer Help",
                    description = "ProBono helps you connect with qualified lawyers for immediate legal support. It ensures timely professional guidance and assistance whenever you face legal challenges.",
                    bgRes = R.drawable.probono_bg
                ),
                FeatureItem(
                    title = "Safe Path",
                    description = "Safe Path helps you navigate through the safest routes with real-time GPS guidance. It highlights secure areas, avoids unsafe zones, and ensures your journey is fast, safe, and reliable.",
                    bgRes = R.drawable.safe_path_bg
                )
            )
            var openedIndex by remember { mutableStateOf<Int?>(null) }
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                features.forEachIndexed { index, item ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        FeatureFlipCard(
                            title = item.title,
                            description = item.description,
                            bgRes = item.bgRes,
                            flipped = openedIndex == index,
                            onToggle = {
                                openedIndex = if (openedIndex == index) null else index
                            },
                            actionLabel = if (item.title == "AI Legal Chatbot") "Open Chatbot" else null,
                            onAction = if (item.title == "AI Legal Chatbot") {
                                { context.startActivity(Intent(context, EnhancedChatbotActivity::class.java)) }
                            } else null
                        )
                    }
                }
            }

            // Footer
            Spacer(modifier = Modifier.height(28.dp))
            AppFooter()
        }

        // Removed confirmation dialog; SOS starts immediately on shield tap

        // PIN Confirmation Dialog
        if (showPINDialog) {
            PINConfirmationDialog(
                onValidate = { pin -> sosViewModel.isPinValid(pin) },
                onSuccess = {
                    sosViewModel.stopSOS(context)
                    showPINDialog = false
                },
                onDismiss = { showPINDialog = false }
            )
        }
    }
    }
