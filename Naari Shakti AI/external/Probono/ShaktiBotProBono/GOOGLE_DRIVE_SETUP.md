# 🔧 Google Drive Setup Guide for ShaktiBot Pro Bono

## 🎯 **Overview**
This guide will help you set up Google Drive integration so that documents uploaded by lawyers are automatically stored in the admin's Google Drive account (`shaktibot.naariai@gmail.com`).

## 📋 **Prerequisites**
- Google account: `shaktibot.naariai@gmail.com`
- Android Studio project access
- Basic understanding of Google Cloud Console

## 🔧 **Step-by-Step Setup**

### **Step 1: Google Cloud Console Setup**

1. **Go to [Google Cloud Console](https://console.cloud.google.com/)**
2. **Sign in with**: `shaktibot.naariai@gmail.com`
3. **Create a new project** or select existing project:
   - Click "Select a project" → "New Project"
   - Name: "ShaktiBot Pro Bono"
   - Click "Create"

### **Step 2: Enable Required APIs**

1. **Go to "APIs & Services" → "Library"**
2. **Search and enable these APIs:**
   - **Google Drive API** - Click "Enable" ✅
   - **Google Identity Services API** (if available) - Click "Enable" ✅
   - **Google+ API** (if available) - Click "Enable" ✅

**Note:** "Google Sign-In API" doesn't exist as a separate API. Google Sign-In is handled through Google Identity Services and the Google Drive API.

### **Step 3: Configure OAuth Consent Screen**

1. **Go to "APIs & Services" → "OAuth consent screen"**
2. **Choose "External"** (unless you have Google Workspace)
3. **Fill in required information:**
   - App name: "ShaktiBot Pro Bono"
   - User support email: `shaktibot.naariai@gmail.com`
   - Developer contact information: `shaktibot.naariai@gmail.com`
   - App logo: Upload a logo (optional)
4. **Add scopes:**
   - Click "Add or Remove Scopes"
   - Add these scopes:
     - `https://www.googleapis.com/auth/drive.file`
     - `https://www.googleapis.com/auth/userinfo.email`
     - `https://www.googleapis.com/auth/userinfo.profile`
5. **Add test users:**
   - Add `shaktibot.naariai@gmail.com` as a test user
6. **Save and Continue**

### **Step 4: Create OAuth 2.0 Credentials**

1. **Go to "APIs & Services" → "Credentials"**
2. **Click "Create Credentials" → "OAuth 2.0 Client IDs"**
3. **Choose "Android"** as application type
4. **Fill in details:**
   - Package name: `com.example.shaktibotprobono`
   - SHA-1 certificate fingerprint: `8D:27:FD:B8:EA:C5:FD:C4:BE:3A:5F:AA:F0:99:BC:3F:45:C6:FC:66`
5. **Click "Create"**
6. **Copy the Client ID** (you'll need this)

### **Step 5: Update App Configuration**

1. **Open `GoogleSignInHelper.kt`**
2. **Replace the Client ID** in line 22:
   ```kotlin
   val clientId = "YOUR_NEW_CLIENT_ID_HERE"
   ```

### **Step 6: Google Drive Setup**

1. **Sign in to Google Drive** with `shaktibot.naariai@gmail.com`
2. **Create a folder** called "Lawyer Documents" (if it doesn't exist)
3. **Set folder permissions** to allow the app to upload files

## 🚀 **Testing the Setup**

### **Test Google Sign-In:**
1. **Build and run** the app
2. **Navigate to Document Upload screen**
3. **Click "Sign in with Google"**
4. **Sign in with**: `shaktibot.naariai@gmail.com`
5. **Grant Drive access** when prompted
6. **Verify sign-in success** in logs

### **Test Document Upload:**
1. **Upload a test document**
2. **Check Google Drive** for the uploaded file
3. **Verify folder structure**: `Lawyer Documents/[Lawyer Name - ID]/Documents/`

## 🔍 **Troubleshooting**

### **Common Issues:**

1. **"Sign-in failed"**
   - ✅ Check OAuth consent screen is configured
   - ✅ Verify Client ID is correct
   - ✅ Ensure test user is added
   - ✅ Check SHA-1 fingerprint matches

2. **"Upload failed"**
   - ✅ Check internet connection
   - ✅ Verify Google Drive API is enabled
   - ✅ Check file size limits (5MB max)
   - ✅ Ensure user granted Drive access

3. **"Permission denied"**
   - ✅ User needs to grant Drive access during sign-in
   - ✅ Check OAuth scopes are correct
   - ✅ Verify admin account has sufficient storage

### **Debug Steps:**
1. **Check Logcat** for detailed error messages
2. **Verify Google Sign-In** is working
3. **Test with small files** first
4. **Check Google Cloud Console** for API usage

## 📁 **File Structure in Google Drive**

```
Google Drive/
└── Lawyer Documents/
    └── [Lawyer Name - ID]/
        ├── Documents/
        │   ├── bar_council_certificate.pdf
        │   └── other_documents.pdf
        └── Photos/
            ├── profile_photo.jpg
            └── verification_photo.jpg
```

## 🔒 **Security Features**

- **OAuth 2.0 Authentication** - Secure user authentication
- **Limited Scope** - Only `drive.file` access (can't see all files)
- **User Control** - Users can revoke access anytime
- **Admin Access** - Files stored in admin's Google Drive

## 📞 **Support**

For issues or questions:
1. Check this setup guide
2. Review Google Cloud Console logs
3. Check Android Logcat for errors
4. Verify OAuth configuration

---

**Note**: This setup uploads documents to the admin's Google Drive account (`shaktibot.naariai@gmail.com`). Make sure the admin account has sufficient storage space. 