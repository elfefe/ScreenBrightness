package com.elfefe.screenbrightness.views

import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.elfefe.screenbrightness.AlarmScheduler
import com.elfefe.screenbrightness.MainActivity
import com.elfefe.screenbrightness.R
import com.elfefe.screenbrightness.SharedPreferenceKeys
import java.util.*

@Composable
fun MainActivity.ScheduleScreen() {
    var time by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedDays by remember { mutableStateOf(setOf<Int>()) }
    var isScheduled by remember { mutableStateOf(false) }
    val sharedPreferences =
        getSharedPreferences(SharedPreferenceKeys.SCHEDULE_PREFS, Context.MODE_PRIVATE)

    val daysOfWeek = listOf(
        Calendar.MONDAY to stringResource(R.string.monday),
        Calendar.TUESDAY to stringResource(R.string.tuesday),
        Calendar.WEDNESDAY to stringResource(R.string.wednesday),
        Calendar.THURSDAY to stringResource(R.string.thursday),
        Calendar.FRIDAY to stringResource(R.string.friday),
        Calendar.SATURDAY to stringResource(R.string.saturday),
        Calendar.SUNDAY to stringResource(R.string.sunday)
    )

    // Load saved schedule
    LaunchedEffect(Unit) {
        val hour =
            sharedPreferences.getInt(SharedPreferenceKeys.HOUR, time.get(Calendar.HOUR_OF_DAY))
        val minute =
            sharedPreferences.getInt(SharedPreferenceKeys.MINUTE, time.get(Calendar.MINUTE))
        val daysSet =
            sharedPreferences.getStringSet(SharedPreferenceKeys.DAYS_OF_WEEK, emptySet())
                ?.map { it.toInt() }?.toSet()
                ?: emptySet()
        isScheduled = sharedPreferences.getBoolean(SharedPreferenceKeys.IS_SCHEDULED, false)
        time.set(Calendar.HOUR_OF_DAY, hour)
        time.set(Calendar.MINUTE, minute)
        selectedDays = daysSet
    }


    Column {
        Text(text = stringResource(R.string.select_time))
        Spacer(modifier = Modifier.height(16.dp))

        // Time Picker
        Button(onClick = {
            val hour = time.get(Calendar.HOUR_OF_DAY)
            val minute = time.get(Calendar.MINUTE)
            TimePickerDialog(this@ScheduleScreen, { _, selectedHour, selectedMinute ->
                time.set(Calendar.HOUR_OF_DAY, selectedHour)
                time.set(Calendar.MINUTE, selectedMinute)
            }, hour, minute, true).show()
        }) {
            Text(
                text = String.format(
                    Locale.getDefault(),
                    "%02d:%02d",
                    time.get(Calendar.HOUR_OF_DAY),
                    time.get(Calendar.MINUTE)
                )
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Days of Week Selection
        Text(text = stringResource(R.string.select_days))
        daysOfWeek.forEach { (day, label) ->
            val isSelected = selectedDays.contains(day)
            Row(
                Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = isSelected,
                        onValueChange = {
                            selectedDays = if (isSelected) {
                                selectedDays - day
                            } else {
                                selectedDays + day
                            }
                        }
                    )
                    .padding(8.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = null // Handled by Row's toggleable
                )
                Text(text = label, modifier = Modifier.padding(8.dp))
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Schedule Button
            Button(
                contentPadding = PaddingValues(16.dp),
                onClick = {
                    if (isScheduled) {
                        // Cancel existing schedule
                        AlarmScheduler.cancelScheduledOverlay(
                            this@ScheduleScreen,
                            selectedDays,
                            enable = false
                        )

                        // Clear saved schedule
                        sharedPreferences.edit()
                            .putBoolean(SharedPreferenceKeys.IS_SCHEDULED, false).apply()
                    } else {
                        // Schedule overlay start and stop
                        AlarmScheduler.scheduleOverlay(
                            context = this@ScheduleScreen,
                            hour = time.get(Calendar.HOUR_OF_DAY),
                            minute = time.get(Calendar.MINUTE),
                            daysOfWeek = selectedDays,
                            enable = true // Start overlay
                        )

                        // Save schedule
                        sharedPreferences.edit().apply {
                            putInt(SharedPreferenceKeys.HOUR, time.get(Calendar.HOUR_OF_DAY))
                            putInt(SharedPreferenceKeys.MINUTE, time.get(Calendar.MINUTE))
                            putStringSet(
                                SharedPreferenceKeys.DAYS_OF_WEEK,
                                selectedDays.map { it.toString() }.toSet()
                            )
                            putBoolean(SharedPreferenceKeys.IS_SCHEDULED, true)
                            apply()
                        }
                    }
                    isScheduled = !isScheduled
                }) {
                Text(
                    text = if (isScheduled) stringResource(R.string.cancel_schedule) else stringResource(
                        R.string.set_schedule
                    )
                )
            }
        }
    }
}

