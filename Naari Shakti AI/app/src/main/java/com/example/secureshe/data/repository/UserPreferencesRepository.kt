package com.example.secureshe.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesRepository @Inject constructor(
    private val context: Context
) {
    
    private object PreferencesKeys {
        val USER_PIN = stringPreferencesKey("user_pin")
        val USER_ID = stringPreferencesKey("user_id")
        val IS_FIRST_TIME = booleanPreferencesKey("is_first_time")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_PHONE = stringPreferencesKey("user_phone")
        val CONTACT_EMAIL = stringPreferencesKey("contact_email")
        val BACKUP_CONNECTED = booleanPreferencesKey("backup_connected")
    }
    
    // These properties are kept for future use when implementing user preferences UI
    val userPin: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.USER_PIN]
    }
    
    val userId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.USER_ID]
    }
    
    val isFirstTime: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.IS_FIRST_TIME] ?: true
    }
    
    val userName: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.USER_NAME]
    }
    
    val userEmail: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.USER_EMAIL]
    }
    
    val userPhone: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.USER_PHONE]
    }

    val contactEmail: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.CONTACT_EMAIL]
    }

    val backupConnected: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.BACKUP_CONNECTED] ?: false
    }
    
    suspend fun saveUserPin(pin: String) {
        try {
            android.util.Log.d("UserPreferences", "Saving PIN: '$pin' (length: ${pin.length})")
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.USER_PIN] = pin
            }
            android.util.Log.d("UserPreferences", "PIN saved successfully")
            
            // Verify the PIN was saved correctly
            val savedPin = context.dataStore.data.map { preferences ->
                preferences[PreferencesKeys.USER_PIN]
            }.first()
            android.util.Log.d("UserPreferences", "Verified saved PIN: '$savedPin'")
        } catch (e: Exception) {
            android.util.Log.e("UserPreferences", "Error saving PIN: ${e.message}")
            e.printStackTrace()
        }
    }
    
    suspend fun saveUserId(userId: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_ID] = userId
        }
    }
    
    suspend fun saveUserName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_NAME] = name
        }
    }
    
    suspend fun saveUserEmail(email: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_EMAIL] = email
        }
        // Also save to SharedPreferences for backward compatibility
        try {
            val sharedPrefs = context.getSharedPreferences("user_preferences", Context.MODE_PRIVATE)
            sharedPrefs.edit().putString("user_email", email).apply()
            android.util.Log.d("UserPreferences", "Saved user email to both DataStore and SharedPreferences: $email")
        } catch (e: Exception) {
            android.util.Log.e("UserPreferences", "Error saving user email to SharedPreferences: ${e.message}")
        }
    }
    
    suspend fun saveUserPhone(phone: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_PHONE] = phone
        }
    }

    suspend fun saveContactEmail(email: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CONTACT_EMAIL] = email
        }
        // Also save to SharedPreferences for backward compatibility
        try {
            val sharedPrefs = context.getSharedPreferences("user_preferences", Context.MODE_PRIVATE)
            sharedPrefs.edit().putString("contact_email", email).apply()
            android.util.Log.d("UserPreferences", "Saved contact email to both DataStore and SharedPreferences: $email")
        } catch (e: Exception) {
            android.util.Log.e("UserPreferences", "Error saving contact email to SharedPreferences: ${e.message}")
        }
    }

    suspend fun setBackupConnected(isConnected: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BACKUP_CONNECTED] = isConnected
        }
    }
    
    suspend fun setFirstTimeComplete() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_FIRST_TIME] = false
        }
    }
    
    suspend fun verifyPin(enteredPin: String): Boolean {
        return try {
            android.util.Log.d("UserPreferences", "Starting PIN verification...")
            android.util.Log.d("UserPreferences", "Entered PIN: '$enteredPin' (length: ${enteredPin.length})")
            
            val storedPin = context.dataStore.data.map { preferences ->
                preferences[PreferencesKeys.USER_PIN]
            }.first()
            
            android.util.Log.d("UserPreferences", "Stored PIN: '$storedPin' (length: ${storedPin?.length})")
            android.util.Log.d("UserPreferences", "PIN comparison: '$storedPin' == '$enteredPin' = ${storedPin == enteredPin}")
            
            if (storedPin == null) {
                android.util.Log.e("UserPreferences", "No PIN stored in preferences!")
                return false
            }
            
            if (enteredPin.isEmpty()) {
                android.util.Log.e("UserPreferences", "Entered PIN is empty!")
                return false
            }
            
            val isValid = storedPin == enteredPin
            android.util.Log.d("UserPreferences", "PIN verification result: $isValid")
            
            isValid
        } catch (e: Exception) {
            android.util.Log.e("UserPreferences", "Error verifying PIN: ${e.message}")
            e.printStackTrace()
            false
        }
    }
    
    // Function kept for future use when implementing logout functionality
    suspend fun clearUserData() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
    
    // Debug function to get stored PIN
    suspend fun getStoredPin(): String? {
        return try {
            val pin = context.dataStore.data.map { preferences ->
                preferences[PreferencesKeys.USER_PIN]
            }.first()
            android.util.Log.d("UserPreferences", "Retrieved stored PIN: '$pin'")
            pin
        } catch (e: Exception) {
            android.util.Log.e("UserPreferences", "Error retrieving PIN: ${e.message}")
            null
        }
    }
    
    // Debug functions to get stored user information
    suspend fun getStoredUserName(): String? {
        return try {
            val name = context.dataStore.data.map { preferences ->
                preferences[PreferencesKeys.USER_NAME]
            }.first()
            android.util.Log.d("UserPreferences", "Retrieved stored name: '$name'")
            name
        } catch (e: Exception) {
            android.util.Log.e("UserPreferences", "Error retrieving name: ${e.message}")
            null
        }
    }
    
    suspend fun getStoredUserEmail(): String? {
        return try {
            val email = context.dataStore.data.map { preferences ->
                preferences[PreferencesKeys.USER_EMAIL]
            }.first()
            android.util.Log.d("UserPreferences", "Retrieved stored email: '$email'")
            email
        } catch (e: Exception) {
            android.util.Log.e("UserPreferences", "Error retrieving email: ${e.message}")
            null
        }
    }
    
    suspend fun getStoredUserPhone(): String? {
        return try {
            val phone = context.dataStore.data.map { preferences ->
                preferences[PreferencesKeys.USER_PHONE]
            }.first()
            android.util.Log.d("UserPreferences", "Retrieved stored phone: '$phone'")
            phone
        } catch (e: Exception) {
            android.util.Log.e("UserPreferences", "Error retrieving phone: ${e.message}")
            null
        }
    }

    suspend fun getStoredContactEmail(): String? {
        return try {
            val email = context.dataStore.data.map { preferences ->
                preferences[PreferencesKeys.CONTACT_EMAIL]
            }.first()
            android.util.Log.d("UserPreferences", "Retrieved contact email: '$email'")
            email
        } catch (e: Exception) {
            android.util.Log.e("UserPreferences", "Error retrieving contact email: ${e.message}")
            null
        }
    }
} 