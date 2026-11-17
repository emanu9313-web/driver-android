package com.example.driver

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TaxiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
