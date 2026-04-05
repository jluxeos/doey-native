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

## Configuration

1. Open the app → Settings tab
2. Enter your **Groq API key** (free at [console.groq.com](https://console.groq.com))
3. Optionally: OpenAI key, Picovoice key (for wake word), language
4. Enable skills you want to use
5. Tap **Save Settings**

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

Uses [Picovoice Porcupine](https://picovoice.ai/):
1. Get a free access key at [console.picovoice.ai](https://console.picovoice.ai)
2. Enter it in Settings → Wake Word
3. Place a custom `.ppn` file in `app/src/main/assets/` for "Hey Doey"
4. Default built-in keyword: "PORCUPINE"

## CI/CD

GitHub Actions builds a release APK on every push to `master`.  
No Node.js step. No npm. Pure Gradle.

To use a real release keystore:
1. Base64-encode your keystore: `base64 -i your.keystore`
2. Add as GitHub secret: `RELEASE_KEYSTORE_BASE64`
