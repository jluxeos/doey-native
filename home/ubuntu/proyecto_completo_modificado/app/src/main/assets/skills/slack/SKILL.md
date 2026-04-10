---
name: slack
category: communication
description: Open Slack and send messages via app intent and UI automation. No API key required. Tools: intent, accessibility.
android_package: com.Slack
permissions: []
---
# Slack Skill

Open Slack and interact with it via UI automation. No OAuth token needed.

## Tool: intent — Open app

### Open Slack
```json
{ "action": "android.intent.action.MAIN", "package": "com.Slack" }
```

---

## Tool: accessibility — Read and send messages

### Workflow: Read latest messages from a channel

1. Open Slack via intent
2. `wait_for_app` with `package_name: "com.Slack"`
3. `get_tree` — find the channel in the sidebar or search for it
4. `click` the channel
5. `get_tree` — read the visible messages (sender + text)
6. Summarize via `tts`

### Workflow: Send a message to a channel

1. Open Slack, `wait_for_app`
2. `get_tree` — find and click the target channel
3. `get_tree` — find the message input field at the bottom
4. `type` the message text
5. Find the Send button (arrow icon), `click`
6. Confirm via `tts`

### Workflow: Send a direct message

1. Open Slack, `wait_for_app`
2. `get_tree` — find "Direct Messages" section, click "+" or search for the person
3. `type` the person's name, select them
4. `get_tree` — find message input, `type` message, send
5. Confirm via `tts`

---

## Examples

- "Read the general channel" → open Slack → navigate to #general → read messages → TTS
- "Send 'I'm running late' to the team channel" → open Slack → find channel → type → send
- "DM Carlos: are you available?" → open Slack → DM → type → send

## Notes
- Always confirm before sending
- Never attempt HTTP calls to slack.com/api — no API key is available
