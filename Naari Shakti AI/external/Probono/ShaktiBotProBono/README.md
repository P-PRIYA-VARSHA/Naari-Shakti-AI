# ShaktiBot Pro Bono - Google Drive Integration

## 🎯 **Overview**

This app uses **Google Sign-In (OAuth 2.0)** for secure document upload to Google Drive. Documents are uploaded to the admin's Google Drive account (`shaktibot.naariai@gmail.com`), providing:

- ✅ **Admin-Controlled Storage** - All files go to admin's Google Drive
- ✅ **Secure** - Uses OAuth 2.0 authentication
- ✅ **Free** - No additional storage costs
- ✅ **Organized** - Automatic folder structure
- ✅ **Easy Access** - Admin can access through Google Drive

## 🔧 **Setup Instructions**

### **Step 1: Google Cloud Console Setup**

1. **Go to [Google Cloud Console](https://console.cloud.google.com/)**
2. **Create a new project** or select existing project
3. **Enable Google Drive API**:
   - Go to "APIs & Services" → "Library"
   - Search for "Google Drive API"
   - Click "Enable"

### **Step 2: Configure OAuth Consent Screen**

1. **Go to "APIs & Services" → "OAuth consent screen"**
2. **Choose "External"** (unless you have Google Workspace)
3. **Fill in required information**:
   - App name: "ShaktiBot Pro Bono"
   - User support email: Your email
   - Developer contact information: Your email
4. **Add scopes**:
   - Click "Add or Remove Scopes"
   - Add: `https://www.googleapis.com/auth/drive.file`
5. **Add test users** (your Google account email)
6. **Save and Continue**

### **Step 3: Create OAuth 2.0 Credentials**

1. **Go to "APIs & Services" → "Credentials"**
2. **Click "Create Credentials" → "OAuth 2.0 Client IDs"**
3. **Choose "Android"** as application type
4. **Fill in details**:
   - Package name: `com.example.shaktibotprobono`
   - SHA-1 certificate fingerprint: Your app's SHA-1
5. **Click "Create"**
6. **Copy the Client ID** (you'll need this)

### **Step 4: Update App Configuration**

1. **Open `GoogleSignInHelper.kt`**
2. **Replace the Client ID** in line 22:
   ```kotlin
   .requestIdToken("YOUR_CLIENT_ID_HERE")
   ```

### **Step 5: Get SHA-1 Certificate Fingerprint**

Run this command in your project directory:
```bash
./gradlew signingReport
```

Look for the SHA-1 value in the debug variant and add it to your OAuth credentials.

### **Step 6: Build and Test**

1. **Sync project** in Android Studio
2. **Build and run** the app
3. **Test upload** functionality

## 🚀 **How It Works**

### **User Flow:**
1. **User opens Document Upload screen**
2. **Clicks "Sign in with Google"**
3. **Google Sign-In popup appears**
4. **User authenticates with their Google account**
5. **App gets access to user's Google Drive**
6. **Documents are uploaded to admin's Drive** (`shaktibot.naariai@gmail.com`)

### **File Structure:**
```
Google Drive/
└── Lawyer Documents/
    └── [Lawyer Name - ID]/
        ├── Documents/
        │   └── [Uploaded files]
        └── Photos/
            └── [Uploaded photos]
```

## 🔒 **Security Features**

- **OAuth 2.0 Authentication** - Secure user authentication
- **Limited Scope** - Only `drive.file` access (can't see all files)
- **User Control** - Users can revoke access anytime
- **Admin Access** - Files stored in admin's Google Drive

## 📱 **App Features**

### **Document Upload:**
- ✅ Bar Council Certificate upload
- ✅ Profile photo upload
- ✅ Verification photo upload
- ✅ Automatic Google Drive backup
- ✅ File validation (size, type)

### **Google Drive Integration:**
- ✅ Automatic folder creation
- ✅ Organized file structure
- ✅ File metadata storage in Firestore
- ✅ Secure file access
- ✅ Admin console access

## 🛠 **Troubleshooting**

### **Common Issues:**

1. **"Sign-in failed"**
   - Check OAuth consent screen is configured
   - Verify Client ID is correct
   - Ensure test user is added

2. **"Upload failed"**
   - Check internet connection
   - Verify Google Drive API is enabled
   - Check file size limits

3. **"Permission denied"**
   - User needs to grant Drive access
   - Check OAuth scopes are correct

### **Debug Steps:**
1. **Check Logcat** for detailed error messages
2. **Verify Google Sign-In** is working
3. **Test with small files** first
4. **Check Google Cloud Console** for API usage

## 📋 **File Limits**

- **Maximum file size**: 5MB
- **Supported formats**: PDF, JPG, JPEG, PNG
- **Storage**: Uses admin's Google Drive quota

## 👨‍💼 **Admin Access**

### **Google Drive:**
1. **Go to [Google Drive](https://drive.google.com/)**
2. **Sign in with**: `shaktibot.naariai@gmail.com`
3. **Navigate to "Lawyer Documents"** folder
4. **Browse files** by lawyer ID
5. **Download, delete, or manage** files

### **File Management:**
- **View all uploaded files**
- **Download documents** for review
- **Delete files** if needed
- **Monitor storage usage**
- **Set up access logs**

## 🔄 **Migration from Previous Setup**

If you were previously using Firebase Storage:
1. **Deleted files**: `FirebaseStorageService.kt`
2. **Updated files**: `DocumentUploadViewModel.kt`, `DocumentUploadScreen.kt`
3. **New files**: `GoogleDriveService.kt`, `GoogleSignInHelper.kt`
4. **Added dependencies**: Google Drive API, Google Sign-In

## 📞 **Support**

For issues or questions:
1. Check this README
2. Review Google Cloud Console logs
3. Check Android Logcat for errors
4. Verify OAuth configuration

---

**Note**: This approach uploads documents to the admin's Google Drive account (`shaktibot.naariai@gmail.com`). Make sure the admin account has sufficient storage space. 