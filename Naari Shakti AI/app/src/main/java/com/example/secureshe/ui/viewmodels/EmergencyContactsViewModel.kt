package com.example.secureshe.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.secureshe.data.EmergencyContact
import com.example.secureshe.data.repository.EmergencyContactsRepository
import com.example.secureshe.utils.PermissionHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EmergencyContactsViewModel @Inject constructor(
    private val emergencyContactsRepository: EmergencyContactsRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<EmergencyContactsUiState>(EmergencyContactsUiState.Loading)
    val uiState: StateFlow<EmergencyContactsUiState> = _uiState
    
    private val _phoneContacts = MutableStateFlow<List<EmergencyContact>>(emptyList())
    val phoneContacts: StateFlow<List<EmergencyContact>> = _phoneContacts
    
    private val _selectedContacts = MutableStateFlow<List<EmergencyContact>>(emptyList())
    val selectedContacts: StateFlow<List<EmergencyContact>> = _selectedContacts
    
    init {
        loadEmergencyContacts()
    }
    
    fun loadEmergencyContacts() {
        viewModelScope.launch {
            _uiState.value = EmergencyContactsUiState.Loading
            try {
                emergencyContactsRepository.getEmergencyContacts().collect { contacts ->
                    _selectedContacts.value = contacts
                    _uiState.value = EmergencyContactsUiState.Success
                }
            } catch (e: Exception) {
                _uiState.value = EmergencyContactsUiState.Error("Failed to load emergency contacts")
            }
        }
    }
    
    fun loadPhoneContacts() {
        viewModelScope.launch {
            try {
                val contacts = emergencyContactsRepository.getPhoneContacts()
                _phoneContacts.value = contacts
            } catch (e: Exception) {
                _uiState.value = EmergencyContactsUiState.Error("Failed to load phone contacts")
            }
        }
    }
    
    fun addEmergencyContact(contact: EmergencyContact) {
        viewModelScope.launch {
            try {
                val currentCount = emergencyContactsRepository.getEmergencyContactCount()
                if (currentCount < 3) {
                    emergencyContactsRepository.addEmergencyContact(contact)
                } else {
                    _uiState.value = EmergencyContactsUiState.Error("Maximum 3 emergency contacts allowed")
                }
            } catch (e: Exception) {
                _uiState.value = EmergencyContactsUiState.Error("Failed to add emergency contact")
            }
        }
    }
    
    fun removeEmergencyContact(contact: EmergencyContact) {
        viewModelScope.launch {
            try {
                emergencyContactsRepository.removeEmergencyContact(contact)
            } catch (e: Exception) {
                _uiState.value = EmergencyContactsUiState.Error("Failed to remove emergency contact")
            }
        }
    }
    
    fun clearError() {
        if (_uiState.value is EmergencyContactsUiState.Error) {
            _uiState.value = EmergencyContactsUiState.Success
        }
    }
}

sealed class EmergencyContactsUiState {
    object Loading : EmergencyContactsUiState()
    object Success : EmergencyContactsUiState()
    data class Error(val message: String) : EmergencyContactsUiState()
} 