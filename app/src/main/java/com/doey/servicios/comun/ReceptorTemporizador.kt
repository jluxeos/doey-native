package com.doey.servicios.comun

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra("timer_id") ?: return
        TimerEngine.onAlarmFired(context, id)
    }
}
