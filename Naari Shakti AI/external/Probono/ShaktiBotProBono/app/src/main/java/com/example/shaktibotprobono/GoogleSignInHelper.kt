package com.example.shaktibotprobono

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes

class GoogleSignInHelper(private val context: Context) {
    
    private lateinit var googleSignInClient: GoogleSignInClient
    
    init {
        setupGoogleSignIn()
    }
    
    private fun setupGoogleSignIn() {
        // IMPORTANT: This is configured for the admin email: shaktibot.naariai@gmail.com
        // The system will automatically sign in with this admin account for Google Drive backup
        
        // If you have a Web OAuth client ID and plan to exchange codes on a backend,
        // set it here. Otherwise, we don't need server auth code.
        val webClientId: String? = null
        
        try {
            // Configure Google Sign-In with Drive API scopes
            val builder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(Scope(DriveScopes.DRIVE_FILE)) // Enable Drive API access

            // Only request ID token / server auth code if you actually use a backend exchange
            if (!webClientId.isNullOrBlank()) {
                builder.requestIdToken(webClientId)
                builder.requestServerAuthCode(webClientId)
            }

            val gso = builder.build()
            
            googleSignInClient = GoogleSignIn.getClient(context, gso)
            
            android.util.Log.d("GoogleSignInHelper", "✅ Google Sign-In configured for admin account")
            android.util.Log.d("GoogleSignInHelper", "📧 Admin email: shaktibot.naariai@gmail.com")
            android.util.Log.d("GoogleSignInHelper", "☁️ Drive API scopes enabled")
            if (!webClientId.isNullOrBlank()) {
                android.util.Log.d("GoogleSignInHelper", "🔑 Web Client ID set")
            } else {
                android.util.Log.d("GoogleSignInHelper", "ℹ️ No Web Client ID (using on-device credential)")
            }
            
        } catch (e: Exception) {
            android.util.Log.e("GoogleSignInHelper", "❌ Failed to setup Google Sign-In: ${e.message}")
            e.printStackTrace()
        }
    }
    
    fun getSignInIntent(): Intent {
        try {
            android.util.Log.d("GoogleSignInHelper", "🔍 Creating Google Sign-In intent...")
            val intent = googleSignInClient.signInIntent
            android.util.Log.d("GoogleSignInHelper", "✅ Google Sign-In intent created successfully")
            return intent
        } catch (e: Exception) {
            android.util.Log.e("GoogleSignInHelper", "❌ Error creating sign-in intent: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
    
    fun signOut() {
        try {
            googleSignInClient.signOut()
                .addOnCompleteListener {
                    android.util.Log.d("GoogleSignInHelper", "🔓 User signed out from Google")
                    // Clear any cached account data
                    val account = GoogleSignIn.getLastSignedInAccount(context)
                    if (account == null) {
                        android.util.Log.d("GoogleSignInHelper", "✅ Account cleared successfully")
                    } else {
                        android.util.Log.w("GoogleSignInHelper", "⚠️ Account still exists after sign-out")
                    }
                }
                .addOnFailureListener { exception ->
                    android.util.Log.e("GoogleSignInHelper", "❌ Sign-out failed: ${exception.message}")
                }
        } catch (e: Exception) {
            android.util.Log.e("GoogleSignInHelper", "❌ Error during sign-out: ${e.message}")
        }
    }
    
    fun revokeAccess() {
        googleSignInClient.revokeAccess()
        android.util.Log.d("GoogleSignInHelper", "🚫 Google access revoked")
    }
    
    // Check if user is signed in with admin account
    fun isSignedInWithAdminAccount(): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return account?.email == "shaktibot.naariai@gmail.com"
    }
    
    // Get current signed in account
    fun getCurrentAccount(): com.google.android.gms.auth.api.signin.GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }
    
    // Check if user has Drive API access
    fun hasDriveAccess(): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return account != null && GoogleSignIn.hasPermissions(
            account,
            Scope(DriveScopes.DRIVE_FILE)
        )
    }
    
    // Debug method to check sign-in status
    fun debugSignInStatus() {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account != null) {
            android.util.Log.d("GoogleSignInHelper", "✅ User is signed in: ${account.email}")
            android.util.Log.d("GoogleSignInHelper", "🔑 Drive scope granted: ${hasDriveAccess()}")
            android.util.Log.d("GoogleSignInHelper", "🔑 Drive access granted: ${hasDriveAccess()}")
        } else {
            android.util.Log.d("GoogleSignInHelper", "❌ No user is currently signed in")
        }
    }
    
    // Test method to verify configuration
    fun testConfiguration() {
        android.util.Log.d("GoogleSignInHelper", "🧪 Testing Google Sign-In configuration...")
        android.util.Log.d("GoogleSignInHelper", "📧 Admin email: shaktibot.naariai@gmail.com")
        android.util.Log.d("GoogleSignInHelper", "🔑 Client ID: 192438304129-om3bti3n5vk5idpf0lrmbcveh0bs303i.apps.googleusercontent.com")
        android.util.Log.d("GoogleSignInHelper", "☁️ Drive scopes enabled: true")
        android.util.Log.d("GoogleSignInHelper", "🔑 Server auth code requested: true")
        
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account != null) {
            android.util.Log.d("GoogleSignInHelper", "✅ User already signed in: ${account.email}")
            android.util.Log.d("GoogleSignInHelper", "🔑 Drive access: ${hasDriveAccess()}")
        } else {
            android.util.Log.d("GoogleSignInHelper", "ℹ️ No user signed in - ready for sign-in")
        }
    }
    
    // Comprehensive debug method to identify issues
    fun debugSignInIssues() {
        android.util.Log.d("GoogleSignInHelper", "🔍 === GOOGLE SIGN-IN DEBUG === 🔍")
        
        // Check if Google Play Services is available
        try {
            val googleApiAvailability = com.google.android.gms.common.GoogleApiAvailability.getInstance()
            val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context)
            if (resultCode == com.google.android.gms.common.ConnectionResult.SUCCESS) {
                android.util.Log.d("GoogleSignInHelper", "✅ Google Play Services available")
            } else {
                android.util.Log.e("GoogleSignInHelper", "❌ Google Play Services not available: $resultCode")
            }
        } catch (e: Exception) {
            android.util.Log.e("GoogleSignInHelper", "❌ Error checking Google Play Services: ${e.message}")
        }
        
        // Check current account status
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account != null) {
            android.util.Log.d("GoogleSignInHelper", "✅ Account found: ${account.email}")
            android.util.Log.d("GoogleSignInHelper", "🔑 ID Token: ${if (!account.idToken.isNullOrEmpty()) "Available" else "Missing"}")
            android.util.Log.d("GoogleSignInHelper", "🔑 Server Auth Code: ${if (!account.serverAuthCode.isNullOrEmpty()) "Available" else "Missing"}")
            android.util.Log.d("GoogleSignInHelper", "🔑 Drive Access: ${hasDriveAccess()}")
        } else {
            android.util.Log.d("GoogleSignInHelper", "ℹ️ No account signed in")
        }
        
        // Check if sign-in client is initialized
        try {
            val intent = googleSignInClient.signInIntent
            android.util.Log.d("GoogleSignInHelper", "✅ Sign-in client initialized successfully")
        } catch (e: Exception) {
            android.util.Log.e("GoogleSignInHelper", "❌ Sign-in client not initialized: ${e.message}")
        }
        
        android.util.Log.d("GoogleSignInHelper", "🔍 === END DEBUG === 🔍")
    }
} 