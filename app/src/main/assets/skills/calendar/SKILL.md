---
name: calendar
category: productivity
description: Read and create calendar events via Google Calendar app UI automation. No API key required. Tools: intent, accessibility.
android_package: com.google.android.calendar
permissions: []
---
# Google Calendar Skill

Read and manage calendar events by automating the Google Calendar app. No OAuth or API key needed.

## Tool: intent — Open app

### Open Google Calendar
```json
{ "action": "android.intent.action.MAIN", "package": "com.google.android.calendar" }
```

### Create a new event (system intent — opens any calendar app)
```json
{
  "action": "android.intent.action.INSERT",
  "uri": "content://com.android.calendar/events",
  "extras": [
    { "key": "title", "value": "Event Title" },
    { "key": "beginTime", "value": "1700000000000" },
    { "key": "endTime", "value": "1700003600000" },
    { "key": "description", "value": "Optional description" }
  ]
}
```
`beginTime` and `endTime` are Unix timestamps in milliseconds. Use the `datetime` tool to calculate them.

---

## Tool: accessibility — Read events

### Workflow: Read today's events

1. Open Google Calendar via intent
2. `wait_for_app` with `package_name: "com.google.android.calendar"`
3. `get_tree` — read visible events for today
4. Summarize via `tts`

### Workflow: Create an event (fully automated)

1. Calculate timestamps with `datetime` tool
2. Fire intent `android.intent.action.INSERT` with title, beginTime, endTime
3. `wait_for_app` on calendar app
4. `get_tree` — find Save button, `click` it
5. Confirm via `tts`

### Workflow: Check a specific day

1. Open Calendar, `wait_for_app`
2. `get_tree` — navigate to the correct date if needed (find forward/back arrows, `click`)
3. Read the events shown for that day

---

## Examples

- "What do I have today?" → open Calendar → read today's events → TTS
- "Am I free tomorrow at 3 PM?" → open Calendar → navigate to tomorrow → read 3 PM slot
- "Create appointment: Doctor on Friday at 10 AM" → datetime tool → intent INSERT → save
- "What's my next appointment?" → open Calendar → read first upcoming event

## Notes
- Use `datetime` tool to convert human dates ("tomorrow at 10 AM") to Unix ms timestamps
- Always confirm event details before creating
- Never attempt HTTP calls to googleapis.com/calendar — no API key is available
