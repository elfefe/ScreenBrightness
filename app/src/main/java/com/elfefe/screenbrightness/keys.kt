package com.elfefe.screenbrightness

/**
 * Object containing constant keys for SharedPreferences.
 */
object SharedPreferenceKeys {
    /** Name of the main shared preferences file. */
    const val APP_PREFS = "AppPrefs"
    /** Name of the shared preferences file for schedule settings. */
    const val SCHEDULE_PREFS = "SchedulePrefs"

    /** Key for storing whether the overlay is enabled. */
    const val OVERLAY_ENABLED = "OVERLAY_ENABLED"

    /** Key for storing the current brightness level. */
    const val CURRENT_BRIGHTNESS = "CURRENT_BRIGHTNESS"
    /** Key for storing the current brightness adjustment step. */
    const val CURRENT_BRIGHTNESS_STEP = "CURRENT_BRIGHTNESS_STEP"
    /** Key for storing the current schedule configuration. */
    const val CURRENT_SCHEDULE = "CURRENT_SCHEDULE"
    /** Key for storing the current overlay color. */
    const val CURRENT_COLOR = "CURRENT_COLOR"

    /** Key for storing the hour of a scheduled event. */
    const val HOUR = "hour"
    /** Key for storing the minute of a scheduled event. */
    const val MINUTE = "minute"
    /** Key for storing the days of the week for a scheduled event. */
    const val DAYS_OF_WEEK = "daysOfWeek"
    /** Key for storing whether a schedule is active. */
    const val IS_SCHEDULED = "isScheduled"
}

/**
 * Object containing constant string keys for Intent actions.
 */
object ActionKeys {
    /** Action to toggle the overlay on or off. */
    const val TOGGLE_OVERLAY = "TOGGLE_OVERLAY"
    /** Action to adjust the brightness level. */
    const val ADJUST_BRIGHTNESS = "ADJUST_BRIGHTNESS"
    /** Action to adjust the brightness step. */
    const val ADJUST_BRIGHTNESS_STEP = "ADJUST_BRIGHTNESS_STEP"
    /** Action to adjust the overlay color. */
    const val ADJUST_COLOR = "ADJUST_COLOR"
    /** Action to reduce the brightness. */
    const val REDUCE_BRIGHTNESS = "REDUCE_BRIGHTNESS"
    /** Action to increase the brightness. */
    const val INCREASE_BRIGHTNESS = "INCREASE_BRIGHTNESS"
    /** Action to start the overlay service (typically via alarm). */
    const val ACTION_START_OVERLAY = "ACTION_START_OVERLAY"
    /** Action to stop the overlay service (typically via alarm). */
    const val ACTION_STOP_OVERLAY = "ACTION_STOP_OVERLAY"
}

/**
 * Object containing constant string keys for Broadcast actions.
 */
object BroadcastKeys {
    /** Broadcast action for brightness updates. */
    const val BROADCAST_BRIGHTNESS = "BROADCAST_BRIGHTNESS"
    /** Broadcast action for overlay state updates. */
    const val BROADCAST_OVERLAY_STATE = "CAST_OVERLAY_STATE"
}

/**
 * Object containing constant string keys for Intent extras.
 */
object IntentKeys {
    /** Intent extra key for updating the color. */
    const val UPDATE_COLOR = "UPDATE_COLOR"
    /** Intent extra key for updating the brightness. */
    const val UPDATE_BRIGHTNESS = "UPDATE_BRIGHTNESS"
    /** Intent extra key for the brightness level. */
    const val BRIGHTNESS_LEVEL = "BRIGHTNESS_LEVEL"
    /** Intent extra key for the brightness step. */
    const val BRIGHTNESS_STEP = "BRIGHTNESS_STEP"
    /** Intent extra key for the overlay state. */
    const val OVERLAY_STATE = "OVERLAY_STATE"
}