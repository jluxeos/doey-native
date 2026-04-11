package com.doey.servicios.comun

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.doey.agente.SettingsStore
import com.doey.servicios.basico.FriendlyModeService
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        
        Log.d("BootReceiver", "$action → re-scheduling and checking auto-start")
        
        SchedulerEngine.reScheduleAll(context)
        TimerEngine.reScheduleAll(context)

        // Implementación de auto-inicio para Modo Friendly
        MainScope().launch {
            val settings = SettingsStore(context)
            if (settings.getFriendlyModeEnabled() && settings.getAutoStartFriendly()) {
                if (android.provider.Settings.canDrawOverlays(context)) {
                    val friendlyIntent = Intent(context, FriendlyModeService::class.java).apply {
                        this.action = FriendlyModeService.ACTION_SHOW
                    }
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        context.startForegroundService(friendlyIntent)
                    } else {
                        context.startService(friendlyIntent)
                    }
                }
            }
        }
    }
}
