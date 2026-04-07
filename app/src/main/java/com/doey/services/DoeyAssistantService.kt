package com.doey.services

import android.content.Intent
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService
import android.util.Log
import com.doey.ui.MainActivity

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

class DoeyVoiceSession(context: android.content.Context) : VoiceInteractionSession(context) {

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        Log.i(TAG, "Sesión de voz mostrada, flags=$showFlags")
        
        // Lanzar MainActivity con flag de asistente
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("from_assistant", true)
            putExtra("auto_listen", true)
        }
        context.startActivity(intent)
        
        // También mostrar overlay si está disponible
        if (android.provider.Settings.canDrawOverlays(context)) {
            val overlayIntent = Intent(context, DoeyOverlayService::class.java).apply {
                action = DoeyOverlayService.ACTION_SHOW
            }
            context.startForegroundService(overlayIntent)
        }
        
        hide()
    }

    override fun onHide() {
        super.onHide()
        Log.i(TAG, "Sesión de voz ocultada")
    }
}
