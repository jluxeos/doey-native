# IRIS v5 — Guía completa de funciones

**IRIS** (Intent Recognition & Interpretation System) es el motor local de Doey.
Procesa comandos **sin internet, sin IA, sin gastar tokens**. Solo cuando IRIS no
reconoce un comando, lo delega a Gemini.

---

## Principios de diseño

- **Vocabulario central único** — los verbos (`abre`, `pon`, `activa`, etc.) se
  definen una sola vez y todos los matchers los reutilizan.
- **Normalización automática** — tildes, signos de puntuación, wake words (`oye`,
  `iris`, `doey`), slang MX y variantes voz-a-texto se eliminan antes del análisis.
- **Orden flexible** — `wifi activa` = `activa wifi` = `prende el wifi`.
- **Sin duplicar sinónimos** — cada concepto vive en un solo lugar del código.

---

## Comandos encadenados

Conecta acciones con:
`y luego` · `y después` · `y también` · `y además` · `y enseguida`

**Ejemplo:**
> "manda un WhatsApp a mamá diciéndole que ya voy y luego pon música en Spotify"

IRIS los separa y los envía a la IA como secuencia ordenada.

---

## 📱 Dispositivo

### Linterna
| Comando | Resultado |
|---------|-----------|
| `enciende la linterna` | ON |
| `apaga el flash` | OFF |
| `prende el torch` | ON |

### Volumen
| Comando | Resultado |
|---------|-----------|
| `sube el volumen` | +1 paso (media) |
| `baja el volumen` | -1 paso |
| `volumen 70` | media al 70% |
| `sube el timbre` | +1 paso (timbre) |
| `volumen de alarma a 80` | alarma al 80% |
| `silencia` | modo silencio |
| `modo vibración` | vibrar |
| `sonido normal` | ring normal |

### Brillo
| Comando | Resultado |
|---------|-----------|
| `sube el brillo` | +30 |
| `brillo 50` | 50% |
| `brillo automático` | auto |

### Conectividad
| Comando | Resultado |
|---------|-----------|
| `activa wifi` | WiFi ON |
| `apaga el wifi` | WiFi OFF |
| `activa bluetooth` | BT ON |
| `activa modo avión` | avión ON |
| `activa no molestar` | DND ON |
| `activa NFC` | NFC ON |
| `activa modo oscuro` | dark mode |
| `activa hotspot` | tethering ON |

### Pantalla y sistema
| Comando | Resultado |
|---------|-----------|
| `toma captura` | screenshot |
| `bloquea el teléfono` | lock screen |
| `ahorro de energía` | power save |
| `pantalla se apague en 30 segundos` | screen timeout |

---

## ⏰ Alarmas y tiempo

### Alarmas
| Comando | Resultado |
|---------|-----------|
| `pon alarma a las 7` | alarma 07:00 |
| `pon alarma a las 6:30 am` | 06:30 |
| `pon alarma a las 10 pm` | 22:00 |
| `despiértame en 2 horas` | alarma relativa |
| `cancela la alarma` | cancela |

### Temporizadores
| Comando | Resultado |
|---------|-----------|
| `timer de 5 minutos` | 5 min |
| `temporizador 1 hora 30 minutos` | 90 min |
| `timer de 45 segundos` | 45 seg |
| `cancela el timer` | cancela |

### Cronómetro
| Comando | Resultado |
|---------|-----------|
| `inicia cronómetro` | start |
| `detén el cronómetro` | stop |

---

## 💬 Comunicación

### Llamadas
| Comando | Resultado |
|---------|-----------|
| `llama a mamá` | marcado |
| `llama al 911` | emergencia |
| `llama a la ambulancia` | emergencia |

### WhatsApp
| Comando | Resultado |
|---------|-----------|
| `manda WhatsApp a Juan diciéndole hola` | envía |
| `manda un mensaje a mamá diciendo te quiero` | envía |
| `wa a Pedro que diga ya voy` | envía |
| `abre WhatsApp con Laura` | abre chat |
| `chatea con mi hermano en WhatsApp` | abre chat |

### Telegram
| Comando | Resultado |
|---------|-----------|
| `manda mensaje a Ana por Telegram diciendo hola` | envía |

### SMS
| Comando | Resultado |
|---------|-----------|
| `manda SMS a papá diciendo ya llegué` | envía |
| `envía texto a 5551234567 diciendo hola` | envía |

---

## 🎵 Música y medios

### Reproducir música
| Comando | Resultado |
|---------|-----------|
| `pon Carin León en Spotify` | busca y reproduce |
| `toca Bad Bunny` | Spotify (default) |
| `pon música en YouTube` | abre YouTube |
| `busca video de GTA en YouTube` | busca en YT |
| `pon música en YouTube Music` | YT Music |

### Buscar y reproducir primer resultado
| Comando | Resultado |
|---------|-----------|
| `abre Spotify y busca música de Carin pon la primera que aparezca` | abre + busca + selecciona |
| `busca Carin León en Spotify y pon el primero` | lo mismo |

> Antes este comando fallaba y añadía "la primera que aparezca" a la lista de compras.
> **Arreglado en v5.**

### Controles de reproducción
| Comando | Resultado |
|---------|-----------|
| `pausa` | pausa |
| `siguiente` | siguiente canción |
| `anterior` | canción anterior |
| `shuffle` | modo aleatorio |
| `repite` | loop |
| `dale play` | reanuda |

---

## 🧮 Calculadora offline

Sin internet. Sin IA. Resultado instantáneo.

| Comando | Resultado |
|---------|-----------|
| `calcula 15% de 230` | 34.5 |
| `cuánto es 450 entre 3` | 150 |
| `cuánto es 2 elevado a 8` | 256 |
| `calcula 1200 más 350 menos 80` | 1470 |
| `cuánto es 75 por 4` | 300 |

---

## 🔄 Conversión de unidades offline

Sin internet. Tipos de cambio de moneda son aproximados.

### Longitud
`km ↔ millas` · `metros ↔ pies` · `cm ↔ pulgadas` · `km ↔ metros`

### Peso
`kg ↔ libras` · `gramos ↔ onzas` · `kg ↔ gramos`

### Temperatura
`°C ↔ °F` · `°C ↔ Kelvin` · `°F ↔ Kelvin`

### Volumen
`litros ↔ ml` · `litros ↔ galones`

### Moneda (aproximada, sin internet)
`USD ↔ MXN` · `EUR ↔ MXN` · `USD ↔ EUR`

**Ejemplos:**
```
convierte 100 dolares a pesos
cuánto son 5 km en millas
convierte 37 celsius a fahrenheit
convierte 1 libra en kilos
```

---

## 📝 Notas rápidas offline

Guardadas localmente en el dispositivo. Sin necesidad de abrir ninguna app.

| Comando | Resultado |
|---------|-----------|
| `anota: llamar al doctor` | guarda nota |
| `apunta contraseña wifi es 1234` | guarda nota |
| `recuerda que tengo cita el jueves` | guarda nota |
| `nota: comprar pan` | guarda nota |
| `qué tengo anotado` | muestra todas las notas |
| `mis notas` | muestra todas las notas |

---

## ⏰ Recordatorios rápidos offline

Usa la API de alarmas del sistema. No necesita internet.

| Comando | Resultado |
|---------|-----------|
| `recuérdame tomar el medicamento en 30 minutos` | notifica en 30 min |
| `avísame en 2 horas que tengo junta` | notifica en 2 h |
| `recuérdame llamar a mamá en 1 hora` | notifica en 60 min |

---

## 🛒 Lista de compras offline

| Comando | Resultado |
|---------|-----------|
| `añade leche a la lista` | agrega |
| `pon pan en la lista de compras` | agrega |
| `a la lista de compras agrega huevos` | agrega |
| `limpia la lista de compras` | borra todo |

> **Nota:** IRIS solo captura ítems si hay mención explícita de "lista" o "compras".
> Antes capturaba frases como "la primera que aparezca" por error. **Arreglado en v5.**

---

## 🗺️ Navegación y búsqueda

| Comando | Resultado |
|---------|-----------|
| `llévame a Walmart` | Google Maps navegación |
| `busca pizza cerca` | Maps (búsqueda) |
| `busca en Google gatos` | búsqueda web |
| `abre www.github.com` | URL directa |

---

## ℹ️ Consultas del sistema

| Comando | Resultado |
|---------|-----------|
| `qué hora es` | hora actual |
| `qué fecha es` | fecha actual |
| `cuánta batería` | porcentaje |
| `cuánto espacio` | almacenamiento libre |
| `cuánta RAM` | memoria disponible |
| `temperatura` | estado del CPU |
| `velocidad de internet` | tipo de red |
| `mi IP` | IP local |
| `tiempo encendido` | uptime |

---

## 📱 Apps rápidas

| Comando | Abre |
|---------|------|
| `abre la cámara` | cámara |
| `abre la galería` / `mis fotos` | galería |
| `abre contactos` | agenda |
| `abre el marcador` | dialer |
| `abre la calculadora` | calculadora |
| `abre el calendario` | calendario |
| `abre Maps` | Google Maps |
| `abre el navegador` / `abre Chrome` | browser |
| `abre mis archivos` | explorador |
| `abre el reloj` | reloj |
| `abre [cualquier app]` | búsqueda genérica |

---

## ⚙️ Ajustes rápidos

`ajustes de wifi` · `ajustes de bluetooth` · `ajustes de batería` · `ajustes de pantalla`
`ajustes de sonido` · `ajustes de ubicación` · `ajustes de seguridad` · `ajustes de apps`
`ajustes de idioma` · `ajustes de notificaciones` · `ajustes de privacidad`
`ajustes de NFC` · `ajustes de VPN` · `ajustes de datos` · `ajustes de teclado`
`ajustes de accesibilidad` · `ajustes de desarrollador`

---

## 📤 Compartir contenido

| Comando | Resultado |
|---------|-----------|
| `comparte esto` | abre menú de compartir |
| `comparte esta publicación con Juan` | share intent |
| `comparte el link` | share intent |

---

## 💬 Respuestas sociales (sin IA)

Saludos, despedidas, agradecimientos y afirmaciones se responden localmente
con variantes aleatorias. Cero tokens consumidos.

---

## 🔤 Normalización automática

IRIS entiende variantes fonéticas y slang antes de analizar el comando:

| Escrito / dicho | Interpretado como |
|-----------------|-------------------|
| `guasap` / `wasa` / `wapp` | whatsapp |
| `flas` / `flash` | linterna |
| `spoti` | spotify |
| `yt` | youtube |
| `cel` / `tele` | teléfono |
| `q` / `k` | que |
| `porfa` / `plis` / `xfa` | (ignorado) |
| `oye iris` / `hey doey` | (ignorado, solo procesa el comando) |
| `por favor` al final | (ignorado) |
| tildes | normalizadas |

---

## Qué NO maneja IRIS → va a Gemini

- Preguntas que empiezan con `cómo`, `qué es`, `por qué`, `explica`...
- Comandos con contexto externo (`del carro`, `del televisor`, `de casa`)
- Comandos muy ambiguos o con razonamiento condicional
- Cualquier cosa que requiera internet o conocimiento actualizado

