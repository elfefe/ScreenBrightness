package com.elfefe.lowerbrightness

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action

        println("Alarm received: $action")

        if (action == ActionKeys.ACTION_START_OVERLAY) {
            val serviceIntent = Intent(context, OverlayService::class.java)
            context.startForegroundService(serviceIntent)
        } else if (action == ActionKeys.ACTION_STOP_OVERLAY) {
            val serviceIntent = Intent(context, OverlayService::class.java)
            context.stopService(serviceIntent)
        }
    }
}
