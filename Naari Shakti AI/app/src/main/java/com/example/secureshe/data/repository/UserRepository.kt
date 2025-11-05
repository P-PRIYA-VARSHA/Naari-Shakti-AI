package com.example.secureshe.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.example.secureshe.utils.EncryptionHelper
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val pin: String = "",
    val trustedContactEmail: String = ""
) {
    // Required no-argument constructor for Firestore
    constructor() : this("", "", "", "", "", "")
}

@Singleton
class UserRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val encryptionHelper: EncryptionHelper
) {
    private val usersCollection = firestore.collection("users")

    suspend fun saveUserProfile(userProfile: UserProfile): Result<Unit> {
        return try {
            android.util.Log.d("UserRepository", "Saving user profile to Firestore: ${userProfile.uid}")
            
            // Encrypt the PIN before storing
            val encryptedPin = encryptionHelper.encryptPin(userProfile.pin)
            val profileWithEncryptedPin = userProfile.copy(pin = encryptedPin)
            
            usersCollection.document(userProfile.uid).set(profileWithEncryptedPin).await()
            android.util.Log.d("UserRepository", "User profile saved successfully with encrypted PIN")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Error saving user profile: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getUserProfile(uid: String): Result<UserProfile?> {
        return try {
            android.util.Log.d("UserRepository", "Fetching user profile from Firestore: $uid")
            val document = usersCollection.document(uid).get().await()
            val userProfile = if (document.exists()) {
                val profile = document.toObject(UserProfile::class.java)
                // Decrypt the PIN when retrieving
                profile?.let { 
                    val decryptedPin = encryptionHelper.decryptPin() ?: it.pin
                    it.copy(pin = decryptedPin)
                }
            } else {
                null
            }
            android.util.Log.d("UserRepository", "User profile fetched with decrypted PIN")
            Result.success(userProfile)
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Error fetching user profile: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun updateUserPin(uid: String, newPin: String): Result<Unit> {
        return try {
            android.util.Log.d("UserRepository", "Updating user PIN in Firestore: $uid")
            
            // Encrypt the new PIN before storing
            val encryptedPin = encryptionHelper.encryptPin(newPin)
            
            val updates = mapOf("pin" to encryptedPin)
            usersCollection.document(uid).update(updates).await()
            
            // Also update local encrypted storage
            encryptionHelper.storeEncryptedPin(newPin)
            
            android.util.Log.d("UserRepository", "User PIN updated successfully with encryption")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Error updating user PIN: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(uid: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            android.util.Log.d("UserRepository", "Updating user profile in Firestore: $uid")
            usersCollection.document(uid).update(updates).await()
            android.util.Log.d("UserRepository", "User profile updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Error updating user profile: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun updateTrustedContactEmail(uid: String, trustedContactEmail: String): Result<Unit> {
        return try {
            android.util.Log.d("UserRepository", "Updating trusted contact email in Firestore: $uid -> $trustedContactEmail")
            val updates = mapOf("trustedContactEmail" to trustedContactEmail)
            usersCollection.document(uid).update(updates).await()
            android.util.Log.d("UserRepository", "Trusted contact email updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Error updating trusted contact email: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun deleteUserProfile(uid: String): Result<Unit> {
        return try {
            android.util.Log.d("UserRepository", "Deleting user profile from Firestore: $uid")
            usersCollection.document(uid).delete().await()
            android.util.Log.d("UserRepository", "User profile deleted successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Error deleting user profile: ${e.message}")
            Result.failure(e)
        }
    }
}