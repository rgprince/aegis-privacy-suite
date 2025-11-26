package com.aegis.privacy

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * AEGIS Privacy Suite Application class.
 * Initializes Hilt dependency injection and Timber logging.
 */
@HiltAndroidApp
class AegisApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Timber logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        
        // Global Exception Handler
        val oldHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Timber.e(throwable, "CRASH: Uncaught exception in thread ${thread.name}")
            oldHandler?.uncaughtException(thread, throwable)
        }
        
        Timber.i("AEGIS Privacy Suite initialized")
        
        // Initialize database on first launch
        val prefs = getSharedPreferences("aegis_prefs", MODE_PRIVATE)
        val isFirstLaunch = prefs.getBoolean("first_launch", true)
        
        if (isFirstLaunch) {
            Timber.i("First launch detected, initializing database...")
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                com.aegis.privacy.util.DatabaseInitializer.initialize(this@AegisApplication)
                prefs.edit().putBoolean("first_launch", false).apply()
            }
        }
    }
}
