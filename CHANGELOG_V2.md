# Doey v2.0 — Registro de Cambios

## Nuevas Funcionalidades

### 1. Onboarding Completo (OnboardingScreen.kt)
- **Pantalla de bienvenida** animada con logo pulsante
- **Selección de perfil de usuario**: Modo Básico (personas mayores/sin experiencia) o Modo Avanzado (usuarios técnicos)
- **Selección de modo de rendimiento**: Bajo Consumo (sin animaciones, para gama baja) o Alto Rendimiento (experiencia completa)
- **Pantalla de permisos**: Solicita todos los permisos necesarios antes de empezar, incluyendo superposición de pantalla
- **Configuración de API Key**: Permite configurar el proveedor de IA y la clave API desde el primer uso
- Indicador de progreso visual con pasos
- Navegación hacia atrás entre pasos

### 2. Burbuja Flotante / Overlay (DoeyOverlayService.kt)
- Servicio en primer plano que muestra una burbuja arrastrable sobre cualquier app
- La burbuja cambia de color según el estado: IDLE, LISTENING, THINKING, ACTING, SUCCESS, ERROR
- Panel expandible con:
  - Estado actual del asistente
  - Mensaje descriptivo de lo que está haciendo
  - Próxima acción prevista
  - Botón para abrir la app completa
- Arrastrable por toda la pantalla
- Animación de pulso cuando está activo
- Reporta estado en tiempo real desde el servicio de accesibilidad

### 3. Asistente Predeterminado del Sistema (DoeyAssistantService.kt)
- Implementa `VoiceInteractionSessionService` para registrarse como asistente del sistema
- Puede ser configurado como asistente predeterminado (reemplazando a Google Assistant, Bixby, etc.)
- Se invoca con botón home largo o comando de voz del sistema
- Lanza Doey directamente con escucha automática
- Activa la burbuja flotante si está disponible
- Intent `ACTION_ASSIST` y `ACTION_VOICE_COMMAND` registrados en el manifest

### 4. Menú Hamburguesa Lateral (DoeyApp.kt)
- Barra inferior simplificada con solo 3 elementos principales + botón de menú
- Drawer lateral con todas las pantallas organizadas por secciones
- Secciones diferentes según el perfil (básico/avanzado)
- Header del drawer con nombre de perfil y modo de rendimiento
- Indicador visual de modo bajo consumo

### 5. Pantalla de Perfil (ProfileScreen.kt)
- Cambio de tipo de usuario en cualquier momento
- Cambio de modo de rendimiento
- Gestión de la burbuja flotante con verificación de permisos
- Configuración del asistente predeterminado del sistema
- Resumen del perfil actual

### 6. Almacenamiento de Perfil (ProfileStore.kt)
- Nuevo store dedicado para configuración de perfil y rendimiento
- Separado de SettingsStore para mayor claridad
- Gestiona: perfil de usuario, modo de rendimiento, estado del overlay, preferencias del asistente

### 7. Integración con Servicio de Accesibilidad
- El servicio de accesibilidad ahora reporta cada acción a la burbuja flotante
- El usuario puede ver en tiempo real qué está tocando/escribiendo Doey
- Mensajes descriptivos para cada tipo de acción
- Reporte de errores en la burbuja

## Optimizaciones para Baja Gama

- Modo Bajo Consumo desactiva animaciones costosas
- Elevación reducida en componentes UI
- Menú simplificado para usuarios básicos (menos opciones = menos carga)
- Onboarding con selección explícita de modo de rendimiento
- Lazy loading de componentes pesados

## Cambios en AndroidManifest.xml
- Nuevo permiso `SYSTEM_ALERT_WINDOW` para la burbuja flotante
- Registro de `DoeyOverlayService` como servicio de primer plano
- Intent filters `ACTION_ASSIST` y `ACTION_VOICE_COMMAND` en MainActivity
- `hardwareAccelerated="true"` para mejor rendimiento de Compose

## Archivos Modificados
- `app/src/main/AndroidManifest.xml` — Nuevos permisos y servicios
- `app/src/main/java/com/doey/ui/DoeyApp.kt` — Menú hamburguesa, onboarding, navegación
- `app/src/main/java/com/doey/ui/MainActivity.kt` — Manejo de intents del asistente
- `app/src/main/java/com/doey/services/DoeyAccessibilityService.kt` — Reporte de estado al overlay

## Archivos Nuevos
- `app/src/main/java/com/doey/ui/OnboardingScreen.kt` — Flujo de onboarding completo
- `app/src/main/java/com/doey/ui/ProfileScreen.kt` — Pantalla de configuración de perfil
- `app/src/main/java/com/doey/services/DoeyOverlayService.kt` — Servicio de burbuja flotante
- `app/src/main/java/com/doey/services/DoeyAssistantService.kt` — Asistente del sistema
- `app/src/main/java/com/doey/agent/ProfileStore.kt` — Almacenamiento de perfil
- `app/src/main/res/xml/interaction_service.xml` — Configuración del asistente de voz
