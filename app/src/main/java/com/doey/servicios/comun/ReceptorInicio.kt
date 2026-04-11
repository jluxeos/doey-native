package com.doey.servicios.comun

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        Log.d("BootReceiver", "$action → re-scheduling")
        SchedulerEngine.reScheduleAll(context)
        TimerEngine.reScheduleAll(context)
    }
}
