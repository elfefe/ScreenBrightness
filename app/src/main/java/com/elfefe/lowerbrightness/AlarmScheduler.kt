package com.elfefe.lowerbrightness

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.Message
import java.util.*

object AlarmScheduler {
    fun scheduleOverlay(
        context: Context,
        hour: Int,
        minute: Int,
        daysOfWeek: Set<Int>, // Days of week as Calendar constants (e.g., Calendar.MONDAY)
        enable: Boolean // true to schedule start, false to schedule stop
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        for (day in daysOfWeek) {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                set(Calendar.DAY_OF_WEEK, day)

                // If the time has already passed for today, schedule for next week
                if (before(Calendar.getInstance())) {
                    add(Calendar.WEEK_OF_YEAR, 1)
                }
            }

            val action = if (enable) ActionKeys.ACTION_START_OVERLAY else ActionKeys.ACTION_STOP_OVERLAY
            val requestCode = if (enable) day else day + 1000 // Differentiate start and stop alarms

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                this.action = action
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                "",
                object: AlarmManager.OnAlarmListener {
                    override fun onAlarm() {
                        println("Alarm received: $action")
                    }
                },
                Handler(Looper.getMainLooper(), object: Handler.Callback {
                    override fun handleMessage(msg: Message): Boolean {
                        println("Alarm scheduled: $action at ${calendar.time}")
                        return true
                    }

                })
            )
        }
    }

    fun cancelScheduledOverlay(
        context: Context,
        daysOfWeek: Set<Int>,
        enable: Boolean
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        for (day in daysOfWeek) {
            val action = if (enable) ActionKeys.ACTION_START_OVERLAY else ActionKeys.ACTION_STOP_OVERLAY
            val requestCode = if (enable) day else day + 1000

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                this.action = action
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.cancel(pendingIntent)
        }
    }
}
