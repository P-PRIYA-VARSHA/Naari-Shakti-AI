@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.shaktibotprobono

import com.example.shaktibotprobono.AdminLoginScreen
import com.example.shaktibotprobono.AdminDashboardScreen
import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.text.style.TextAlign
import android.util.Log
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import androidx.activity.compose.BackHandler

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    
    // Create a single shared DocumentUploadViewModel instance
    val sharedDocumentUploadViewModel: DocumentUploadViewModel = viewModel()
    
    // Handle system back button
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    
    BackHandler(enabled = currentRoute != "userTypeSelection") {
        if (currentRoute != "userTypeSelection") {
            navController.popBackStack()
        }
    }
    
    NavHost(navController = navController, startDestination = "userTypeSelection") {

        // Pro Bono Feature - Entry Point
        composable("proBono") {
            ProBonoFeature(navController = navController)
        }

        composable("userTypeSelection") {
            UserTypeSelectionScreen(
                onUserTypeSelected = { userType ->
                    try {
                        when (userType) {
                            UserType.VICTIM -> {
                                Log.d("AppNavigation", "🔄 Navigating to lawyerList")
                                navController.navigate("lawyerList")
                            }
                            UserType.LAWYER -> {
                                Log.d("AppNavigation", "🔄 Navigating to lawyerEntryChoice")
                                navController.navigate("lawyerEntryChoice")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("AppNavigation", "❌ Navigation error: ${e.message}")
                        e.printStackTrace()
                    }
                },
                onAdminLogin = {
                    try {
                        Log.d("AppNavigation", "🔄 Navigating to adminLogin")
                        navController.navigate("adminLogin")
                    } catch (e: Exception) {
                        Log.e("AppNavigation", "❌ Navigation error: ${e.message}")
                        e.printStackTrace()
                    }
                }
            )
        }

        // Lawyer Registration Flow
        composable("lawyerRegistration") {
            LawyerRegistrationScreen(
                onRegistrationComplete = {
                    try {
                        Log.d("AppNavigation", "🔄 Navigating to emailVerification")
                        navController.navigate("emailVerification")
                    } catch (e: Exception) {
                        Log.e("AppNavigation", "❌ Navigation error: ${e.message}")
                        e.printStackTrace()
                    }
                },
                onBackPressed = {
                    try {
                        Log.d("AppNavigation", "🔄 Navigating back from lawyerRegistration")
                        navController.popBackStack()
                    } catch (e: Exception) {
                        Log.e("AppNavigation", "❌ Navigation error: ${e.message}")
                        e.printStackTrace()
                    }
                }
            )
        }

// Admin Login
        composable("adminLogin") {
            AdminLoginScreen(
                onLoginSuccess = {
                    try {
                        Log.d("AppNavigation", "🔄 Navigating to adminDashboard")
                        navController.navigate("adminDashboard") {
                            popUpTo("adminLogin") { inclusive = true }
                        }
                    } catch (e: Exception) {
                        Log.e("AppNavigation", "❌ Navigation error: ${e.message}")
                        e.printStackTrace()
                    }
                },
                onBackPressed = {
                    try {
                        Log.d("AppNavigation", "🔄 Navigating back from adminLogin")
                        navController.popBackStack()
                    } catch (e: Exception) {
                        Log.e("AppNavigation", "❌ Navigation error: ${e.message}")
                        e.printStackTrace()
                    }
                }
            )
        }

        // Admin Dashboard
        composable("adminDashboard") {
            AdminDashboardScreen(
                onLogout = {
                    try {
                        Log.d("AppNavigation", "🔄 Navigating to adminLogin")
                        navController.navigate("adminLogin") {
                            popUpTo("adminDashboard") { inclusive = true }
                        }
                    } catch (e: Exception) {
                        Log.e("AppNavigation", "❌ Navigation error: ${e.message}")
                        e.printStackTrace()
                    }
                },
                onLawyerClick = { lawyerId ->
                    try {
                        Log.d("AppNavigation", "🔄 Navigating to lawyerDetail with ID: $lawyerId")
                        navController.navigate("lawyerDetail/$lawyerId")
                    } catch (e: Exception) {
                        Log.e("AppNavigation", "❌ Navigation error: ${e.message}")
                        e.printStackTrace()
                    }
                }
            )
        }
        // Email Verification
        composable("emailVerification") {
            EmailVerificationScreen(
                onEmailVerified = {
                    try {
                        Log.d("AppNavigation", "🔄 Navigating to documentUpload")
                        navController.navigate("documentUpload")
                        Log.d("AppNavigation", "✅ Navigation to documentUpload successful")
                    } catch (e: Exception) {
                        Log.e("AppNavigation", "❌ Navigation error to documentUpload: ${e.message}")
                        e.printStackTrace()
                        // Don't rethrow the exception to prevent app crash
                    }
                },
                onBackPressed = {
                    try {
                        Log.d("AppNavigation", "🔄 Back from emailVerification")
                        navController.popBackStack()
                    } catch (e: Exception) {
                        Log.e("AppNavigation", "❌ Navigation error: ${e.message}")
                        e.printStackTrace()
                    }
                }
            )
        }

        // Document Upload
        composable("documentUpload") {
            DocumentUploadScreen(
                onUploadComplete = {
                    try {
                        Log.d("AppNavigation", "🔄 Navigating to success screen")
                        navController.navigate("success") {
                            popUpTo("documentUpload") { inclusive = true }
                        }
                        Log.d("AppNavigation", "✅ Navigation to success screen successful")
                    } catch (e: Exception) {
                        Log.e("AppNavigation", "❌ Navigation error to success: ${e.message}")
                        e.printStackTrace()
                    }
                },
                onBackPressed = {
                    try {
                        Log.d("AppNavigation", "🔄 Back from documentUpload")
                        navController.popBackStack()
                    } catch (e: Exception) {
                        Log.e("AppNavigation", "❌ Navigation error: ${e.message}")
                        e.printStackTrace()
                    }
                }
            )
        }

        // Success route to land after submission
        composable("success") {
            VerificationCompleteScreen(
                onContinue = {
                    try {
                        Log.d("AppNavigation", "🔄 From success -> userTypeSelection")
                        navController.navigate("userTypeSelection") {
                            popUpTo("userTypeSelection") { inclusive = true }
                        }
                    } catch (e: Exception) {
                        Log.e("AppNavigation", "❌ Navigation error from success: ${e.message}")
                        e.printStackTrace()
                    }
                },
                onBackPressed = {
                    try {
                        Log.d("AppNavigation", "🔄 Back from success")
                        navController.popBackStack()
                    } catch (e: Exception) {
                        Log.e("AppNavigation", "❌ Navigation error: ${e.message}")
                        e.printStackTrace()
                    }
                }
            )
        }

        // Verification Complete
        composable("verificationComplete") {
            VerificationCompleteScreen(
                onContinue = {
                    try {
                        Log.d("AppNavigation", "🔄 Navigating to lawyerList")
                        navController.navigate("lawyerList") {
                            popUpTo("proBono") { inclusive = true }
                        }
                    } catch (e: Exception) {
                        Log.e("AppNavigation", "❌ Navigation error: ${e.message}")
                        e.printStackTrace()
                    }
                },
                onBackPressed = {
                    try {
                        Log.d("AppNavigation", "🔄 Back from verificationComplete")
                        navController.popBackStack()
                    } catch (e: Exception) {
                        Log.e("AppNavigation", "❌ Navigation error: ${e.message}")
                        e.printStackTrace()
                    }
                }
            )
        }

        // Victim Flow - Lawyer List
        composable("lawyerList") {
            LawyerListScreen(
                onLawyerClick = { lawyer ->
                    try {
                        val idOrBar = (lawyer.id ?: lawyer.barId).orEmpty()
                        if (idOrBar.isBlank()) {
                            Log.e("AppNavigation", "❌ Cannot navigate to profile: both id and barId are blank for ${lawyer.name}")
                        } else {
                            val safeId = android.net.Uri.encode(idOrBar)
                            val json = com.google.gson.Gson().toJson(lawyer)
                            val safeJson = android.net.Uri.encode(json)
                            Log.d("AppNavigation", "🔄 Navigating to lawyerProfile?lawyerId=$safeId&lawyerJson=$safeJson (raw=$idOrBar)")
                            navController.navigate("lawyerProfile?lawyerId=$safeId&lawyerJson=$safeJson")
                        }
                    } catch (e: Exception) {
                        Log.e("AppNavigation", "❌ Navigation error: ${e.message}")
                        e.printStackTrace()
                    }
                },
                onBackPressed = {
                    try {
                        Log.d("AppNavigation", "🔄 Back from lawyerList")
                        navController.popBackStack()
                    } catch (e: Exception) {
                        Log.e("AppNavigation", "❌ Navigation error: ${e.message}")
                        e.printStackTrace()
                    }
                }
            )
        }

        // Lawyer Profile
        composable(
            route = "lawyerProfile?lawyerId={lawyerId}&lawyerJson={lawyerJson}",
            arguments = listOf(
                navArgument("lawyerId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = ""
                },
                navArgument("lawyerJson") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val rawId = backStackEntry.arguments?.getString("lawyerId") ?: ""
            val rawJson = backStackEntry.arguments?.getString("lawyerJson") ?: ""
            val decodedId = android.net.Uri.decode(rawId)
            val decodedJson = android.net.Uri.decode(rawJson)
            val initialLawyer = try {
                if (decodedJson.isNotBlank()) com.google.gson.Gson().fromJson(decodedJson, Lawyer::class.java) else null
            } catch (e: Exception) { null }
            LawyerProfileScreen(
                lawyerIdOrBarId = decodedId,
                initialLawyer = initialLawyer,
                onBackPressed = {
                    try {
                        Log.d("AppNavigation", "🔄 Back from lawyerProfile")
                        navController.popBackStack()
                    } catch (e: Exception) {
                        Log.e("AppNavigation", "❌ Navigation error: ${e.message}")
                        e.printStackTrace()
                    }
                }
            )
        }
        
        // New vs Existing lawyer choice
        composable("lawyerEntryChoice") {
            LawyerEntryChoiceScreen(
                onNew = { navController.navigate("lawyerRegistration") },
                onExisting = {
                    // Always require explicit sign-in for existing lawyers
                    navController.navigate("lawyerLogin")
                },
                onBack = { navController.popBackStack() }
            )
        }

        // Lawyer email/password login
        composable("lawyerLogin") {
            LawyerLoginScreen(
                onLoginSuccessVerified = { navController.navigate("myLawyerProfile") },
                onLoginSuccessUnverified = { navController.navigate("emailVerification") },
                onBackPressed = { navController.popBackStack() }
            )
        }

        // Self profile screen for logged-in lawyer
        composable("myLawyerProfile") {
            MyLawyerProfileScreen(
                onBackPressed = {
                    try {
                        Log.e("AppNavigation", "🔄 BACK FROM MYLAWYERPROFILE - NAVIGATION TRIGGERED")
                        Log.d("AppNavigation", "🔄 Back from myLawyerProfile")
                        val success = navController.popBackStack()
                        Log.e("AppNavigation", "✅ PopBackStack result: $success")
                        Log.d("AppNavigation", "✅ PopBackStack result: $success")
                    } catch (e: Exception) {
                        Log.e("AppNavigation", "❌ Navigation error: ${e.message}")
                        e.printStackTrace()
                    }
                }
            )
        }
        // Lawyer Detail (Admin View)
        composable("lawyerDetail/{lawyerId}") { backStackEntry ->
            val lawyerId = backStackEntry.arguments?.getString("lawyerId")
            lawyerId?.let { id ->
                LawyerDetailScreen(
                    lawyerId = id,
                    onBackPressed = {
                        try {
                            Log.d("AppNavigation", "🔄 Navigating back from lawyerDetail")
                            navController.popBackStack()
                        } catch (e: Exception) {
                            Log.e("AppNavigation", "❌ Navigation error: ${e.message}")
                            e.printStackTrace()
                        }
                    },
                    onRefreshRequested = {
                        try {
                            Log.d("AppNavigation", "🔄 Refreshing admin dashboard after lawyer action")
                            // Navigate back to admin dashboard to trigger refresh
                            navController.popBackStack()
                        } catch (e: Exception) {
                            Log.e("AppNavigation", "❌ Navigation error: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ProBonoFeature(navController: NavHostController) {
    // This is the entry point for the Pro Bono feature
    // It will show the user type selection screen
    UserTypeSelectionScreen(
        onUserTypeSelected = { userType ->
            try {
                when (userType) {
                    UserType.VICTIM -> {
                        Log.d("ProBonoFeature", "🔄 Navigating to lawyerList")
                        navController.navigate("lawyerList")
                    }
                    UserType.LAWYER -> {
                        Log.d("ProBonoFeature", "🔄 Navigating to lawyerEntryChoice")
                        navController.navigate("lawyerEntryChoice")
                    }
                }
            } catch (e: Exception) {
                Log.e("ProBonoFeature", "❌ Navigation error: ${e.message}")
                e.printStackTrace()
            }
        },
        onAdminLogin = {
            try {
                Log.d("ProBonoFeature", "🔄 Navigating to adminLogin")
                navController.navigate("adminLogin")
            } catch (e: Exception) {
                Log.e("ProBonoFeature", "❌ Navigation error: ${e.message}")
                e.printStackTrace()
            }
        },
        onBackPressed = {
            try {
                Log.d("ProBonoFeature", "🔄 Back from ProBonoFeature")
                navController.popBackStack()
            } catch (e: Exception) {
                Log.e("ProBonoFeature", "❌ Navigation error: ${e.message}")
                e.printStackTrace()
            }
        }
    )
}
