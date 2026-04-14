package com.doey.agente.iris

/**
 * IRIS — Diccionario de Sinónimos y Vocabulario Central
 *
 * ┌──────────────────────────────────────────────────────────────────────────┐
 * │  EDITA AQUÍ para agregar/quitar variantes de comandos.                   │
 * │  Cada constante se usa en todos los matchers — define UNA vez.           │
 * └──────────────────────────────────────────────────────────────────────────┘
 */
object IrisDiccionario {

    // ══════════════════════════════════════════════════════════════════════════
    // VERBOS CENTRALES — regex alternaciones reutilizables
    // ══════════════════════════════════════════════════════════════════════════

    /** Verbos de apertura / lanzamiento de apps */
    const val V_OPEN = "abre?|lanza|inicia|entra a|ve a|open|launch|start|dale a|pon|jala|carga|muestra|muestrame|ejecuta|activa la app|abre la app"

    /** Verbos de activación (encender algo) */
    const val V_ON = "activa|enciende|prende|turn on|enable|mete|conecta|pon|prender|activar|encender|dale|habilita"

    /** Verbos de desactivación (apagar algo) */
    const val V_OFF = "desactiva|apaga|turn off|disable|quita|saca|corta|desconecta|apagar|desactivar|deshabilita|para"

    /** Verbos de envío de mensajes */
    const val V_SEND = "manda|envia|escribe|send|dile|avisa|manda un|envia un|mandame|enviame|escribele|mandar|enviar"

    /** Verbos de reproducción de música */
    const val V_PLAY = "pon|reproduce|play|toca|escucha|quiero escuchar|ponme|dale play a|quiero oir|oye|poner|reproducir"

    /** Verbos de subir (volumen, brillo, etc.) */
    const val V_UP = "sube|aumenta|incrementa|mas|raise|up|eleva|pon mas"

    /** Verbos de bajar (volumen, brillo, etc.) */
    const val V_DOWN = "baja|reduce|disminuye|menos|lower|down|pon menos"

    /** Verbos de búsqueda */
    const val V_SEARCH = "busca|encuentra|search|halla|muestrame|dime|googlea|buscar|encontrar"

    /** Verbos de navegación GPS / ir a un lugar */
    const val V_NAV =
        "llevame a|llevame al|llevame a la|llevame a los|llevame a las|" +
        "lleva a|lleva al|lleva a la|" +
        "navega a|navega al|navega a la|" +
        "como llego a|como llego al|como llego a la|" +
        "como voy a|como voy al|como voy a la|" +
        "navigate to|directions to|direction to|" +
        "ir a|ira|ir al|ir a la|ire a|ire al|" +
        "quiero ir a|quiero ir al|quiero ir a la|" +
        "lleva me a|llevame pal|llevame pa|" +
        "dime como ir a|dime como llegar a|dime como llegar al|" +
        "como se va a|como se llega a|como se llega al|" +
        "como puedo llegar a|como puedo ir a|" +
        "muevete a|mueveme a|ruta a|ruta al|ruta a la|" +
        "guia a|guiame a|guiame al|guiame hacia|" +
        "mapa de|mapa a|" +
        "ve para|ve pa|vamos a|vamos al|vamos a la|" +
        "lleva me al|llevame hacia"

    /** Conector preposicional flexible */
    const val P_IN = "(?:en|al|a|de|en el|en la|dentro de|para|hacia|pal|pa el|pa la)?"

    // ══════════════════════════════════════════════════════════════════════════
    // SINÓNIMOS DE CONCEPTOS CLAVE
    // ══════════════════════════════════════════════════════════════════════════

    /** Variantes de "WhatsApp" por voz */
    const val S_WHATSAPP = "whatsapp|whats app|wapp|wasa|wa\\b|w\\.a\\.|guasap|wasap|guasap|uasap"

    /** Variantes de "linterna / flash" */
    const val S_FLASH = "linterna|flashlight|flash|flas|torch|lamparita|lampara del cel"

    /** Variantes de "Spotify" */
    const val S_SPOTIFY = "spotify|spoti|spotti"

    /** Variantes de "YouTube" */
    const val S_YOUTUBE = "youtube|you tube|yt|you-tube"

    /** Variantes de "Bluetooth" */
    const val S_BT = "bluetooth|bt\\b|b\\.t\\.|blutus|bluetoth|blue tooth|bluetoo?th|bletut"

    /** Variantes de "WiFi" */
    const val S_WIFI = "wifi|wi-fi|wi fi|red inalambrica|wireless|wai fai|waifi"

    /** Variantes de volumen/sonido */
    const val S_VOL = "volumen|vol|audio|sonido"

    /** Variantes de brillo/pantalla */
    const val S_BRILLO = "brillo|brightness|luminosidad|pantalla|luz de pantalla|iluminacion"

    /** Variantes de alarma */
    const val S_ALARM = "alarma|alarm|despertador|desperta"

    /** Variantes de cronómetro */
    const val S_CHRONO = "cronometro|stopwatch|crono|contador|cronómetro"

    /** Variantes de "lista de compras" */
    const val S_SHOPPING = "lista|compras|lista de compras|lista del super|lista del mandado|mercado|mi lista"

    /** Palabras geográficas — evitan que matchOpenApp capture destinos */
    val GEO_WORDS = listOf(
        "calle", "avenida", "colonia", "ciudad", "pais", "cerca", "pueblo",
        "estado", "nota", "lista", "primera", "barrio", "municipio", "localidad",
        "carretera", "autopista", "boulevard", "calzada", "fraccionamiento"
    )

    // ══════════════════════════════════════════════════════════════════════════
    // RESPUESTAS SOCIALES
    // ══════════════════════════════════════════════════════════════════════════

    val GREETING_RESPONSES = listOf(
        "¡Hola! 👋 ¿En qué te ayudo?",
        "¡Hey! ¿Qué necesitas?",
        "¡Buenas! Listo para lo que necesites 😊",
        "¡Hola! Dime, ¿qué hacemos?",
        "¡Aquí estoy! ¿Qué se te ofrece?",
        "¡Hey! ¿Qué onda? ¿En qué te echo la mano?",
        "¡Hola hola! Cuéntame. 🤖",
        "¡Qué tal! ¿Y tú?",
        "¡Ey, qué más! Dime en qué te ayudo. 😄",
        "¡Hola! Estoy aquí. ¿Qué se te antoja hacer?"
    )

    val FAREWELL_RESPONSES = listOf(
        "¡Hasta luego! 👋",
        "¡Cuídate mucho! 😊",
        "¡Chao! Vuelve cuando quieras.",
        "¡Hasta pronto!",
        "¡Que te vaya bien! 👋",
        "¡Nos vemos!",
        "¡Bye! Aquí estaré. 🤖",
        "¡Hasta la próxima!"
    )

    val GRATITUDE_RESPONSES = listOf(
        "¡De nada! 😊 Para eso estoy.",
        "¡Con gusto! ¿Algo más?",
        "¡A tus órdenes siempre! 🤖",
        "No hay de qué. ¿Algo más?",
        "¡Fue un placer! 😄",
        "¡Siempre! Para eso soy tu asistente. 💙"
    )

    val AFFIRMATION_RESPONSES = listOf(
        "👍", "¡Perfecto!", "¡Entendido!", "¡Claro!", "¡Listo!"
    )

    // ══════════════════════════════════════════════════════════════════════════
    // ALIASES DE UNIDADES para conversión offline
    // ══════════════════════════════════════════════════════════════════════════

    val UNIT_ALIASES = mapOf(
        "km" to "km", "kilometros" to "km", "kilómetros" to "km",
        "mi" to "mi", "millas" to "mi", "milla" to "mi",
        "m"  to "m",  "metros" to "m",   "metro" to "m",
        "cm" to "cm", "centimetros" to "cm", "centímetros" to "cm",
        "ft" to "ft", "pies" to "ft", "pie" to "ft", "feet" to "ft",
        "in" to "in", "pulgadas" to "in", "pulgada" to "in", "inches" to "in",
        "kg" to "kg", "kilos" to "kg", "kilo" to "kg", "kilogramos" to "kg",
        "lb" to "lb", "libras" to "lb", "libra" to "lb", "pounds" to "lb",
        "g"  to "g",  "gramos" to "g",  "gramo" to "g",
        "oz" to "oz", "onzas" to "oz",  "onza" to "oz",
        "c"  to "c",  "celsius" to "c", "centigrados" to "c", "centígrados" to "c",
        "f"  to "f",  "fahrenheit" to "f",
        "k"  to "k",  "kelvin" to "k",
        "usd" to "usd", "dolares" to "usd", "dólares" to "usd",
        "dolar" to "usd", "dollar" to "usd", "dóllar" to "usd",
        "mxn" to "mxn", "pesos" to "mxn", "peso mexicano" to "mxn",
        "eur" to "eur", "euros" to "eur", "euro" to "eur",
        "l"   to "l",   "litros" to "l",  "litro" to "l",
        "ml"  to "ml",  "mililitros" to "ml",
        "gal" to "gal", "galones" to "gal", "galon" to "gal", "gallon" to "gal"
    )

    // ══════════════════════════════════════════════════════════════════════════
    // SLANG / FONÉTICA voz-a-texto MX — normalizaciones
    // ══════════════════════════════════════════════════════════════════════════

    /** Pares (patrón regex, reemplazo) aplicados durante normalize() */
    val SLANG_NORMALIZATIONS = listOf(
        Pair("\\bk\\b",        "que"),
        Pair("\\bq\\b",        "que"),
        Pair("\\bxfa\\b",      ""),
        Pair("\\b(plis|pliss|porfa|por fis)\\b", ""),
        Pair("\\bguasap\\b",   "whatsapp"),
        Pair("\\bwasa\\b",     "whatsapp"),
        Pair("\\bwapp\\b",     "whatsapp"),
        Pair("\\buasap\\b",    "whatsapp"),
        Pair("\\bwasap\\b",    "whatsapp"),
        Pair("\\bflas\\b",     "linterna"),
        Pair("\\bflash\\b",    "linterna"),
        Pair("\\bspoti\\b",    "spotify"),
        Pair("\\bspotti\\b",   "spotify"),
        Pair("\\byt\\b",       "youtube"),
        Pair("\\btele\\b",     "telefono"),
        Pair("\\bcel\\b",      "telefono"),
        Pair("\\bmovi\\b",     "telefono"),
        Pair("\\bblutus\\b",   "bluetooth"),
        Pair("\\bbletut\\b",   "bluetooth"),
        Pair("\\bwai fai\\b",  "wifi"),
        Pair("\\bwaifi\\b",    "wifi"),
        Pair("\\bpa el\\b",    "al"),
        Pair("\\bpa la\\b",    "a la"),
        Pair("\\bpal\\b",      "al"),
        Pair("\\bpa\\b",       "a"),
        Pair("\\bira\\b",      "ir a"),
        Pair("\\bire a\\b",    "ir a"),
        Pair("\\bire al\\b",   "ir al"),
        Pair("\\blleva me\\b", "llevame")
    )

    /** Wake-words y cortesías a eliminar antes del matching */
    val WAKE_WORDS_PREFIX = Regex(
        "^(oye|hey|eh|ey|iris|doey|asistente|ok|okay|oiga),?\\s+",
        RegexOption.IGNORE_CASE
    )
    val COURTESY_SUFFIX = Regex(
        "\\s+(por favor|porfa|plis|gracias)\\.?$",
        RegexOption.IGNORE_CASE
    )
}
