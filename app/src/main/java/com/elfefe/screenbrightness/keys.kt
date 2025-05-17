package com.elfefe.screenbrightness

object SharedPreferenceKeys {
    const val APP_PREFS = "AppPrefs"
    const val SCHEDULE_PREFS = "SchedulePrefs"

    const val OVERLAY_ENABLED = "OVERLAY_ENABLED"

    const val CURRENT_BRIGHTNESS = "CURRENT_BRIGHTNESS"
    const val CURRENT_BRIGHTNESS_STEP = "CURRENT_BRIGHTNESS_STEP"
    const val CURRENT_SCHEDULE = "CURRENT_SCHEDULE"
    const val CURRENT_COLOR = "CURRENT_COLOR"


    const val HOUR = "hour"
    const val MINUTE = "minute"
    const val DAYS_OF_WEEK = "daysOfWeek"
    const val IS_SCHEDULED = "isScheduled"
}

object ActionKeys {
    const val TOGGLE_OVERLAY = "TOGGLE_OVERLAY"
    const val ADJUST_BRIGHTNESS = "ADJUST_BRIGHTNESS"
    const val ADJUST_BRIGHTNESS_STEP = "ADJUST_BRIGHTNESS_STEP"
    const val ADJUST_COLOR = "ADJUST_COLOR"
    const val REDUCE_BRIGHTNESS = "REDUCE_BRIGHTNESS"
    const val INCREASE_BRIGHTNESS = "INCREASE_BRIGHTNESS"
    const val ACTION_START_OVERLAY = "ACTION_START_OVERLAY"
    const val ACTION_STOP_OVERLAY = "ACTION_STOP_OVERLAY"
}

object BroadcastKeys {
    const val BROADCAST_BRIGHTNESS = "BROADCAST_BRIGHTNESS"
    const val BROADCAST_OVERLAY_STATE = "CAST_OVERLAY_STATE"
}

object IntentKeys {
    const val UPDATE_COLOR = "UPDATE_COLOR"
    const val UPDATE_BRIGHTNESS = "UPDATE_BRIGHTNESS"
    const val BRIGHTNESS_LEVEL = "BRIGHTNESS_LEVEL"
    const val BRIGHTNESS_STEP = "BRIGHTNESS_STEP"
    const val OVERLAY_STATE = "OVERLAY_STATE"
}