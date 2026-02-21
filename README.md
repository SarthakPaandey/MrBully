# Brutal Accountability (Android MVP)

Android-first accountability app MVP with:

- Deep onboarding profile
- Restricted app selection
- Foreground app monitoring via Accessibility Service
- Intervention lock screen with exact phrase unlock
- Daily check-in notification + local persistence
- Optional LLM-generated short intervention line (API key based)

## Tech

- Kotlin + Jetpack Compose
- Room (local DB)
- WorkManager (daily prompt)
- DataStore (settings)
- OkHttp (LLM API call)

## Run

1. Open this folder in Android Studio.
2. Let Gradle sync and install SDK 35.
3. Run app on Android device/emulator (API 26+).
4. In app:
   - Complete onboarding
   - Select restricted apps
   - Open **Accessibility Settings** and enable service for the app

## LLM setup (optional)

- Save your OpenAI API key in app dashboard.
- If no key is set, the app uses a local fallback intervention line.

## Notes

- iOS monitoring constraints are not implemented in MVP.
- Current MVP uses one punishment mode (strict intervention phrase unlock).
- Tone is intentionally strict but avoids unsafe/self-harm content.
