---
name: mega_skill
category: system
description: Skill Fusión Base que contiene la lógica detallada de todas las herramientas (Spotify, Mapas, Ajustes, etc.). Solicita el fragmento específico que necesites usando el parámetro fragment.
---
# Mega Skill (Fusión Base)

Esta skill contiene la lógica detallada de múltiples aplicaciones. Para ahorrar memoria, solo se te mostrará el fragmento que solicites.

## Fragmento: spotify
Control Spotify playback via intents, accessibility UI automation, and media keys. No API key required.

### Tool: intent — Open & basic control

#### Open Spotify
```json
{ "action": "android.intent.action.MAIN", "package": "com.spotify.music" }
```

#### Play a specific song/artist/playlist by URI (if you have the URI)
```json
{ "action": "android.intent.action.VIEW", "uri": "spotify:artist:ARTIST_ID", "package": "com.spotify.music" }
```

#### Search inside Spotify (opens search screen)
```json
{ "action": "android.intent.action.VIEW", "uri": "spotify:search:SEARCH_TERM", "package": "com.spotify.music" }
```
Replace spaces with `%20`. Example: `spotify:search:Bad%20Bunny`

#### Media key intents (play/pause/next/prev)
Use `intent` with broadcast action and `flags`:
```json
{ "action": "com.spotify.music.playbackstatechanged" }
```

For reliable play/pause/next/prev use the `accessibility` tool instead (see below).

### Tool: accessibility — Full control without API

This is the primary method for all playback control.

#### Workflow: Play a song by name
1. Open Spotify via intent (`android.intent.action.MAIN`)
2. `wait_for_app` with `package_name: "com.spotify.music"`
3. `get_tree` — find the Search tab/button and click it
4. `get_tree` — find the search input field, `type` the song/artist name
5. `get_tree` — find and click the first relevant result
6. Confirm playback started

#### Workflow: Play/Pause
1. `get_tree` on `com.spotify.music`
2. Find the play/pause button (usually labeled "Play" or "Pause", or has resource-id containing "play")
3. `click` it

#### Workflow: Next / Previous track
1. `get_tree`
2. Find "Next" or "Previous" button
3. `click` it

#### Workflow: Set volume
Use Android volume keys via accessibility or the volume slider visible in Spotify's UI.

#### Workflow: What's playing?
1. `get_tree` on `com.spotify.music`
2. Read the track title and artist name visible in the mini-player or now-playing screen
3. Respond with what you found

### Examples
- "Play Bad Bunny" → intent `spotify:search:Bad%20Bunny` → accessibility: click first artist result
- "Next song" → accessibility: find and click Next button
- "Pause" → accessibility: find and click Pause button
- "What's playing?" → accessibility `get_tree` → read track/artist labels
- "Play my Liked Songs" → intent `spotify:user:me:collection` or search via accessibility
- "Volume up" → accessibility: find volume slider or use Android volume

### Notes
- Always use `wait_for_app` after opening Spotify before reading the tree
- If the screen shows a login page, inform the user that Spotify needs to be logged in manually
- Never attempt HTTP calls to api.spotify.com — no API key is available

## Fragmento: whatsapp
Read and send messages via WhatsApp app UI automation. No API key required. Tools: intent, accessibility.

### Tool: intent — Open & basic control

#### Open WhatsApp
```json
{ "action": "android.intent.action.MAIN", "package": "com.whatsapp" }
```

#### Send a message to a contact
```json
{
  "action": "android.intent.action.VIEW",
  "uri": "https://wa.me/PHONE_NUMBER?text=MESSAGE_TEXT",
  "package": "com.whatsapp"
}
```
Replace `PHONE_NUMBER` with the full international number (e.g., `+15551234567`) and `MESSAGE_TEXT` with the URL-encoded message. Example: `https://wa.me/+15551234567?text=Hello%20there!`

### Tool: accessibility — Full control without API

#### Workflow: Send a message to a contact
1. Use `intent` to open a chat with the contact (see above).
2. `wait_for_app` with `package_name: "com.whatsapp"`.
3. `get_tree` — find the message input field and `type` the message.
4. `get_tree` — find the send button (usually a paper plane icon) and `click` it.
5. Confirm message sent.

#### Workflow: Read recent messages
1. Open WhatsApp via intent.
2. `wait_for_app`.
3. `get_tree` — read the chat list or open a specific chat.
4. Summarize visible messages.

### Examples
- "Send 'Hello' to John on WhatsApp" → intent to wa.me → accessibility: type message, click send.
- "Read my latest WhatsApp messages" → open WhatsApp → read chat list → summarize.

### Notes
- Always confirm recipient and message before sending.
- Phone numbers must be in international format.

## Fragmento: maps
Control Google Maps via intents and accessibility. No API key required.

### Tool: intent — Open & basic control

#### Open Google Maps
```json
{ "action": "android.intent.action.MAIN", "package": "com.google.android.apps.maps" }
```

#### Start navigation to a destination
```json
{
  "action": "android.intent.action.VIEW",
  "uri": "google.navigation:q=DESTINATION",
  "package": "com.google.android.apps.maps"
}
```
`DESTINATION` can be an address, place name, or lat/lng coordinates. Example: `google.navigation:q=Eiffel+Tower` or `google.navigation:q=48.8584,2.2945`.

#### Search for a place
```json
{
  "action": "android.intent.action.VIEW",
  "uri": "geo:0,0?q=QUERY",
  "package": "com.google.android.apps.maps"
}
```
`QUERY` can be a place name, type of business, or address. Example: `geo:0,0?q=restaurants+near+me`.

### Tool: accessibility — Interact with UI

#### Workflow: Stop navigation
1. `wait_for_app` with `package_name: "com.google.android.apps.maps"`.
2. `get_tree` — find the "Exit navigation" or "Stop" button and `click` it.

#### Workflow: Find nearby gas stations
1. Use intent to search for "gas stations near me" (see above).
2. `wait_for_app`.
3. `get_tree` — read the list of results.
4. Summarize the top results.

### Examples
- "Navigate to the nearest gas station" → intent search for gas stations → accessibility: read results, then intent navigate to first result.
- "Stop navigation" → accessibility: find and click stop button.

### Notes
- Never attempt HTTP calls to routes.googleapis.com — no API key is available.
- Replace spaces with `+` in URIs.

## Fragmento: gmail
Read and send emails via Gmail app UI automation. No API key required. Tools: intent, accessibility.

### Tool: intent — Open app

#### Open Gmail
```json
{ "action": "android.intent.action.MAIN", "package": "com.google.android.gm" }
```

#### Compose a new email (pre-filled)
```json
{
  "action": "android.intent.action.SENDTO",
  "uri": "mailto:recipient@example.com?subject=SUBJECT&body=BODY"
}
```
URL-encode the subject and body (spaces = `%20`).

### Tool: accessibility — Read and interact

#### Workflow: Check recent emails
1. Open Gmail via intent.
2. `wait_for_app` with `package_name: "com.google.android.gm"`.
3. `get_tree` — read the inbox list: sender names, subjects, preview text visible on screen.
4. Summarize via `tts`.

#### Workflow: Read a specific email
1. Open Gmail, `wait_for_app`.
2. `get_tree` — find the email in the list by sender or subject.
3. `click` it to open.
4. `get_tree` — read the full body text.
5. Summarize via `tts`.

#### Workflow: Send an email
**Option A — via intent (fastest, opens compose screen):**
1. Build mailto URI with recipient, subject, body (URL-encoded).
2. Fire intent `android.intent.action.SENDTO`.
3. `wait_for_app` on Gmail.
4. `get_tree` — find Send button.
5. `click` Send.
6. Confirm via `tts`.

**Option B — fully via accessibility:**
1. Open Gmail, `wait_for_app`.
2. `get_tree` — find Compose button (pencil/edit icon), `click`.
3. `get_tree` — find "To" field, `type` recipient email.
4. Find "Subject" field, `type` subject.
5. Find body field, `type` message.
6. Find Send button (paper plane icon), `click`.
7. Confirm via `tts`.

#### Workflow: Search emails
1. Open Gmail, `wait_for_app`.
2. `get_tree` — find Search icon, `click`.
3. `type` search query (e.g. sender name, subject keyword).
4. `get_tree` — read results.

### Examples
- "Check my inbox" → open Gmail → read inbox list → TTS summary.
- "Read the email from Pedro" → open Gmail → find email → open → read body → TTS.
- "Send email to itzy@example.com: I love you" → intent mailto → send via accessibility.
- "Do I have unread emails?" → open Gmail → read inbox, look for bold/unread indicators.

### Notes
- Always summarize email content — never read verbatim unless explicitly asked.
- Always confirm before sending.
- Never attempt HTTP calls to gmail.googleapis.com — no API key is available.

## Fragmento: calendar
Read and create calendar events via Google Calendar app UI automation. No API key required. Tools: intent, accessibility.

### Tool: intent — Open app

#### Open Google Calendar
```json
{ "action": "android.intent.action.MAIN", "package": "com.google.android.calendar" }
```

#### Create a new event (system intent — opens any calendar app)
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

### Tool: accessibility — Read events

#### Workflow: Read today's events
1. Open Google Calendar via intent.
2. `wait_for_app` with `package_name: "com.google.android.calendar"`.
3. `get_tree` — read visible events for today.
4. Summarize via `tts`.

#### Workflow: Create an event (fully automated)
1. Calculate timestamps with `datetime` tool.
2. Fire intent `android.intent.action.INSERT` with title, beginTime, endTime.
3. `wait_for_app` on calendar app.
4. `get_tree` — find Save button, `click` it.
5. Confirm via `tts`.

#### Workflow: Check a specific day
1. Open Calendar, `wait_for_app`.
2. `get_tree` — navigate to the correct date if needed (find forward/back arrows, `click`).
3. Read the events shown for that day.

### Examples
- "What do I have today?" → open Calendar → read today's events → TTS.
- "Am I free tomorrow at 3 PM?" → open Calendar → navigate to tomorrow → read 3 PM slot.
- "Create appointment: Doctor on Friday at 10 AM" → datetime tool → intent INSERT → save.
- "What's my next appointment?" → open Calendar → read first upcoming event.

### Notes
- Use `datetime` tool to convert human dates ("tomorrow at 10 AM") to Unix ms timestamps.
- Always confirm event details before creating.
- Never attempt HTTP calls to googleapis.com/calendar — no API key is available.

## Fragmento: settings
Control system settings via device tool.

### Tool: device — Adjust system settings

#### Volume
- Set volume: `{"action": "set_volume", "volume": 50}` (volume from 0 to 100).
- Get current volume: `{"action": "get_volume"}`.

#### Brightness
- Set brightness: `{"action": "set_brightness", "brightness": 128}` (brightness from 0 to 255).
- Set auto brightness: `{"action": "set_brightness_auto"}`.

#### Wi-Fi
- Enable Wi-Fi: `{"action": "enable_wifi"}`.
- Disable Wi-Fi: `{"action": "disable_wifi"}`.
- Get Wi-Fi status: `{"action": "get_wifi_status"}`.

#### Bluetooth
- Enable Bluetooth: `{"action": "enable_bluetooth"}`.
- Disable Bluetooth: `{"action": "disable_bluetooth"}`.
- Get Bluetooth status: `{"action": "get_bluetooth_status"}`.

#### Ringer Mode
- Set ringer mode: `{"action": "set_ringer_mode", "mode": "silent"}` (modes: `silent`, `vibrate`, `normal`).

#### Flashlight
- Turn flashlight on: `{"action": "flashlight_on"}`.
- Turn flashlight off: `{"action": "flashlight_off"}`.

### Examples
- "Set volume to 70" → `device` tool with `set_volume`.
- "Turn off Wi-Fi" → `device` tool with `disable_wifi`.
- "Is Bluetooth on?" → `device` tool with `get_bluetooth_status`.

## Fragmento: contacts
Search and manage contacts.

### Tool: query_contacts — Search contacts

#### Search by name
```json
{ "query": "John Doe" }
```
Returns contact details like phone numbers and email addresses.

### Examples
- "What's John's phone number?" → `query_contacts` with `query: "John"`.

## Fragmento: sms
Send SMS messages.

### Tool: send_sms — Send text message

#### Send SMS
```json
{ "phone_number": "PHONE_NUMBER", "message": "MESSAGE_TEXT" }
```
`PHONE_NUMBER` should be the recipient's phone number, and `MESSAGE_TEXT` is the content of the message.

### Examples
- "Send 'I'll be late' to Mom" → `send_sms` tool.

## Fragmento: phone
Make phone calls.

### Tool: intent — Make a call

#### Make a phone call
```json
{ "action": "android.intent.action.CALL", "uri": "tel:PHONE_NUMBER" }
```
`PHONE_NUMBER` should be the recipient's phone number.

### Examples
- "Call Dad" → `intent` tool with `tel:DAD_NUMBER`.

## Fragmento: weather
Check weather information.

### Tool: http (requires API key) or intent to weather app

#### Open Weather App
```json
{ "action": "android.intent.action.MAIN", "package": "com.google.android.apps.weather" }
```

#### Get weather via HTTP (if API key is available)
```json
{
  "method": "GET",
  "url": "https://api.weatherapi.com/v1/current.json?key=YOUR_API_KEY&q=LOCATION"
}
```

### Examples
- "What's the weather like today?" → `intent` to weather app or `http` tool.

