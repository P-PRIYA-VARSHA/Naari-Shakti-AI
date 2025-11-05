package com.example.shaktibotprobono

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class GoogleDriveService(private val context: Context) {

    companion object {
        private const val TAG = "GoogleDriveService"
        private const val ADMIN_EMAIL = "shaktibot.naariai@gmail.com"
        private const val CLIENT_ID = "192438304129-om3bti3n5vk5idpf0lrmbcveh0bs303i.apps.googleusercontent.com"
    }

    private var driveService: Drive? = null
    private var currentAccount: GoogleSignInAccount? = null

    fun setGoogleAccount(account: GoogleSignInAccount?) {
        currentAccount = account
        driveService = null
        if (account != null) {
            Log.d(TAG, "✅ Google account set: ${account.email}")
        } else {
            Log.d(TAG, "❌ Google account cleared")
        }
    }

    suspend fun uploadDocumentToAdminDrive(
        fileUri: Uri,
        fileName: String,
        lawyerId: String,
        lawyerName: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🚀 Starting Google Drive upload for lawyer: $lawyerName")

            val account = currentAccount ?: run {
                Log.e(TAG, "❌ No Google account signed in")
                return@withContext null
            }

            val drive = getDriveService(account)

            val documentsFolderId = createLawyerDocumentsFolder(drive, lawyerId, lawyerName)
            val fileId = uploadFileToFolder(drive, fileUri, fileName, documentsFolderId)
            Log.d(TAG, "✅ Document uploaded to admin Google Drive: $fileId")
            return@withContext fileId
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error uploading to Google Drive: ${e.message}")
            e.printStackTrace()
            return@withContext null
        }
    }

    suspend fun uploadPhotoToAdminDrive(
        fileUri: Uri,
        fileName: String,
        lawyerId: String,
        lawyerName: String,
        photoType: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🚀 Starting Google Drive photo upload for lawyer: $lawyerName, Type: $photoType")

            val account = currentAccount ?: run {
                Log.e(TAG, "❌ No Google account signed in")
                return@withContext null
            }

            val drive = getDriveService(account)
            val photosFolderId = createLawyerPhotosFolder(drive, lawyerId, lawyerName)
            val fileId = uploadFileToFolder(drive, fileUri, fileName, photosFolderId)
            Log.d(TAG, "✅ Photo uploaded to admin Google Drive: $fileId")
            return@withContext fileId
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error uploading photo to Google Drive: ${e.message}")
            e.printStackTrace()
            return@withContext null
        }
    }

    private fun getDriveService(account: GoogleSignInAccount): Drive {
        if (driveService == null) {
            Log.d(TAG, "🔧 Initializing Google Drive service with GoogleAccountCredential...")
            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                listOf(DriveScopes.DRIVE_FILE)
            ).apply {
                selectedAccount = account.account
                if (selectedAccount == null && account.email != null) {
                    selectedAccountName = account.email
                }
            }

            driveService = Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            )
                .setApplicationName("ShaktiBot Pro Bono")
                .build()

            Log.d(TAG, "✅ Google Drive service initialized with GoogleAccountCredential")
            Log.d(TAG, "👤 User: ${account.email}")
        }
        return driveService!!
    }

    private suspend fun createLawyerDocumentsFolder(
        driveService: Drive,
        lawyerId: String,
        lawyerName: String
    ): String = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📁 Creating documents folder structure for lawyer: $lawyerName")
            val mainFolderId = getOrCreateFolder(driveService, "Lawyer Documents")
            val lawyerFolderName = "$lawyerName - $lawyerId"
            val lawyerFolderId = getOrCreateFolder(driveService, lawyerFolderName, mainFolderId)
            val documentsFolderId = getOrCreateFolder(driveService, "Documents", lawyerFolderId)
            Log.d(TAG, "✅ Created lawyer documents folder structure: Lawyer Documents/$lawyerFolderName/Documents")
            return@withContext documentsFolderId
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creating documents folder: ${e.message}")
            throw e
        }
    }

    private suspend fun createLawyerPhotosFolder(
        driveService: Drive,
        lawyerId: String,
        lawyerName: String
    ): String = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📁 Creating photos folder structure for lawyer: $lawyerName")
            val mainFolderId = getOrCreateFolder(driveService, "Lawyer Documents")
            val lawyerFolderName = "$lawyerName - $lawyerId"
            val lawyerFolderId = getOrCreateFolder(driveService, lawyerFolderName, mainFolderId)
            val photosFolderId = getOrCreateFolder(driveService, "Photos", lawyerFolderId)
            Log.d(TAG, "✅ Created lawyer photos folder structure: Lawyer Documents/$lawyerFolderName/Photos")
            return@withContext photosFolderId
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creating photos folder: ${e.message}")
            throw e
        }
    }

    private suspend fun getOrCreateFolder(
        driveService: Drive,
        folderName: String,
        parentId: String? = null
    ): String = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔍 Checking if folder exists: $folderName")
            var query = "name='$folderName' and mimeType='application/vnd.google-apps.folder'"
            if (parentId != null) {
                query += " and '$parentId' in parents"
            }

            val result = driveService.files().list()
                .setQ(query)
                .setSpaces("drive")
                .execute()

            if (result.files.isNotEmpty()) {
                Log.d(TAG, "✅ Folder already exists: $folderName")
                return@withContext result.files[0].id
            }

            Log.d(TAG, "📁 Creating new folder: $folderName")
            val folderMetadata = com.google.api.services.drive.model.File().apply {
                name = folderName
                mimeType = "application/vnd.google-apps.folder"
                if (parentId != null) {
                    parents = listOf(parentId)
                }
            }

            val folder = driveService.files().create(folderMetadata)
                .setFields("id")
                .execute()

            Log.d(TAG, "✅ Created folder: $folderName with ID: ${folder.id}")
            return@withContext folder.id
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting/creating folder: ${e.message}")
            throw e
        }
    }

    private suspend fun uploadFileToFolder(
        driveService: Drive,
        fileUri: Uri,
        fileName: String,
        folderId: String
    ): String = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📤 Uploading file: $fileName")
            val tempFile = createTempFileFromUri(fileUri, fileName)
            Log.d(TAG, "📄 Created temp file: ${tempFile.absolutePath}")

            val fileMetadata = com.google.api.services.drive.model.File().apply {
                name = fileName
                parents = listOf(folderId)
            }

            val mediaContent = FileContent("application/octet-stream", tempFile)
            val file = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id")
                .execute()

            tempFile.delete()

            Log.d(TAG, "✅ File uploaded successfully: ${file.id}")
            return@withContext file.id
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error uploading file: ${e.message}")
            throw e
        }
    }

    private fun createTempFileFromUri(uri: Uri, fileName: String): File {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val tempFile = File.createTempFile("upload_", "_$fileName")
            val outputStream = FileOutputStream(tempFile)

            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            Log.d(TAG, "✅ Created temp file: ${tempFile.absolutePath}")
            return tempFile
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creating temp file: ${e.message}")
            throw e
        }
    }

    suspend fun deleteFileFromDrive(fileId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🗑️ Deleting file from Google Drive: $fileId")

            val account = currentAccount ?: return@withContext false
            val drive = getDriveService(account)
            drive.files().delete(fileId).execute()
            Log.d(TAG, "✅ File deleted from Google Drive: $fileId")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error deleting file from Google Drive: ${e.message}")
            return@withContext false
        }
    }

    suspend fun downloadFileFromDrive(fileId: String): java.io.InputStream? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📥 Downloading file from Google Drive: $fileId")

            val account = currentAccount ?: return@withContext null
            val drive = getDriveService(account)
            val response = drive.files().get(fileId).executeMediaAsInputStream()
            Log.d(TAG, "✅ File downloaded from Google Drive: $fileId")
            return@withContext response
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error downloading file from Google Drive: ${e.message}")
            return@withContext null
        }
    }
}