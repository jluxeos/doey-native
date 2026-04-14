# Doey v4 — Refactorización Total

## Resumen de cambios

### ❌ Eliminado
- **Sistema de Skills completo** — `assets/skills/`, `CargadorHabilidades.kt` (ahora stub vacío),
  `PantallaHabilidades.kt`, `PantallaMacros.kt`, `PantallaModoFlujo.kt`
- **Múltiples proveedores de IA** — Groq y OpenRouter eliminados de la UI y del código
- **Sistema ARIA** (fallback con tokens) — reemplazado por mensajes de error locales
- **SkillDetailTool** — ya no se registra en el ToolRegistry

### ✅ Un solo proveedor: Gemini
**¿Por qué Gemini?**
- 1,500 requests/día gratuitos (sin límite de tokens/día)
- Tool calling nativo y confiable
- Contexto de 1,000,000 tokens
- Modelos: `gemini-2.5-flash` (default) o `gemini-2.0-flash-lite` (más ligero)
- Obtén tu API key gratis en: https://aistudio.google.com

### 📉 System prompt ultra-compacto — 3 niveles

| Nivel | Tokens | Cuándo se usa |
|-------|--------|---------------|
| **nano** | ~8 tokens | Comandos triviales delegados a IA |
| **mini** | ~40 tokens | Comandos simples con 1 acción |
| **full** | ~150-200 tokens | Comandos complejos/encadenados |

Antes el system prompt usaba ~1,500-2,000 tokens **siempre**.
Ahora usa 8-200 según necesidad. **Ahorro: 85-99% en tokens de sistema.**

### 📉 maxTokens por complejidad

| Complejidad | maxTokens respuesta |
|-------------|---------------------|
| TRIVIAL | 128 |
| SIMPLE | 256 |
| MODERATE | 512 |
| COMPLEX | 768 |

Antes: siempre 4,096. Ahora: 128-768. **Ahorro: 81-97% en tokens de respuesta.**

### 🧠 IRIS expandida
Nuevos patrones añadidos que antes requerían IA:
- `matchShare` — "comparte esto con X", "comparte esta publicación"
- `matchRingtoneVolume` — "sube el timbre"
- `matchAlarmVolume` — "volumen de alarma a 80"
- `executeLocalAction` con casos para ShareText, SetRingtoneVolume, SetAlarmVolume

### 🏗️ Arquitectura limpia
- `SkillLoader` es ahora un stub vacío (compatibilidad de compilación)
- `AlmacenAjustes.getCustomSkills()` sigue existiendo pero no se usa
- `RotatingProvider` sigue compilando como no-op (compatibilidad)
- Pantalla de configuración simplificada: solo muestra Gemini

## Estimación de tokens por tipo de petición

| Petición | Antes (v3) | Ahora (v4) | Ahorro |
|----------|-----------|-----------|--------|
| "qué hora es" (IRIS local) | 0 | 0 | — |
| "abre Spotify" (IRIS local) | 0 | 0 | — |
| "pon alarma a las 8" (IA) | ~1,800 | ~170 | **91%** |
| "manda WhatsApp a mamá" (IA) | ~2,000 | ~200 | **90%** |
| "envía WA a hijito y pon música" | ~4,500 | ~450 | **90%** |

