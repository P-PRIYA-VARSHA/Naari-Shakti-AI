# SMS Emergency Feature - SecureShe App

## Overview
The SMS emergency feature in SecureShe ensures that emergency contacts receive immediate notifications with GPS location information when the SOS button is pressed, even when mobile data is turned off.

## Key Features

### 1. **Immediate SMS Notification**
- Sends instant SMS to all emergency contacts when SOS is activated
- Includes device information and timestamp
- Works independently of mobile data connection

### 2. **GPS Location SMS**
- Sends detailed location information including:
  - Google Maps link for easy navigation
  - Precise GPS coordinates (latitude/longitude)
  - Location accuracy and speed information
  - Device manufacturer and model
  - Timestamp

### 3. **Redundant SMS System**
- **Primary SMS**: Detailed emergency message with full location info
- **Coordinates SMS**: Separate SMS with just GPS coordinates and maps link
- **Backup SMS**: Minimal "SOS! Help!" message if main SMS fails

### 4. **Periodic Location Updates**
- Sends location updates every 2 minutes while SOS is active
- Keeps emergency contacts informed of movement
- Includes update counter for tracking

### 5. **Final Status SMS**
- Sends confirmation when SOS is deactivated
- Informs contacts that emergency situation is resolved

## Technical Implementation

### Permissions Required
```xml
<uses-permission android:name="android.permission.SEND_SMS" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

### SMS Message Format

#### Immediate Emergency SMS:
```
🚨 EMERGENCY SOS 🚨
I need immediate help!
⏰ SOS activated at: [timestamp]
📱 Device: [manufacturer] [model]
📍 GPS location will be sent shortly.
📞 I will also try to call you.
🆘 Please respond immediately!
⚠️ If no response, please call emergency services.
```

#### Location SMS:
```
🚨 EMERGENCY SOS 🚨
I need immediate help!
📍 Location:
• Google Maps: https://maps.google.com/?q=[lat],[lng]
• Coordinates: [latitude], [longitude]
• Accuracy: ±[accuracy]m
• Speed: [speed] km/h
⏰ Time: [timestamp]
📱 Device: [manufacturer] [model]
🆘 Please respond immediately!
📞 Call me back if possible.
```

#### Coordinates SMS (Redundant):
```
📍 GPS: [latitude], [longitude]
🗺️ Maps: https://maps.google.com/?q=[lat],[lng]
```

### Error Handling

#### SMS Availability Check
- Checks SIM card state before sending SMS
- Shows notifications for:
  - No SIM card detected
  - SIM PIN required
  - SIM PUK required
  - Network locked SIM

#### SMS Failure Recovery
- Attempts to send backup SMS if main SMS fails
- Logs all SMS success/failure events
- Shows user notifications for SMS status

## Testing Features

### SMS Test Function
```kotlin
// Test SMS functionality without emergency calls
sosViewModel.testSMSFunctionality(context)
```

### Test Messages
- **Test SMS**: "🧪 SMS Test Message" with device info
- **Test Location SMS**: "🧪 Location Test" with GPS coordinates
- **Test Mode**: Brief 10-second SOS test with auto-stop

## Usage Instructions

### For Users:
1. **Add Emergency Contacts**: Go to Emergency Contacts screen and add up to 3 contacts
2. **Grant Permissions**: Ensure SMS and Location permissions are granted
3. **Press SOS Button**: Tap the red SOS button on the dashboard
4. **Confirm Emergency**: Confirm the emergency action in the dialog
5. **Monitor Status**: Check notifications for SMS status updates

### For Emergency Contacts:
1. **Receive SMS**: You'll receive immediate SMS with emergency alert
2. **Check Location**: Click the Google Maps link to see exact location
3. **Respond**: Call back or respond to the emergency contact
4. **Monitor Updates**: Receive periodic location updates every 2 minutes

## Network Independence

### Why SMS Works Without Mobile Data:
- SMS uses the cellular network's signaling system
- Works on 2G/3G/4G/5G networks without internet
- Independent of mobile data connection
- Reliable in areas with poor internet coverage

### Fallback Mechanisms:
1. **Primary SMS**: Full emergency message
2. **Coordinates SMS**: GPS coordinates only
3. **Backup SMS**: Minimal "SOS! Help!" message
4. **Emergency Calls**: Voice calls as backup communication

## Security Features

### PIN Protection
- SOS can only be stopped with correct PIN
- Prevents unauthorized deactivation
- PIN verification required for emergency stop

### Privacy
- Only sends location to pre-configured emergency contacts
- No location data stored on servers
- All communication is direct SMS/calls

## Troubleshooting

### Common Issues:
1. **SMS Not Sending**: Check SIM card and SMS permissions
2. **Location Not Accurate**: Ensure GPS is enabled
3. **Contacts Not Receiving**: Verify phone numbers are correct
4. **App Crashes**: Check all required permissions are granted

### Debug Features:
- Detailed logging for all SMS operations
- Permission status checking
- SIM card state verification
- SMS success/failure notifications

## Future Enhancements

### Planned Features:
1. **SMS Delivery Reports**: Confirm when SMS is delivered
2. **Custom SMS Templates**: Allow users to customize messages
3. **Emergency Services Integration**: Direct SMS to emergency numbers
4. **Offline Mode**: Enhanced functionality without internet
5. **SMS History**: Log of sent emergency messages

---

**Note**: This SMS feature is designed to work reliably in emergency situations, providing immediate communication to emergency contacts regardless of internet connectivity. 