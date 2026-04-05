package com.doey.tools

import android.service.notification.StatusBarNotification

// Clase simple para manejar notificaciones
class NotificationListenerTool {

    fun processNotification(sbn: StatusBarNotification) {
        // Solo imprime algo por ahora
        println("Notificación recibida: ${sbn.packageName} - ${sbn.notification.tickerText}")
    }

    fun clearNotifications() {
        // Función ejemplo
        println("Notificaciones borradas")
    }
}