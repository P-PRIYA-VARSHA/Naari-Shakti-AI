package com.example.secureshe.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.secureshe.data.repository.AuthRepository
import com.example.secureshe.data.repository.UserPreferencesRepository
import com.example.secureshe.data.repository.UserRepository
import com.example.secureshe.data.repository.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState
    
    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.signIn(email, password)
            _authState.value = if (result.isSuccess) {
                val user = result.getOrNull()!!
                android.util.Log.d("AuthViewModel", "Sign in successful, loading user data...")
                
                try {
                    // Try to load user profile from Firestore first
                    val userProfileResult = userRepository.getUserProfile(user.uid)
                    if (userProfileResult.isSuccess) {
                        val userProfile = userProfileResult.getOrNull()
                        if (userProfile != null) {
                            android.util.Log.d("AuthViewModel", "User profile loaded from Firestore: $userProfile")
                            
                            // Save to local preferences
                            userPreferencesRepository.saveUserName(userProfile.name)
                            userPreferencesRepository.saveUserEmail(userProfile.email)
                            userPreferencesRepository.saveUserPhone(userProfile.phoneNumber)
                            userPreferencesRepository.saveUserPin(userProfile.pin)
                            userPreferencesRepository.saveUserId(userProfile.uid)
                            
                            // Also save trusted contact email if available
                            if (userProfile.trustedContactEmail.isNotEmpty()) {
                                userPreferencesRepository.saveContactEmail(userProfile.trustedContactEmail)
                                android.util.Log.d("AuthViewModel", "Trusted contact email loaded from Firestore: ${userProfile.trustedContactEmail}")
                            }
                            
                            android.util.Log.d("AuthViewModel", "User data loaded from Firestore and saved to local preferences")
                        } else {
                            android.util.Log.d("AuthViewModel", "No user profile found in Firestore, using Firebase Auth data")
                            // Fallback to Firebase Auth data
                            user.displayName?.let { name ->
                                userPreferencesRepository.saveUserName(name)
                                android.util.Log.d("AuthViewModel", "User name from Firebase Auth: '$name'")
                            }
                            
                            userPreferencesRepository.saveUserEmail(user.email ?: email)
                            userPreferencesRepository.saveUserId(user.uid)
                        }
                    } else {
                        android.util.Log.e("AuthViewModel", "Error loading user profile from Firestore: ${userProfileResult.exceptionOrNull()?.message}")
                        // Fallback to Firebase Auth data
                        user.displayName?.let { name ->
                            userPreferencesRepository.saveUserName(name)
                        }
                        userPreferencesRepository.saveUserEmail(user.email ?: email)
                        userPreferencesRepository.saveUserId(user.uid)
                    }
                    
                    AuthState.Success(user)
                } catch (e: Exception) {
                    android.util.Log.e("AuthViewModel", "Error loading user data: ${e.message}")
                    e.printStackTrace()
                    AuthState.Error("Failed to load user data: ${e.message}")
                }
            } else {
                AuthState.Error(result.exceptionOrNull()?.message ?: "Sign in failed")
            }
        }
    }
    
    fun signUp(name: String, email: String, phoneNumber: String, password: String, pin: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            android.util.Log.d("AuthViewModel", "Starting signup process...")
            android.util.Log.d("AuthViewModel", "PIN to be saved: '$pin' (length: ${pin.length})")
            
            val result = authRepository.signUp(email, password, name, phoneNumber)
            _authState.value = if (result.isSuccess) {
                // Save PIN and user ID
                val user = result.getOrNull()!!
                android.util.Log.d("AuthViewModel", "User created successfully, saving PIN...")
                
                try {
                    // Save user profile to Firestore
                    val userProfile = UserProfile(
                        uid = user.uid,
                        name = name,
                        email = email,
                        phoneNumber = phoneNumber,
                        pin = pin
                    )
                    
                    val saveResult = userRepository.saveUserProfile(userProfile)
                    if (saveResult.isSuccess) {
                        android.util.Log.d("AuthViewModel", "User profile saved to Firestore successfully")
                    } else {
                        android.util.Log.e("AuthViewModel", "Failed to save user profile to Firestore: ${saveResult.exceptionOrNull()?.message}")
                    }
                    
                    // Also save to local preferences for quick access
                    userPreferencesRepository.saveUserPin(pin)
                    android.util.Log.d("AuthViewModel", "PIN saved to local preferences")
                    
                    userPreferencesRepository.saveUserId(user.uid)
                    android.util.Log.d("AuthViewModel", "User ID saved to local preferences")
                    
                    userPreferencesRepository.saveUserName(name)
                    android.util.Log.d("AuthViewModel", "User name saved to local preferences")
                    
                    userPreferencesRepository.saveUserEmail(email)
                    android.util.Log.d("AuthViewModel", "User email saved to local preferences")
                    
                    userPreferencesRepository.saveUserPhone(phoneNumber)
                    android.util.Log.d("AuthViewModel", "User phone saved to local preferences")
                    
                    userPreferencesRepository.setFirstTimeComplete()
                    android.util.Log.d("AuthViewModel", "First time flag set to false")
                    
                    // Verify PIN was saved correctly
                    val savedPin = userPreferencesRepository.getStoredPin()
                    android.util.Log.d("AuthViewModel", "Verified saved PIN: '$savedPin'")
                    
                    AuthState.Success(user)
                } catch (e: Exception) {
                    android.util.Log.e("AuthViewModel", "Error saving user preferences: ${e.message}")
                    e.printStackTrace()
                    AuthState.Error("Failed to save user preferences: ${e.message}")
                }
            } else {
                android.util.Log.e("AuthViewModel", "Signup failed: ${result.exceptionOrNull()?.message}")
                AuthState.Error(result.exceptionOrNull()?.message ?: "Sign up failed")
            }
        }
    }
    
    fun signOut() {
        authRepository.signOut()
        _authState.value = AuthState.Initial
    }
    
    fun checkAuthState() {
        val currentUser = authRepository.currentUser
        _authState.value = if (currentUser != null) {
            AuthState.Success(currentUser)
        } else {
            AuthState.Initial
        }
    }
    
    fun clearError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.Initial
        }
    }

    fun deleteAccount(onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                val currentUser = authRepository.currentUser
                val uid = currentUser?.uid
                    ?: run {
                        _authState.value = AuthState.Initial
                        onResult(false, "No authenticated user")
                        return@launch
                    }

                // Delete Firestore profile (best-effort)
                val firestoreResult = userRepository.deleteUserProfile(uid)
                if (firestoreResult.isFailure) {
                    android.util.Log.e("AuthViewModel", "Failed to delete Firestore profile: ${firestoreResult.exceptionOrNull()?.message}")
                }

                // Delete FirebaseAuth user
                val authDelete = authRepository.deleteCurrentUser()
                if (authDelete.isFailure) {
                    val msg = authDelete.exceptionOrNull()?.message
                    _authState.value = AuthState.Error(msg ?: "Account deletion failed")
                    onResult(false, msg)
                    return@launch
                }

                // Clear local preferences
                userPreferencesRepository.clearUserData()

                _authState.value = AuthState.Initial
                onResult(true, null)
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "Error deleting account: ${e.message}")
                _authState.value = AuthState.Error(e.message ?: "Unknown error")
                onResult(false, e.message)
            }
        }
    }

    fun reauthenticateAndDelete(password: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                val currentUser = authRepository.currentUser
                val uid = currentUser?.uid
                    ?: run {
                        _authState.value = AuthState.Initial
                        onResult(false, "No authenticated user")
                        return@launch
                    }

                val email = currentUser.email
                    ?: userPreferencesRepository.getStoredUserEmail()
                    ?: run {
                        _authState.value = AuthState.Initial
                        onResult(false, "No email available for re-authentication")
                        return@launch
                    }

                val reauth = authRepository.reauthenticate(email, password)
                if (reauth.isFailure) {
                    val msg = reauth.exceptionOrNull()?.message ?: "Re-authentication failed"
                    _authState.value = AuthState.Error(msg)
                    onResult(false, msg)
                    return@launch
                }

                // After successful reauth, proceed with deletion similar to deleteAccount
                val firestoreResult = userRepository.deleteUserProfile(uid)
                if (firestoreResult.isFailure) {
                    android.util.Log.e("AuthViewModel", "Failed to delete Firestore profile after reauth: ${firestoreResult.exceptionOrNull()?.message}")
                }

                val authDelete = authRepository.deleteCurrentUser()
                if (authDelete.isFailure) {
                    val msg = authDelete.exceptionOrNull()?.message
                    _authState.value = AuthState.Error(msg ?: "Account deletion failed")
                    onResult(false, msg)
                    return@launch
                }

                userPreferencesRepository.clearUserData()
                _authState.value = AuthState.Initial
                onResult(true, null)
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "Error reauthenticating and deleting account: ${e.message}")
                _authState.value = AuthState.Error(e.message ?: "Unknown error")
                onResult(false, e.message)
            }
        }
    }
}

sealed class AuthState {
    object Initial : AuthState()
    object Loading : AuthState()
    data class Success(val user: com.google.firebase.auth.FirebaseUser) : AuthState()
    data class Error(val message: String) : AuthState()
} 