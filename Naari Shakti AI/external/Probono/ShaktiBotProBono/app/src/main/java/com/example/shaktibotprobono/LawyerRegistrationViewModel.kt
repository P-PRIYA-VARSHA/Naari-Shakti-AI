package com.example.shaktibotprobono

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.regex.Pattern

data class RegistrationState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

class LawyerRegistrationViewModel : ViewModel() {
    
    private val probonoApp = FirebaseApp.getInstance("probono")
    private val auth = FirebaseAuth.getInstance(probonoApp)
    private val db = FirebaseFirestore.getInstance(probonoApp)
    
    // Form fields
    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name
    
    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email
    
    private val _phone = MutableStateFlow("")
    val phone: StateFlow<String> = _phone
    
    private val _barId = MutableStateFlow("")
    val barId: StateFlow<String> = _barId
    
    private val _state = MutableStateFlow("")
    val state: StateFlow<String> = _state
    
    private val _verificationPhoto = MutableStateFlow<Bitmap?>(null)
    val verificationPhoto: StateFlow<Bitmap?> = _verificationPhoto
    
    private val _verificationStatus = MutableStateFlow<VerificationStatus>(VerificationStatus.PENDING)
    val verificationStatus: StateFlow<VerificationStatus> = _verificationStatus
    
    private val _specialization = MutableStateFlow("")
    val specialization: StateFlow<String> = _specialization
    
    private val _selectedLanguages = MutableStateFlow<List<String>>(listOf())
    val selectedLanguages: StateFlow<List<String>> = _selectedLanguages
    
    private val _gender = MutableStateFlow("")
    val gender: StateFlow<String> = _gender
    
    // Password fields
    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password
    private val _confirmPassword = MutableStateFlow("")
    val confirmPassword: StateFlow<String> = _confirmPassword
    private val _passwordError = MutableStateFlow("")
    val passwordError: StateFlow<String> = _passwordError
    
    // Error states
    private val _nameError = MutableStateFlow("")
    val nameError: StateFlow<String> = _nameError
    
    private val _emailError = MutableStateFlow("")
    val emailError: StateFlow<String> = _emailError
    
    private val _phoneError = MutableStateFlow("")
    val phoneError: StateFlow<String> = _phoneError
    
    private val _barIdError = MutableStateFlow("")
    val barIdError: StateFlow<String> = _barIdError
    
    // State flow
    private val _registrationState = MutableStateFlow(RegistrationState())
    val registrationState: StateFlow<RegistrationState> = _registrationState
    
    // Form validity state
    private val _isFormValid = MutableStateFlow(false)
    val isFormValid: StateFlow<Boolean> = _isFormValid
    
    // Form validation
    private fun checkFormValid(): Boolean {
        return _name.value.isNotBlank() && 
               _email.value.isNotBlank() && 
               _phone.value.isNotBlank() && 
               _barId.value.isNotBlank() && 
               _state.value.isNotBlank() && 
               _specialization.value.isNotBlank() && 
               _selectedLanguages.value.isNotEmpty() &&
               _gender.value.isNotBlank() &&
               _password.value.isNotBlank() &&
               _confirmPassword.value.isNotBlank() &&
               _nameError.value.isEmpty() && 
               _emailError.value.isEmpty() && 
               _phoneError.value.isEmpty() && 
               _barIdError.value.isEmpty() &&
               _passwordError.value.isEmpty()
    }
    
    // Update form validity state
    private fun updateFormValidity() {
        _isFormValid.value = checkFormValid()
    }
    
    // Update functions
    fun updateName(newName: String) {
        _name.value = newName
        validateName()
        updateFormValidity()
    }
    
    fun updateEmail(newEmail: String) {
        _email.value = newEmail
        validateEmail()
        updateFormValidity()
    }
    
    fun updatePhone(newPhone: String) {
        _phone.value = newPhone
        validatePhone()
        updateFormValidity()
    }
    
    fun updateBarId(newBarId: String) {
        _barId.value = newBarId
        validateBarId()
        updateFormValidity()
    }
    
    fun updateState(newState: String) {
        _state.value = newState
        updateFormValidity()
    }
    
    fun updateSpecialization(newSpecialization: String) {
        _specialization.value = newSpecialization
        updateFormValidity()
    }
    
    fun updateGender(newGender: String) {
        _gender.value = newGender
        updateFormValidity()
    }
    
    fun updatePassword(newPassword: String) {
        _password.value = newPassword
        validatePassword()
        updateFormValidity()
    }
    
    fun updateConfirmPassword(newConfirm: String) {
        _confirmPassword.value = newConfirm
        validatePassword()
        updateFormValidity()
    }
    
    fun addLanguage(language: String) {
        if (!_selectedLanguages.value.contains(language)) {
            _selectedLanguages.value = _selectedLanguages.value + language
            updateFormValidity()
        }
    }
    
    fun removeLanguage(language: String) {
        _selectedLanguages.value = _selectedLanguages.value.filter { it != language }
        updateFormValidity()
    }
    
    // Validation functions
    private fun validateName() {
        _nameError.value = when {
            _name.value.isEmpty() -> "Name is required"
            _name.value.length < 2 -> "Name must be at least 2 characters"
            _name.value.length > 50 -> "Name must be less than 50 characters"
            !Pattern.matches("^[a-zA-Z\\s]+$", _name.value) -> "Name can only contain letters and spaces"
            else -> ""
        }
    }
    
    private fun validateEmail() {
        _emailError.value = when {
            _email.value.isEmpty() -> "Email is required"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(_email.value).matches() -> "Please enter a valid email address"
            else -> ""
        }
    }
    
    private fun validatePhone() {
        _phoneError.value = when {
            _phone.value.isEmpty() -> "Phone number is required"
            !Pattern.matches("^[+]?[0-9]{10,15}$", _phone.value.replace("\\s".toRegex(), "")) -> "Please enter a valid phone number"
            else -> ""
        }
    }
    
    private fun validateBarId() {
        _barIdError.value = when {
            _barId.value.isEmpty() -> "Bar Council ID is required"
            _barId.value.length < 3 -> "Bar Council ID must be at least 3 characters"
            _barId.value.length > 20 -> "Bar Council ID must be less than 20 characters"
            else -> ""
        }
    }
    
    private fun validatePassword() {
        val pwd = _password.value
        val confirm = _confirmPassword.value
        _passwordError.value = when {
            pwd.length < 8 -> "Password must be at least 8 characters"
            !pwd.any { it.isUpperCase() } -> "Include at least one uppercase letter"
            !pwd.any { it.isLowerCase() } -> "Include at least one lowercase letter"
            !pwd.any { it.isDigit() } -> "Include at least one number"
            pwd != confirm -> "Passwords do not match"
            else -> ""
        }
    }
    
    // Registration function
    fun registerLawyer() {
        if (!checkFormValid()) {
            _registrationState.value = RegistrationState(
                errorMessage = "Please fill all required fields correctly"
            )
            return
        }
        
        viewModelScope.launch {
            try {
                _registrationState.value = RegistrationState(isLoading = true)
                
                // Create Firebase Auth user with email and chosen password
                val authResult = auth.createUserWithEmailAndPassword(_email.value, _password.value).await()
                
                val userId = authResult.user?.uid
                if (userId == null) {
                    throw Exception("Failed to create user account")
                }
                
                // Send email verification
                authResult.user?.sendEmailVerification()?.await()
                
                // Create lawyer document using userId as document ID
                val lawyerData = hashMapOf(
                    "userId" to userId,
                    "name" to _name.value,
                    "email" to _email.value,
                    "contactNumber" to _phone.value,
                    "barId" to _barId.value,
                    "state" to _state.value,
                    "specialization" to _specialization.value,
                    "languages" to _selectedLanguages.value,
                    "gender" to _gender.value,
                    "casesHandled" to "",
                    "verified" to false,
                    "availability" to true,
                    "verificationStatus" to "pending",
                    "emailVerified" to false,
                    "createdAt" to com.google.firebase.Timestamp.now(),
                    "updatedAt" to com.google.firebase.Timestamp.now()
                )
                
                // Save lawyer data using userId as document ID
                db.collection("lawyers")
                    .document(userId)
                    .set(lawyerData)
                    .await()
                
                // Create user document for admin role check
                val userData = hashMapOf(
                    "email" to _email.value,
                    "role" to "lawyer",
                    "name" to _name.value,
                    "createdAt" to com.google.firebase.Timestamp.now()
                )
                
                db.collection("users")
                    .document(userId)
                    .set(userData)
                    .await()
                
                _registrationState.value = RegistrationState(isSuccess = true)
                
            } catch (e: Exception) {
                println("❌ Registration error: ${e.message}")
                println("❌ Error type: ${e.javaClass.simpleName}")
                e.printStackTrace()
                _registrationState.value = RegistrationState(
                    errorMessage = "Registration failed: ${e.message}. Please check Firebase configuration."
                )
            }
        }
    }
    
    // Removed temp password generator; lawyers set their own password
} 