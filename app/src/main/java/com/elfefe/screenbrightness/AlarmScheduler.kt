package com.elfefe.screenbrightness

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.*

/**
 * Object responsible for scheduling and canceling alarms for starting and stopping the overlay service.
 */
object AlarmScheduler {
    /**
     * Schedules an alarm to start or stop the overlay service at a specific time on given days of the week.
     *
     * @param context The application context.
     * @param hour The hour of the day to schedule the alarm (0-23).
     * @param minute The minute of the hour to schedule the alarm (0-59).
     * @param daysOfWeek A set of [Calendar] constants representing the days of the week for the alarm (e.g., [Calendar.MONDAY]).
     * @param enable True to schedule an alarm to start the overlay, false to schedule an alarm to stop it.
     */
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
                pendingIntent
            )
        }
    }

    /**
     * Cancels a previously scheduled alarm for starting or stopping the overlay service.
     *
     * @param context The application context.
     * @param daysOfWeek A set of [Calendar] constants representing the days of the week for which to cancel the alarm.
     * @param enable True to cancel a start overlay alarm, false to cancel a stop overlay alarm.
     */
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
