package com.example.secureshe.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.secureshe.data.repository.UserPreferencesRepository
import com.example.secureshe.data.repository.UserRepository
import com.example.secureshe.utils.EncryptionHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserPreferencesViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val userRepository: UserRepository,
    private val encryptionHelper: EncryptionHelper
) : ViewModel() {

    private val _userName = MutableStateFlow<String?>(null)
    val userName: StateFlow<String?> = _userName

    private val _userEmail = MutableStateFlow<String?>(null)
    val userEmail: StateFlow<String?> = _userEmail

    private val _userPhone = MutableStateFlow<String?>(null)
    val userPhone: StateFlow<String?> = _userPhone

    private val _userPin = MutableStateFlow<String?>(null)
    val userPin: StateFlow<String?> = _userPin

    private val _userId = MutableStateFlow<String?>(null)
    val userId: StateFlow<String?> = _userId

    private val _isFirstTime = MutableStateFlow(true)
    val isFirstTime: StateFlow<Boolean> = _isFirstTime

    private val _contactEmail = MutableStateFlow<String?>(null)
    val contactEmail: StateFlow<String?> = _contactEmail

    private val _backupConnected = MutableStateFlow(false)
    val backupConnected: StateFlow<Boolean> = _backupConnected

    private val _emailSendStatus = MutableStateFlow<String?>(null)
    val emailSendStatus: StateFlow<String?> = _emailSendStatus

    init {
        loadUserPreferences()
    }

    private fun loadUserPreferences() {
        viewModelScope.launch {
            userPreferencesRepository.userName.collect { name ->
                _userName.value = name
                android.util.Log.d("UserPreferencesViewModel", "Name updated: '$name'")
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.userEmail.collect { email ->
                _userEmail.value = email
                android.util.Log.d("UserPreferencesViewModel", "Email updated: '$email'")
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.userPhone.collect { phone ->
                _userPhone.value = phone
                android.util.Log.d("UserPreferencesViewModel", "Phone updated: '$phone'")
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.contactEmail.collect { email ->
                _contactEmail.value = email
                android.util.Log.d("UserPreferencesViewModel", "Contact email updated: '$email'")
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.backupConnected.collect { isConnected ->
                _backupConnected.value = isConnected
                android.util.Log.d("UserPreferencesViewModel", "Backup connected updated: $isConnected")
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.userPin.collect { pin ->
                _userPin.value = pin
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.userId.collect { id ->
                _userId.value = id
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.isFirstTime.collect { isFirst ->
                _isFirstTime.value = isFirst
            }
        }
    }

    // Function to manually refresh user data
    fun refreshUserData() {
        viewModelScope.launch {
            android.util.Log.d("UserPreferencesViewModel", "Manually refreshing user data...")

            // Get current values from repository
            val name = userPreferencesRepository.userName.first()
            val email = userPreferencesRepository.userEmail.first()
            val phone = userPreferencesRepository.userPhone.first()
            val contact = userPreferencesRepository.contactEmail.first()
            val connected = userPreferencesRepository.backupConnected.first()

            // Update StateFlow values
            _userName.value = name
            _userEmail.value = email
            _userPhone.value = phone
            _contactEmail.value = contact
            _backupConnected.value = connected

            android.util.Log.d("UserPreferencesViewModel", "Refresh complete:")
            android.util.Log.d("UserPreferencesViewModel", "Name: '$name'")
            android.util.Log.d("UserPreferencesViewModel", "Email: '$email'")
            android.util.Log.d("UserPreferencesViewModel", "Phone: '$phone'")
            android.util.Log.d("UserPreferencesViewModel", "Contact: '$contact', Connected: $connected")
        }
    }

    // Function to update user PIN with encryption
    fun updateUserPin(newPin: String) {
        viewModelScope.launch {
            // Update local encrypted storage
            encryptionHelper.storeEncryptedPin(newPin)
            
            // Update Firestore with encrypted PIN
            val currentUserId = _userId.value
            if (currentUserId != null) {
                userRepository.updateUserPin(currentUserId, newPin)
            }
            
            // Update local preferences
            userPreferencesRepository.saveUserPin(newPin)
        }
    }

    // Function to update first time status
    fun updateFirstTimeStatus(isFirst: Boolean) {
        viewModelScope.launch {
            if (!isFirst) {
                userPreferencesRepository.setFirstTimeComplete()
            }
        }
    }

    fun saveContactEmail(email: String) {
        viewModelScope.launch {
            // Save to local preferences
            userPreferencesRepository.saveContactEmail(email)
            
            // Also save to Firebase if we have a user ID
            val currentUserId = _userId.value
            if (currentUserId != null && currentUserId.isNotEmpty()) {
                try {
                    val result = userRepository.updateTrustedContactEmail(currentUserId, email)
                    if (result.isSuccess) {
                        android.util.Log.d("UserPreferencesViewModel", "Trusted contact email saved to Firebase: $email")
                    } else {
                        android.util.Log.e("UserPreferencesViewModel", "Failed to save trusted contact email to Firebase: ${result.exceptionOrNull()?.message}")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("UserPreferencesViewModel", "Error saving trusted contact email to Firebase: ${e.message}")
                }
            } else {
                android.util.Log.w("UserPreferencesViewModel", "No user ID available, skipping Firebase save")
            }
        }
    }

    fun markBackupConnected(connected: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setBackupConnected(connected)
            _backupConnected.value = connected
        }
    }

    // Function to check if backup is connected by verifying the setup status
    fun checkBackupConnectionStatus() {
        viewModelScope.launch {
            val isConnected = userPreferencesRepository.backupConnected.first()
            _backupConnected.value = isConnected
            android.util.Log.d("UserPreferencesViewModel", "Backup connection status checked: $isConnected")
        }
    }

    // Function to mark backup as connected after successful setup
    fun markBackupAsConnected() {
        viewModelScope.launch {
            userPreferencesRepository.setBackupConnected(true)
            _backupConnected.value = true
            android.util.Log.d("UserPreferencesViewModel", "Backup marked as connected")
        }
    }

    fun generateSetupLinkFor(contactEmail: String): String {
        val token = generateTokenWithTimestamp()
        val encodedEmail = java.net.URLEncoder.encode(contactEmail, "UTF-8")
        return "https://she-56fea.web.app/setup-drive?token=$token&email=$encodedEmail"
    }

    private fun generateTokenWithTimestamp(): String {
        val hexChars = "0123456789abcdef"
        val random = java.util.Random()
        val sb = StringBuilder()
        repeat(24) { sb.append(hexChars[random.nextInt(hexChars.length)]) }
        val nowSeconds = (System.currentTimeMillis() / 1000L).toString(16).padStart(8, '0').takeLast(8)
        sb.append(nowSeconds)
        return sb.toString()
    }

    // Send setup link email using EmailJS public REST API (no server needed)
    fun sendSetupEmailViaEmailJS(contactEmail: String, userEmail: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val setupLink = generateSetupLinkFor(contactEmail)
                val payload = """
                    {
                      "service_id": "service_wmfy1ym",
                      "template_id": "template_dygrjqs",
                      "user_id": "4_khaQaZdPlN0GJA1",
                      "template_params": {
                        "user_email": "$userEmail",
                        "to_email": "$contactEmail",
                        "setup_link": "$setupLink",
                        "subject": "SecureShe: Setup Google Drive for $userEmail"
                      }
                    }
                """.trimIndent()

                val url = java.net.URL("https://api.emailjs.com/api/v1.0/email/send")
                val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    // Some EmailJS configurations require a known origin; use our hosted domain
                    setRequestProperty("Origin", "https://she-56fea.web.app")
                    setRequestProperty("Referer", "https://she-56fea.web.app/")
                }

                conn.outputStream.use { os ->
                    val input = payload.toByteArray(Charsets.UTF_8)
                    os.write(input, 0, input.size)
                }

                val code = conn.responseCode
                if (code in 200..299) {
                    _emailSendStatus.value = "sent"
                } else {
                    val errorMsg = try {
                        conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    } catch (e: Exception) { "" }
                    _emailSendStatus.value = if (errorMsg.isNotBlank()) "failed: HTTP $code $errorMsg" else "failed: HTTP $code"
                }
            } catch (e: Exception) {
                _emailSendStatus.value = "error: ${e.message}"
            }
        }
    }

    fun clearEmailStatus() {
        _emailSendStatus.value = null
    }

    // Function to load user data from Firestore
    fun loadUserDataFromFirestore(userId: String) {
        viewModelScope.launch {
            android.util.Log.d("UserPreferencesViewModel", "Loading user data from Firestore for user: $userId")
            val userProfileResult = userRepository.getUserProfile(userId)
            if (userProfileResult.isSuccess) {
                val userProfile = userProfileResult.getOrNull()
                if (userProfile != null) {
                    android.util.Log.d("UserPreferencesViewModel", "User profile loaded from Firestore: $userProfile")
                    
                    // Update local preferences
                    userPreferencesRepository.saveUserName(userProfile.name)
                    userPreferencesRepository.saveUserEmail(userProfile.email)
                    userPreferencesRepository.saveUserPhone(userProfile.phoneNumber)
                    userPreferencesRepository.saveUserPin(userProfile.pin)
                    userPreferencesRepository.saveUserId(userProfile.uid)
                    
                    android.util.Log.d("UserPreferencesViewModel", "User data loaded from Firestore and saved to local preferences")
                } else {
                    android.util.Log.d("UserPreferencesViewModel", "No user profile found in Firestore")
                }
            } else {
                android.util.Log.e("UserPreferencesViewModel", "Error loading user profile from Firestore: ${userProfileResult.exceptionOrNull()?.message}")
            }
        }
    }

    // Debug function to manually check stored data
    fun debugCheckStoredData() {
        viewModelScope.launch {
            android.util.Log.d("UserPreferencesViewModel", "=== DEBUG: Checking stored user data ===")
            android.util.Log.d("UserPreferencesViewModel", "Current StateFlow values:")
            android.util.Log.d("UserPreferencesViewModel", "Name: '${_userName.value}'")
            android.util.Log.d("UserPreferencesViewModel", "Email: '${_userEmail.value}'")
            android.util.Log.d("UserPreferencesViewModel", "Phone: '${_userPhone.value}'")
            android.util.Log.d("UserPreferencesViewModel", "PIN: '${_userPin.value}'")
            android.util.Log.d("UserPreferencesViewModel", "User ID: '${_userId.value}'")
            android.util.Log.d("UserPreferencesViewModel", "Is First Time: ${_isFirstTime.value}")
            android.util.Log.d("UserPreferencesViewModel", "Contact Email: '${_contactEmail.value}', Connected: ${_backupConnected.value}")
        }
    }
}