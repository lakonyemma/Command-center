# Lakony Command Center

A custom Android command center built with Kotlin and Jetpack Compose.

## Current features

- Lakony Blue interface
- Home dashboard
- Developer, School, Money, and Smith modes
- Bottom navigation
- Saved task list
- Task categories
- Complete and clear tasks
- Saved display name
- Offline Smith command engine
- Quick command routing
- `add task ...` Smith command
- Simple money snapshot
- Custom blue launcher icon
- Unit tests for Smith commands
- GitHub Actions Android build
- Automatic debug APK artifact

## Smith commands

Try commands such as:

- `Smith, dev mode`
- `Smith, school mode`
- `Smith, money mode`
- `Smith, brief me`
- `add task Finish database`
- `settings`

## Run in Android Studio

1. Clone `https://github.com/lakonyemma/Command-center.git`.
2. Open the repository folder in Android Studio.
3. Use JDK 17.
4. Allow Gradle sync to complete.
5. Connect an Android phone with USB debugging enabled, or start an emulator.
6. Press Run.

## Download APK from GitHub

1. Open the repository on GitHub.
2. Open the Actions tab.
3. Select the latest successful Android CI run.
4. Under Artifacts, download `lakony-command-center-debug`.
5. Extract the ZIP and install `app-debug.apk` on your Android phone.

Android might ask you to allow installation from your browser or file manager for a debug APK.

## Technical details

- Package: `com.lakony.commandcenter`
- Minimum Android: API 26
- Target Android: API 35
- Kotlin: 2.0.21
- Android Gradle Plugin: 8.7.3
- Gradle CI runtime: 8.9
- Java: 17

## Integration roadmap

The app is structured for later authenticated integrations such as GitHub, calendar, email, and an online AI backend. API keys and account credentials should never be committed to this public repository.
