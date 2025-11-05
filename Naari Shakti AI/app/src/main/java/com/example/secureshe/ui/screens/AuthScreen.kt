package com.example.secureshe.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import androidx.navigation.NavController
import com.example.secureshe.ui.components.LoginForm
import com.example.secureshe.ui.components.SignUpForm
import com.example.secureshe.ui.viewmodels.AuthState
import com.example.secureshe.ui.viewmodels.AuthViewModel

@Composable
fun AuthScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    var isLogin by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        authViewModel.checkAuthState()
    }
    
    val authState by authViewModel.authState.collectAsState()
    
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> {
                navController.navigate("dashboard") {
                    popUpTo("auth") { inclusive = true }
                }
            }
            else -> {}
        }
    }
    
    // Render auth UI only when not authenticated and not loading (prevents flash from AI tools back path)
    if (authState !is AuthState.Success && authState !is AuthState.Loading) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = if (isSystemInDarkTheme()) {
                            // Dark mode Midnight Blue (same as dashboard)
                            listOf(Color(0xFF1A2A3D), Color(0xFF2A3D57))
                        } else {
                            // Light mode Rose palette (same as dashboard)
                            listOf(Color(0xFFF8D7E8), Color(0xFFE3C7FF))
                        }
                    )
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Show image only on the login page, above title and subtitle
            if (isLogin) {
                Image(
                    painter = painterResource(id = com.example.secureshe.R.drawable.login_image),
                    contentDescription = "Login Illustration",
                    modifier = Modifier
                        .size(180.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(
                text = "Naari Shakti AI",
                style = TextStyle(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Black,
                    fontSize = 44.sp,
                    letterSpacing = 2.5.sp,
                    shadow = Shadow(
                        color = if (isSystemInDarkTheme()) Color(0xFFB71C1C).copy(alpha = 0.75f) else Color(0xFFC62828).copy(alpha = 0.6f),
                        offset = Offset(5f, 5f),
                        blurRadius = 10f
                    ),
                    brush = Brush.linearGradient(
                        colors = if (isSystemInDarkTheme()) {
                            // Dark mode: deeper red shades
                            listOf(
                                Color(0xFFE53935), // red 600
                                Color(0xFFD32F2F), // red 700
                                Color(0xFFC62828), // red 800
                                Color(0xFFB71C1C)  // red 900
                            )
                        } else {
                            // Light mode: darker red shades
                            listOf(
                                Color(0xFFEF5350), // red 400
                                Color(0xFFE53935), // red 600
                                Color(0xFFD32F2F), // red 700
                                Color(0xFFC62828)  // red 800
                            )
                        }
                    )
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(18.dp))
            
            Text(
                text = "Your Safety, Our Priority",
                style = TextStyle(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 22.sp,
                    letterSpacing = 1.8.sp,
                    shadow = Shadow(
                        color = if (isSystemInDarkTheme()) Color(0xFFC62828).copy(alpha = 0.6f) else Color(0xFFE53935).copy(alpha = 0.5f),
                        offset = Offset(3f, 3f),
                        blurRadius = 8f
                    ),
                    brush = Brush.linearGradient(
                        colors = if (isSystemInDarkTheme()) {
                            listOf(
                                Color(0xFFE57373), // red 300
                                Color(0xFFEF5350), // red 400
                                Color(0xFFE53935), // red 600
                                Color(0xFFD32F2F)  // red 700
                            )
                        } else {
                            listOf(
                                Color(0xFFEF9A9A), // red 200
                                Color(0xFFE57373), // red 300
                                Color(0xFFEF5350), // red 400
                                Color(0xFFE53935)  // red 600
                            )
                        }
                    )
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            if (isLogin) {
                LoginForm(
                    onLoginClick = { email, password ->
                        authViewModel.signIn(email, password)
                    },
                    onSwitchToSignUp = { isLogin = false },
                    isLoading = authState is AuthState.Loading,
                    errorMessage = if (authState is AuthState.Error) (authState as AuthState.Error).message else null
                )
            } else {
                SignUpForm(
                    onSignUpClick = { name, email, phone, password, pin ->
                        authViewModel.signUp(name, email, phone, password, pin)
                    },
                    onSwitchToLogin = { isLogin = true },
                    isLoading = authState is AuthState.Loading,
                    errorMessage = if (authState is AuthState.Error) (authState as AuthState.Error).message else null
                )
            }
        }
    } else {
        // Minimal placeholder to avoid visual flicker while navigation occurs
        Box(modifier = Modifier.fillMaxSize()) {}
    }
}