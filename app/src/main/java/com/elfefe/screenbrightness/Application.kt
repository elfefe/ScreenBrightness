package com.elfefe.screenbrightness

import android.app.Application

class Application: Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        Thread.setDefaultUncaughtExceptionHandler(object: Thread.UncaughtExceptionHandler {
            override fun uncaughtException(t: Thread, e: Throwable) {

            }
        })
    }

    companion object {
        lateinit var instance: com.elfefe.screenbrightness.Application
    }
}