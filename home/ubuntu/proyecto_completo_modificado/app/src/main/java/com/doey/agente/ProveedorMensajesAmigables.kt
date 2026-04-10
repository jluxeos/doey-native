package com.doey.agente

import kotlin.random.Random

/**
 * Proveedor de mensajes amigables y naturales que se muestran mientras
 * la IA está procesando, para que el usuario no sienta que la app está "muerta".
 */
object FriendlyMessagesProvider {
    
    // Mensajes iniciales (cuando empieza a procesar)
    private val startMessages = listOf(
        "Déjame ver qué puedo hacer...",
        "Un momento, estoy pensando...",
        "Déjame buscar la mejor forma de ayudarte...",
        "Estoy en ello...",
        "Analizando tu solicitud...",
        "Déjame procesarlo...",
        "Voy a ver qué encuentro...",
        "Dame un segundo...",
        "Estoy trabajando en ello...",
        "Déjame revisar...",
        "Pensando en la mejor respuesta...",
        "Procesando tu petición..."
    )
    
    // Mensajes de espera (mientras sigue procesando)
    private val waitingMessages = listOf(
        "Casi listo...",
        "Sigue un poco más...",
        "Estoy en la mejor parte...",
        "Casi tengo la respuesta...",
        "Déjame terminar de revisar...",
        "Un momento más...",
        "Estoy muy cerca...",
        "Dándole los últimos toques...",
        "Ya casi...",
        "Terminando de procesar...",
        "Revisando los detalles...",
        "Finalizando..."
    )
    
    // Mensajes cuando está buscando información específica
    private val searchingMessages = listOf(
        "Buscando en tus datos...",
        "Revisando tus memorias...",
        "Consultando tus preferencias...",
        "Buscando la información...",
        "Revisando lo que sé de ti...",
        "Accediendo a tus datos...",
        "Buscando en el historial...",
        "Consultando tus notas..."
    )
    
    // Mensajes cuando está usando herramientas
    private val toolMessages = listOf(
        "Usando mis herramientas...",
        "Ejecutando acciones...",
        "Aplicando lo que sé...",
        "Usando mis habilidades...",
        "Activando herramientas...",
        "Procesando con mis skills...",
        "Aplicando la lógica..."
    )
    
    // Mensajes cuando está a punto de terminar
    private val finishingMessages = listOf(
        "Preparando la respuesta...",
        "Organizando la información...",
        "Formando la respuesta...",
        "Casi listo para responder...",
        "Dándole forma a la respuesta...",
        "Finalizando..."
    )
    
    private val random = Random(System.currentTimeMillis())
    
    /**
     * Obtiene un mensaje aleatorio de inicio.
     */
    fun getStartMessage(): String = startMessages.random(random)
    
    /**
     * Obtiene un mensaje aleatorio de espera.
     */
    fun getWaitingMessage(): String = waitingMessages.random(random)
    
    /**
     * Obtiene un mensaje aleatorio de búsqueda.
     */
    fun getSearchingMessage(): String = searchingMessages.random(random)
    
    /**
     * Obtiene un mensaje aleatorio de uso de herramientas.
     */
    fun getToolMessage(): String = toolMessages.random(random)
    
    /**
     * Obtiene un mensaje aleatorio de finalización.
     */
    fun getFinishingMessage(): String = finishingMessages.random(random)
    
    /**
     * Obtiene un mensaje según el contexto del procesamiento.
     * @param stage Etapa del procesamiento: "start", "waiting", "searching", "tools", "finishing"
     */
    fun getMessageForStage(stage: String): String = when (stage) {
        "start" -> getStartMessage()
        "waiting" -> getWaitingMessage()
        "searching" -> getSearchingMessage()
        "tools" -> getToolMessage()
        "finishing" -> getFinishingMessage()
        else -> getWaitingMessage()
    }
    
    /**
     * Obtiene una secuencia de mensajes para una conversación larga.
     * Útil para mostrar diferentes mensajes a lo largo del procesamiento.
     */
    fun getMessageSequence(count: Int): List<String> {
        val sequence = mutableListOf<String>()
        sequence.add(getStartMessage())
        
        repeat(maxOf(0, count - 2)) {
            sequence.add(getWaitingMessage())
        }
        
        if (count > 1) {
            sequence.add(getFinishingMessage())
        }
        
        return sequence
    }
    
    /**
     * Obtiene un mensaje motivacional para cuando la IA está procesando algo complejo.
     */
    fun getMotivationalMessage(): String = listOf(
        "Esto es interesante, déjame pensar...",
        "Buena pregunta, estoy en ello...",
        "Me encanta este tipo de tareas...",
        "Esto requiere atención, voy lento pero seguro...",
        "Déjame darle mi mejor esfuerzo...",
        "Esto es un reto interesante..."
    ).random(random)
}
