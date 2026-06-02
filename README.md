# Device Admin Manager

An Android application demonstrating Enterprise Device Administration features using the `DevicePolicyManager` API. This project showcases how to implement security policies, monitor device events, and manage device restrictions.

## 🚀 Features

### 🛡️ Device Administration
*   **Activation/Deactivation**: One-tap activation of Device Admin privileges.
*   **Force Lock**: Immediately lock the device programmatically.
*   **Wipe Data**: Perform a factory reset (Enterprise reset).
*   **Camera Control**: Remotely enable or disable the device camera.

### 🔐 Security Policies
*   **Password Quality**: Enforce alphanumeric password requirements.
*   **Password Length**: Set a minimum password length (e.g., 6 characters).
*   **Auto-Lock Timeout**: Configure the maximum time allowed before the device automatically locks (demo set to 30 seconds).

### 📊 Monitoring & Logging
*   **Login Monitoring**: Detects and logs successful and failed unlock attempts.
*   **Persistent Logs**: Logs are saved to `SharedPreferences`, ensuring they survive when the app is in the background or the device is locked.
*   **Real-time Dashboard**: A Material 3 based UI with a terminal-like log viewer.
*   **Color-coded Status**: 
    *   <span style="color:green">SUCCESS</span>: Operations and successful logins.
    *   <span style="color:red">ERROR</span>: Failures and failed unlock attempts.
    *   <span style="color:blue">INFO</span>: System status updates.
*   **Export Capabilities**: Copy logs to clipboard or share them via the system share sheet.

## 🛠️ Tech Stack
*   **Language**: Java
*   **UI Framework**: Material Design 3 (M3)
*   **Core API**: `DevicePolicyManager`, `DeviceAdminReceiver`
*   **Architecture**: Shared Preferences for persistent logging and BroadcastReceivers for real-time UI updates.

## 📋 Prerequisites
*   Android 10 (API level 29) or higher (Targeting API 36).
*   Device Admin privileges must be granted manually through the app's UI.

## 🔧 Installation & Setup
1. Clone the repository.
2. Open in Android Studio.
3. Build and run on a physical device (recommended for Device Admin features).
4. Click **"Enable Device Admin Features"** on the main screen.
5. Click **"Activate"** to grant administrative rights.

## 💻 ADB Commands (Device Owner)

Device Owner is the highest level of privilege. To set the app as the Device Owner, you must first remove all existing accounts (Google, etc.) from the device.

### Set Device Owner
```bash
adb shell dpm set-device-owner com.ram.deviceadmin/.MyDeviceAdminReceiver
```

### Remove Device Owner
Note: Removing a Device Owner via ADB usually requires the app to be a "test-only" build.
```bash
adb shell dpm remove-active-admin com.ram.deviceadmin/.MyDeviceAdminReceiver
```

## ⚠️ Warning
The **Wipe** feature will perform a factory reset on the device. Use this feature with extreme caution.

## 📝 License
This project is for educational purposes.
