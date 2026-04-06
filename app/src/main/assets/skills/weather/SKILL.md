---
name: weather
category: information
description: Current weather and forecasts via Open-Meteo. No API key. Uses GPS or city name. Tools: http, device.
permissions:
 - android.permission.INTERNET
---
# Weather Skill

Weather data via Open-Meteo — free, no API key, reliable.

## Tools

| Tool | Purpose |
|------|---------|
| `http` | Fetch weather and geocoding data |
| `device` | Get GPS coordinates |

---

## Step 1: Resolve location

### If user gave a city name → geocode it
```json
{
  "method": "GET",
  "url": "https://geocoding-api.open-meteo.com/v1/search?name={CITY}&count=1&language=en"
}
```
Extract `results[0].latitude`, `results[0].longitude`, `results[0].name` (use this as display name).

### If no location given → use GPS
```json
{ "action": "get_location" }
```
Returns `lat, lon`. Use directly — no need to reverse-geocode, just say "en tu ubicación actual".

---

## Step 2: Fetch weather

### Current conditions
```json
{
  "method": "GET",
  "url": "https://api.open-meteo.com/v1/forecast?latitude={LAT}&longitude={LON}&current=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weathercode,windspeed_10m,winddirection_10m&timezone=auto"
}
```

Response fields under `current`:
- `temperature_2m` — temperature °C
- `apparent_temperature` — feels-like °C
- `relative_humidity_2m` — humidity %
- `precipitation` — precipitation mm (last hour)
- `weathercode` — condition code (see table below)
- `windspeed_10m` — wind speed km/h
- `winddirection_10m` — wind direction degrees

### Forecast (up to 7 days)
```json
{
  "method": "GET",
  "url": "https://api.open-meteo.com/v1/forecast?latitude={LAT}&longitude={LON}&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max,weathercode,windspeed_10m_max&timezone=auto&forecast_days=7"
}
```

Response: `daily` object with arrays indexed by day (day 0 = today).

### Hourly precipitation (for "will it rain today/tomorrow?")
```json
{
  "method": "GET",
  "url": "https://api.open-meteo.com/v1/forecast?latitude={LAT}&longitude={LON}&hourly=precipitation_probability,precipitation&timezone=auto&forecast_days=2"
}
```

---

## Weathercode reference

| Code | Condition |
|------|-----------|
| 0 | Despejado ☀️ |
| 1–3 | Parcialmente nublado ⛅ |
| 45–48 | Niebla 🌫️ |
| 51–55 | Llovizna 🌦️ |
| 61–65 | Lluvia 🌧️ |
| 71–75 | Nieve 🌨️ |
| 80–82 | Chubascos 🌦️ |
| 95 | Tormenta ⛈️ |

---

## Workflows

### "¿Cómo está el clima?" (sin ubicación)
1. `device get_location` → lat, lon
2. Open-Meteo current conditions con esas coordenadas
3. Responder: "En tu ubicación: [condición], [temp]°C, se siente como [feels-like]°C, humedad [%], viento [km/h]."

### "Clima en [ciudad]"
1. Geocodificar ciudad → lat, lon, nombre
2. Open-Meteo current conditions
3. Responder con el nombre de la ciudad

### "¿Va a llover hoy/mañana?"
1. Resolver ubicación (GPS o geocoding)
2. Open-Meteo hourly con `precipitation_probability` para el día solicitado
3. Buscar horas con probabilidad > 40%
4. Responder claramente: "Sí, hay probabilidad de lluvia por la tarde (~70%)" o "No se espera lluvia."

### "Pronóstico de [N] días"
1. Resolver ubicación
2. Open-Meteo daily forecast con `forecast_days={N}` (máx 7)
3. Leer cada día: fecha, min/max temp, weathercode, probabilidad de lluvia
4. Resumir en lenguaje natural

### "¿Necesito paraguas?"
→ Igual que lluvia: revisar `precipitation_probability` del día actual.
Si alguna hora supera 40% → "Sí, lleva paraguas."

---

## Output rules

- Siempre en el idioma configurado del usuario
- Voz (TTS): máximo 3 oraciones. Directo al punto.
- Nunca mostrar coordenadas, códigos numéricos ni JSON al usuario
- Usar °C siempre
- Para lluvia: respuesta clara sí/no + cuándo (mañana/tarde/noche)
- Si `precipitation_probability_max` ≥ 40% → considerar lluvia probable

---

## Examples

- "¿Cómo está el clima?" → GPS → current → "En tu ubicación está nublado, 19°C, se siente como 17°C."
- "Clima en Huamantla" → geocode → current → "En Huamantla: despejado, 22°C, humedad 55%."
- "¿Va a llover hoy?" → hourly precipitation_probability → sí/no + hora
- "Pronóstico para esta semana" → daily 7 días → resumen por día
- "¿Necesito paraguas mañana?" → hourly day 1 → sí/no
- "Temperatura en Cancún" → geocode → current → "En Cancún: 31°C, se siente como 36°C."
