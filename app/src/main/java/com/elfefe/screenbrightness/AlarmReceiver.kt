package com.elfefe.screenbrightness

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * BroadcastReceiver that handles alarms for starting and stopping the [OverlayService].
 */
class AlarmReceiver : BroadcastReceiver() {
    /**
     * Called when the BroadcastReceiver is receiving an Intent broadcast.
     * Starts or stops the [OverlayService] based on the received action.
     *
     * @param context The Context in which the receiver is running.
     * @param intent The Intent being received.
     */
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action

        println("AlarmReceiver: $action")

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
