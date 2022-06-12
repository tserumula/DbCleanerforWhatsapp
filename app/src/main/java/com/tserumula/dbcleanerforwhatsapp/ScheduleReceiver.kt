package com.tserumula.dbcleanerforwhatsapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ScheduleReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        Log.d("Alarm Bell", "Alarm just fired. starting service")
        val serviceIntent = Intent(context, ScheduleService::class.java)
        context.startService(serviceIntent)
    }
}