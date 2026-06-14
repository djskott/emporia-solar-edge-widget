# Emporia Solar Widget

Android home screen widget that compares live home load from Emporia with live solar production from SolarEdge.

The widget shows:

- Current solar production in kW
- Current home load in kW
- Net balance in kW
- A color-coded background for quick at-a-glance status
- The last refresh time

## What It Does

This app connects to:

- Your Emporia account using your Emporia email and password
- Your SolarEdge Monitoring API using your SolarEdge API key

On first save, the app:

- Detects the first active SolarEdge site automatically
- Detects the Emporia monitor that has your mains channels automatically
- Stores the resolved device/site details for later refreshes

After setup, the widget:

- Refreshes automatically every 15 minutes when network is available
- Supports manual refresh from the widget refresh button

## Requirements

- Android 8.0 or newer
- An Emporia account with a supported monitor
- A SolarEdge site and API key
- Internet access on the device

## How To Use The App

1. Install the app on your Android device.
2. Open the app.
3. Enter:
   - Your Emporia email
   - Your Emporia password
   - Your SolarEdge API key
4. Tap `Save and Refresh`.
5. Wait for the app to validate the accounts and auto-detect your site/device.
6. Add the `Emporia Solar Widget` to your home screen.
7. Use the refresh icon on the widget any time you want an immediate update.

## Reading The Widget

- `Solar` is your current solar production
- `Load` is your current home consumption
- `Net` is solar minus load

Color meaning:

- Green: solar is covering load with extra production left over
- Yellow: solar and load are close to balanced
- Red: home load is higher than solar production

## Clearing Saved Credentials

Open the app and tap `Clear` to remove stored credentials and reset the widget.

## Security Notes

- No live user credentials are stored in this repository.
- `local.properties` is intentionally not committed because it contains a machine-local Android SDK path.
- App credentials are entered at runtime and stored only on the device.
- Stored credentials are encrypted using the Android Keystore.
- The app excludes its encrypted secrets from Android backup and device-transfer rules.

## Building The Project

### Android Studio

1. Open the project in Android Studio.
2. Let Android Studio install or point to the required Android SDK.
3. If needed, Android Studio will generate `local.properties` for your machine.
4. Build and run the `app` configuration on a device or emulator.

### Command Line

If you prefer the command line, use a local Gradle 8.13+ install with your Android SDK configured, then run:

```powershell
gradle assembleDebug
```

## Project Layout

- `app/` - Android app source
- `gradle/` - Gradle wrapper metadata
- `.tools/` - local-only toolchain files, ignored from Git
- `artifacts/` - local build outputs, ignored from Git

## Notes

- This project is not affiliated with Emporia or SolarEdge.
- Emporia access in this app uses the user's own account login rather than a separate public API key flow.
