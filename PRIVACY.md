# Lakony Command Center Privacy

## Current version

The current app does not send personal data to a remote server.

It stores the following information locally on the Android device:

- Display name
- Task titles
- Task categories
- Task completion state

The offline Smith command engine processes commands inside the app.

## Future integrations

GitHub, email, calendar, and online AI features must use authenticated APIs and explicit account authorization.

Secrets, access tokens, API keys, passwords, and signing keys must never be committed to this public repository.

Any future remote integration should use encrypted transport and the smallest permissions needed for its feature.
