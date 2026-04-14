package com.doey.agente

import android.content.Context
import com.doey.agente.iris.IrisClasificador

/**
 * ProcesadorIntencionLocal — Wrapper de compatibilidad para IRIS v6
 *
 * El motor IRIS ha sido modularizado en:
 *   com.doey.agente.iris.IrisDiccionario   → sinónimos y vocabulario
 *   com.doey.agente.iris.IrisMotor         → funciones match*() por dominio
 *   com.doey.agente.iris.IrisClasificador  → tipos, guardas, orquestación
 *
 * Este objeto mantiene la API pública original para que todo el código
 * existente siga compilando sin cambios.
 */
object LocalIntentProcessor {

    // ── Tipos re-exportados ───────────────────────────────────────────────────
    typealias IntentClass   = IrisClasificador.IntentClass
    typealias LocalAction   = IrisClasificador.LocalAction
    typealias VolumeStream  = IrisClasificador.VolumeStream
    typealias SilentMode    = IrisClasificador.SilentMode
    typealias InfoType      = IrisClasificador.InfoType

    // ── API pública delegada ──────────────────────────────────────────────────

    fun classify(input: String): IntentClass =
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
