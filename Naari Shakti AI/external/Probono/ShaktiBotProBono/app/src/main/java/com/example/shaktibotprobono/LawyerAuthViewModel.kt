package com.example.shaktibotprobono

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class LawyerLoginState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val isEmailVerified: Boolean = false,
    val errorMessage: String? = null
)

class LawyerAuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance(FirebaseApp.getInstance("probono"))

    private val _state = MutableStateFlow(LawyerLoginState())
    val state: StateFlow<LawyerLoginState> = _state

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isLoading = true, errorMessage = null, isSuccess = false)
                val sanitizedEmail = email.trim()
                val sanitizedPassword = password.trim()
                val result = auth.signInWithEmailAndPassword(sanitizedEmail, sanitizedPassword).await()
                val user = result.user
                val isVerified = user?.isEmailVerified == true
                _state.value = _state.value.copy(
                    isLoading = false,
                    isSuccess = true,
                    isEmailVerified = isVerified,
                    errorMessage = null
                )
            } catch (e: Exception) {
                val friendlyMessage = when (e) {
                    is FirebaseAuthInvalidCredentialsException -> "Invalid email or password. Please check your credentials."
                    is FirebaseAuthInvalidUserException -> "No account found for this email. Please register first."
                    else -> e.message ?: "Login failed"
                }
                _state.value = _state.value.copy(
                    isLoading = false,
                    isSuccess = false,
                    errorMessage = friendlyMessage
                )
            }
        }
    }
}


