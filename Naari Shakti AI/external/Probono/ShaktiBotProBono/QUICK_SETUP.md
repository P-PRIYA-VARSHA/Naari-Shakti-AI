# 🚀 Quick Setup Guide - Google Drive Integration

## 📋 **Files Modified:**
1. `GoogleSignInHelper.kt` - Updated with Drive API scopes
2. `GoogleDriveService.kt` - Fixed authentication flow
3. `DocumentUploadScreen.kt` - Added configuration testing
4. `GOOGLE_DRIVE_SETUP.md` - Comprehensive setup guide

## 🔧 **Google Cloud Console Steps:**

### **1. Create Project**
- Go to [Google Cloud Console](https://console.cloud.google.com/)
- Sign in with: `shaktibot.naariai@gmail.com`
- Create new project: "ShaktiBot Pro Bono"

### **2. Enable APIs**
- Go to "APIs & Services" → "Library"
- Enable: Google Drive API, Google Identity Services API (if available)

### **3. Configure OAuth Consent Screen**
- Go to "APIs & Services" → "OAuth consent screen"
- Choose "External"
- App name: "ShaktiBot Pro Bono"
- Add scopes: `drive.file`, `userinfo.email`, `userinfo.profile`
- Add test user: `shaktibot.naariai@gmail.com`

### **4. Create OAuth Credentials**
- Go to "APIs & Services" → "Credentials"
- Create "OAuth 2.0 Client IDs" → "Android"
- Package: `com.example.shaktibotprobono`
- SHA-1: `8D:27:FD:B8:EA:C5:FD:C4:BE:3A:5F:AA:F0:99:BC:3F:45:C6:FC:66`

### **5. Update App**
- Copy new Client ID from Google Cloud Console
- Replace in `GoogleSignInHelper.kt` line 22

## 🧪 **Testing:**

1. **Build and run** the app
2. **Navigate to Document Upload**
3. **Click "Sign in with Google"**
4. **Sign in with**: `shaktibot.naariai@gmail.com`
5. **Grant Drive access**
6. **Upload test document**
7. **Check Google Drive** for uploaded file

## 🔍 **Troubleshooting:**

- **Check Logcat** for detailed logs
- **Verify SHA-1** fingerprint matches
- **Ensure test user** is added to OAuth consent screen
- **Check Drive API** is enabled

## 📁 **Expected File Structure:**
```
Google Drive/
└── Lawyer Documents/
    └── [Lawyer Name - ID]/
        ├── Documents/
        └── Photos/
```

## ✅ **Success Indicators:**
- Google Sign-In works without errors
- Documents upload to Google Drive
- Proper folder structure is created
- No authentication errors in logs 