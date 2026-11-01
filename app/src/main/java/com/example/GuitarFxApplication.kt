package com.example

import android.app.Application
import android.util.Log
import com.example.ads.AppOpenAdManager
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GuitarFxApplication : Application() {

    lateinit var appOpenAdManager: AppOpenAdManager
        private set

    override fun onCreate() {
        super.onCreate()

        // Initialize AdMob SDK in background
        CoroutineScope(Dispatchers.IO).launch {
            try {
                MobileAds.initialize(this@GuitarFxApplication) { status ->
                    Log.d("GuitarFxApp", "MobileAds initialized: $status")
                }
            } catch (e: Exception) {
                Log.e("GuitarFxApp", "MobileAds initialization failed", e)
            }
        }

        appOpenAdManager = AppOpenAdManager(this)
    }
}
