package com.example.shaktibotprobono

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.FirebaseApp
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log

data class UploadState(
    val isLoading: Boolean = false,
    val uploadProgress: Float = 0f,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val uploadedDocuments: List<DocumentInfo> = emptyList(),
    val uploadedPhotos: List<PhotoInfo> = emptyList()
)

data class DocumentInfo(
    val id: String = "",
    val name: String = "",
    val type: String = "",
    val size: Long = 0L,
    val googleDriveFileId: String? = null, // Changed from downloadUrl to googleDriveFileId
    val uploadTime: Long = 0L
) {
    // No-argument constructor for Firestore
    constructor() : this("", "", "", 0L, null, 0L)
}

data class PhotoInfo(
    val id: String = "",
    val name: String = "",
    val type: String = "", // "profile" or "verification"
    val downloadUrl: String = "", // This will store Google Drive file ID for photos
    val uploadTime: Long = 0L
) {
    // No-argument constructor for Firestore
    constructor() : this("", "", "", "", 0L)
}

class DocumentUploadViewModel : ViewModel() {
    
    private val probonoApp = FirebaseApp.getInstance("probono")
    private val auth = FirebaseAuth.getInstance(probonoApp)
    private val db = FirebaseFirestore.getInstance(probonoApp)
    private val adminApi by lazy {
        val base = AppConfig.ADMIN_BACKEND_BASE_URL
        if (base.isNotBlank()) com.example.shaktibotprobono.network.AdminUploadApi.create(base) else null
    }
    
    // Google Drive integration
    private var googleDriveService: GoogleDriveService? = null
    private var lastGoogleAccount: GoogleSignInAccount? = null
    private var excelVerificationService: ExcelVerificationService? = null
    
    private val _uploadState = MutableStateFlow(UploadState())
    val uploadState: StateFlow<UploadState> = _uploadState.asStateFlow()
    
    // File validation constants
    companion object {
        const val MAX_FILE_SIZE = 5 * 1024 * 1024 // 5MB
        val ALLOWED_DOCUMENT_TYPES = listOf("pdf", "jpg", "jpeg", "png")
        val ALLOWED_PHOTO_TYPES = listOf("jpg", "jpeg", "png")
    }

    private fun getApplicationFileStream(uri: Uri): okhttp3.RequestBody {
        val input = com.example.shaktibotprobono.Utils.appContext.contentResolver.openInputStream(uri)
        val bytes = input?.use { it.readBytes() } ?: ByteArray(0)
        return bytes.toRequestBody("application/octet-stream".toMediaTypeOrNull())
    }
    
    fun initializeGoogleDrive(context: android.content.Context) {
        try {
            println("🔧 Initializing Google Drive service...")
            googleDriveService = GoogleDriveService(context)
            excelVerificationService = ExcelVerificationService(context)
            println("✅ Google Drive service initialized successfully")
            // Apply any previously captured Google account
            lastGoogleAccount?.let {
                googleDriveService?.setGoogleAccount(it)
                println("🔄 Applied saved Google account to Drive service: ${it.email}")
            }
        } catch (e: Exception) {
            println("❌ Failed to initialize Google Drive service: ${e.message}")
            e.printStackTrace()
        }
    }
    
    fun setGoogleAccount(account: GoogleSignInAccount?) {
        lastGoogleAccount = account
        if (googleDriveService != null) {
            googleDriveService?.setGoogleAccount(account)
        }
        if (account != null) {
            println("✅ Google account captured: ${account.email}")
        } else {
            println("❌ Google account cleared")
        }
    }
    
    fun uploadDocument(uri: Uri, fileName: String) {
        println("🚀 Starting direct document upload - File: $fileName")
        viewModelScope.launch {
            try {
                _uploadState.value = _uploadState.value.copy(isLoading = true, errorMessage = null)
                
                // Validate file
                val validationResult = validateDocument(uri, fileName)
                if (!validationResult.isValid) {
                    _uploadState.value = _uploadState.value.copy(
                        isLoading = false,
                        errorMessage = validationResult.errorMessage
                    )
                    return@launch
                }
                
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    _uploadState.value = _uploadState.value.copy(
                        isLoading = false,
                        errorMessage = "User not authenticated"
                    )
                    return@launch
                }
                
                val driveFileId = if (adminApi.let { it != null }) {
                    // Upload via backend (service account)
                    println("🌐 Uploading document via admin backend ...")
                    val stream = getApplicationFileStream(uri)
                    val part = okhttp3.MultipartBody.Part.createFormData(
                        name = "file",
                        filename = fileName,
                        body = stream
                    )
                    val lawyerIdBody = currentUser.uid.toRequestBody()
                    val lawyerNameBody = (currentUser.displayName ?: "Unknown Lawyer").toRequestBody()
                    val typeBody = "document".toRequestBody()
                    val api = adminApi
                    val resp = api!!.upload(part, lawyerIdBody, lawyerNameBody, typeBody)
                    if (!resp.isSuccessful) {
                        println("❌ Backend upload failed: HTTP ${'$'}{resp.code()} body=${'$'}{resp.errorBody()?.string()}")
                        null
                    } else {
                        // Expect a JSON like {"fileId":"..."} or a plain ID string. Handle both.
                        val body = resp.body() ?: ""
                        val fileId = body.trim()
                        if (fileId.startsWith("{")) {
                            val id = Regex("\\\"fileId\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").find(fileId)?.groupValues?.get(1)
                            id
                        } else fileId
                    }
                } else {
                    // Fallback: on-device Google Drive
                    if (googleDriveService == null) {
                        _uploadState.value = _uploadState.value.copy(isLoading = false, errorMessage = "Upload service unavailable")
                        return@launch
                    }
                    println("📁 Uploading directly to admin Google Drive (device auth)...")
                    googleDriveService!!.uploadDocumentToAdminDrive(
                        fileUri = uri,
                        fileName = fileName,
                        lawyerId = currentUser.uid,
                        lawyerName = currentUser.displayName ?: "Unknown Lawyer"
                    )
                }
                
                if (driveFileId != null) {
                    // Create document info
                    val documentInfo = DocumentInfo(
                        id = "${System.currentTimeMillis()}_${fileName.replace(" ", "_")}",
                        name = fileName,
                        type = fileName.substringAfterLast(".", "").lowercase(),
                        size = 0,
                        googleDriveFileId = driveFileId,
                        uploadTime = System.currentTimeMillis()
                    )
                    
                    // Save to Firestore
                    saveDocumentInfoToFirestore(documentInfo)
                    
                    // Update state
                    val currentDocuments = _uploadState.value.uploadedDocuments.toMutableList()
                    currentDocuments.add(documentInfo)
                    
                    _uploadState.value = _uploadState.value.copy(
                        isLoading = false,
                        uploadProgress = 100f,
                        uploadedDocuments = currentDocuments,
                        isSuccess = true
                    )
                    
                    println("✅ Document uploaded successfully to admin Google Drive: $driveFileId")
                } else {
                    _uploadState.value = _uploadState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to upload to Google Drive. Please try again."
                    )
                }
                
            } catch (e: Exception) {
                println("❌ Document upload error: ${e.message}")
                e.printStackTrace()
                _uploadState.value = _uploadState.value.copy(
                    isLoading = false,
                    uploadProgress = 0f,
                    errorMessage = "Upload failed: ${e.message}. Please try again."
                )
            }
        }
    }
    
    fun uploadPhoto(uri: Uri, fileName: String, photoType: String) {
        println("🚀 Starting direct photo upload - File: $fileName, Type: $photoType")
        viewModelScope.launch {
            try {
                _uploadState.value = _uploadState.value.copy(isLoading = true, errorMessage = null)
                
                // Validate photo
                val validationResult = validatePhoto(uri, fileName, photoType)
                if (!validationResult.isValid) {
                    _uploadState.value = _uploadState.value.copy(
                        isLoading = false,
                        errorMessage = validationResult.errorMessage
                    )
                    return@launch
                }
                
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    _uploadState.value = _uploadState.value.copy(
                        isLoading = false,
                        errorMessage = "User not authenticated"
                    )
                    return@launch
                }
                
                val driveFileId = if (adminApi.let { it != null }) {
                    println("🌐 Uploading photo via admin backend ...")
                    val stream = getApplicationFileStream(uri)
                    val part = okhttp3.MultipartBody.Part.createFormData(
                        name = "file",
                        filename = fileName,
                        body = stream
                    )
                    val lawyerIdBody = currentUser.uid.toRequestBody()
                    val lawyerNameBody = (currentUser.displayName ?: "Unknown Lawyer").toRequestBody()
                    val typeBody = photoType.toRequestBody()
                    val api = adminApi
                    val resp = api!!.upload(part, lawyerIdBody, lawyerNameBody, typeBody)
                    if (!resp.isSuccessful) {
                        println("❌ Backend upload failed: HTTP ${'$'}{resp.code()} body=${'$'}{resp.errorBody()?.string()}")
                        null
                    } else {
                        val body = resp.body() ?: ""
                        val fileId = body.trim()
                        if (fileId.startsWith("{")) {
                            val id = Regex("\\\"fileId\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").find(fileId)?.groupValues?.get(1)
                            id
                        } else fileId
                    }
                } else {
                    if (googleDriveService == null) {
                        _uploadState.value = _uploadState.value.copy(isLoading = false, errorMessage = "Upload service unavailable")
                        return@launch
                    }
                    println("📸 Uploading directly to admin Google Drive (device auth)...")
                    googleDriveService!!.uploadPhotoToAdminDrive(
                        fileUri = uri,
                        fileName = fileName,
                        lawyerId = currentUser.uid,
                        lawyerName = currentUser.displayName ?: "Unknown Lawyer",
                        photoType = photoType
                    )
                }
                
                if (driveFileId != null) {
                    // Create photo info
                    val photoInfo = PhotoInfo(
                        id = "${photoType}_${System.currentTimeMillis()}_${fileName.replace(" ", "_")}",
                        name = fileName,
                        type = photoType,
                        downloadUrl = driveFileId, // Store Google Drive file ID for photos
                        uploadTime = System.currentTimeMillis()
                    )
                    
                    // Save to Firestore
                    savePhotoInfoToFirestore(photoInfo)
                    
                    // Update state with new photo
                    val currentPhotos = _uploadState.value.uploadedPhotos.toMutableList()
                    // Remove existing photo of same type if exists
                    currentPhotos.removeAll { it.type == photoType }
                    currentPhotos.add(photoInfo)
                    
                    _uploadState.value = _uploadState.value.copy(
                        isLoading = false,
                        uploadProgress = 100f,
                        uploadedPhotos = currentPhotos,
                        isSuccess = true
                    )
                    
                    println("✅ Photo uploaded successfully to admin Google Drive: $driveFileId")
                } else {
                    _uploadState.value = _uploadState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to upload photo to Google Drive. Please try again."
                    )
                }
                
            } catch (e: Exception) {
                println("❌ Photo upload error: ${e.message}")
                e.printStackTrace()
                _uploadState.value = _uploadState.value.copy(
                    isLoading = false,
                    uploadProgress = 0f,
                    errorMessage = "Photo upload failed: ${e.message}. Please try again."
                )
            }
        }
    }
    
    private fun validateDocument(uri: Uri, fileName: String): ValidationResult {
        val fileExtension = fileName.substringAfterLast(".", "").lowercase()
        
        println("🔍 Document validation - File: $fileName, Extension: $fileExtension")
        println("🔍 Allowed types: $ALLOWED_DOCUMENT_TYPES")
        
        // More flexible validation - check if extension is in allowed types
        if (fileExtension.isNotEmpty() && !ALLOWED_DOCUMENT_TYPES.contains(fileExtension)) {
            println("❌ Document validation failed - unsupported extension: $fileExtension")
            return ValidationResult(false, "File type not supported. Please upload PDF, JPG, JPEG, or PNG files.")
        }
        
        println("✅ Document validation passed")
        // If no extension, allow it (some files might not have extensions)
        return ValidationResult(true, null)
    }
    
    private fun validatePhoto(uri: Uri, fileName: String, photoType: String): ValidationResult {
        val fileExtension = fileName.substringAfterLast(".", "").lowercase()
        
        println("🔍 Photo validation - File: $fileName, Extension: $fileExtension, Type: $photoType")
        println("🔍 Allowed photo types: $ALLOWED_PHOTO_TYPES")
        
        // More flexible validation for photos
        if (fileExtension.isNotEmpty() && !ALLOWED_PHOTO_TYPES.contains(fileExtension)) {
            println("❌ Photo validation failed - unsupported extension: $fileExtension")
            return ValidationResult(false, "Photo type not supported. Please upload JPG, JPEG, or PNG files.")
        }
        
        if (photoType !in listOf("profile", "verification")) {
            println("❌ Photo validation failed - invalid type: $photoType")
            return ValidationResult(false, "Invalid photo type. Must be 'profile' or 'verification'.")
        }
        
        println("✅ Photo validation passed")
        return ValidationResult(true, null)
    }
    
    private suspend fun saveDocumentInfoToFirestore(documentInfo: DocumentInfo) {
        try {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                db.collection("lawyers")
                    .document(currentUser.uid)
                    .collection("documents")
                    .document(documentInfo.id)
                    .set(documentInfo)
                    .await()
                println("✅ Document info saved to Firestore")
            }
        } catch (e: Exception) {
            println("❌ Failed to save document info to Firestore: ${e.message}")
        }
    }
    
    private suspend fun savePhotoInfoToFirestore(photoInfo: PhotoInfo) {
        try {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                db.collection("lawyers")
                    .document(currentUser.uid)
                    .collection("photos")
                    .document(photoInfo.id)
                    .set(photoInfo)
                    .await()
                println("✅ Photo info saved to Firestore")
            }
        } catch (e: Exception) {
            println("❌ Failed to save photo info to Firestore: ${e.message}")
        }
    }
    
    fun deleteDocument(documentId: String) {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    // Find the document to get its Google Drive file ID
                    val documentToDelete = _uploadState.value.uploadedDocuments.find { it.id == documentId }
                    
                    // Delete from Google Drive if file ID exists
                    if (documentToDelete?.googleDriveFileId != null) {
                        println("🗑️ Deleting document from Google Drive: ${documentToDelete.googleDriveFileId}")
                        val driveDeleted = googleDriveService?.deleteFileFromDrive(documentToDelete.googleDriveFileId!!)
                        if (driveDeleted == true) {
                            println("✅ Document deleted from Google Drive")
                        } else {
                            println("⚠️ Failed to delete document from Google Drive")
                        }
                    }
                    
                    // Delete from Firestore
                    db.collection("lawyers")
                        .document(currentUser.uid)
                        .collection("documents")
                        .document(documentId)
                        .delete()
                        .await()
                    
                    // Update state
                    val currentDocuments = _uploadState.value.uploadedDocuments.toMutableList()
                    currentDocuments.removeAll { it.id == documentId }
                    _uploadState.value = _uploadState.value.copy(uploadedDocuments = currentDocuments)
                    
                    println("✅ Document deleted from Firestore")
                }
            } catch (e: Exception) {
                _uploadState.value = _uploadState.value.copy(
                    errorMessage = "Failed to delete document: ${e.message}"
                )
            }
        }
    }
    
    fun deletePhoto(photoId: String) {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    // Find the photo to get its Google Drive file ID
                    val photoToDelete = _uploadState.value.uploadedPhotos.find { it.id == photoId }
                    
                    // Delete from Google Drive if file ID exists
                    if (photoToDelete?.downloadUrl != null && photoToDelete.downloadUrl.isNotEmpty() && photoToDelete.downloadUrl != "photo_uploaded") {
                        println("🗑️ Deleting photo from Google Drive: ${photoToDelete.downloadUrl}")
                        val driveDeleted = googleDriveService?.deleteFileFromDrive(photoToDelete.downloadUrl)
                        if (driveDeleted == true) {
                            println("✅ Photo deleted from Google Drive")
                        } else {
                            println("⚠️ Failed to delete photo from Google Drive")
                        }
                    }
                    
                    // Delete from Firestore
                    db.collection("lawyers")
                        .document(currentUser.uid)
                        .collection("photos")
                        .document(photoId)
                        .delete()
                        .await()
                    
                    // Update state
                    val currentPhotos = _uploadState.value.uploadedPhotos.toMutableList()
                    currentPhotos.removeAll { it.id == photoId }
                    _uploadState.value = _uploadState.value.copy(uploadedPhotos = currentPhotos)
                    
                    println("✅ Photo deleted from Firestore")
                }
            } catch (e: Exception) {
                _uploadState.value = _uploadState.value.copy(
                    errorMessage = "Failed to delete photo: ${e.message}"
                )
            }
        }
    }
    
    fun loadExistingUploads() {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    println("🔍 Loading existing uploads for user: ${currentUser.uid}")
                    println("🔍 User email: ${currentUser.email}")
                    
                    // Load documents
                    val documentsSnapshot = db.collection("lawyers")
                        .document(currentUser.uid)
                        .collection("documents")
                        .get()
                        .await()
                    
                    val documents = documentsSnapshot.mapNotNull { doc ->
                        doc.toObject(DocumentInfo::class.java)
                    }
                    
                    // Load photos
                    val photosSnapshot = db.collection("lawyers")
                        .document(currentUser.uid)
                        .collection("photos")
                        .get()
                        .await()
                    
                    val photos = photosSnapshot.mapNotNull { doc ->
                        doc.toObject(PhotoInfo::class.java)
                    }
                    
                    _uploadState.value = _uploadState.value.copy(
                        uploadedDocuments = documents,
                        uploadedPhotos = photos
                    )
                    
                    println("✅ Loaded ${documents.size} documents and ${photos.size} photos")
                }
            } catch (e: Exception) {
                // Do not surface an error banner on initial load; log silently
                println("⚠️ Skipping existing uploads error: ${e.message}")
            }
        }
    }
    
    fun clearError() {
        _uploadState.value = _uploadState.value.copy(errorMessage = null)
    }
    
    fun setGoogleSignInError(errorMessage: String) {
        _uploadState.value = _uploadState.value.copy(errorMessage = errorMessage)
    }
    
    fun isGoogleDriveAvailable(): Boolean {
        return googleDriveService != null
    }
    
    /**
     * Trigger Excel verification and save results to Firestore for admin viewing
     */
    suspend fun verifyAndSaveToFirestore(lawyerName: String, enrollmentNumber: String?) {
        try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Log.e("DocumentUploadViewModel", "❌ User not authenticated for verification")
                return
            }
            
            val service = excelVerificationService
            if (service == null) {
                Log.e("DocumentUploadViewModel", "❌ Excel verification service not initialized")
                return
            }
            
            Log.d("DocumentUploadViewModel", "🔍 Starting Excel verification for: $lawyerName (Enrollment: $enrollmentNumber)")
            
            // Run verification
            val result = service.verifyLawyer(lawyerName, enrollmentNumber)
            
            // Save result to Firestore for admin viewing
            service.saveVerificationResultToFirestore(currentUser.uid, result)
            
            Log.d("DocumentUploadViewModel", "✅ Excel verification completed and saved to Firestore")
            Log.d("DocumentUploadViewModel", "📊 Result: ${result.isVerified} (${(result.confidence * 100).toInt()}% confidence)")
            
        } catch (e: Exception) {
            Log.e("DocumentUploadViewModel", "❌ Error during Excel verification: ${e.message}", e)
        }
    }
    
    suspend fun downloadFileAsBitmap(fileId: String): Bitmap? {
        return try {
            println("📥 Downloading file from Google Drive: $fileId")
            val inputStream = googleDriveService?.downloadFileFromDrive(fileId)
            if (inputStream != null) {
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                println("✅ File downloaded successfully")
                bitmap
            } else {
                println("❌ Failed to download file from Google Drive")
                null
            }
        } catch (e: Exception) {
            println("❌ Error downloading file: ${e.message}")
            null
        }
    }
}

data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String?
) 