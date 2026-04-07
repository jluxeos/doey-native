# Doey – Android Native AI Assistant

Doey is a personal AI assistant for Android, built entirely in **Kotlin + Jetpack Compose**.  
No React Native. No Expo. No JavaScript. Pure Android.

## Architecture

```
app/src/main/java/com/doey/
├── DoeyApplication.kt          # App singleton, global state
├── agent/
│   ├── ConversationPipeline.kt # Orchestrates STT → LLM → TTS
│   ├── SkillLoader.kt          # Loads SKILL.md files from assets/skills/
│   ├── SettingsStore.kt        # DataStore + EncryptedSharedPreferences
│   ├── SystemPromptBuilder.kt  # Builds LLM system prompt
│   └── ToolLoop.kt             # LLM tool-call execution loop
├── llm/
│   └── LLMProvider.kt          # OpenAI-compatible provider (Groq/OpenAI/custom)
├── tools/
│   ├── ToolRegistry.kt         # Tool registration and dispatch
│   ├── AllTools.kt             # Core tools (Intent, SMS, Device, HTTP, TTS, etc.)
│   └── SchedulerTimerJournalTools.kt  # Scheduler, Timer, Journal, Notifications
├── services/
│   ├── DoeyTTSEngine.kt        # Android TTS engine (singleton)
│   ├── DoeySpeechRecognizer.kt # Android STT (SpeechRecognizer)
│   ├── WakeWordService.kt      # Porcupine wake word foreground service
│   ├── DoeyAccessibilityService.kt    # UI automation service
│   ├── DoeyNotificationListenerService.kt # Notification monitoring
│   ├── SchedulerEngine.kt      # AlarmManager-based task scheduler
│   ├── SchedulerJobService.kt  # Background LLM execution for schedules
│   └── TimerEngine.kt          # Countdown timer management
└── ui/
    ├── MainActivity.kt         # Entry point
    ├── DoeyApp.kt              # Compose root + navigation
    ├── MainViewModel.kt        # UI state + pipeline wiring
    ├── HomeScreen.kt           # Chat interface
    ├── SettingsScreen.kt       # Configuration
    └── OtherScreens.kt         # Schedules, Journal, Permissions
```

## Skills

Skills are Markdown files in `app/src/main/assets/skills/<name>/SKILL.md`.  
Each skill teaches the LLM how to use specific tools (Google Maps, Gmail, WhatsApp, etc.).  
Add a new skill by creating a folder with a `SKILL.md` following the frontmatter format.

## Build

### Requirements
- JDK 17+
- Android SDK (API 34)
- No Node.js, no npm, no Expo

### Debug build
```bash
./gradlew assembleDebug
```

### Release build
```bash
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`

### Signing behavior

- If `app/doey-release.keystore` exists, release signing uses environment variables for credentials.
- If no release keystore is present, the project falls back to the debug signing configuration so the release variant can still be built in development environments.

## Configuration

1. Open the app → Settings tab
2. Enter your **provider API key** for the provider you selected in the app
3. Optionally configure language, wake phrase, memory, personality, and enabled skills
4. Enable only the skills you actually want Doey to use
5. Tap **Save Settings**

### Important notes

- The main assistant now applies the enabled skills configuration consistently when building the system prompt.
- Exclusive tools tied to disabled skills are removed from the active toolset, reducing prompt noise and preventing incoherent tool selection.
- If you change provider, model, language, memory, or enabled skills, the pipeline is rebuilt so the new configuration takes effect immediately.

## Permissions Required

| Permission | Purpose |
|---|---|
| Microphone | Voice input + wake word |
| Accessibility Service | UI automation (controlling other apps) |
| Notification Listener | Monitoring notifications and auto-responding |
| Contacts | Looking up contacts for SMS/calls |
| SMS | Reading and sending text messages |
| Location | Navigation, weather queries |

## Wake Word

The current implementation uses Android's built-in `SpeechRecognizer` for continuous wake-phrase detection.

1. Configure the wake phrase in Settings
2. Enable Wake Word from the app
3. Keep in mind that detection quality depends on the device speech services and microphone availability

> This is not currently a Picovoice Porcupine integration. If you want Porcupine, it should be added explicitly in code and dependencies.

## CI/CD

GitHub Actions builds a release APK on every push to `master`.  
No Node.js step. No npm. Pure Gradle.

To use a real release keystore:
1. Base64-encode your keystore: `base64 -i your.keystore`
2. Add as GitHub secret: `RELEASE_KEYSTORE_BASE64`
