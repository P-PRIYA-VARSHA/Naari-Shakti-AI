package com.example.secureshe.services

import android.app.*
import android.content.Intent
import android.location.Location
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.net.Uri
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.net.toUri
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.secureshe.MainActivity
import com.example.secureshe.R
import com.example.secureshe.data.EmergencyContact
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.io.FileInputStream
import java.io.IOException
import android.util.Base64
import org.json.JSONObject
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import com.google.firebase.firestore.FirebaseFirestore
import android.provider.Settings

class SOSService : Service() {

    // Initialize location client directly instead of injection
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isActive = false
    private var emergencyContacts = listOf<EmergencyContact>()
    private var currentContactIndex = 0
    private var lastLocation: Location? = null
    private var lastSentLocation: Location? = null  // For adaptive GPS tracking
    private var locationUpdateJob: Job? = null
    private var videoRecordingJob: Job? = null
    private var locationCallback: LocationCallback? = null

    // OkHttp client for uploads
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    // Call state tracking
    private var telephonyManager: TelephonyManager? = null
    private var currentCallJob: Job? = null

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "sos_service_channel"
        const val ACTION_START_SOS = "START_SOS"
        const val ACTION_STOP_SOS = "STOP_SOS"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // Initialize location client
        fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(this)

        // Initialize telephony manager for call state monitoring
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SOS -> {
                // Get emergency contacts from intent extras
                val contactCount = intent.getIntExtra("contact_count", 0)
                val contacts = mutableListOf<EmergencyContact>()

                for (i in 0 until contactCount) {
                    val name = intent.getStringExtra("contact_${i}_name") ?: ""
                    val phone = intent.getStringExtra("contact_${i}_phone") ?: ""
                    if (name.isNotEmpty() && phone.isNotEmpty()) {
                        contacts.add(EmergencyContact(id = i.toString(), name = name, phoneNumber = phone))
                    }
                }

                emergencyContacts = contacts
                startSOS()
            }
            ACTION_STOP_SOS -> {
                stopSOS()
            }
        }
        return START_STICKY
    }

    private fun startSOS() {
        if (isActive) return

        android.util.Log.d("SOSService", "Starting SOS with ${emergencyContacts.size} contacts")

        // Check and log permissions status
        checkPermissionsStatus()

        isActive = true
        startForeground(NOTIFICATION_ID, createNotification())

        serviceScope.launch {
            // Send immediate SMS to all contacts first
            sendImmediateSMSToAllContacts()

            // Start location tracking IMMEDIATELY (most important)
            startLocationTracking()

            // Start other SOS activities
            startEmergencyCalling()
            startVideoRecording()
        }
    }

    private fun stopSOS() {
        android.util.Log.d("SOSService", "Stopping SOS - setting isActive to false")
        isActive = false

        // Send final status SMS to all contacts
        sendFinalStatusSMS()

        android.util.Log.d("SOSService", "Stopping location tracking")
        stopLocationTracking()

        android.util.Log.d("SOSService", "Stopping video recording")
        stopVideoRecording()

        // List all recorded videos for debugging
        listRecordedVideos()

        android.util.Log.d("SOSService", "Stopping foreground service")
        @Suppress("DEPRECATION")
        stopForeground(true)

        android.util.Log.d("SOSService", "Stopping service")
        stopSelf()
    }

    private fun sendFinalStatusSMS() {
        val timestamp = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())

        val finalMessage = "✅ SOS Deactivated\n" +
                "⏰ Stopped at: $timestamp\n" +
                "📱 Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}\n" +
                "🆘 Emergency situation resolved.\n" +
                "Thank you for your concern."

        android.util.Log.d("SOSService", "Sending final status SMS to ${emergencyContacts.size} contacts")

        emergencyContacts.forEach { contact ->
            sendSMS(contact.phoneNumber, finalMessage)
            android.util.Log.d("SOSService", "Final status SMS sent to ${contact.name}: ${contact.phoneNumber}")
        }
    }

    private fun startEmergencyCalling() {
        serviceScope.launch {
            android.util.Log.d("SOSService", "Starting conference call with ${emergencyContacts.size} emergency contacts")

            if (emergencyContacts.isEmpty()) {
                android.util.Log.d("SOSService", "No emergency contacts to call")
                return@launch
            }

            // Start conference call with all emergency contacts
            makeConferenceCall()
        }
    }

    private fun makeConferenceCall() {
        try {
            android.util.Log.d("SOSService", "Initiating conference call with ${emergencyContacts.size} contacts")

            if (emergencyContacts.isEmpty()) {
                android.util.Log.d("SOSService", "No emergency contacts to call")
                return
            }

            // Start conference call process
            startConferenceCallProcess()

        } catch (e: Exception) {
            android.util.Log.e("SOSService", "Failed to make conference call: ${e.message}")
            // Fallback to individual calls if conference call fails
            makeIndividualCalls()
        }
    }

    private fun startConferenceCallProcess() {
        serviceScope.launch {
            try {
                android.util.Log.d("SOSService", "Starting emergency calling - will stop when someone answers")



                // Call emergency contacts one by one
                android.util.Log.d("SOSService", "Total emergency contacts to call: ${emergencyContacts.size}")

                for ((index, contact) in emergencyContacts.withIndex()) {
                    if (!isActive) {
                        android.util.Log.d("SOSService", "SOS stopped - stopping calls")
                        break
                    }

                    android.util.Log.d("SOSService", "Calling contact ${index + 1}/${emergencyContacts.size}: ${contact.name} at ${contact.phoneNumber}")

                    // Make call
                    makeEmergencyCall(contact.phoneNumber)

                    // Wait for call timeout (30 seconds)
                    currentCallJob = launch {
                        kotlinx.coroutines.delay(30000) // 30 second timeout
                        android.util.Log.d("SOSService", "Call to ${contact.name} timed out after 30 seconds, moving to next contact")
                    }

                    // Wait for timeout
                    currentCallJob?.join()

                    android.util.Log.d("SOSService", "Completed call to ${contact.name}, continuing to next contact")
                }

                android.util.Log.d("SOSService", "Emergency calling completed")

            } catch (e: Exception) {
                android.util.Log.e("SOSService", "Emergency calling process failed: ${e.message}")
            }
        }
    }

    private fun showConferenceCallSetupInstructions() {
        try {
            val notificationManager = getSystemService(android.app.NotificationManager::class.java)

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Conference Call Setup Started")
                .setContentText("Follow these steps to create conference call")
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText("Conference Call Setup:\n" +
                            "1. First contact should answer the call\n" +
                            "2. App will add other contacts automatically\n" +
                            "3. You need to manually merge calls in phone app\n" +
                            "4. Look for 'Merge' or 'Add Call' button\n" +
                            "5. Tap it to create conference call"))
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(false)
                .setOngoing(true)
                .build()

            notificationManager.notify(NOTIFICATION_ID + 999, notification)

            android.util.Log.d("SOSService", "Conference call setup instructions shown")
        } catch (e: Exception) {
            android.util.Log.e("SOSService", "Failed to show conference call setup instructions: ${e.message}")
        }
    }

    private fun showConferenceCallInstructions(contactNumber: Int) {
        try {
            val notificationManager = getSystemService(android.app.NotificationManager::class.java)

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Conference Call Setup")
                .setContentText("Open phone app and tap 'Merge' or 'Add Call' button")
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText("To create conference call:\n" +
                            "1. Open your phone app\n" +
                            "2. Look for 'Merge' or 'Add Call' button\n" +
                            "3. Tap it to add contact $contactNumber\n" +
                            "4. Repeat for all contacts"))
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(NOTIFICATION_ID + contactNumber, notification)

            android.util.Log.d("SOSService", "Conference call instructions shown for contact $contactNumber")
        } catch (e: Exception) {
            android.util.Log.e("SOSService", "Failed to show conference call instructions: ${e.message}")
        }
    }

    private fun addContactToConferenceCall(phoneNumber: String) {
        // Check call permission
        if (ContextCompat.checkSelfPermission(this@SOSService, android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            android.util.Log.e("SOSService", "Call permission not granted for conference call")
            return
        }

        try {
            android.util.Log.d("SOSService", "Adding $phoneNumber to conference call")

            // Use ACTION_CALL to add to existing call
            // This will trigger the "Add Call" feature in the phone app
            val addCallIntent = Intent(Intent.ACTION_CALL)
            addCallIntent.data = "tel:$phoneNumber".toUri()
            addCallIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(addCallIntent)

            android.util.Log.d("SOSService", "Add call intent sent for $phoneNumber")

        } catch (e: SecurityException) {
            android.util.Log.e("SOSService", "Security exception when adding to conference call: ${e.message}")
        } catch (e: Exception) {
            android.util.Log.e("SOSService", "Failed to add $phoneNumber to conference call: ${e.message}")
        }
    }

    private fun makeRapidSequentialCalls() {
        serviceScope.launch {
            android.util.Log.d("SOSService", "Making rapid sequential calls to all emergency contacts")

            for (contact in emergencyContacts) {
                if (!isActive) break

                android.util.Log.d("SOSService", "Calling ${contact.name} at ${contact.phoneNumber}")

                // Make call
                makeEmergencyCall(contact.phoneNumber)

                // Shorter delay between calls (10 seconds instead of 30)
                kotlinx.coroutines.delay(40000)
            }
        }
    }

    private fun makeIndividualCalls() {
        serviceScope.launch {
            android.util.Log.d("SOSService", "Falling back to individual calls")

            while (isActive && currentContactIndex < emergencyContacts.size) {
                val contact = emergencyContacts[currentContactIndex]
                android.util.Log.d("SOSService", "Calling ${contact.name} at ${contact.phoneNumber}")

                // Make individual call
                makeEmergencyCall(contact.phoneNumber)

                // Wait for call to end or timeout
                kotlinx.coroutines.delay(30000) // 30 seconds timeout

                // Move to next contact
                currentContactIndex++
            }
        }
    }

    private fun makeEmergencyCall(phoneNumber: String) {
        // Check if SOS is still active before making call
        if (!isActive) {
            android.util.Log.d("SOSService", "SOS stopped - not making emergency call")
            return
        }

        // Check call permission
        if (ContextCompat.checkSelfPermission(this@SOSService, android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            android.util.Log.e("SOSService", "Call permission not granted")
            return
        }

        try {
            android.util.Log.d("SOSService", "Attempting to call: $phoneNumber")

            val intent = Intent(Intent.ACTION_CALL)
            intent.data = "tel:$phoneNumber".toUri()
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)

            android.util.Log.d("SOSService", "Call initiated successfully")
        } catch (e: SecurityException) {
            android.util.Log.e("SOSService", "Security exception when making call: ${e.message}")
        } catch (e: Exception) {
            android.util.Log.e("SOSService", "Failed to make call: ${e.message}")
        }
    }

    private fun startLocationTracking() {
        android.util.Log.d("SOSService", "=== STARTING LOCATION TRACKING ===")

        // Check location permission
        if (ContextCompat.checkSelfPermission(this@SOSService, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            android.util.Log.e("SOSService", "Location permission not granted")
            sendLocationPermissionError()
            return
        }

        // Don't send pending message to avoid too many messages

        locationUpdateJob = serviceScope.launch {
            try {
                android.util.Log.d("SOSService", "Location tracking coroutine started")

                // Get initial location but don't send immediately
                android.util.Log.d("SOSService", "Requesting last known location...")
                
                // Check location permission before making request
                if (ContextCompat.checkSelfPermission(this@SOSService, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    try {
                        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                            if (location != null) {
                                android.util.Log.d("SOSService", "SUCCESS: Got last known location: ${location.latitude}, ${location.longitude}")
                                lastLocation = location
                                // Don't send immediately - wait for periodic updates
                            } else {
                                android.util.Log.d("SOSService", "No last known location, will try location updates")
                            }
                        }.addOnFailureListener { e ->
                            android.util.Log.e("SOSService", "FAILED to get last known location: ${e.message}")
                        }
                    } catch (e: SecurityException) {
                        android.util.Log.e("SOSService", "SecurityException when requesting last known location: ${e.message}")
                    }
                } else {
                    android.util.Log.e("SOSService", "Location permission not granted for initial location request")
                }

                // Request location updates optimized for adaptive sampling
                android.util.Log.d("SOSService", "Requesting location updates for adaptive sampling...")
                val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 15000) // 15 seconds for better accuracy
                    .setMinUpdateDistanceMeters(5f) // Only update if moved at least 5 meters (reduced sensitivity)
                    .setMaxUpdates(240) // Get up to 240 updates (1 hour worth)
                    .setWaitForAccurateLocation(true) // Wait for accurate GPS fix
                    .build()





                locationCallback = object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {
                        if (!isActive) {
                            android.util.Log.d("SOSService", "SOS stopped - ignoring location updates")
                            return
                        }

                        locationResult.lastLocation?.let { location ->
                            // Validate location accuracy and freshness
                            val isAccurate = location.accuracy <= 15f // Accuracy within 15 meters (increased tolerance)
                            val isFresh = System.currentTimeMillis() - location.time < 45000 // Less than 45 seconds old (increased tolerance)
                            val isSignificantAccuracy = location.accuracy > 0f // Must have some accuracy data
                            
                            android.util.Log.d("SOSService", "Location update: ${location.latitude}, ${location.longitude}")
                            android.util.Log.d("SOSService", "Accuracy: ${location.accuracy}m, Fresh: $isFresh, Accurate: $isAccurate, HasAccuracy: $isSignificantAccuracy")
                            
                            if (isAccurate && isFresh && isSignificantAccuracy) {
                                lastLocation = location
                                android.util.Log.d("SOSService", "✅ Location accepted - accurate and fresh")
                            } else {
                                android.util.Log.d("SOSService", "⚠️ Location rejected - accuracy: ${location.accuracy}m, age: ${(System.currentTimeMillis() - location.time)/1000}s")
                                android.util.Log.d("SOSService", "Rejection reason: ${if (!isAccurate) "poor accuracy" else if (!isFresh) "old data" else "no accuracy data"}")
                            }
                            // Don't send location immediately - only through periodic updates
                        }
                    }

                    override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                        android.util.Log.d("SOSService", "Location available: ${locationAvailability.isLocationAvailable}")
                    }
                }

                // Check location permission before requesting updates
                if (ContextCompat.checkSelfPermission(this@SOSService, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    try {
                        // Use the method that takes LocationCallback and Looper
                        fusedLocationClient.requestLocationUpdates(
                            locationRequest,
                            locationCallback!!,
                            mainLooper
                        ).addOnSuccessListener {
                            android.util.Log.d("SOSService", "Location updates started successfully")
                        }.addOnFailureListener { e ->
                            android.util.Log.e("SOSService", "Failed to start location updates: ${e.message}")
                        }
                    } catch (e: SecurityException) {
                        android.util.Log.e("SOSService", "SecurityException when requesting location updates: ${e.message}")
                    }
                } else {
                    android.util.Log.e("SOSService", "Location permission not granted - cannot start location updates")
                    sendLocationPermissionError()
                }

                // Start periodic location SMS updates
                startPeriodicLocationSMS()

                // Final fallback: Try to get location once more
                kotlinx.coroutines.delay(5000) // Wait 5 seconds
                if (lastLocation == null && isActive) {
                    android.util.Log.d("SOSService", "Final fallback attempt...")
                    
                    // Check location permission before making request
                    if (ContextCompat.checkSelfPermission(this@SOSService, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        try {
                            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                                if (location != null && isActive) {
                                    android.util.Log.d("SOSService", "SUCCESS: Final fallback location: ${location.latitude}, ${location.longitude}")
                                    lastLocation = location
                                    // Don't send immediately - wait for periodic updates
                                }
                            }.addOnFailureListener { e ->
                                android.util.Log.e("SOSService", "Final fallback location request failed: ${e.message}")
                            }
                        } catch (e: SecurityException) {
                            android.util.Log.e("SOSService", "SecurityException when requesting final fallback location: ${e.message}")
                        }
                    } else {
                        android.util.Log.e("SOSService", "Location permission not granted for final fallback request")
                    }
                }

            } catch (e: Exception) {
                android.util.Log.e("SOSService", "Location tracking error: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun startPeriodicLocationSMS() {
        serviceScope.launch {
            var updateCount = 0

            // Wait for location to be available, then send first update
            var attempts = 0
            while (lastLocation == null && isActive && attempts < 10) {
                kotlinx.coroutines.delay(3000) // Wait 3 seconds
                attempts++
            }

            // Send first location update immediately when available
            if (isActive && lastLocation != null) {
                android.util.Log.d("SOSService", "🆕 SENDING INITIAL LOCATION UPDATE")
                sendLocationToContacts(lastLocation!!)
                lastSentLocation = lastLocation // Store the sent location
            }

                            // Start adaptive GPS location checking (every 30 seconds for more responsive updates)
                while (isActive) {
                    kotlinx.coroutines.delay(30000) // 30 seconds delay for more responsive adaptive sampling

                    if (!isActive) break

                    // Force a fresh location reading if we don't have one or it's old
                    if (lastLocation == null || (System.currentTimeMillis() - lastLocation!!.time) > 60000) {
                        android.util.Log.d("SOSService", "🔄 Requesting fresh GPS location...")
                        
                        // Check location permission before making request
                        if (ContextCompat.checkSelfPermission(this@SOSService, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            try {
                                fusedLocationClient.lastLocation.addOnSuccessListener { freshLocation ->
                                    if (freshLocation != null && isActive) {
                                        android.util.Log.d("SOSService", "🆕 Fresh location obtained: ${freshLocation.latitude}, ${freshLocation.longitude}")
                                        lastLocation = freshLocation
                                    }
                                }.addOnFailureListener { e ->
                                    android.util.Log.e("SOSService", "Failed to get fresh location: ${e.message}")
                                }
                            } catch (e: SecurityException) {
                                android.util.Log.e("SOSService", "SecurityException when requesting fresh location: ${e.message}")
                            }
                        } else {
                            android.util.Log.e("SOSService", "Location permission not granted for fresh location request")
                        }
                    }

                    lastLocation?.let { currentLocation ->
                        checkAndSendLocationIfChanged(currentLocation, updateCount)
                        updateCount++
                    }
                    
                    // Send periodic status update every 5 minutes (10 checks) even if location hasn't changed
                    if (updateCount % 10 == 0 && updateCount > 0) {
                        lastLocation?.let { currentLocation ->
                            sendPeriodicStatusUpdate(currentLocation, updateCount)
                        }
                    }
                }
        }
    }

    private fun checkAndSendLocationIfChanged(currentLocation: Location, updateCount: Int) {
        if (!isActive) {
            android.util.Log.d("SOSService", "SOS stopped - ignoring location check")
            return
        }

        try {
            lastSentLocation?.let { lastSent ->
                // Check if we're comparing the same location object (shouldn't happen but safety check)
                if (currentLocation === lastSent) {
                    android.util.Log.d("SOSService", "⚠️ Same location object detected - skipping comparison")
                    return
                }
                
                // Calculate actual distance moved from previous location
                val results = FloatArray(1)
                android.location.Location.distanceBetween(
                    lastSent.latitude, lastSent.longitude,
                    currentLocation.latitude, currentLocation.longitude,
                    results
                )
                val distanceInMeters = results[0]
                
                // Check if distance moved is greater than 80 meters
                if (distanceInMeters > 80.0) {
                    android.util.Log.d("SOSService", "✅ DISTANCE MOVED > 80m - SENDING GPS LOCATION UPDATE #$updateCount")
                    android.util.Log.d("SOSService", "Previous: ${lastSent.latitude}, ${lastSent.longitude}")
                    android.util.Log.d("SOSService", "Current: ${currentLocation.latitude}, ${currentLocation.longitude}")
                    android.util.Log.d("SOSService", "Distance moved: ${String.format("%.2f", distanceInMeters)} meters")
                    
                    sendAdaptiveLocationUpdate(currentLocation, updateCount)
                    lastSentLocation = currentLocation // Reset reference point - make current location the new "initial location"
                } else {
                    android.util.Log.d("SOSService", "⏸️ DISTANCE < 80m - NO SMS SENT (User stationary) - Check #$updateCount")
                    android.util.Log.d("SOSService", "Distance moved: ${String.format("%.2f", distanceInMeters)} meters")
                }
            } ?: run {
                android.util.Log.d("SOSService", "🆕 FIRST LOCATION CHECK - SENDING INITIAL UPDATE #$updateCount")
                sendAdaptiveLocationUpdate(currentLocation, updateCount)
                lastSentLocation = currentLocation
            }
        } catch (e: Exception) {
            android.util.Log.e("SOSService", "Error in adaptive GPS check: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun sendAdaptiveLocationUpdate(location: Location, updateCount: Int) {
        // Only send SMS if SOS is still active
        if (!isActive) {
            android.util.Log.d("SOSService", "SOS stopped - not sending adaptive location update")
            return
        }

        val timestamp = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())

        val message = "📍 GPS LOCATION UPDATE #$updateCount\n" +
                "🚨 SOS Still Active\n" +
                "📍 My current location:\n" +
                "https://maps.google.com/?q=${location.latitude},${location.longitude}\n" +
                "📍 Coordinates: ${location.latitude}, ${location.longitude}\n" +
                "⏰ Time: $timestamp\n" +
                "🔄 Moved >80 meters from previous location\n" +
                "🆘 Still need help!"

        android.util.Log.d("SOSService", "Sending adaptive location update #$updateCount to ${emergencyContacts.size} contacts")

        emergencyContacts.forEach { contact ->
            sendSMS(contact.phoneNumber, message)
        }
    }

    private fun sendPeriodicLocationUpdate(location: Location, updateCount: Int = 1) {
        // Only send SMS if SOS is still active
        if (!isActive) {
            android.util.Log.d("SOSService", "SOS stopped - not sending periodic location update")
            return
        }

        val timestamp = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())

        val message = "📍 Location Update #$updateCount\n" +
                "🚨 SOS Still Active\n" +
                "📍 My current location:\n" +
                "https://maps.google.com/?q=${location.latitude},${location.longitude}\n" +
                "📍 Coordinates: ${location.latitude}, ${location.longitude}\n" +
                "⏰ Time: $timestamp\n" +
                "🆘 Still need help!"

        android.util.Log.d("SOSService", "Sending periodic location update #$updateCount to ${emergencyContacts.size} contacts")

        emergencyContacts.forEach { contact ->
            sendSMS(contact.phoneNumber, message)
        }
    }

    private fun sendPeriodicStatusUpdate(location: Location, updateCount: Int) {
        // Only send SMS if SOS is still active
        if (!isActive) {
            android.util.Log.d("SOSService", "SOS stopped - not sending periodic status update")
            return
        }

        val timestamp = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())

        val message = "📱 SOS Status Update\n" +
                "🚨 SOS Still Active\n" +
                "📍 My current location:\n" +
                "https://maps.google.com/?q=${location.latitude},${location.longitude}\n" +
                "📍 Coordinates: ${location.latitude}, ${location.longitude}\n" +
                "⏰ Time: $timestamp\n" +
                "📊 Check #$updateCount completed\n" +
                "🔄 Location tracking active\n" +
                "🆘 Still need help!"

        android.util.Log.d("SOSService", "Sending periodic status update #$updateCount to ${emergencyContacts.size} contacts")

        emergencyContacts.forEach { contact ->
            sendSMS(contact.phoneNumber, message)
        }
    }

    private fun stopLocationTracking() {
        android.util.Log.d("SOSService", "Stopping location tracking...")
        locationUpdateJob?.cancel()
        
        // Remove location updates using LocationCallback
        try {
            if (locationCallback != null) {
                fusedLocationClient.removeLocationUpdates(locationCallback!!)
                android.util.Log.d("SOSService", "Location updates removed successfully")
            } else {
                android.util.Log.d("SOSService", "No location callback to remove")
            }
        } catch (e: Exception) {
            android.util.Log.e("SOSService", "Error removing location updates: ${e.message}")
        }
        
        // Clear the callback reference
        locationCallback = null
    }

    private fun sendLocationToContacts(location: Location) {
        // Only send SMS if SOS is still active
        if (!isActive) {
            android.util.Log.d("SOSService", "SOS stopped - not sending location SMS")
            return
        }

        // Create a more detailed and robust SMS message
        val timestamp = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())

        val message = "🚨 EMERGENCY SOS 🚨\n" +
                "I need immediate help!\n" +
                "📍 My current location:\n" +
                "https://maps.google.com/?q=${location.latitude},${location.longitude}\n" +
                "📍 Coordinates: ${location.latitude}, ${location.longitude}\n" +
                "⏰ Time: $timestamp\n" +
                "🆘 Please respond immediately!"

        android.util.Log.d("SOSService", "Sending location SMS to ${emergencyContacts.size} contacts")

        emergencyContacts.forEach { contact ->
            // Send only the rich location message (no redundant coordinates SMS)
            sendRichLocationMessage(contact.phoneNumber, location)
            android.util.Log.d("SOSService", "Sending location SMS to ${contact.name}: ${contact.phoneNumber}")
        }
    }

    private fun sendLocationPendingMessage() {
        val pendingMessage = "📍 GPS Location Update\n" +
                "🚨 SOS Still Active\n" +
                "⏰ Time: ${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())}\n" +
                "🔄 Obtaining GPS location...\n" +
                "🆘 Please wait for location details."

        android.util.Log.d("SOSService", "Sending location pending message to ${emergencyContacts.size} contacts")

        emergencyContacts.forEach { contact ->
            sendSMS(contact.phoneNumber, pendingMessage)
        }
    }

    private fun sendLocationPermissionError() {
        val errorMessage = "📍 GPS Location Error\n" +
                "🚨 SOS Still Active\n" +
                "⏰ Time: ${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())}\n" +
                "❌ Location permission not granted\n" +
                "🆘 Please enable location permission in app settings."

        android.util.Log.d("SOSService", "Sending location permission error message to ${emergencyContacts.size} contacts")

        emergencyContacts.forEach { contact ->
            sendSMS(contact.phoneNumber, errorMessage)
        }
    }

    private fun sendLocationUnavailableMessage() {
        val unavailableMessage = "📍 GPS Location Unavailable\n" +
                "🚨 SOS Still Active\n" +
                "⏰ Time: ${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())}\n" +
                "❌ GPS location not available\n" +
                "🆘 Please enable GPS and try again."

        android.util.Log.d("SOSService", "Sending location unavailable message to ${emergencyContacts.size} contacts")

        emergencyContacts.forEach { contact ->
            sendSMS(contact.phoneNumber, unavailableMessage)
        }
    }

    private fun sendLocationFailedMessage() {
        val failedMessage = "📍 GPS Location Failed\n" +
                "🚨 SOS Still Active\n" +
                "⏰ Time: ${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())}\n" +
                "❌ Could not obtain GPS location\n" +
                "🆘 Emergency calls and SMS still active."

        android.util.Log.d("SOSService", "Sending location failed message to ${emergencyContacts.size} contacts")

        emergencyContacts.forEach { contact ->
            sendSMS(contact.phoneNumber, failedMessage)
        }
    }

    // Send a rich location message that will display with map preview
    private fun sendRichLocationMessage(phoneNumber: String, location: Location) {
        // Check SMS permission
        if (ContextCompat.checkSelfPermission(this@SOSService, android.Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            android.util.Log.e("SOSService", "SMS permission not granted for rich location SMS")
            return
        }

        try {
            // Format message to trigger rich preview in modern SMS apps
            val richMessage = "🚨 EMERGENCY SOS 🚨\n" +
                    "I need immediate help!\n" +
                    "📍 My current location:\n" +
                    "https://maps.google.com/?q=${location.latitude},${location.longitude}\n" +
                    "📍 Coordinates: ${location.latitude}, ${location.longitude}\n" +
                    "⏰ Time: ${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())}\n" +
                    "🆘 Please respond immediately!"

            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                this.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            // Split long messages if needed
            if (richMessage.length > 160) {
                val parts = smsManager.divideMessage(richMessage)
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
                android.util.Log.d("SOSService", "Rich location SMS sent in ${parts.size} parts to: $phoneNumber")
            } else {
                smsManager.sendTextMessage(phoneNumber, null, richMessage, null, null)
                android.util.Log.d("SOSService", "Rich location SMS sent to: $phoneNumber")
            }

        } catch (e: SecurityException) {
            android.util.Log.e("SOSService", "Security exception when sending rich location SMS to $phoneNumber: ${e.message}")
        } catch (e: Exception) {
            android.util.Log.e("SOSService", "Failed to send rich location SMS to $phoneNumber: ${e.message}")
        }
    }

    private fun sendCoordinatesSMS(phoneNumber: String, location: Location) {
        // Check SMS permission
        if (ContextCompat.checkSelfPermission(this@SOSService, android.Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            android.util.Log.e("SOSService", "SMS permission not granted for coordinates SMS")
            return
        }

        try {
            val coordinatesMessage = "📍 GPS Location:\n" +
                    "https://maps.google.com/?q=${location.latitude},${location.longitude}\n" +
                    "📍 Coordinates: ${location.latitude}, ${location.longitude}"

            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                this.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            smsManager.sendTextMessage(phoneNumber, null, coordinatesMessage, null, null)
            android.util.Log.d("SOSService", "Coordinates SMS sent to: $phoneNumber")

        } catch (e: SecurityException) {
            android.util.Log.e("SOSService", "Security exception when sending coordinates SMS to $phoneNumber: ${e.message}")
        } catch (e: Exception) {
            android.util.Log.e("SOSService", "Failed to send coordinates SMS to $phoneNumber: ${e.message}")
        }
    }

    private fun sendSMS(phoneNumber: String, message: String) {
        // Check SMS permission
        if (ContextCompat.checkSelfPermission(this@SOSService, android.Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            android.util.Log.e("SOSService", "SMS permission not granted")
            showSMSFailedNotification(phoneNumber, "SMS permission not granted")
            return
        }

        try {
            android.util.Log.d("SOSService", "Sending SMS to: $phoneNumber")

            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                this.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            // Split long messages if needed (SMS limit is 160 characters)
            if (message.length > 160) {
                val parts = smsManager.divideMessage(message)
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
                android.util.Log.d("SOSService", "Long SMS sent in ${parts.size} parts to: $phoneNumber")
            } else {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
                android.util.Log.d("SOSService", "SMS sent successfully to: $phoneNumber")
            }

            // Show notification that SMS was sent
            showSMSSentNotification(phoneNumber)

        } catch (e: SecurityException) {
            android.util.Log.e("SOSService", "Security exception when sending SMS to $phoneNumber: ${e.message}")
            showSMSFailedNotification(phoneNumber, "Security exception: ${e.message}")
            // Try to send a backup SMS with minimal information
            sendBackupSMS(phoneNumber)
        } catch (e: Exception) {
            android.util.Log.e("SOSService", "Failed to send SMS to $phoneNumber: ${e.message}")
            // Show notification that SMS failed
            showSMSFailedNotification(phoneNumber, e.message ?: "Unknown error")
            // Try to send a backup SMS with minimal information
            sendBackupSMS(phoneNumber)
        }
    }

    private fun sendBackupSMS(phoneNumber: String) {
        // Check SMS permission
        if (ContextCompat.checkSelfPermission(this@SOSService, android.Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            android.util.Log.e("SOSService", "SMS permission not granted for backup SMS")
            return
        }

        try {
            android.util.Log.d("SOSService", "Sending backup SMS to: $phoneNumber")

            val backupMessage = "SOS! Help! ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())}"

            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                this.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            smsManager.sendTextMessage(phoneNumber, null, backupMessage, null, null)
            android.util.Log.d("SOSService", "Backup SMS sent successfully to: $phoneNumber")

        } catch (e: SecurityException) {
            android.util.Log.e("SOSService", "Security exception when sending backup SMS to $phoneNumber: ${e.message}")
        } catch (e: Exception) {
            android.util.Log.e("SOSService", "Failed to send backup SMS to $phoneNumber: ${e.message}")
        }
    }

    private fun showSMSSentNotification(phoneNumber: String) {
        try {
            val notificationManager = getSystemService(android.app.NotificationManager::class.java)

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SMS Sent Successfully")
                .setContentText("Emergency SMS sent to $phoneNumber")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: Exception) {
            android.util.Log.e("SOSService", "Failed to show SMS sent notification: ${e.message}")
        }
    }

    private fun showSMSFailedNotification(phoneNumber: String, error: String) {
        try {
            val notificationManager = getSystemService(android.app.NotificationManager::class.java)

            // Build Settings deep link action to help user grant restricted SMS
            val settingsIntent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:$packageName")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
            val settingsPendingIntent = PendingIntent.getActivity(this, 100, settingsIntent, flags)

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SMS Failed")
                .setContentText("Failed to send SMS to $phoneNumber")
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText("Failed to send SMS to $phoneNumber\nError: $error"))
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .addAction(0, getString(R.string.open_settings), settingsPendingIntent)
                .build()

            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: Exception) {
            android.util.Log.e("SOSService", "Failed to show SMS failed notification: ${e.message}")
        }
    }

    private fun startVideoRecording() {
        android.util.Log.d("SOSService", "Starting video recording service")

        // Check camera availability first
        if (!checkCameraAvailability()) {
            android.util.Log.e("SOSService", "Camera availability check failed - cannot record videos")
            return
        }

        videoRecordingJob = serviceScope.launch {
            while (isActive) {
                try {
                    // Record from both cameras simultaneously
                    recordFromBothCameras()

                    // Check if SOS is still active before continuing
                    if (!isActive) {
                        android.util.Log.d("SOSService", "SOS stopped - stopping video recording loop")
                        break
                    }

                    kotlinx.coroutines.delay(10000) // 10 seconds delay between recordings

                } catch (e: Exception) {
                    android.util.Log.e("SOSService", "Video recording loop error: ${e.message}")
                    kotlinx.coroutines.delay(5000) // Wait 5 seconds before retrying
                }
            }
        }
    }

    private fun stopVideoRecording() {
        videoRecordingJob?.cancel()
    }

    // New function for testing video recording only (no emergency calls/SMS)
    private fun startVideoRecordingTestOnly() {
        android.util.Log.d("SOSService", "Starting video recording test only - no emergency features")

        // Start foreground service immediately to prevent crash
        startForeground(NOTIFICATION_ID, createNotification())

        // Check camera availability first
        if (!checkCameraAvailability()) {
            android.util.Log.e("SOSService", "Camera availability check failed - cannot record videos")
            stopSelf()
            return
        }

        videoRecordingJob = serviceScope.launch {
            try {
                // Test recording from both cameras using MediaRecorder
                android.util.Log.d("SOSService", "Testing MediaRecorder dual camera recording")
                recordFromBothCameras()

                android.util.Log.d("SOSService", "MediaRecorder video recording test completed")

                // Stop the service after test
                stopForeground(true)
                stopSelf()

            } catch (e: Exception) {
                android.util.Log.e("SOSService", "MediaRecorder video recording test error: ${e.message}")
                stopForeground(true)
                stopSelf()
            }
        }
    }

    // Function for testing SMS functionality only
    private fun startSMSTestOnly() {
        android.util.Log.d("SOSService", "Starting SMS test only - no emergency calls/video")

        // Start foreground service immediately to prevent crash
        startForeground(NOTIFICATION_ID, createNotification())

        serviceScope.launch {
            try {
                // Send test SMS to all contacts
                sendTestSMSToAllContacts()

                // Check location permission before getting location
                if (ContextCompat.checkSelfPermission(this@SOSService, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    // Get location and send location SMS
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        location?.let {
                            sendTestLocationSMS(it)
                        }
                    }
                } else {
                    android.util.Log.e("SOSService", "Location permission not granted for SMS test")
                }

                android.util.Log.d("SOSService", "SMS test completed")

                // Stop the service after test
                kotlinx.coroutines.delay(3000) // Wait 3 seconds for SMS to be sent
                stopForeground(true)
                stopSelf()

            } catch (e: Exception) {
                android.util.Log.e("SOSService", "SMS test error: ${e.message}")
                stopForeground(true)
                stopSelf()
            }
        }
    }

    // Function for testing SMS during SOS (brief test)
    private fun startSMSTest() {
        android.util.Log.d("SOSService", "Starting SMS test during SOS")

        isActive = true
        startForeground(NOTIFICATION_ID, createNotification())

        serviceScope.launch {
            try {
                // Send immediate SMS to all contacts
                sendImmediateSMSToAllContacts()

                // Check location permission before getting location
                if (ContextCompat.checkSelfPermission(this@SOSService, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    // Get location and send location SMS
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        location?.let {
                            sendLocationToContacts(it)
                        }
                    }
                } else {
                    android.util.Log.e("SOSService", "Location permission not granted for SMS test during SOS")
                }

                android.util.Log.d("SOSService", "SMS test during SOS completed")

                // Auto-stop after 10 seconds for test
                kotlinx.coroutines.delay(10000)
                stopSOS()

            } catch (e: Exception) {
                android.util.Log.e("SOSService", "SMS test during SOS error: ${e.message}")
                stopSOS()
            }
        }
    }

    // Function for testing location only
    private fun startLocationTestOnly() {
        android.util.Log.d("SOSService", "=== STARTING LOCATION TEST ONLY ===")

        // Start foreground service immediately to prevent crash
        startForeground(NOTIFICATION_ID, createNotification())

        // Check location permission
        if (ContextCompat.checkSelfPermission(this@SOSService, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            android.util.Log.e("SOSService", "Location permission not granted for test")
            stopSelf()
            return
        }

        serviceScope.launch {
            try {
                android.util.Log.d("SOSService", "Testing location service...")

                // Try to get location immediately
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        android.util.Log.d("SOSService", "SUCCESS: Test location obtained: ${location.latitude}, ${location.longitude}")

                        // Send test location SMS if we have contacts (only once)
                        if (emergencyContacts.isNotEmpty()) {
                            sendTestLocationSMS(location)
                        }
                    } else {
                        android.util.Log.e("SOSService", "FAILED: No location available for test")
                    }
                }.addOnFailureListener { e ->
                    android.util.Log.e("SOSService", "FAILED: Location test error: ${e.message}")
                }

                // Wait and try again
                kotlinx.coroutines.delay(5000)
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        android.util.Log.d("SOSService", "SUCCESS: Second test location: ${location.latitude}, ${location.longitude}")
                    } else {
                        android.util.Log.e("SOSService", "FAILED: Still no location on second try")
                    }
                }

                // Stop after test
                kotlinx.coroutines.delay(3000)
                stopForeground(true)
                stopSelf()

            } catch (e: Exception) {
                android.util.Log.e("SOSService", "Location test error: ${e.message}")
                stopForeground(true)
                stopSelf()
            }
        }
    }

    // Function for testing adaptive GPS only
    private fun startAdaptiveGPSTestOnly() {
        android.util.Log.d("SOSService", "=== STARTING ADAPTIVE GPS TEST ONLY ===")

        // Start foreground service immediately to prevent crash
        startForeground(NOTIFICATION_ID, createNotification())

        // Check location permission
        if (ContextCompat.checkSelfPermission(this@SOSService, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            android.util.Log.e("SOSService", "Location permission not granted for adaptive GPS test")
            stopSelf()
            return
        }

        // Set up test contacts
        emergencyContacts = listOf(EmergencyContact("0", "Test Contact", "1234567890"))
        isActive = true

        serviceScope.launch {
            try {
                android.util.Log.d("SOSService", "Testing adaptive GPS functionality...")

                // Start location tracking with adaptive GPS
                startLocationTracking()

                // Run test for 3 minutes to demonstrate adaptive behavior
                android.util.Log.d("SOSService", "Running adaptive GPS test for 3 minutes...")
                kotlinx.coroutines.delay(180000) // 3 minutes

                // Stop the test
                android.util.Log.d("SOSService", "Adaptive GPS test completed")
                stopSOS()

            } catch (e: Exception) {
                android.util.Log.e("SOSService", "Adaptive GPS test error: ${e.message}")
                e.printStackTrace()
                stopSOS()
            }
        }
    }

    private fun sendTestSMSToAllContacts() {
        // Check SMS permission
        if (ContextCompat.checkSelfPermission(this@SOSService, android.Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            android.util.Log.e("SOSService", "SMS permission not granted for test SMS")
            return
        }

        val testMessage = "🧪 SMS Test Message\n" +
                "This is a test SMS from SecureShe app.\n" +
                "Time: ${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())}\n" +
                "✅ SMS functionality is working!"

        android.util.Log.d("SOSService", "Sending test SMS to ${emergencyContacts.size} contacts")

        emergencyContacts.forEach { contact ->
            sendSMS(contact.phoneNumber, testMessage)
            android.util.Log.d("SOSService", "Test SMS sent to ${contact.name}: ${contact.phoneNumber}")
        }
    }

    private fun sendTestLocationSMS(location: Location) {
        // Check SMS permission
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            android.util.Log.e("SOSService", "SMS permission not granted for test location SMS")
            return
        }

        val testLocationMessage = "🧪 Location Test\n" +
                "📍 My current location:\n" +
                "https://maps.google.com/?q=${location.latitude},${location.longitude}\n" +
                "📍 Coordinates: ${location.latitude}, ${location.longitude}\n" +
                "✅ GPS location SMS is working!"

        android.util.Log.d("SOSService", "Sending test location SMS to ${emergencyContacts.size} contacts")

        emergencyContacts.forEach { contact ->
            sendSMS(contact.phoneNumber, testLocationMessage)
            android.util.Log.d("SOSService", "Test location SMS sent to ${contact.name}: ${contact.phoneNumber}")
        }
    }

    private suspend fun recordVideo(isFrontCamera: Boolean) {
        try {
            android.util.Log.d("SOSService", "Starting video recording for ${if (isFrontCamera) "front" else "back"} camera")

            // Check permissions
            if (!checkAllPermissions()) {
                android.util.Log.e("SOSService", "Missing required permissions for video recording")
                return
            }

            // Get the correct camera ID
            val cameraId = getCameraId(isFrontCamera)
            if (cameraId == null) {
                android.util.Log.e("SOSService", "Could not find camera ID for ${if (isFrontCamera) "front" else "back"} camera")
                return
            }

            // Create video file
            val timestamp = System.currentTimeMillis()
            val videoDir = File(getExternalFilesDir(null), "sos_videos")
            if (!videoDir.exists()) {
                videoDir.mkdirs()
            }

            val cameraType = if (isFrontCamera) "front" else "back"
            val videoFileName = "sos_video_${cameraType}_$timestamp.mp4"
            val videoFile = File(videoDir, videoFileName)

            android.util.Log.d("SOSService", "Video file path: ${videoFile.absolutePath}")

            // Create a dummy surface for headless recording
            val surfaceTexture = android.graphics.SurfaceTexture(0)
            val dummySurface = android.view.Surface(surfaceTexture)

            android.util.Log.d("SOSService", "Created dummy surface for recording")

            // Create a simple video recording using MediaRecorder
            val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            try {
                android.util.Log.d("SOSService", "Configuring MediaRecorder for camera ID: $cameraId")

                // Configure MediaRecorder
                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
                android.util.Log.d("SOSService", "Audio source set")

                // Set camera source with specific camera ID
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // For Android 12+ (API 31+), we can set the camera ID directly
                    mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA)
                    // Note: MediaRecorder doesn't directly support camera ID selection
                    // We'll use Camera2 API approach instead
                } else {
                    mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA)
                }
                android.util.Log.d("SOSService", "Video source set")

                mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                android.util.Log.d("SOSService", "Output format set")

                mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                android.util.Log.d("SOSService", "Audio encoder set")

                mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                android.util.Log.d("SOSService", "Video encoder set")

                // Use standard video parameters that work reliably
                mediaRecorder.setVideoSize(640, 480)
                mediaRecorder.setVideoFrameRate(30)
                mediaRecorder.setVideoEncodingBitRate(1000000) // 1 Mbps
                android.util.Log.d("SOSService", "Video parameters set")

                mediaRecorder.setOutputFile(videoFile.absolutePath)
                android.util.Log.d("SOSService", "Output file set")

                // Set the dummy surface
                mediaRecorder.setPreviewDisplay(dummySurface)
                android.util.Log.d("SOSService", "Preview display set")

                // Prepare and start recording
                android.util.Log.d("SOSService", "Preparing MediaRecorder...")
                mediaRecorder.prepare()
                android.util.Log.d("SOSService", "MediaRecorder prepared successfully")

                android.util.Log.d("SOSService", "Starting MediaRecorder...")
                mediaRecorder.start()
                android.util.Log.d("SOSService", "Video recording started successfully for ${if (isFrontCamera) "front" else "back"} camera (ID: $cameraId)")

                // Record for 30 seconds
                android.util.Log.d("SOSService", "Recording for 30 seconds...")
                kotlinx.coroutines.delay(30000)

                // Stop recording
                android.util.Log.d("SOSService", "Stopping MediaRecorder...")
                mediaRecorder.stop()
                android.util.Log.d("SOSService", "MediaRecorder stopped successfully")

                mediaRecorder.release()
                android.util.Log.d("SOSService", "MediaRecorder released")

                // Release the dummy surface
                dummySurface.release()
                surfaceTexture.release()
                android.util.Log.d("SOSService", "Dummy surface released")

                android.util.Log.d("SOSService", "Video recording completed: ${videoFile.absolutePath}")

                // Verify file was created
                if (videoFile.exists() && videoFile.length() > 10000) {
                    android.util.Log.d("SOSService", "Video file verified: ${videoFile.length()} bytes")
                } else {
                    android.util.Log.e("SOSService", "Video file is too small or doesn't exist")
                }

                // Upload video to Google Drive
                uploadVideoToGoogleDrive(videoFile, cameraType)

            } catch (e: Exception) {
                android.util.Log.e("SOSService", "Error recording video: ${e.message}")
                try {
                    mediaRecorder.release()
                    dummySurface.release()
                    surfaceTexture.release()
                } catch (releaseException: Exception) {
                    android.util.Log.e("SOSService", "Error releasing MediaRecorder: ${releaseException.message}")
                }
                throw e
            }

        } catch (e: Exception) {
            android.util.Log.e("SOSService", "Failed to record video: ${e.message}")
            e.printStackTrace()
        }
    }

    // Record only from the back camera and upload
    private suspend fun recordFromBothCameras() {
        try {
            android.util.Log.d("SOSService", "Starting single camera recording: back camera only")

            // Check permissions
            if (!checkAllPermissions()) {
                android.util.Log.e("SOSService", "Missing required permissions for video recording")
                return
            }

            // Create video directory
            val timestamp = System.currentTimeMillis()
            val videoDir = File(getExternalFilesDir(null), "sos_videos")
            if (!videoDir.exists()) {
                videoDir.mkdirs()
            }

            val backVideoFile = File(videoDir, "sos_video_back_$timestamp.mp4")
            android.util.Log.d("SOSService", "Back video file (MediaRecorder): ${backVideoFile.absolutePath}")

            // Record from back camera using MediaRecorder
            android.util.Log.d("SOSService", "Recording from back camera using MediaRecorder")
            recordBackCameraWithMediaRecorder(backVideoFile)

            android.util.Log.d("SOSService", "Back camera recording completed")

        } catch (e: Exception) {
            android.util.Log.e("SOSService", "Back camera recording failed: ${e.message}")
            e.printStackTrace()
        }
    }

    // Front camera recording intentionally removed per requirements

    // Record from back camera using Camera2 API
    private suspend fun recordBackCameraWithMediaRecorder(videoFile: File) {
        try {
            android.util.Log.d("SOSService", "=== STARTING BACK CAMERA RECORDING ===")
            android.util.Log.d("SOSService", "Starting Camera2 recording for back camera to: ${videoFile.absolutePath}")

            // Get back camera ID
            val backCameraId = getBackCameraId()
            if (backCameraId == null) {
                android.util.Log.e("SOSService", "Back camera not found, using fallback")
                recordWithDefaultCamera(videoFile, "back")
                return
            }

            android.util.Log.d("SOSService", "Back camera ID detected: $backCameraId")

            var recorded = false
            try {
                recordWithLegacyCamera(videoFile, "back")
                recorded = true
            } catch (e: Exception) {
                android.util.Log.e("SOSService", "Legacy back recording failed: ${e.message}")
            }

            if (!recorded || videoFile.length() < 100_000L) {
                android.util.Log.d("SOSService", "Attempting Camera2 API for back camera as fallback")
                try {
                    recordWithCamera2API(backCameraId, videoFile, "back")
                    recorded = true
                } catch (e: Exception) {
                    android.util.Log.e("SOSService", "Camera2 back recording failed: ${e.message}")
                }
            }
            android.util.Log.d("SOSService", "=== COMPLETED BACK CAMERA RECORDING ===")

            if (recorded && videoFile.exists() && videoFile.length() > 100_000L) {
                uploadVideoToGoogleDrive(videoFile, "back")
            } else {
                android.util.Log.e("SOSService", "Back video invalid or too small: ${videoFile.length()} bytes")
            }

        } catch (e: Exception) {
            android.util.Log.e("SOSService", "Failed to record from back camera: ${e.message}")
            e.printStackTrace()
            // Fallback to default camera
            recordWithDefaultCamera(videoFile, "back")
        }
    }

    // Function to get the front camera ID
    private fun getFrontCameraId(): String? {
        return try {
            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
            val cameraIds = cameraManager.cameraIdList

            for (cameraId in cameraIds) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val facing = characteristics.get(android.hardware.camera2.CameraCharacteristics.LENS_FACING)

                if (facing == android.hardware.camera2.CameraCharacteristics.LENS_FACING_FRONT) {
                    android.util.Log.d("SOSService", "Found front camera ID: $cameraId")
                    return cameraId
                }
            }

            android.util.Log.e("SOSService", "No front camera found")
            null
        } catch (e: Exception) {
            android.util.Log.e("SOSService", "Error getting front camera ID: ${e.message}")
            null
        }
    }

    // Function to get the back camera ID
    private fun getBackCameraId(): String? {
        return try {
            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
            val cameraIds = cameraManager.cameraIdList

            for (cameraId in cameraIds) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val facing = characteristics.get(android.hardware.camera2.CameraCharacteristics.LENS_FACING)

                if (facing == android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK) {
                    android.util.Log.d("SOSService", "Found back camera ID: $cameraId")
                    return cameraId
                }
            }

            android.util.Log.e("SOSService", "No back camera found")
            null
        } catch (e: Exception) {
            android.util.Log.e("SOSService", "Error getting back camera ID: ${e.message}")
            null
        }
    }

    // Function to get the correct camera ID for front or back camera (legacy)
    private fun getCameraId(isFrontCamera: Boolean): String? {
        return if (isFrontCamera) getFrontCameraId() else getBackCameraId()
    }





    // Record using Camera2 API with specific camera ID
    private suspend fun recordWithCamera2API(cameraId: String, videoFile: File, cameraType: String) {
        try {
            android.util.Log.d("SOSService", "=== ENTERING Camera2 API RECORDING ===")
            android.util.Log.d("SOSService", "Starting Camera2 API recording for $cameraType camera (ID: $cameraId)")

            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager

            // Create a dummy surface for recording
            val surfaceTexture = android.graphics.SurfaceTexture(0)
            val dummySurface = android.view.Surface(surfaceTexture)

            // Create MediaRecorder
            val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            try {
                // Configure MediaRecorder with different video sources
                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)

                // Try different video sources for different cameras
                if (cameraType == "front") {
                    // For front camera, try to use a different approach
                    mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA)
                    android.util.Log.d("SOSService", "Using CAMERA video source for front camera")
                } else {
                    // For back camera, use default camera source
                    mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA)
                    android.util.Log.d("SOSService", "Using CAMERA video source for back camera")
                }
                mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                mediaRecorder.setAudioEncodingBitRate(64_000)
                mediaRecorder.setAudioSamplingRate(16_000)
                mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                mediaRecorder.setVideoSize(640, 480)
                mediaRecorder.setVideoFrameRate(24)
                mediaRecorder.setVideoEncodingBitRate(700_000)
                mediaRecorder.setOutputFile(videoFile.absolutePath)
                mediaRecorder.setPreviewDisplay(dummySurface)

                // Note: setCameraId is not available on all devices, using default camera behavior
                android.util.Log.d("SOSService", "Current API level: ${Build.VERSION.SDK_INT}")
                android.util.Log.d("SOSService", "Using default camera behavior for $cameraType camera (detected ID: $cameraId)")

                // Prepare and start recording
                mediaRecorder.prepare()
                mediaRecorder.start()
                android.util.Log.d("SOSService", "Started Camera2 API recording for $cameraType camera")

                // Record for 10 seconds to speed up Drive processing and upload
                kotlinx.coroutines.delay(10_000)

                // Stop recording
                mediaRecorder.stop()
                mediaRecorder.release()
                dummySurface.release()
                surfaceTexture.release()

                android.util.Log.d("SOSService", "Completed Camera2 API recording for $cameraType camera: ${videoFile.absolutePath}")

            } catch (e: Exception) {
                android.util.Log.e("SOSService", "Error recording with Camera2 API for $cameraType: ${e.message}")
                try {
                    mediaRecorder.release()
                    dummySurface.release()
                    surfaceTexture.release()
                } catch (releaseException: Exception) {
                    android.util.Log.e("SOSService", "Error releasing MediaRecorder: ${releaseException.message}")
                }
                throw e
            }

        } catch (e: Exception) {
            android.util.Log.e("SOSService", "Failed to record with Camera2 API for $cameraType: ${e.message}")
            e.printStackTrace()
            // Fallback to default camera
            recordWithDefaultCamera(videoFile, cameraType)
        }
    }

    // Fallback function to record with default camera when specific camera is not available
    private suspend fun recordWithDefaultCamera(videoFile: File, cameraType: String) {
        try {
            android.util.Log.d("SOSService", "Recording with default camera for $cameraType camera")

            // Create a dummy surface for headless recording
            val surfaceTexture = android.graphics.SurfaceTexture(0)
            val dummySurface = android.view.Surface(surfaceTexture)

            // Create MediaRecorder
            val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            try {
                // Configure MediaRecorder with default camera
                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
                mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA)
                mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                mediaRecorder.setAudioEncodingBitRate(64_000)
                mediaRecorder.setAudioSamplingRate(16_000)
                mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                mediaRecorder.setVideoSize(640, 480)
                mediaRecorder.setVideoFrameRate(24)
                mediaRecorder.setVideoEncodingBitRate(700_000)
                mediaRecorder.setOutputFile(videoFile.absolutePath)
                mediaRecorder.setPreviewDisplay(dummySurface)

                // Prepare and start recording
                mediaRecorder.prepare()
                mediaRecorder.start()
                android.util.Log.d("SOSService", "Started MediaRecorder recording with default camera for $cameraType")

                // Record for 10 seconds to reduce processing time
                kotlinx.coroutines.delay(10_000)

                // Stop recording
                mediaRecorder.stop()
                mediaRecorder.release()
                dummySurface.release()
                surfaceTexture.release()

                android.util.Log.d("SOSService", "Completed MediaRecorder recording with default camera for $cameraType: ${videoFile.absolutePath}")

                // Upload video to Google Drive
                uploadVideoToGoogleDrive(videoFile, cameraType)

            } catch (e: Exception) {
                android.util.Log.e("SOSService", "Error recording with default camera for $cameraType: ${e.message}")
                try {
                    mediaRecorder.release()
                    dummySurface.release()
                    surfaceTexture.release()
                } catch (releaseException: Exception) {
                    android.util.Log.e("SOSService", "Error releasing MediaRecorder: ${releaseException.message}")
                }
                throw e
            }

        } catch (e: Exception) {
            android.util.Log.e("SOSService", "Failed to record with default camera for $cameraType: ${e.message}")
            e.printStackTrace()
        }
    }

    // Legacy API recording that can choose front/back camera explicitly
    private suspend fun recordWithLegacyCamera(videoFile: File, cameraType: String) {
        try {
            android.util.Log.d("SOSService", "Legacy record for $cameraType camera")
            val cameraFacing = if (cameraType == "front") android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT else android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK

            // Find camera index
            var cameraIndex = -1
            val cameraCount = android.hardware.Camera.getNumberOfCameras()
            val info = android.hardware.Camera.CameraInfo()
            for (i in 0 until cameraCount) {
                android.hardware.Camera.getCameraInfo(i, info)
                if (info.facing == cameraFacing) {
                    cameraIndex = i
                    break
                }
            }

            if (cameraIndex == -1) {
                android.util.Log.e("SOSService", "No $cameraType camera found for legacy API; falling back")
                recordWithDefaultCamera(videoFile, cameraType)
                return
            }

            val camera = android.hardware.Camera.open(cameraIndex)
            camera.unlock()

            val mediaRecorder = MediaRecorder()
            try {
                mediaRecorder.setCamera(camera)
                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
                mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA)
                mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                mediaRecorder.setVideoSize(640, 480)
                mediaRecorder.setVideoFrameRate(30)
                mediaRecorder.setVideoEncodingBitRate(1000000)
                mediaRecorder.setOutputFile(videoFile.absolutePath)

                mediaRecorder.prepare()
                mediaRecorder.start()
                kotlinx.coroutines.delay(30000)
                mediaRecorder.stop()
                mediaRecorder.release()
                camera.release()

                // Upload
                uploadVideoToGoogleDrive(videoFile, cameraType)
            } catch (e: Exception) {
                try { mediaRecorder.release() } catch (_: Exception) {}
                try { camera.release() } catch (_: Exception) {}
                throw e
            }

        } catch (e: Exception) {
            android.util.Log.e("SOSService", "Legacy record failed for $cameraType: ${e.message}")
            e.printStackTrace()
            recordWithDefaultCamera(videoFile, cameraType)
        }
    }

    // Check all required permissions
    private fun checkAllPermissions(): Boolean {
        val permissions = arrayOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO
        )

        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(this@SOSService, permission) != PackageManager.PERMISSION_GRANTED) {
                android.util.Log.e("SOSService", "Permission not granted: $permission")
                return false
            }
        }

        android.util.Log.d("SOSService", "All permissions granted")
        return true
    }

    // Check and log current permissions status
    private fun checkPermissionsStatus() {
        val cameraPermission = ContextCompat.checkSelfPermission(this@SOSService, android.Manifest.permission.CAMERA)
        val audioPermission = ContextCompat.checkSelfPermission(this@SOSService, android.Manifest.permission.RECORD_AUDIO)
        val locationPermission = ContextCompat.checkSelfPermission(this@SOSService, android.Manifest.permission.ACCESS_FINE_LOCATION)
        val smsPermission = ContextCompat.checkSelfPermission(this@SOSService, android.Manifest.permission.SEND_SMS)
        val callPermission = ContextCompat.checkSelfPermission(this@SOSService, android.Manifest.permission.CALL_PHONE)

        android.util.Log.d("SOSService", "Permission Status:")
        android.util.Log.d("SOSService", "Camera: ${if (cameraPermission == PackageManager.PERMISSION_GRANTED) "GRANTED" else "DENIED"}")
        android.util.Log.d("SOSService", "Audio: ${if (audioPermission == PackageManager.PERMISSION_GRANTED) "GRANTED" else "DENIED"}")
        android.util.Log.d("SOSService", "Location: ${if (locationPermission == PackageManager.PERMISSION_GRANTED) "GRANTED" else "DENIED"}")
        android.util.Log.d("SOSService", "SMS: ${if (smsPermission == PackageManager.PERMISSION_GRANTED) "GRANTED" else "DENIED"}")
        android.util.Log.d("SOSService", "Call: ${if (callPermission == PackageManager.PERMISSION_GRANTED) "GRANTED" else "DENIED"}")

        // Check if SMS is available
        checkSMSAvailability()
    }

    private fun checkSMSAvailability() {
        try {
            val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

            when (telephonyManager.simState) {
                TelephonyManager.SIM_STATE_READY -> {
                    android.util.Log.d("SOSService", "SMS Status: SIM is ready - SMS should work")
                }
                TelephonyManager.SIM_STATE_ABSENT -> {
                    android.util.Log.e("SOSService", "SMS Status: No SIM card - SMS will not work")
                    showSMSUnavailableNotification("No SIM card detected")
                }
                TelephonyManager.SIM_STATE_PIN_REQUIRED -> {
                    android.util.Log.e("SOSService", "SMS Status: SIM PIN required - SMS may not work")
                    showSMSUnavailableNotification("SIM PIN required")
                }
                TelephonyManager.SIM_STATE_PUK_REQUIRED -> {
                    android.util.Log.e("SOSService", "SMS Status: SIM PUK required - SMS will not work")
                    showSMSUnavailableNotification("SIM PUK required")
                }
                TelephonyManager.SIM_STATE_NETWORK_LOCKED -> {
                    android.util.Log.e("SOSService", "SMS Status: SIM network locked - SMS may not work")
                    showSMSUnavailableNotification("SIM network locked")
                }
                else -> {
                    android.util.Log.e("SOSService", "SMS Status: Unknown SIM state - SMS may not work")
                    showSMSUnavailableNotification("Unknown SIM state")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SOSService", "Failed to check SMS availability: ${e.message}")
        }
    }

    private fun showSMSUnavailableNotification(reason: String) {
        try {
            val notificationManager = getSystemService(android.app.NotificationManager::class.java)

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SMS Unavailable")
                .setContentText("Emergency SMS may not work: $reason")
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText("SMS may not work: $reason\nEmergency calls will still be attempted."))
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: Exception) {
            android.util.Log.e("SOSService", "Failed to show SMS unavailable notification: ${e.message}")
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SecureShe Service")
            .setContentText("Video recording service is active")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SOS Service",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows when SOS is active"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        stopLocationTracking()
        stopVideoRecording()
    }



    // Function kept for future use when service is properly injected
    // @Suppress("UNUSED_FUNCTION")
    fun setEmergencyContacts(contacts: List<EmergencyContact>) {
        emergencyContacts = contacts
    }

    // Send immediate SMS to all emergency contacts
    private fun sendImmediateSMSToAllContacts() {
        val timestamp = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())

        val immediateMessage = "🚨 EMERGENCY SOS 🚨\n" +
                "I need immediate help!\n" +
                "⏰ SOS activated at: $timestamp\n" +
                "📍 GPS location will be sent shortly.\n" +
                "📞 I will also try to call you.\n" +
                "🆘 Please respond immediately!"

        android.util.Log.d("SOSService", "Sending immediate SMS to ${emergencyContacts.size} contacts")

        emergencyContacts.forEach { contact ->
            sendSMS(contact.phoneNumber, immediateMessage)
            android.util.Log.d("SOSService", "Immediate SMS sent to ${contact.name}: ${contact.phoneNumber}")
        }
    }

    // Function to check camera availability
    private fun checkCameraAvailability(): Boolean {
        return try {
            android.util.Log.d("SOSService", "Checking camera availability...")

            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
            val cameraIds = cameraManager.cameraIdList

            android.util.Log.d("SOSService", "Found ${cameraIds.size} cameras:")
            cameraIds.forEach { cameraId ->
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val facing = characteristics.get(android.hardware.camera2.CameraCharacteristics.LENS_FACING)
                val facingName = when (facing) {
                    android.hardware.camera2.CameraCharacteristics.LENS_FACING_FRONT -> "Front"
                    android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK -> "Back"
                    else -> "Unknown"
                }
                android.util.Log.d("SOSService", "Camera $cameraId: $facingName")
            }

            // Check if we have both front and back cameras
            val hasFrontCamera = cameraIds.any { cameraId ->
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val facing = characteristics.get(android.hardware.camera2.CameraCharacteristics.LENS_FACING)
                facing == android.hardware.camera2.CameraCharacteristics.LENS_FACING_FRONT
            }

            val hasBackCamera = cameraIds.any { cameraId ->
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val facing = characteristics.get(android.hardware.camera2.CameraCharacteristics.LENS_FACING)
                facing == android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK
            }

            android.util.Log.d("SOSService", "Camera availability - Front: $hasFrontCamera, Back: $hasBackCamera")

            // Log specific camera IDs for debugging
            val frontCameraId = getFrontCameraId()
            val backCameraId = getBackCameraId()
            android.util.Log.d("SOSService", "Front camera ID: $frontCameraId")
            android.util.Log.d("SOSService", "Back camera ID: $backCameraId")

            hasFrontCamera && hasBackCamera
        } catch (e: Exception) {
            android.util.Log.e("SOSService", "Camera availability check failed: ${e.message}")
            false
        }
    }

    // Function to list all recorded videos
    fun listRecordedVideos() {
        try {
            val videoDir = File(getExternalFilesDir(null), "sos_videos")
            if (videoDir.exists()) {
                val videoFiles = videoDir.listFiles { file ->
                    file.extension.lowercase() == "mp4"
                }
                android.util.Log.d("SOSService", "Found ${videoFiles?.size ?: 0} MP4 files in ${videoDir.absolutePath}:")
                videoFiles?.forEach { file ->
                    android.util.Log.d("SOSService", "MP4: ${file.name} (${file.length()} bytes)")
                }
            } else {
                android.util.Log.d("SOSService", "Video directory does not exist: ${videoDir.absolutePath}")
            }
        } catch (e: Exception) {
            android.util.Log.e("SOSService", "Error listing MP4 files: ${e.message}")
        }
    }

    // Upload video to Google Drive via Firebase Functions
    private suspend fun uploadVideoToGoogleDrive(videoFile: File, cameraType: String) {
        try {
            android.util.Log.d("SOSService", "Starting upload to Google Drive: ${videoFile.name}")

            // Check if video file exists and has content
            if (!videoFile.exists() || videoFile.length() == 0L) {
                android.util.Log.e("SOSService", "Video file doesn't exist or is empty: ${videoFile.absolutePath}")
                return
            }

            // Read video file and convert to base64
            val videoData = readVideoFileAsBase64(videoFile)
            if (videoData.isEmpty()) {
                android.util.Log.e("SOSService", "Failed to read video file as base64")
                return
            }

            // Get user email and trusted contact email from preferences
            val userEmail = getUserEmail()
            val trustedContactEmail = getTrustedContactEmail()
            
            if (userEmail.isEmpty()) {
                android.util.Log.e("SOSService", "User email not found")
                return
            }

            if (trustedContactEmail.isEmpty()) {
                android.util.Log.e("SOSService", "Trusted contact email not found")
                return
            }

            // Create upload payload with canonical file name based on cameraType
            val uploadFileName = "sos_video_${cameraType}_${System.currentTimeMillis()}.mp4"
            val uploadPayload = JSONObject().apply {
                put("userEmail", userEmail)
                put("trustedContactEmail", trustedContactEmail)
                put("videoData", videoData)
                put("fileName", uploadFileName)
            }

            // Upload to Firebase Functions
            val uploadResult = uploadToFirebaseFunctions(uploadPayload)
            if (uploadResult) {
                android.util.Log.d("SOSService", "✅ Video uploaded successfully to Google Drive: ${videoFile.name}")
                showUploadSuccessNotification(videoFile.name)
            } else {
                android.util.Log.e("SOSService", "❌ Failed to upload video to Google Drive: ${videoFile.name}")
                showUploadFailedNotification(videoFile.name)
            }

        } catch (e: Exception) {
            android.util.Log.e("SOSService", "Error uploading video to Google Drive: ${e.message}")
            e.printStackTrace()
            showUploadFailedNotification(videoFile.name)
        }
    }

    // Read video file as base64
    private fun readVideoFileAsBase64(videoFile: File): String {
        return try {
            val inputStream = FileInputStream(videoFile)
            val bytes = inputStream.readBytes()
            inputStream.close()
            Base64.encodeToString(bytes, Base64.DEFAULT)
        } catch (e: IOException) {
            android.util.Log.e("SOSService", "Error reading video file: ${e.message}")
            ""
        }
    }

    // Get user email from preferences
    private suspend fun getUserEmail(): String {
        return try {
            val sharedPrefs = getSharedPreferences("user_preferences", Context.MODE_PRIVATE)
            val userEmail = sharedPrefs.getString("user_email", "") ?: ""
            android.util.Log.d("SOSService", "Retrieved user email from SharedPreferences: $userEmail")
            userEmail
        } catch (e: Exception) {
            android.util.Log.e("SOSService", "Error getting user email: ${e.message}")
            ""
        }
    }

    // Get trusted contact email from preferences
    private suspend fun getTrustedContactEmail(): String {
        return try {
            // First try to get from SharedPreferences
            val sharedPrefs = getSharedPreferences("user_preferences", Context.MODE_PRIVATE)
            var contactEmail = sharedPrefs.getString("contact_email", "") ?: ""
            
            if (contactEmail.isEmpty()) {
                // If not found in SharedPreferences, try to get from Firebase
                android.util.Log.d("SOSService", "Contact email not found in SharedPreferences, trying Firebase...")
                contactEmail = getTrustedContactEmailFromFirebase()
            }
            
            android.util.Log.d("SOSService", "Retrieved trusted contact email: $contactEmail")
            contactEmail
        } catch (e: Exception) {
            android.util.Log.e("SOSService", "Error getting trusted contact email: ${e.message}")
            ""
        }
    }

    // Get trusted contact email from Firebase
    private suspend fun getTrustedContactEmailFromFirebase(): String {
        return try {
            val sharedPrefs = getSharedPreferences("user_preferences", Context.MODE_PRIVATE)
            val userId = sharedPrefs.getString("user_id", "") ?: ""
            
            if (userId.isEmpty()) {
                android.util.Log.w("SOSService", "No user ID found, cannot fetch from Firebase")
                return ""
            }
            
            android.util.Log.d("SOSService", "Fetching trusted contact email from Firebase for user: $userId")
            
            // Use Firebase Firestore directly
            val firestore = FirebaseFirestore.getInstance()
            val userDoc = firestore.collection("users").document(userId).get().await()
            
            if (userDoc.exists()) {
                val trustedContactEmail = userDoc.getString("trustedContactEmail") ?: ""
                android.util.Log.d("SOSService", "Retrieved trusted contact email from Firebase: $trustedContactEmail")
                
                // Also save to SharedPreferences for future use
                if (trustedContactEmail.isNotEmpty()) {
                    sharedPrefs.edit().putString("contact_email", trustedContactEmail).apply()
                    android.util.Log.d("SOSService", "Saved trusted contact email to SharedPreferences: $trustedContactEmail")
                }
                
                trustedContactEmail
            } else {
                android.util.Log.w("SOSService", "User document not found in Firebase")
                ""
            }
        } catch (e: Exception) {
            android.util.Log.e("SOSService", "Error fetching trusted contact email from Firebase: ${e.message}")
            ""
        }
    }

    // Upload to Firebase Functions
    private suspend fun uploadToFirebaseFunctions(uploadPayload: JSONObject): Boolean {
        return try {
            android.util.Log.d("SOSService", "🚀 Starting video upload to Google Drive...")
            android.util.Log.d("SOSService", "Upload payload prepared: ${uploadPayload.toString()}")
            
            // Use local emulator URL for debug builds, cloud URL for release
            val isDebug = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
            val url = if (isDebug) {
                // On Android emulator, 10.0.2.2 points to the host machine
                "https://script.google.com/macros/s/AKfycbxvr6a8JD6J2oDW7J9XxKayMbCXxbHLH_K6pbNyJl7UhXDXxbqvOX2hujRGlIxJ0kOiEA/exec"
            } else {
                "https://script.google.com/macros/s/AKfycbxvr6a8JD6J2oDW7J9XxKayMbCXxbHLH_K6pbNyJl7UhXDXxbqvOX2hujRGlIxJ0kOiEA/exec"
            }
            android.util.Log.d("SOSService", "📤 Uploading to: $url")
            
            // Create request body
            val jsonMediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = uploadPayload.toString().toRequestBody(jsonMediaType)
            
            // Create request
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .addHeader("User-Agent", "SecureShe-Android-App")
                .build()
            
            android.util.Log.d("SOSService", "📡 Sending HTTP request...")
            
            // Execute request
            val response = httpClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                android.util.Log.d("SOSService", "✅ Video upload successful!")
                android.util.Log.d("SOSService", "📄 Response: $responseBody")
                android.util.Log.d("SOSService", "📁 Check Google Drive for folder: SOS_Evidence_[user_email]")
                true
            } else {
                val errorBody = response.body?.string()
                android.util.Log.e("SOSService", "❌ Upload failed with status: ${response.code}")
                android.util.Log.e("SOSService", "❌ Error response: $errorBody")
                false
            }
            
        } catch (e: Exception) {
            android.util.Log.e("SOSService", "❌ Error uploading to Firebase Functions: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    // Show upload success notification
    private fun showUploadSuccessNotification(fileName: String) {
        try {
            val notificationManager = getSystemService(android.app.NotificationManager::class.java)

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Video Uploaded Successfully")
                .setContentText("Evidence video uploaded to Google Drive: $fileName")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: Exception) {
            android.util.Log.e("SOSService", "Failed to show upload success notification: ${e.message}")
        }
    }

    // Show upload failed notification
    private fun showUploadFailedNotification(fileName: String) {
        try {
            val notificationManager = getSystemService(android.app.NotificationManager::class.java)

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Video Upload Failed")
                .setContentText("Failed to upload evidence video: $fileName")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: Exception) {
            android.util.Log.e("SOSService", "Failed to show upload failed notification: ${e.message}")
        }
    }

    // Test function to verify upload functionality
    fun testUploadFunctionality() {
        android.util.Log.d("SOSService", "🧪 Testing upload functionality...")
        
        serviceScope.launch {
            try {
                // Get user email and trusted contact email
                val userEmail = getUserEmail()
                val trustedContactEmail = getTrustedContactEmail()
                
                android.util.Log.d("SOSService", "🧪 Test - User email: $userEmail")
                android.util.Log.d("SOSService", "🧪 Test - Trusted contact email: $trustedContactEmail")
                
                if (userEmail.isEmpty()) {
                    android.util.Log.e("SOSService", "🧪 Test failed - User email not found")
                    showUploadFailedNotification("test_video.mp4")
                    return@launch
                }
                
                if (trustedContactEmail.isEmpty()) {
                    android.util.Log.e("SOSService", "🧪 Test failed - Trusted contact email not found")
                    showUploadFailedNotification("test_video.mp4")
                    return@launch
                }
                
                // Create a test payload with dummy video data
                val testVideoData = "dGVzdF92aWRlb19kYXRhX2Zvcl90ZXN0aW5n" // base64 encoded "test_video_data_for_testing"
                val testPayload = JSONObject().apply {
                    put("userEmail", userEmail)
                    put("trustedContactEmail", trustedContactEmail)
                    put("videoData", testVideoData)
                    put("fileName", "test_video_${System.currentTimeMillis()}.mp4")
                }
                
                android.util.Log.d("SOSService", "🧪 Test - Sending test payload: ${testPayload.toString()}")
                
                // Upload to Firebase Functions
                val uploadResult = uploadToFirebaseFunctions(testPayload)
                if (uploadResult) {
                    android.util.Log.d("SOSService", "🧪 Test - ✅ Upload test successful!")
                    showUploadSuccessNotification("test_video.mp4")
                } else {
                    android.util.Log.e("SOSService", "🧪 Test - ❌ Upload test failed!")
                    showUploadFailedNotification("test_video.mp4")
                }
                
            } catch (e: Exception) {
                android.util.Log.e("SOSService", "🧪 Test - Error during test: ${e.message}")
                e.printStackTrace()
                showUploadFailedNotification("test_video.mp4")
            }
        }
    }

}