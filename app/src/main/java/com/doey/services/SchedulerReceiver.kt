package com.doey.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class SchedulerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra("schedule_id") ?: return
        SchedulerEngine.onAlarmFired(context, id)
    }
}
