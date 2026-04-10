package com.doey.services

import android.content.Intent
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService
import android.util.Log
import com.doey.agente.SettingsStore
import com.doey.ui.MainActivity
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

private const val TAG = "DoeyAssistant"

/**
 * Servicio de asistente de voz del sistema.
 * Permite que Doey sea configurado como asistente predeterminado del dispositivo
 * (reemplazando a Google Assistant, Bixby, etc.).
 * 
 * Cuando el usuario invoca el asistente (botón home largo, "Hey Google", etc.),
 * este servicio lanza Doey en modo overlay o en pantalla completa según preferencia.
 */
class DoeyAssistantService : VoiceInteractionSessionService() {

    override fun onNewSession(args: Bundle?): VoiceInteractionSession {
        Log.i(TAG, "Nueva sesión de asistente iniciada")
        return DoeyVoiceSession(this)
    }
}

// FIX BUG-6: usar ctx en lugar de context para evitar ambigüedad con VoiceInteractionSession.context
class DoeyVoiceSession(private val ctx: android.content.Context) : VoiceInteractionSession(ctx) {

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        Log.i(TAG, "Sesión de voz mostrada, flags=$showFlags")

        // Leer ajustes para decidir qué modo usar
        MainScope().launch {
            val settings = SettingsStore(ctx)
            val friendlyEnabled = settings.getFriendlyModeEnabled()
            val contextRead     = settings.getFriendlyContextRead()

            // Obtener contexto de la app activa si está habilitado
            val appContext = if (contextRead) {
                DoeyAccessibilityService.instance?.getCurrentAppLabel() ?: ""
            } else ""

            if (friendlyEnabled && android.provider.Settings.canDrawOverlays(ctx)) {
                // ── Modo Friendly: mostrar barra inferior ────────────────────────────
                val friendlyIntent = Intent(ctx, FriendlyModeService::class.java).apply {
                    action = FriendlyModeService.ACTION_SHOW
                    putExtra(FriendlyModeService.EXTRA_CONTEXT_APP, appContext)
                }
                ctx.startForegroundService(friendlyIntent)
            } else {
                // ── Modo normal: abrir MainActivity ──────────────────────────────────
                val intent = Intent(ctx, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra("from_assistant", true)
                    putExtra("auto_listen", true)
                    putExtra("app_context", appContext)
                }
                ctx.startActivity(intent)
            }
        }

        // Ocultar la sesión del sistema inmediatamente
        hide()
    }

    override fun onHide() {
        super.onHide()
        Log.i(TAG, "Sesión de voz ocultada")
    }
}
