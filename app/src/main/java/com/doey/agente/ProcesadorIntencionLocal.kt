package com.doey.agente

import android.content.Context
import com.doey.agente.iris.IrisClasificador

/**
 * ProcesadorIntencionLocal — Puente transparente hacia IRIS
 *
 * ╔══════════════════════════════════════════════════════════════════════╗
 * ║  Este archivo YA NO duplica los tipos de Iris.                      ║
 * ║  Todos los tipos (LocalAction, IntentClass, etc.) viven en          ║
 * ║  IrisClasificador.kt y se usan directamente aquí.                   ║
 * ║                                                                      ║
 * ║  Cuando añades una acción nueva en Iris, NO tienes que tocar        ║
 * ║  nada en este archivo. Solo edita los 3 archivos de la carpeta      ║
 * ║  iris/ y listo.                                                      ║
 * ╚══════════════════════════════════════════════════════════════════════╝
 *
 * El resto del proyecto (ViewModelPrincipal, ServicioModoAmigable, etc.)
 * llama a este objeto igual que antes — la API pública no cambió.
 */
object LocalIntentProcessor {

    // Alias de tipos — el resto del proyecto puede seguir usando
    // LocalIntentProcessor.LocalAction, .IntentClass, etc. sin cambios.
    typealias LocalAction  = IrisClasificador.LocalAction
    typealias IntentClass  = IrisClasificador.IntentClass
    typealias VolumeStream = IrisClasificador.VolumeStream
    typealias SilentMode   = IrisClasificador.SilentMode
    typealias InfoType     = IrisClasificador.InfoType

    // ── Clasificador principal ────────────────────────────────────────────────
    fun classify(input: String): IntentClass = IrisClasificador.classify(input)

    // ── Respuestas sociales ───────────────────────────────────────────────────
    fun greetingResponse(): String    = IrisClasificador.greetingResponse()
    fun farewellResponse(): String    = IrisClasificador.farewellResponse()
    fun gratitudeResponse(): String   = IrisClasificador.gratitudeResponse()
    fun affirmationResponse(): String = IrisClasificador.affirmationResponse()

    // ── Contactos y utilidades ────────────────────────────────────────────────
    fun resolveContactNumber(context: Context, nameOrNumber: String): String? =
        IrisClasificador.resolveContactNumber(context, nameOrNumber)

    fun resolveContactFromMemory(memory: String, nameQuery: String): String? =
        IrisClasificador.resolveContactFromMemory(memory, nameQuery)

    fun buildOptimizedPrompt(subtasks: List<String>, originalInput: String): String =
        IrisClasificador.buildOptimizedPrompt(subtasks, originalInput)
}
