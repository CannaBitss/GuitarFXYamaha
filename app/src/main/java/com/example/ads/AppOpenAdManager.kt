package com.example.ads

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import java.util.Date

/**
 * Manages AdMob App Open Ads for GuitarFX Pro
 * Ad Unit: ca-app-pub-1254774552944426/6058380451
 */
class AppOpenAdManager(private val application: Application) :
    Application.ActivityLifecycleCallbacks {

    companion object {
        private const val TAG = "AppOpenAdManager"
        // User requested Ad Unit ID
        const val AD_UNIT_ID = "ca-app-pub-1254774552944426/6058380451"
        // Google official test Ad Unit ID for development fallback
        const val TEST_AD_UNIT_ID = "ca-app-pub-3940256099942544/9257395921"
    }

    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    var isShowingAd = false
    private var loadTime: Long = 0
    private var currentActivity: Activity? = null
    private var numStartedActivities = 0

    init {
        application.registerActivityLifecycleCallbacks(this)
        loadAd(application)
    }

    fun loadAd(context: Context) {
        if (isLoadingAd || isAdAvailable()) {
            return
        }

        isLoadingAd = true
        val request = AdRequest.Builder().build()

        AppOpenAd.load(
            context,
            AD_UNIT_ID,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    Log.d(TAG, "AdMob App Open Ad loaded successfully.")
                    appOpenAd = ad
                    isLoadingAd = false
                    loadTime = Date().time
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.w(TAG, "User ad unit failed to load (${loadAdError.code}: ${loadAdError.message}). Loading test fallback.")
                    isLoadingAd = false
                    loadFallbackAd(context)
                }
            }
        )
    }

    private fun loadFallbackAd(context: Context) {
        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            context,
            TEST_AD_UNIT_ID,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    Log.d(TAG, "Fallback App Open Ad loaded.")
                    appOpenAd = ad
                    loadTime = Date().time
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.w(TAG, "Fallback ad load failed: ${loadAdError.message}")
                }
            }
        )
    }

    private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
        val dateDifference: Long = Date().time - loadTime
        val numMilliSecondsPerHour: Long = 3600000
        return dateDifference < numMilliSecondsPerHour * numHours
    }

    private fun isAdAvailable(): Boolean {
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4)
    }

    fun showAdIfAvailable(activity: Activity, onShowAdCompleteListener: () -> Unit = {}) {
        if (isShowingAd) {
            Log.d(TAG, "App Open Ad is already displaying.")
            return
        }

        if (!isAdAvailable()) {
            Log.d(TAG, "App Open Ad not ready yet.")
            onShowAdCompleteListener()
            loadAd(activity)
            return
        }

        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "App Open Ad dismissed.")
                appOpenAd = null
                isShowingAd = false
                onShowAdCompleteListener()
                loadAd(activity)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "App Open Ad failed to show: ${adError.message}")
                appOpenAd = null
                isShowingAd = false
                onShowAdCompleteListener()
                loadAd(activity)
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "App Open Ad displayed on screen.")
                isShowingAd = true
            }
        }

        appOpenAd?.show(activity)
    }

    // ActivityLifecycleCallbacks
    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
        numStartedActivities++
        if (numStartedActivities == 1) {
            // App entered foreground from background/launch
            showAdIfAvailable(activity)
        }
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {
        numStartedActivities = (numStartedActivities - 1).coerceAtLeast(0)
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity == activity) {
            currentActivity = null
        }
    }
}
