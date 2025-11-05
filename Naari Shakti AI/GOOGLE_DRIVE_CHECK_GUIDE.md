# 🔍 Google Drive Video Storage Check Guide

## 📁 **How to Check if Videos are Stored in Google Drive**

### **Method 1: Check Google Drive Directly (Recommended)**

1. **Open Google Drive**
   - Go to [drive.google.com](https://drive.google.com)
   - Sign in with the **trusted contact's email** (the email you entered in the app)

2. **Look for Evidence Folder**
   - Search for folder named: `SOS_Evidence_[user_email]`
   - Example: `SOS_Evidence_user_at_example.com`
   - The folder name replaces `@` with `_at_`

3. **Check Folder Contents**
   - Open the evidence folder
   - Look for video files with names like:
     - `sos_video_front_1234567890_back_1234567890.mp4`
     - `sos_video_back_1234567890_front_1234567890.mp4`

### **Method 2: Check App Logs**

1. **Enable Developer Options**
   - Go to Settings → About Phone
   - Tap "Build Number" 7 times
   - Go back to Settings → Developer Options
   - Enable "USB Debugging"

2. **Check Logs via ADB**
   ```bash
   adb logcat | grep "SOSService"
   ```

3. **Look for Upload Logs**
   - `🚀 Starting video upload to Google Drive...`
   - `📤 Uploading to: https://us-central1-she-56fea.cloudfunctions.net/uploadEvidenceVideo`
   - `✅ Video upload simulation completed`
   - `📁 Check Google Drive for folder: SOS_Evidence_[user_email]`

### **Method 3: Check App Notifications**

1. **Look for Upload Notifications**
   - When SOS is activated, check for notifications:
     - ✅ "Video Uploaded Successfully" (Green notification)
     - ❌ "Video Upload Failed" (Red notification)

2. **Notification Details**
   - Success: "Evidence video uploaded to Google Drive: [filename]"
   - Failed: "Failed to upload evidence video: [filename]"

### **Method 4: Check Firebase Functions Logs**

1. **Access Firebase Console**
   - Go to [Firebase Console](https://console.firebase.google.com)
   - Select your project: `she-56fea`

2. **Check Functions Logs**
   - Go to Functions → Logs
   - Look for `uploadEvidenceVideo` function calls
   - Check for success/error messages

### **Method 5: Test Upload Functionality**

1. **Activate SOS**
   - Open the SecureShe app
   - Go to Dashboard
   - Tap the SOS button
   - Enter your PIN to confirm

2. **Check Video Recording**
   - Videos should be recorded for 30 seconds
   - Check local storage: `Android/data/com.example.secureshe/files/sos_videos/`

3. **Monitor Upload Process**
   - Watch for notifications
   - Check app logs
   - Verify in Google Drive

## 🔧 **Troubleshooting**

### **If Videos Aren't Uploading:**

1. **Check Internet Connection**
   - Ensure device has stable internet
   - Try on WiFi and mobile data

2. **Verify Google Drive Setup**
   - Ensure trusted contact completed setup
   - Check if "Connected" status shows in app

3. **Check Firebase Functions**
   - Verify functions are deployed
   - Check for any errors in Firebase console

4. **Review App Permissions**
   - Camera permission granted
   - Storage permission granted
   - Internet permission granted

### **If Videos Are Uploading But Not Visible:**

1. **Check Correct Google Account**
   - Ensure you're checking the trusted contact's Google Drive
   - Not the user's Google Drive

2. **Search for Folder**
   - Use Google Drive search: `SOS_Evidence`
   - Check "Shared with me" section

3. **Check Folder Permissions**
   - Ensure folder is not hidden
   - Check if folder is in trash

## 📊 **Expected Folder Structure**

```
Google Drive (Trusted Contact's Account)
└── SOS_Evidence_user_at_example.com/
    ├── sos_video_front_1234567890_back_1234567890.mp4
    ├── sos_video_back_1234567890_front_1234567890.mp4
    ├── sos_video_front_1234567891_back_1234567891.mp4
    └── ... (more evidence videos)
```

## 🎯 **Quick Check Steps**

1. **Activate SOS** → Record videos
2. **Check notifications** → Look for upload status
3. **Open Google Drive** → Search for `SOS_Evidence` folder
4. **Verify videos** → Check folder contents
5. **Review logs** → Check for any errors

## 📞 **Support**

If videos are not uploading:
1. Check app logs for errors
2. Verify Google Drive setup is complete
3. Ensure internet connection is stable
4. Contact support with error logs
