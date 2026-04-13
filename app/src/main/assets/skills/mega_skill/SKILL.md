---
name: mega_skill
category: system
description: Skill fusión para Spotify, WhatsApp, Maps, Gmail, Calendar, Settings, Contacts, SMS, Phone, Weather. Solicita fragmento específico con el parámetro fragment.
---
# Mega Skill

Lógica detallada por app. Se te mostrará solo el fragmento solicitado.

## Fragmento: spotify
Control via intents + accessibility. No API key.

**Open**: `{action:"android.intent.action.MAIN", package:"com.spotify.music"}`
**Search**: `{action:"VIEW", uri:"spotify:search:TERM%20URL_ENCODED", package:"com.spotify.music"}`

**Workflow play song**: open → `wait_for_app` → `get_tree` → click Search → type name → click result → confirm
**Play/Pause/Next/Prev**: `get_tree` → find button by label/resource-id → `click`
**What's playing**: `get_tree` → read track+artist from mini-player

Rules: `wait_for_app` before `get_tree`. No HTTP to api.spotify.com.

## Fragmento: whatsapp
**Open**: `{action:"MAIN", package:"com.whatsapp"}`
**Open chat**: `{action:"VIEW", uri:"https://wa.me/+INTLNUM?text=URL_ENCODED_MSG", package:"com.whatsapp"}`

**Send**: open chat via intent → `wait_for_app` → `get_tree` → find input → `type` → find send → `click` → confirm
**Read**: open → `wait_for_app` → `get_tree` → read chat list or open chat → summarize

Rules: confirm before send. International phone format. No HTTP to whatsapp.

## Fragmento: maps
**Navigate**: `{action:"VIEW", uri:"google.navigation:q=DEST", package:"com.google.android.apps.maps"}`
**Search**: `{action:"VIEW", uri:"geo:0,0?q=QUERY"}`
**Stop nav**: `wait_for_app` → `get_tree` → click "Exit navigation"

Spaces → `+` in URIs. No HTTP to routes.googleapis.com.

## Fragmento: gmail
**Open**: `{action:"MAIN", package:"com.google.android.gm"}`
**Compose**: `{action:"SENDTO", uri:"mailto:addr?subject=SUBJ&body=BODY"}` (URL-encode)

**Check inbox**: open → `wait_for_app` → `get_tree` → read senders/subjects → TTS
**Read email**: find in list → `click` → `get_tree` → read body → TTS
**Send (fast)**: compose intent → `wait_for_app` → find Send → `click`
**Send (full)**: open → Compose button → fill To/Subject/Body → Send → confirm

Rules: summarize emails (never verbatim unless asked). Confirm before send. No HTTP to gmail.googleapis.com.

## Fragmento: calendar
**Open**: `{action:"MAIN", package:"com.google.android.calendar"}`
**Create event**: `{action:"INSERT", uri:"content://com.android.calendar/events", extras:[{key:"title",...},{key:"beginTime",value:UNIX_MS},...]}` 
Use `datetime` tool for timestamps.

**Read today**: open → `wait_for_app` → `get_tree` → read events → TTS
**Create (auto)**: datetime tool → INSERT intent → `wait_for_app` → click Save → confirm

Rules: use `datetime` for date conversion. Confirm before creating. No HTTP to googleapis.com/calendar.

## Fragmento: settings
Tool: `device`

| action | params |
|---|---|
| `set_volume` | `volume: 0-100` |
| `get_volume` | — |
| `set_brightness` | `brightness: 0-255` |
| `set_brightness_auto` | — |
| `enable/disable_wifi` | — |
| `get_wifi_status` | — |
| `enable/disable_bluetooth` | — |
| `get_bluetooth_status` | — |
| `set_ringer_mode` | `mode: silent\|vibrate\|normal` |
| `flashlight_on/off` | — |

## Fragmento: contacts
Tool: `query_contacts`
```json
{ "query": "John Doe" }
```
Returns phone numbers + emails.

## Fragmento: sms
→ Ver skill `sms` para detalles completos.
Tool: `send_sms {phone_number, message}`. Confirm before send.

## Fragmento: phone
```json
{ "action": "android.intent.action.CALL", "uri": "tel:PHONE_NUMBER" }
```

## Fragmento: weather
**With API key**: `GET https://api.weatherapi.com/v1/current.json?key=KEY&q=LOCATION`
**No API key**: `{action:"MAIN", package:"com.google.android.apps.weather"}`
