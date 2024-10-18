package com.elfefe.screenbrightness

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.util.Calendar

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            // Retrieve saved schedule
            val sharedPreferences = context.getSharedPreferences(SharedPreferenceKeys.SCHEDULE_PREFS, Context.MODE_PRIVATE)
            val isScheduled = sharedPreferences.getBoolean("isScheduled", false)

            if (isScheduled) {
                val hour = sharedPreferences.getInt("hour", 22)
                val minute = sharedPreferences.getInt("minute", 0)
                val daysSet = sharedPreferences.getStringSet("daysOfWeek", emptySet())?.map { it.toInt() }?.toSet() ?: emptySet()

                // Reschedule overlay start and stop
                AlarmScheduler.scheduleOverlay(
                    context = context,
                    hour = hour,
                    minute = minute,
                    daysOfWeek = daysSet,
                    enable = true // Start overlay
                )

                val stopTime = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    add(Calendar.HOUR_OF_DAY, 8)
                }

                AlarmScheduler.scheduleOverlay(
                    context = context,
                    hour = stopTime.get(Calendar.HOUR_OF_DAY),
                    minute = stopTime.get(Calendar.MINUTE),
                    daysOfWeek = daysSet,
                    enable = false // Stop overlay
                )
            }
        }
    }
}
