package com.doey.agente

import android.content.Context
import com.doey.agente.iris.IrisClasificador

// ── Alias de paquete ─────────────────────────────────────────────────────────
// Permiten que ViewModelPrincipal y ServicioModoAmigable sigan usando
// LocalIntentProcessor.LocalAction, .IntentClass, etc. sin cambios.
typealias LocalAction  = IrisClasificador.LocalAction
typealias IntentClass  = IrisClasificador.IntentClass
typealias VolumeStream = IrisClasificador.VolumeStream
typealias SilentMode   = IrisClasificador.SilentMode
typealias InfoType     = IrisClasificador.InfoType

/**
 * ProcesadorIntencionLocal — Puente transparente hacia IRIS
 *
 * Ya no duplica tipos ni tiene mapeador. Todo vive en IrisClasificador.
 * Cuando añades una acción nueva en Iris, NO hay que tocar este archivo.
 */
object LocalIntentProcessor {

    fun classify(input: String): IrisClasificador.IntentClass =
        IrisClasificador.classify(input)

    fun greetingResponse(): String    = IrisClasificador.greetingResponse()
    fun farewellResponse(): String    = IrisClasificador.farewellResponse()
    fun gratitudeResponse(): String   = IrisClasificador.gratitudeResponse()
    fun affirmationResponse(): String = IrisClasificador.affirmationResponse()

    fun resolveContactNumber(context: Context, nameOrNumber: String): String? =
        IrisClasificador.resolveContactNumber(context, nameOrNumber)

    fun resolveContactFromMemory(memory: String, nameQuery: String): String? =
        IrisClasificador.resolveContactFromMemory(memory, nameQuery)

    fun buildOptimizedPrompt(subtasks: List<String>, originalInput: String): String =
        IrisClasificador.buildOptimizedPrompt(subtasks, originalInput)
}
