package com.tserumula.dbcleanerforwhatsapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log

class ScheduleReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        Log.d("Alarm Bell", "Alarm just fired. starting service")
        val serviceIntent = Intent(context, ScheduleService::class.java)
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, javaClass.canonicalName)
        wakelock.acquire(5*60*1000L /*5 minutes*/)
        SleepLock.instance.setMyWakeLock(wakelock)
        context.startService(serviceIntent)
    }
}