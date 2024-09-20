package com.elfefe.lowerbrightness

import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.*

@Composable
fun ScheduleScreen(context: Context) {
    var time by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedDays by remember { mutableStateOf(setOf<Int>()) }
    var isScheduled by remember { mutableStateOf(false) }
    val sharedPreferences = context.getSharedPreferences("SchedulePrefs", Context.MODE_PRIVATE)

    val daysOfWeek = listOf(
        Calendar.MONDAY to "Monday",
        Calendar.TUESDAY to "Tuesday",
        Calendar.WEDNESDAY to "Wednesday",
        Calendar.THURSDAY to "Thursday",
        Calendar.FRIDAY to "Friday",
        Calendar.SATURDAY to "Saturday",
        Calendar.SUNDAY to "Sunday"
    )

    // Load saved schedule
    LaunchedEffect(Unit) {
        val hour = sharedPreferences.getInt("hour", time.get(Calendar.HOUR_OF_DAY))
        val minute = sharedPreferences.getInt("minute", time.get(Calendar.MINUTE))
        val daysSet =
            sharedPreferences.getStringSet("daysOfWeek", emptySet())?.map { it.toInt() }?.toSet()
                ?: emptySet()
        isScheduled = sharedPreferences.getBoolean("isScheduled", false)
        time.set(Calendar.HOUR_OF_DAY, hour)
        time.set(Calendar.MINUTE, minute)
        selectedDays = daysSet
    }


    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Set Overlay Schedule", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        // Time Picker
        Button(onClick = {
            val hour = time.get(Calendar.HOUR_OF_DAY)
            val minute = time.get(Calendar.MINUTE)
            TimePickerDialog(context, { _, selectedHour, selectedMinute ->
                time.set(Calendar.HOUR_OF_DAY, selectedHour)
                time.set(Calendar.MINUTE, selectedMinute)
            }, hour, minute, true).show()
        }) {
            Text(
                text = "Select Time: ${
                    String.format(
                        "%02d:%02d",
                        time.get(Calendar.HOUR_OF_DAY),
                        time.get(Calendar.MINUTE)
                    )
                }"
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Days of Week Selection
        Text(text = "Select Days:")
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
                    .padding(8.dp)
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
            verticalArrangement = Arrangement.Bottom
        ) {
            // Schedule Button
            Button(onClick = {
                if (isScheduled) {
                    // Cancel existing schedule
                    AlarmScheduler.cancelScheduledOverlay(
                        context,
                        selectedDays,
                        enable = false
                    )

                    // Clear saved schedule
                    sharedPreferences.edit().clear().apply()
                } else {
                    // Schedule overlay start and stop
                    AlarmScheduler.scheduleOverlay(
                        context = context,
                        hour = time.get(Calendar.HOUR_OF_DAY),
                        minute = time.get(Calendar.MINUTE),
                        daysOfWeek = selectedDays,
                        enable = true // Start overlay
                    )

                    // Save schedule
                    sharedPreferences.edit().apply {
                        putInt("hour", time.get(Calendar.HOUR_OF_DAY))
                        putInt("minute", time.get(Calendar.MINUTE))
                        putStringSet(
                            "daysOfWeek",
                            selectedDays.map { it.toString() }.toSet()
                        )
                        putBoolean("isScheduled", true)
                        apply()
                    }
                }
                isScheduled = !isScheduled
            }) {
                Text(text = if (isScheduled) "Cancel Schedule" else "Set Schedule")
            }
        }
    }
}

