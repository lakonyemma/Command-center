# Architecture

## Layers

### UI

`ui/CommandCenterApp.kt`

Contains the five main destinations:

- Home
- Tasks
- Smith
- Money
- Settings

### Theme

`ui/LakonyTheme.kt`

Defines the Lakony Blue design system.

### Logic

`logic/SmithCommandEngine.kt`

Parses local Smith commands and returns actions without network access.

### Data

`data/LocalStore.kt`

Stores lightweight user data on the Android device with SharedPreferences.

### Model

`model/AppTask.kt`

Defines the local task object.

## Integration boundary

Future external services should live behind dedicated service classes instead of being called directly from Compose screens.

Suggested modules:

- `integration/github/GitHubService.kt`
- `integration/calendar/CalendarService.kt`
- `integration/email/EmailService.kt`
- `integration/ai/SmithAiService.kt`

Each service should expose simple app-level models and keep authentication details outside the UI layer.

## Security rules

- Do not hardcode access tokens.
- Do not commit API secrets.
- Store user tokens with Android Keystore-backed encrypted storage.
- Request only required OAuth scopes.
- Keep the repository buildable without private credentials.
