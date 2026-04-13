---
name: slack
category: communication
description: Open Slack, read channels/DMs, send messages via UI automation. No API key. Tools: intent, accessibility.
android_package: com.Slack
permissions: []
---
# Slack Skill

## Open: intent
```json
{ "action": "android.intent.action.MAIN", "package": "com.Slack" }
```

## Workflows (all via accessibility)

**Read channel**: open → `wait_for_app` → `get_tree` → find+click channel → read messages → TTS

**Send to channel**: open → find channel → find input → `type` → click send → confirm TTS

**DM**: open → find "Direct Messages" → "+" or search person → find input → `type` → send → confirm TTS

Rules: always confirm before sending. No HTTP calls to slack.com/api.
