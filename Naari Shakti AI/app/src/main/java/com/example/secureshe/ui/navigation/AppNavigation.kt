package com.example.secureshe.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.secureshe.ui.screens.*
import com.example.secureshe.ui.viewmodels.AuthViewModel
import com.example.shaktibotprobono.*

@Composable
fun AppNavigation(navController: NavHostController, openDrawerInitially: Boolean = false) {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    BackHandler(enabled = currentRoute != "userTypeSelection" && currentRoute != "myLawyerProfile") {
        if (currentRoute != "userTypeSelection") {
            navController.popBackStack()
        }
    }

    // Use a fixed start destination; AuthScreen will redirect to dashboard if already signed in.
    NavHost(navController = navController, startDestination = "auth") {
        composable("auth") {
            val authViewModel: AuthViewModel = hiltViewModel()
            AuthScreen(navController, authViewModel)
        }
        composable("dashboard") {
            val authViewModel: AuthViewModel = hiltViewModel()
            DashboardScreen(navController, authViewModel, openDrawerInitially = openDrawerInitially)
        }
        composable("emergency_contacts") {
            EmergencyContactsScreen(navController)
        }
        composable("profile") {
            val authViewModel: AuthViewModel = hiltViewModel()
            ProfileScreen(navController, authViewModel)
        }
        composable("safe_path") {
            SafePathScreen(navController)
        }
        composable("help") {
            HelpScreen(navController)
        }
        composable("privacy_security") {
            PrivacySecurityScreen(navController)
        }
        composable("permissions") {
            PermissionRequestScreen(
                navController = navController,
                onPermissionsGranted = {
                    navController.navigate("dashboard") {
                        popUpTo("permissions") { inclusive = true }
                    }
                }
            )
        }

        // Pro Bono Legal Help - External Flow
        composable("userTypeSelection") {
            UserTypeSelectionScreen(
                onUserTypeSelected = { userType ->
                    when (userType) {
                        UserType.VICTIM -> navController.navigate("lawyerList")
                        UserType.LAWYER -> navController.navigate("lawyerEntryChoice")
                    }
                },
                onAdminLogin = { navController.navigate("adminLogin") },
                onBackPressed = { navController.navigateUp() }
            )
        }

        composable("lawyerList") {
            LawyerListScreen(
                onLawyerClick = { lawyer ->
                    val idOrBar = (lawyer.id ?: lawyer.barId).orEmpty()
                    if (idOrBar.isNotBlank()) {
                        val safeId = android.net.Uri.encode(idOrBar)
                        val json = com.google.gson.Gson().toJson(lawyer)
                        val safeJson = android.net.Uri.encode(json)
                        navController.navigate("lawyerProfile?lawyerId=$safeId&lawyerJson=$safeJson")
                    }
                },
                onBackPressed = { navController.navigateUp() }
            )
        }

        composable(
            route = "lawyerProfile?lawyerId={lawyerId}&lawyerJson={lawyerJson}",
            arguments = listOf(
                navArgument("lawyerId") { type = NavType.StringType; nullable = true },
                navArgument("lawyerJson") { type = NavType.StringType; nullable = true }
            )
        ) { backStackEntry ->
            val rawId = backStackEntry.arguments?.getString("lawyerId") ?: ""
            val rawJson = backStackEntry.arguments?.getString("lawyerJson") ?: ""
            val decodedId = android.net.Uri.decode(rawId)
            val decodedJson = android.net.Uri.decode(rawJson)
            val initialLawyer = try {
                if (decodedJson.isNotBlank()) com.google.gson.Gson().fromJson(decodedJson, com.example.shaktibotprobono.Lawyer::class.java) else null
            } catch (e: Exception) { null }
            LawyerProfileScreen(
                lawyerIdOrBarId = decodedId,
                initialLawyer = initialLawyer,
                onBackPressed = { navController.navigateUp() }
            )
        }

        composable("lawyerEntryChoice") {
            LawyerEntryChoiceScreen(
                onNew = { navController.navigate("lawyerRegistration") },
                onExisting = { navController.navigate("lawyerLogin") },
                onBack = { navController.navigateUp() }
            )
        }

        composable("lawyerRegistration") {
            LawyerRegistrationScreen(
                onRegistrationComplete = { navController.navigate("emailVerification") },
                onBackPressed = { navController.navigateUp() }
            )
        }

        composable("emailVerification") {
            EmailVerificationScreen(
                onEmailVerified = { navController.navigate("documentUpload") },
                onBackPressed = { navController.navigateUp() }
            )
        }

        composable("documentUpload") {
            DocumentUploadScreen(
                onUploadComplete = { navController.navigate("success") },
                onBackPressed = { navController.navigateUp() }
            )
        }

        composable("success") {
            VerificationCompleteScreen(
                onContinue = {
                    navController.navigate("userTypeSelection") {
                        popUpTo("userTypeSelection") { inclusive = false }
                    }
                },
                onBackPressed = { navController.navigateUp() }
            )
        }

        composable("lawyerLogin") {
            LawyerLoginScreen(
                onLoginSuccessVerified = {
                    navController.navigate("myLawyerProfile") {
                        popUpTo("lawyerLogin") { inclusive = true }
                    }
                },
                onLoginSuccessUnverified = { navController.navigate("emailVerification") },
                onBackPressed = { navController.navigateUp() }
            )
        }

        composable("myLawyerProfile") {
            // Ensure back always pops to previous screen immediately
            BackHandler(enabled = true) { navController.popBackStack() }
            MyLawyerProfileScreen(
                onBackPressed = { navController.popBackStack() }
            )
        }

        composable("adminLogin") {
            AdminLoginScreen(
                onLoginSuccess = { navController.navigate("adminDashboard") },
                onBackPressed = { navController.navigateUp() }
            )
        }

        composable("adminDashboard") {
            AdminDashboardScreen(
                onLogout = { navController.popBackStack("userTypeSelection", inclusive = false) },
                onLawyerClick = { lawyerId -> navController.navigate("lawyerDetail/$lawyerId") }
            )
        }

        composable(
            route = "lawyerDetail/{lawyerId}",
            arguments = listOf(navArgument("lawyerId") { type = NavType.StringType })
        ) { backStackEntry ->
            val lawyerId = backStackEntry.arguments?.getString("lawyerId") ?: return@composable
            LawyerDetailScreen(
                lawyerId = lawyerId,
                onBackPressed = { navController.popBackStack() },
                onRefreshRequested = { }
            )
        }
    }
} 