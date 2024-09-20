package com.elfefe.lowerbrightness

data class Schedule(
    val hour: Int,
    val minute: Int,
    val daysOfWeek: Set<Int>,
    val isScheduled: Boolean
)
