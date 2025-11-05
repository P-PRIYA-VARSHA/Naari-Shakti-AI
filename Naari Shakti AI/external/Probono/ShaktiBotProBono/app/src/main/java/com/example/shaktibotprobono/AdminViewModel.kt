package com.example.shaktibotprobono

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class AdminState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val errorMessage: String? = null,
    val lawyers: List<AdminLawyer> = emptyList(),
    val currentAdmin: Admin? = null
)

class AdminViewModel : ViewModel() {

    private val probonoApp: FirebaseApp = FirebaseApp.getInstance("probono")
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(probonoApp)
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(probonoApp)

    private val _adminState = MutableStateFlow(AdminState())
    val adminState: StateFlow<AdminState> = _adminState.asStateFlow()

    init {
        checkAdminLoginStatus()
    }

    private fun checkAdminLoginStatus() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Check if user is admin
            viewModelScope.launch {
                try {
                    val adminDoc = db.collection("admins")
                        .document(currentUser.uid)
                        .get()
                        .await()

                    if (adminDoc.exists()) {
                        val admin = adminDoc.toObject(Admin::class.java)
                        _adminState.value = _adminState.value.copy(
                            isLoggedIn = true,
                            currentAdmin = admin,
                            errorMessage = null // Clear any previous errors
                        )
                        loadLawyers()
                    } else {
                        // User is logged in but not an admin, sign them out silently
                        auth.signOut()
                    }
                } catch (e: Exception) {
                    // Silently handle admin status check errors
                    // This is called on app startup and shouldn't show errors to regular users
                    android.util.Log.d("AdminViewModel", "Admin status check failed: ${e.message}")
                }
            }
        }
    }

    fun adminLogin(email: String, password: String) {
        viewModelScope.launch {
            try {
                _adminState.value = _adminState.value.copy(isLoading = true, errorMessage = null)

                // Sign in with Firebase Auth
                val sanitizedEmail = email.trim()
                val sanitizedPassword = password.trim()
                val result = auth.signInWithEmailAndPassword(sanitizedEmail, sanitizedPassword).await()

                // Check if user is admin
                val adminDoc = db.collection("admins")
                    .document(result.user?.uid ?: "")
                    .get()
                    .await()

                if (adminDoc.exists()) {
                    val admin = adminDoc.toObject(Admin::class.java)
                    _adminState.value = _adminState.value.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        currentAdmin = admin,
                        errorMessage = null // Clear any previous errors
                    )
                    loadLawyers()
                } else {
                    // Sign out if not admin
                    auth.signOut()
                    _adminState.value = _adminState.value.copy(
                        isLoading = false,
                        errorMessage = "Access denied. Admin privileges required."
                    )
                }

            } catch (e: Exception) {
                val msg = when (e) {
                    is FirebaseAuthInvalidCredentialsException -> "Invalid admin email or password."
                    is FirebaseAuthInvalidUserException -> "Admin account not found."
                    else -> e.message ?: "Login failed"
                }
                _adminState.value = _adminState.value.copy(
                    isLoading = false,
                    errorMessage = "Login failed: $msg"
                )
            }
        }
    }
    
    // Function to create admin account (for development)
    fun createAdminAccount(email: String, password: String) {
        viewModelScope.launch {
            try {
                _adminState.value = _adminState.value.copy(isLoading = true, errorMessage = null)
                
                // Create user with Firebase Auth
                val result = auth.createUserWithEmailAndPassword(email.trim(), password.trim()).await()
                
                // Create admin document in Firestore
                val admin = Admin(
                    id = result.user?.uid ?: "",
                    email = email,
                    name = "Admin",
                    role = "admin"
                )
                
                db.collection("admins")
                    .document(result.user?.uid ?: "")
                    .set(admin)
                    .await()
                
                _adminState.value = _adminState.value.copy(
                    isLoading = false,
                    isLoggedIn = true,
                    currentAdmin = admin,
                    errorMessage = "Admin account created successfully!"
                )
                loadLawyers()
                
            } catch (e: Exception) {
                _adminState.value = _adminState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to create admin: ${e.message}"
                )
            }
        }
    }
    
    // Function to create default admin account (for testing)
    fun createDefaultAdminAccount() {
        createAdminAccount("shaktibot.naariai@gmail.com", "shaktibot@2324")
    }

    fun loadLawyers() {
        viewModelScope.launch {
            try {
                _adminState.value = _adminState.value.copy(isLoading = true)

                val lawyersSnapshot = db.collection("lawyers")
                    .get()
                    .await()

                val lawyers = lawyersSnapshot.mapNotNull { doc ->
                    doc.toObject(AdminLawyer::class.java)?.copy(id = doc.id)
                }

                _adminState.value = _adminState.value.copy(
                    isLoading = false,
                    lawyers = lawyers,
                    errorMessage = null // Clear any previous errors
                )

            } catch (e: Exception) {
                _adminState.value = _adminState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load lawyers: ${e.message}"
                )
            }
        }
    }

    fun deleteLawyer(lawyerId: String) {
        viewModelScope.launch {
            try {
                _adminState.value = _adminState.value.copy(isLoading = true)

                // Delete from Firestore
                db.collection("lawyers")
                    .document(lawyerId)
                    .delete()
                    .await()

                // Remove from local state
                val updatedLawyers = _adminState.value.lawyers.filter { it.id != lawyerId }
                _adminState.value = _adminState.value.copy(
                    isLoading = false,
                    lawyers = updatedLawyers
                )

            } catch (e: Exception) {
                _adminState.value = _adminState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to delete lawyer: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _adminState.value = _adminState.value.copy(errorMessage = null)
    }
    
    fun logout() {
        auth.signOut()
        _adminState.value = AdminState()
    }
}