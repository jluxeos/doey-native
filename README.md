# Doey Delta

**Doey Delta** es la reconstrucción limpia de Doey — mismo asistente Android, arquitectura depurada.

## Cambios principales en Delta

- **Nuevo sistema de diseño: DeltaTheme** — tema oscuro (Deep Night) con acento violeta `#7C5DFA`. Reemplaza el sistema "Glassmorphism" que ya no usaba glassmorphism real.
- **Navegación limpia** — eliminadas las 5 rutas fantasma (Reloj, Alarmas, Recordatorios, Temporizadores, Cronómetro) que apuntaban todas a la misma pantalla.
- **HomeScreen rediseñada** — burbujas de chat con esquinas asimétricas, estado vacío elegante, barra de input con BasicTextField nativo.
- **Sin alias muertos** — `Label1Light`, `Surface0Light`, `OnPurple`, etc. eliminados y reemplazados por tokens Delta.
- **Colores del sistema** (`statusBarColor`, `navigationBarColor`) actualizados al fondo oscuro Delta.

## Arquitectura

```
agente/         ConversationPipeline, BucleHerramientas, TokenOptimizer, FlowModeEngine, IRIS
herramientas/   TodasHerramientas, ToolRegistry
llm/            GeminiProvider, GroqProvider, OpenRouterProvider, PollinationsProvider
servicios/      ServicioAccesibilidad, ServicioOverlay, ServicioNotificaciones, TTS, STT
ui/core/        DeltaTheme, DoeyComponents, CustomIcons, NavegacionPrincipal
ui/comun/       HomeScreen, SettingsScreen, ProfileScreen, MemoriesScreen, SchedulesScreen
ui/basico/      JournalScreen, FriendlySettingsScreen
ui/avanzado/    LogScreen, PermissionsScreen
```

## Build

GitHub Actions → `.github/workflows/android.yml`  
APK firmado con `debug.keystore` por defecto. Para release, proveer `doey-release.keystore`.
