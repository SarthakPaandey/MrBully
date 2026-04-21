# MrBully

A **brutal Android accountability app** that uses AI interventions and strict phrase-based unlocks to stop distracting app usage. Designed to enforce digital discipline through aggressive but safe intervention mechanics.

## ✨ Features

- 📝 **Deep Onboarding Profile** — Personalized setup to understand user habits and goals
- 🚧 **App Restriction Selection** — Choose which distracting apps to block
- 🔍 **Foreground App Monitoring** — Uses Android Accessibility Service to detect app usage
- 🔒 **Intervention Lock Screen** — Exact phrase unlock required to dismiss the block
- ⏰ **Daily Check-in Notifications** — WorkManager-scheduled reminders with local persistence
- 🤖 **AI Intervention Lines** — Optional LLM-generated personalized intervention messages (OpenAI API)

## 🛠️ Tech Stack

| Technology | Purpose |
|-----------|--------|
| Kotlin + Jetpack Compose | Android UI |
| Room | Local database |
| WorkManager | Daily check-in scheduling |
| DataStore | App settings persistence |
| OkHttp | LLM API calls |
| OpenAI API | AI intervention messages (optional) |

## 🏗️ Architecture

```mermaid
flowchart TB
    user([User]) --> ui

    subgraph app["Android App"]
        direction TB
        ui["UI Layer (Jetpack Compose)<br/>Onboarding • Apps • Home • Chat • Settings"]
        nav["Navigation (NavGraph)"]
        repo["LocalRepository<br/>(single app data gateway)"]
        ui --> nav --> repo
    end

    subgraph platform["Android Platform Services"]
        direction TB
        acc["AppAccessibilityService<br/>(foreground app monitoring + interventions)"]
        work["WorkManager + DailyCheckInWorker<br/>(scheduled reminders)"]
        boot["BootReceiver<br/>(re-schedules work on reboot)"]
        notif["NotificationManager"]
        audio["MediaPlayer / Device TTS"]
    end

    subgraph storage["Local Storage"]
        direction TB
        room[("Room Database<br/>Profile • Restricted Apps • Events • Memories")]
        prefs[("DataStore Preferences<br/>Strict Mode • Phrase • Encrypted API Key")]
    end

    subgraph ai["AI Layer (Optional)"]
        direction TB
        groq["GroqClient (OkHttp)"]
        api[("Groq / OpenAI-Compatible API")]
        groq --> api
    end

    repo --> room
    repo --> prefs
    repo --> groq

    acc --> repo
    acc --> notif
    acc --> audio

    work --> repo
    work --> notif
    boot --> work
```

## 🚀 Getting Started

1. Open this folder in **Android Studio**
2. Let Gradle sync and install SDK 35
3. Run on Android device/emulator (API 26+)
4. Complete onboarding, select restricted apps
5. Open **Accessibility Settings** and enable the service for this app

## 🤖 LLM Setup (Optional)

- Save your OpenAI API key in the app dashboard
- If no key is set, the app uses a local fallback intervention message

## ⚠️ Notes

- iOS is not supported in this MVP
- Currently uses one punishment mode: strict intervention phrase unlock
- Tone is intentionally strict but avoids any unsafe or self-harm content

## 📄 License

MIT
