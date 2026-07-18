package com.cycling.sonic

import android.app.Application
import com.cycling.core.ui.theme.M3ThemeManager
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class SonicApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        M3ThemeManager.initialize(this)
        
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
