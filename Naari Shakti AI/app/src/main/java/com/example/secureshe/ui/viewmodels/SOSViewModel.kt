package com.example.secureshe.ui.viewmodels

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.secureshe.data.EmergencyContact
import com.example.secureshe.data.repository.EmergencyContactsRepository
import com.example.secureshe.data.repository.UserPreferencesRepository
import com.example.secureshe.services.SOSService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SOSViewModel @Inject constructor(
    private val emergencyContactsRepository: EmergencyContactsRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _sosState = MutableStateFlow<SOSState>(SOSState.Idle)
    val sosState: StateFlow<SOSState> = _sosState

    private val _emergencyContacts = MutableStateFlow<List<EmergencyContact>>(emptyList())
    val emergencyContacts: StateFlow<List<EmergencyContact>> = _emergencyContacts

    init {
        loadEmergencyContacts()
    }

    private fun loadEmergencyContacts() {
        viewModelScope.launch {
            emergencyContactsRepository.getEmergencyContacts().collect { contacts ->
                _emergencyContacts.value = contacts
            }
        }
    }

    fun startSOS(context: Context) {
        viewModelScope.launch {
            try {
                _sosState.value = SOSState.Loading

                val contacts = _emergencyContacts.value
                if (contacts.isEmpty()) {
                    _sosState.value = SOSState.Error("No emergency contacts found")
                    return@launch
                }

                // Start SOS service
                val intent = Intent(context, com.example.secureshe.services.SOSService::class.java).apply {
                    action = com.example.secureshe.services.SOSService.ACTION_START_SOS
                    putExtra("contact_count", contacts.size)
                    contacts.forEachIndexed { index, contact ->
                        putExtra("contact_${index}_name", contact.name)
                        putExtra("contact_${index}_phone", contact.phoneNumber)
                    }
                }

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    @Suppress("DEPRECATION")
                    context.startService(intent)
                }

                _sosState.value = SOSState.Active
                android.util.Log.d("SOSViewModel", "SOS started with ${contacts.size} contacts")

            } catch (e: Exception) {
                android.util.Log.e("SOSViewModel", "Failed to start SOS: ${e.message}")
                _sosState.value = SOSState.Error("Failed to start SOS: ${e.message}")
            }
        }
    }

    fun stopSOS(context: Context) {
        viewModelScope.launch {
            try {
                val intent = Intent(context, com.example.secureshe.services.SOSService::class.java).apply {
                    action = com.example.secureshe.services.SOSService.ACTION_STOP_SOS
                }

                context.startService(intent)
                _sosState.value = SOSState.Idle
                android.util.Log.d("SOSViewModel", "SOS stopped")

            } catch (e: Exception) {
                android.util.Log.e("SOSViewModel", "Failed to stop SOS: ${e.message}")
                _sosState.value = SOSState.Error("Failed to stop SOS: ${e.message}")
            }
        }
    }

    fun verifyPinAndStopSOS(context: Context, pin: String) {
        viewModelScope.launch {
            try {
                android.util.Log.d("SOSViewModel", "Starting PIN verification for SOS stop...")
                android.util.Log.d("SOSViewModel", "Entered PIN: '$pin' (length: ${pin.length})")

                // First, let's check what PIN is stored
                val storedPin = userPreferencesRepository.getStoredPin()
                android.util.Log.d("SOSViewModel", "Stored PIN from repository: '$storedPin'")

                val isValid = userPreferencesRepository.verifyPin(pin)
                android.util.Log.d("SOSViewModel", "PIN verification result: $isValid")

                if (isValid) {
                    android.util.Log.d("SOSViewModel", "PIN is valid, stopping SOS...")
                    stopSOS(context)
                } else {
                    android.util.Log.e("SOSViewModel", "PIN is invalid!")
                    _sosState.value = SOSState.Error("Incorrect PIN. Please try again.")
                }
            } catch (e: Exception) {
                android.util.Log.e("SOSViewModel", "Failed to verify PIN: ${e.message}")
                e.printStackTrace()
                _sosState.value = SOSState.Error("Failed to verify PIN: ${e.message}")
            }
        }
    }

    suspend fun isPinValid(pin: String): Boolean {
        return try {
            userPreferencesRepository.verifyPin(pin)
        } catch (e: Exception) {
            false
        }
    }

    fun clearError() {
        if (_sosState.value is SOSState.Error) {
            _sosState.value = SOSState.Idle
        }
    }

    // Debug function to check stored PIN
    fun debugCheckStoredPin() {
        viewModelScope.launch {
            try {
                val storedPin = userPreferencesRepository.getStoredPin()
                android.util.Log.d("SOSViewModel", "DEBUG: Stored PIN is: '$storedPin'")
            } catch (e: Exception) {
                android.util.Log.e("SOSViewModel", "DEBUG: Error checking stored PIN: ${e.message}")
            }
        }
    }
}

sealed class SOSState {
    object Idle : SOSState()
    object Loading : SOSState()
    object Active : SOSState()
    data class Error(val message: String) : SOSState()
}