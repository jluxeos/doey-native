# Doey v3 — Cambios y Mejoras

## 🎯 Problema raíz resuelto: la IA ya no genera texto innecesario

El principal desperdicio de tokens era que la IA generaba texto explicando lo que iba
a hacer, en lugar de simplemente hacerlo. Ahora es un **parser puro de acciones**.

---

## ✅ Cambios aplicados

### 1. `ProveedorLLM.kt` — RotatingProvider
**Problema:** Un solo proveedor se agotaba y todo colapsaba.
**Solución:** Nuevo `RotatingProvider` que rota entre proveedores automáticamente.
- Si Groq devuelve 429 → pasa a OpenRouter de forma transparente
- Si hay timeout → intenta el siguiente proveedor
- El usuario nunca ve un error de límite de tokens
- `LLMOptions.maxTokens` reducido de 4096 → 1024 (la IA nunca necesita generar texto largo)
- `LLMOptions.temperature` reducido a 0.1 (más determinista = menos alucinaciones)

### 2. `ConstructorPromptSistema.kt` — Prompt quirúrgico
**Problema:** System prompt invitaba a la IA a conversar, gastando ~500-1500 tokens extra por llamada.
**Solución:** Nuevo prompt con filosofía "PARSER PURO":
- R1 ahora es: "Tu único trabajo es usar herramientas. NUNCA escribas texto antes de ejecutar."
- R11 nuevo: Comandos encadenados ejecutados SECUENCIALMENTE uno por uno
- Nuevo `buildMinimal()` para comandos simples: ~80% menos tokens que el prompt completo
- Texto de respuesta máximo: 1 oración corta

### 3. `MotorConversacion.kt` — Pipeline limpio
**Problema:** Sistema ARIA con lógica de fallback compleja que a veces producía respuestas extrañas.
**Solución:** Lógica simplificada y limpia:
- `LLMOptions` con maxTokens bajo pasados explícitamente al bucle
- Cache separado para prompt mínimo y prompt completo
- Mensajes de error amigables locales (sin gastar tokens en ARIA)
- maxIterations cap en 8 (más no ayuda, solo gasta)
- maxHistoryMessages reducido de 20 → 16

### 4. `BucleHerramientas.kt` — Recibe LLMOptions
**Cambio:** Ahora acepta `LLMOptions` para que el pipeline controle exactamente
cuántos tokens se gastan según la complejidad del comando.

### 5. `ViewModelPrincipal.kt` — RotatingProvider + respuesta local
**Cambios:**
- Si el usuario tiene API key de OpenRouter configurada, se usa como proveedor de respaldo automático
- Cuando la IA no genera texto (solo ejecutó herramientas), muestra "✅ Listo." local sin gastar tokens
- Comando encadenado (Complex): confirmación local si la IA no generó respuesta

### 6. `ProcesadorIntencionLocal.kt` — buildOptimizedPrompt compacto
**Problema:** El prompt para comandos encadenados era conversacional y provocaba que la IA elaborara.
**Solución:** Prompt ultra-compacto:
```
SECUENCIA:
1.enviar whatsapp a hijito: te quiero mucho
2.compartir publicación de Facebook con hijito
REGLA:ejecuta herramienta por herramienta.Sin texto entre pasos.
```

### 7. `OptimizadorTokens.kt` — Estrategias ajustadas
- TRIVIAL: 0 mensajes de historial, máximo 2 iteraciones
- SIMPLE: 2 mensajes de historial, máximo 4 iteraciones  
- MODERATE: 6 mensajes, máximo 7 iteraciones
- COMPLEX: 10 mensajes, máximo 8 iteraciones (cap duro)

---

## 📊 Ahorro de tokens estimado

| Tipo de comando | Antes | Después | Ahorro |
|---|---|---|---|
| Trivial (hora, batería) | ~800 tokens | ~0 (IRIS local) | 100% |
| Simple (abrir app) | ~1200 tokens | ~300 tokens | 75% |
| Moderado (WhatsApp) | ~2000 tokens | ~600 tokens | 70% |
| Complejo encadenado | ~5000 tokens | ~1200 tokens | 76% |

---

## 🚀 Para activar el RotatingProvider

Ve a Ajustes → configura tu provider principal (Groq/Gemini) Y opcionalmente
agrega una API key de OpenRouter (gratuita en openrouter.ai).
Doey rotará automáticamente si el principal se agota.

