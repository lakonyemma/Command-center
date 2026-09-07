# Lakony Command Center

A custom Android command center built with Kotlin and Jetpack Compose.

## Core experience

- Lakony Blue interface with Bauhaus and Swiss-inspired layout
- Unified dashboard and Control Center
- Local tasks and Taskly integration
- Smith command interface
- Money planning and Absa banking space
- Profile picture and theme settings
- Sound and vibration task notifications
- Offline-first local storage
- GitHub Actions Android build with downloadable debug APK

## Control Center modules

- Calendar: schedule, reminders, Google Calendar connection surface and Taskly due-date sync
- Inbox: Gmail connection surface, priority inbox and mail alerts
- Developer: GitHub projects, cloud deployments, databases and security status
- Trading: MT5/EA control surface, risk guard, news guard, journal and market alerts
- Devices: Android, IoT and remote automation surfaces
- Systems: Render, PostgreSQL, automations and finance connections

## Finance

The Absa space currently provides the application layer for:

- Account balance
- Recent transactions
- Spending categories
- Budgets
- Planned expenses
- Projected balance

Real Absa customer data requires approved Absa API access, OAuth/customer consent and production credentials. Payment initiation remains disabled until the bank grants the required permissions.

## Secure integration model

The public repository must never contain personal API keys, banking credentials, access tokens or production secrets. External services should connect through OAuth, encrypted local session storage, Android secure storage, or a backend secret store.

## Run in Android Studio

1. Clone `https://github.com/lakonyemma/Command-center.git`.
2. Open the repository folder in Android Studio.
3. Use JDK 17.
4. Allow Gradle sync to finish.
5. Connect an Android phone with USB debugging enabled, or start an emulator.
6. Press Run.

## APK from GitHub

1. Open the repository on GitHub.
2. Open Actions.
3. Select the latest successful Android CI run.
4. Under Artifacts, download `lakony-command-center-debug`.
5. Extract the ZIP and install `app-debug.apk`.

## Technical details

- Package: `com.lakony.commandcenter`
- Minimum Android: API 26
- Target Android: API 35
- Kotlin: 2.0.21
- Android Gradle Plugin: 8.7.3
- Gradle CI runtime: 8.9
- Java: 17

## Integration roadmap

Next production connections are Gmail, Google Calendar, GitHub account data, Render service data, online Smith AI, MT5 bridge data and approved Absa APIs. Each integration stays isolated behind its own service layer so the Android UI remains stable when providers or credentials change.
