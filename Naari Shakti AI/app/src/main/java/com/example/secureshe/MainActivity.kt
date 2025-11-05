package com.example.secureshe

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.secureshe.ui.navigation.AppNavigation
import com.example.secureshe.ui.components.AppDrawerHost
import com.example.secureshe.ui.theme.SecureSheTheme
import dagger.hilt.android.AndroidEntryPoint
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    // Controls opening the drawer from external activities (e.g., AI Legal Tools)
    private var openDrawerInitiallyState by mutableStateOf(false)
    
    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.SEND_SMS,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.POST_NOTIFICATIONS
    )
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            android.util.Log.d("MainActivity", "All permissions granted")
        } else {
            android.util.Log.w("MainActivity", "Some permissions denied")
            permissions.entries.forEach { (permission, granted) ->
                if (!granted) {
                    android.util.Log.w("MainActivity", "Permission denied: $permission")
                }
            }

            // Handle restricted SMS on Android 14+ (SDK 34+) specially
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val smsDenied = permissions[Manifest.permission.SEND_SMS] == false
                if (smsDenied) {
                    // If rationale is not shown, it's likely restricted or "Don't ask again"
                    val showRationale = shouldShowRequestPermissionRationale(Manifest.permission.SEND_SMS)
                    if (!showRationale) {
                        showRestrictedSMSHelp()
                    }
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Request permissions on app start
        requestPermissions()
        
        // Initialize UI immediately to prevent ANR
        // Read intent extra to optionally open the drawer on dashboard
        openDrawerInitiallyState = intent?.getBooleanExtra("open_drawer", false) == true

        setContent {
            SecureSheTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    AppDrawerHost(navController = navController, openInitially = openDrawerInitiallyState) {
                        AppNavigation(navController, openDrawerInitially = false)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // When MainActivity is brought to front, react to the request to open the drawer
        val shouldOpenDrawer = intent.getBooleanExtra("open_drawer", false)
        if (shouldOpenDrawer) {
            // Toggle to force LaunchedEffect(openInitially) to run again
            openDrawerInitiallyState = false
            openDrawerInitiallyState = true
        }
    }

    override fun onResume() {
        super.onResume()
        // Fallback: if the extra is present on resume, open the drawer and clear it
        val shouldOpenDrawer = intent?.getBooleanExtra("open_drawer", false) == true
        if (shouldOpenDrawer) {
            openDrawerInitiallyState = false
            openDrawerInitiallyState = true
            intent?.removeExtra("open_drawer")
        }
    }
    
    private fun requestPermissions() {
        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
        
        if (permissionsToRequest.isNotEmpty()) {
            android.util.Log.d("MainActivity", "Requesting permissions: ${permissionsToRequest.joinToString()}")
            permissionLauncher.launch(permissionsToRequest)
        } else {
            android.util.Log.d("MainActivity", "All permissions already granted")
        }
    }

    private fun openAppSettings() {
        try {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${packageName}")
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to open app settings: ${e.message}")
        }
    }

    private fun showRestrictedSMSHelp() {
        try {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.permission_sms_required_title))
                .setMessage(getString(R.string.permission_sms_restricted_message))
                .setPositiveButton(getString(R.string.open_settings)) { _, _ ->
                    openAppSettings()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to show restricted SMS dialog: ${e.message}")
        }
    }
}