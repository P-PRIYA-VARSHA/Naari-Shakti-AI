package com.example.shaktibotprobono

import com.example.shaktibotprobono.LawyerListScreen
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.shaktibotprobono.Lawyer
import com.example.shaktibotprobono.LawyerListScreen
import com.example.shaktibotprobono.LawyerProfileScreen
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.shaktibotprobono.ui.theme.ShaktiBotProBonoTheme

// ✅ Firebase imports
import com.google.firebase.FirebaseApp // ✅ ADD THIS
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import android.util.Log
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import kotlinx.coroutines.CoroutineExceptionHandler
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button

class MainActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set up global exception handler
        setupGlobalExceptionHandler()

        try {
            // ✅ Initialize Firebase
            FirebaseApp.initializeApp(this) // 🟢 CRITICAL LINE
            Log.d(TAG, "✅ Firebase initialized successfully")

            // ✅ Firebase Firestore test
            val db = Firebase.firestore
            db.collection("test")
                .add(mapOf("message" to "Hello from ShaktiBot!"))
                .addOnSuccessListener { docRef ->
                    Log.d(TAG, "✅ Document added with ID: ${docRef.id}")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "❌ Error adding document: $e")
                }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error initializing Firebase: ${e.message}")
            e.printStackTrace()
        }

        enableEdgeToEdge()
        setContent {
            ShaktiBotProBonoTheme {
                LawyerStatusWatcher()
                // Wrap AppNavigation in error boundary
                ErrorBoundary { AppNavigation() }
            }
        }
    }
    
    private fun setupGlobalExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "🚨 Uncaught exception in thread ${thread.name}: ${throwable.message}")
            throwable.printStackTrace()
            
            // Call the default handler to prevent app from crashing
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}

@Composable
fun ErrorBoundary(content: @Composable () -> Unit) {
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    if (hasError) {
        // Show error UI instead of crashing
        Scaffold { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Something went wrong",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        hasError = false
                        errorMessage = ""
                    }
                ) {
                    Text("Try Again")
                }
            }
        }
    } else {
        content()
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ShaktiBotProBonoTheme {
        Greeting("Android")
    }
}

