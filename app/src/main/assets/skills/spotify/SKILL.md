---
name: spotify
category: media
description: Control Spotify playback via intents, accessibility UI automation, and media keys. No API key required.
android_package: com.spotify.music
permissions:
 - android.permission.INTERNET
---
# Spotify Skill

Control Spotify entirely via Android intents and UI automation. No OAuth or API key needed.

## Tool: intent — Open & basic control

### Open Spotify
```json
{ "action": "android.intent.action.MAIN", "package": "com.spotify.music" }
```

### Play a specific song/artist/playlist by URI (if you have the URI)
```json
{ "action": "android.intent.action.VIEW", "uri": "spotify:artist:ARTIST_ID", "package": "com.spotify.music" }
```

### Search inside Spotify (opens search screen)
```json
{ "action": "android.intent.action.VIEW", "uri": "spotify:search:SEARCH_TERM", "package": "com.spotify.music" }
```
Replace spaces with `%20`. Example: `spotify:search:Bad%20Bunny`

### Media key intents (play/pause/next/prev)
Use `intent` with broadcast action and `flags`:
```json
{ "action": "com.spotify.music.playbackstatechanged" }
```

For reliable play/pause/next/prev use the `accessibility` tool instead (see below).

---

## Tool: accessibility — Full control without API

This is the primary method for all playback control.

### Workflow: Play a song by name

1. Open Spotify via intent (`android.intent.action.MAIN`)
2. `wait_for_app` with `package_name: "com.spotify.music"`
3. `get_tree` — find the Search tab/button and click it
4. `get_tree` — find the search input field, `type` the song/artist name
5. `get_tree` — find and click the first relevant result
6. Confirm playback started

### Workflow: Play/Pause
1. `get_tree` on `com.spotify.music`
2. Find the play/pause button (usually labeled "Play" or "Pause", or has resource-id containing "play")
3. `click` it

### Workflow: Next / Previous track
1. `get_tree`
2. Find "Next" or "Previous" button
3. `click` it

### Workflow: Set volume
Use Android volume keys via accessibility or the volume slider visible in Spotify's UI.

### Workflow: What's playing?
1. `get_tree` on `com.spotify.music`
2. Read the track title and artist name visible in the mini-player or now-playing screen
3. Respond with what you found

---

## Examples

- "Play Bad Bunny" → intent `spotify:search:Bad%20Bunny` → accessibility: click first artist result
- "Next song" → accessibility: find and click Next button
- "Pause" → accessibility: find and click Pause button
- "What's playing?" → accessibility `get_tree` → read track/artist labels
- "Play my Liked Songs" → intent `spotify:user:me:collection` or search via accessibility
- "Volume up" → accessibility: find volume slider or use Android volume

## Notes
- Always use `wait_for_app` after opening Spotify before reading the tree
- If the screen shows a login page, inform the user that Spotify needs to be logged in manually
- Never attempt HTTP calls to api.spotify.com — no API key is available
