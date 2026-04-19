# Doey — Arquitectura Modular

> Cada subsistema tiene un **nombre oficial en mayúsculas** y vive en su propia carpeta.
> Edita solo la carpeta del subsistema que te interesa. El resto no se toca.

---

## Los 8 Subsistemas

| Nombre | Qué es | Carpeta |
|--------|--------|---------|
| **IRIS** | Clasificador de intenciones offline | `IRIS/` |
| **NEXUS** | Motor de conversación con la IA | `NEXUS/` |
| **ORACLE** | Proveedor de modelos LLM | `ORACLE/` |
| **FORGE** | Herramientas que ejecuta la IA | `FORGE/` |
| **ECHO** | Voz: STT + TTS + Palabra de activación | `ECHO/` |
| **VEIL** | Servicios del sistema Android | `VEIL/` |
| **DELTA** | UI, tema visual y navegación | `DELTA/` |
| **VAULT** | Ajustes, perfiles y memoria | `VAULT/` |

---

## IRIS — Clasificador de intenciones offline

> *"Antes de gastar un token, IRIS resuelve lo que puede solo."*

IRIS intercepta cada mensaje y decide si lo resuelve localmente (sin IA) o delega.
Maneja +100 tipos de comando: volumen, alarmas, llamadas, bluetooth, apps, cálculos, etc.

```
IRIS/
├── IrisClasificador.kt   ← CATÁLOGO: define cada acción y la lógica de clasificación
├── IrisMotor.kt          ← DETECCIÓN: una función match*() por cada tipo de comando
└── IrisDiccionario.kt    ← VOCABULARIO: palabras clave, slang, respuestas sociales
```

**Añadir un comando nuevo (ej: "modo cine"):**
1. `IrisClasificador.kt` → añade `class ActivarModoCine : LocalAction()`
2. `IrisClasificador.kt` → registra `?: matchModoCine(lo)` en `tryLocal()`
3. `IrisMotor.kt` → escribe `fun matchModoCine(lo: String): LocalAction?`
4. `ViewModelPrincipal.kt` → añade el branch `is LocalAction.ActivarModoCine ->` en `executeLocalAction()`

---

## NEXUS — Motor de conversación con la IA

> *"El cerebro que orquesta: historial, herramientas, tokens y respuestas."*

Cuando IRIS no puede resolver algo, NEXUS toma el control. Gestiona historial,
elige temperatura según complejidad, ejecuta el bucle de herramientas y optimiza tokens.

```
NEXUS/
├── MotorConversacion.kt        ← ConversationPipeline: punto de entrada principal
├── BucleHerramientas.kt        ← runToolLoop(): itera LLM ↔ herramientas hasta resolver
├── ConstructorPromptSistema.kt ← SystemPromptBuilder: construye el system prompt
├── OptimizadorTokens.kt        ← TokenOptimizer: compresión de historial y caché
├── ModoFlujoAgente.kt          ← FlowModeEngine: modo menú táctil sin voz
├── ProcesadorIntencionLocal.kt ← puente entre NEXUS e IRIS
├── CargadorHabilidades.kt      ← SkillLoader: carga skills externas
├── RegistradorDoey.kt          ← DoeyLogger: logs internos
└── ProveedorMensajesAmigables.kt ← mensajes de espera mientras la IA procesa
```

**Qué editar:**
- Prompt del sistema → `ConstructorPromptSistema.kt`
- Mensajes de historial que guarda → `MotorConversacion.kt` (`maxHistoryMessages`)
- Iteraciones máximas de herramientas → `BucleHerramientas.kt` (`maxIterations`)
- Mensajes de "un momento..." → `ProveedorMensajesAmigables.kt`

---

## ORACLE — Proveedor de modelos LLM

> *"El conector universal: cambia de modelo sin que nadie se entere."*

ORACLE abstrae el proveedor de IA. El resto de la app solo conoce `LLMProvider`.
Soporta Gemini, Groq, Pollinations y OpenRouter.

```
ORACLE/
└── ProveedorLLM.kt   ← interfaz LLMProvider + GeminiProvider + GroqProvider +
                         PollinationsProvider + OpenRouterProvider +
                         FallbackProvider + RotatingProvider + LLMProviderFactory
```

**Añadir un proveedor nuevo (ej: Claude, OpenAI):**
1. Crea `class ClaudeProvider : LLMProvider` en `ProveedorLLM.kt`
2. Implementa `chat()`, `getCurrentModel()` y `testConnection()`
3. Regístrala en `LLMProviderFactory.create()`

---

## FORGE — Herramientas que ejecuta la IA

> *"El taller: todo lo que la IA puede hacer en el mundo real."*

Cada herramienta es una clase `Tool` independiente. Desde SMS hasta HTTP,
contactos, control de UI o ejecución de shell.

```
FORGE/
├── TodasHerramientas.kt       ← todas las clases Tool (IntentTool, SmsTool, DeviceTool…)
├── RegistroHerramientas.kt    ← ToolRegistry: registro central + tipos ToolResult
├── HerramientaControlUI.kt    ← control de UI por accesibilidad
├── HerramientaShell.kt        ← ejecución de comandos shell
├── HerramientaDiario.kt       ← lectura/escritura del diario
└── HerramientasProgramadorTemporizadorDiario.kt ← alarmas y timers programados
```

**Añadir una herramienta nueva (ej: clima):**
1. Crea `class WeatherTool : Tool` en `TodasHerramientas.kt`
2. Define su `definition` (JSON schema para la IA) y su `execute()`
3. Regístrala en `RegistroHerramientas.kt` → `ToolRegistry`

---

## ECHO — Voz: escucha, habla y palabra de activación

> *"Los oídos y la boca de Doey."*

Todo lo relacionado con audio: STT, TTS, palabra de activación, alarmas y timers.

```
ECHO/
├── ReconocedorVozDoey.kt        ← DoeySpeechRecognizer: escucha y transcribe
├── MotorTTSDoey.kt              ← DoeyTTSEngine: habla las respuestas
├── ServicioPalabraActivacion.kt ← escucha "Doey" en background
├── MotorTemporizador.kt         ← timers en tiempo real
├── MotorProgramador.kt          ← alarmas programadas
├── ProgramadorAlarmas.kt        ← gestión de AlarmManager
├── ReceptorAlarmas.kt           ← BroadcastReceiver para alarmas
├── ReceptorTemporizador.kt      ← BroadcastReceiver para timers
├── ReceptorProgramador.kt       ← BroadcastReceiver del programador
├── ReceptorInicio.kt            ← arranca servicios al encender el móvil
└── ServicioTrabajoProgramador.kt ← WorkManager para tareas periódicas
```

**Qué editar:**
- Palabra de activación → `ServicioPalabraActivacion.kt`
- Voz TTS / velocidad → `MotorTTSDoey.kt` → `selectBestVoice()`
- Idioma de reconocimiento → `ReconocedorVozDoey.kt` → `listen()`

---

## VEIL — Servicios del sistema Android

> *"Los tentáculos de Doey en el sistema operativo."*

Servicios de larga duración: overlay flotante, accesibilidad, notificaciones.

```
VEIL/
├── ServicioAccesibilidadDoey.kt       ← lee/controla UI de otras apps
├── ServicioSuperposicionDoey.kt       ← burbuja flotante sobre el sistema
├── ServicioModoAmigable.kt            ← chat flotante (modo amigable)
├── ServicioInteraccionVozDoey.kt      ← interacción de voz en background
├── ServicioEscuchaNotificacionesDoey.kt ← intercepta notificaciones
└── DoeyAssistantService.kt            ← integración con assistant API de Android
```

**Qué editar:**
- Diseño de la burbuja flotante → `ServicioSuperposicionDoey.kt`
- Capacidades de accesibilidad → `ServicioAccesibilidadDoey.kt`
- Qué notificaciones procesa → `ServicioEscuchaNotificacionesDoey.kt`

---

## DELTA — UI, tema visual y navegación

> *"La cara de Doey: todo lo que el usuario ve y toca."*

Sistema de diseño completo: colores, tipografía, componentes y todas las pantallas.

```
DELTA/
├── MainActivity.kt           ← actividad principal
├── core/
│   ├── DeltaTheme.kt         ← DeltaColors + DoeyTypography: tokens de diseño globales
│   ├── DoeyComponents.kt     ← componentes reutilizables (burbujas, botones, inputs)
│   ├── NavegacionPrincipal.kt ← grafo de navegación
│   ├── TextoMarkdown.kt      ← renderer de markdown en el chat
│   ├── ProveedorIconos.kt    ← catálogo de iconos
│   └── CustomIcons.kt        ← iconos SVG personalizados
├── comun/
│   ├── PantallaInicio.kt     ← HomeScreen: chat principal
│   ├── PantallaConfiguracion.kt
│   ├── PantallaPerfil.kt
│   ├── PantallaMemorias.kt
│   ├── PantallaAgendas.kt
│   └── PantallaBienvenida.kt
├── basico/
│   ├── PantallaAjustesBasicos.kt
│   └── PantallaDiario.kt
└── avanzado/
    ├── PantallaPermisos.kt
    └── PantallaRegistros.kt
```

**Qué editar:**
- Colores globales → `DELTA/core/DeltaTheme.kt` → `DeltaColors`
- Tipografía → `DELTA/core/DeltaTheme.kt` → `DoeyTypography`
- Pantalla nueva → crear `PantallaNueva.kt` + registrar ruta en `NavegacionPrincipal.kt`
- Chat principal → `DELTA/comun/PantallaInicio.kt`

---

## VAULT — Ajustes, perfiles y memoria persistente

> *"La memoria a largo plazo de Doey."*

Almacenamiento persistente cifrado. DataStore + EncryptedSharedPreferences.

```
VAULT/
├── AlmacenAjustes.kt   ← SettingsStore: API key, modelo LLM, idioma, todos los ajustes
└── AlmacenPerfiles.kt  ← ProfileStore: nombre, alma (personalidad), memoria personal
```

**Qué editar:**
- Ajuste nuevo → `AlmacenAjustes.kt` → nuevo `Preferences.Key` + getter/setter
- Perfil del usuario → `AlmacenPerfiles.kt`
- Cómo se guarda la memoria → `AlmacenPerfiles.kt` → `savePersonalMemory()`

---

## Diagrama de dependencias

```
Usuario
  │
  ▼
DELTA ──────────────────────► ViewModelPrincipal
                                      │
           ┌──────────────────────────┤
           │                          │
           ▼                          ▼
         IRIS                       NEXUS
  (offline, sin IA)          (conversación con IA)
           │                          │
           │                    ┌─────┴──────┐
           │                    ▼            ▼
           │                 ORACLE        FORGE
           │             (modelo LLM)  (herramientas)
           │                                 │
           └─────────────────────────────────┤
                                             │
                           ┌─────────────────┘
                           │
                     ┌─────┴─────┐
                     ▼           ▼
                   ECHO        VEIL
                  (voz)     (sistema)
                     │           │
                     └─────┬─────┘
                           ▼
                         VAULT
                      (persistencia)
```

---

## Regla de oro

| Quiero... | Toco solo... |
|-----------|-------------|
| Añadir un comando de voz offline | **IRIS** + 1 branch en `ViewModelPrincipal.kt` |
| Cambiar cómo razona la IA | **NEXUS** → `ConstructorPromptSistema.kt` |
| Cambiar de Gemini a otro modelo | **ORACLE** → `ProveedorLLM.kt` |
| Dar a la IA una capacidad nueva | **FORGE** → `TodasHerramientas.kt` |
| Cambiar la voz o la palabra de activación | **ECHO** |
| Cambiar la burbuja flotante | **VEIL** → `ServicioSuperposicionDoey.kt` |
| Cambiar colores, fuentes o pantallas | **DELTA** |
| Añadir un ajuste de configuración | **VAULT** → `AlmacenAjustes.kt` |

---

## Build

GitHub Actions → `.github/workflows/android.yml`
APK firmado con `debug.keystore` por defecto.
Para release, proveer `doey-release.keystore`.
