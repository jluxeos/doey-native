---
name: google-maps
category: information
description: Start navigation and get location info via Google Maps intents. No API key required. Tools: intent, accessibility, device.
android_package: com.google.android.apps.maps
permissions:
 - android.permission.ACCESS_FINE_LOCATION
---
# Google Maps Skill

Start navigation, show locations, and get directions via Android intents. No API key needed.

## Tool: intent — Navigation & location

### Start navigation (driving)
```json
{
  "action": "android.intent.action.VIEW",
  "uri": "google.navigation:q=DESTINATION",
  "package": "com.google.android.apps.maps"
}
```
Replace spaces with `+`. Example: `google.navigation:q=Mexico+City`

### Navigation with travel mode
- Driving: `google.navigation:q=DESTINATION&mode=d`
- Walking: `google.navigation:q=DESTINATION&mode=w`
- Cycling: `google.navigation:q=DESTINATION&mode=b`

### Show location on map (no navigation)
```json
{
  "action": "android.intent.action.VIEW",
  "uri": "geo:0,0?q=SEARCH_TERM",
  "package": "com.google.android.apps.maps"
}
```

### Navigate to GPS coordinates
```json
{
  "action": "android.intent.action.VIEW",
  "uri": "google.navigation:q=19.4326,-99.1332",
  "package": "com.google.android.apps.maps"
}
```

---

## Tool: device — Current location

### Get current GPS coordinates
```json
{ "action": "get_location" }
```

### Calculate straight-line distance between two points
```json
{
  "action": "calculate_distance",
  "latitude1": 19.4326, "longitude1": -99.1332,
  "latitude2": 20.9674, "longitude2": -89.6237
}
```
Returns distance in km. No API key needed.

---

## Tool: accessibility — Stop navigation

1. Bring Maps to foreground: intent `android.intent.action.MAIN`
2. `wait_for_app` with `package_name: "com.google.android.apps.maps"`
3. `get_tree` — find "Stop" or "Exit navigation" button
4. `click` it

---

## Examples

- "Navigate to Walmart" → intent `google.navigation:q=Walmart`
- "Take me home" → intent `google.navigation:q=home`
- "Show me where the nearest hospital is" → intent `geo:0,0?q=hospital+near+me`
- "Stop navigation" → accessibility: find stop button
- "How far am I from Puebla?" → `device get_location` → `device calculate_distance` with Puebla coords → TTS
- "Navigate to these coordinates: 19.43, -99.13" → intent with lat/lon

## Notes
- For travel time or traffic info without API key: open Maps with destination, then read ETA from screen via accessibility
- Never attempt HTTP calls to routes.googleapis.com — no API key is available
- Replace spaces with `+` in URIs
