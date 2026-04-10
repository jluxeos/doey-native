package com.doey.services

import android.os.Bundle
import android.service.voice.VoiceInteractionService

/**
 * Fix #1: Wrapper raíz requerido por Android para registrar un asistente.
 *
 * Android exige DOS clases separadas:
 *  1. VoiceInteractionService  → punto de entrada del sistema (esta clase)
 *  2. VoiceInteractionSessionService → crea la sesión (DoeyAssistantService)
 *
 * El Manifest registra ESTA clase con el intent-filter VoiceInteractionService.
 * El XML interaction_service.xml apunta a DoeyAssistantService como sessionService.
 */
class DoeyVoiceInteractionService : VoiceInteractionService() {

    override fun onReady() {
        super.onReady()
        android.util.Log.i("DoeyVIS", "VoiceInteractionService listo")
    }

    override fun onShutdown() {
        super.onShutdown()
        android.util.Log.i("DoeyVIS", "VoiceInteractionService apagado")
    }
}
