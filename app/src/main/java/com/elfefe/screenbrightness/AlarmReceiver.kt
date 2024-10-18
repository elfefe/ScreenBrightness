package com.elfefe.screenbrightness

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action

        if (action == ActionKeys.ACTION_START_OVERLAY) {
            val serviceIntent = Intent(context, OverlayService::class.java)
            serviceIntent.action = ActionKeys.ACTION_START_OVERLAY
            context.startForegroundService(serviceIntent)
        } else if (action == ActionKeys.ACTION_STOP_OVERLAY) {
            val serviceIntent = Intent(context, OverlayService::class.java)
            serviceIntent.action = ActionKeys.ACTION_STOP_OVERLAY
            context.stopService(serviceIntent)
        }
    }
}
