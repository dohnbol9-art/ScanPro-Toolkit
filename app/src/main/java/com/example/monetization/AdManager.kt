package com.example.monetization

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Date

/**
 * Manages Google AdMob initialization, lifecycle handling, App Open Ads,
 * and Interstitial Ads for Scan Pro Toolkit.
 */
class AdManager(private val application: Application) :
    Application.ActivityLifecycleCallbacks,
    DefaultLifecycleObserver {

    companion object {
        private const val TAG = "AdManager"

        // Production AdMob Ad Unit IDs
        const val ADMOB_APP_ID = "ca-app-pub-5393167223448532~1166515038"
        const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-5393167223448532/1338292460"
        const val BANNER_AD_UNIT_ID = "ca-app-pub-5393167223448532/2166281647"
        const val APP_OPEN_AD_UNIT_ID = "ca-app-pub-5393167223448532/9036501854"

        // Minimum time interval (45s) between interstitial ads to protect UX
        private const val MIN_INTERSTITIAL_INTERVAL_MS = 45_000L

        // Minimum time interval (60s) between App Open ads to avoid frequent popups
        private const val MIN_APP_OPEN_INTERVAL_MS = 60_000L

        // Minimum time in background (10s) before showing App Open ad on return
        private const val MIN_BACKGROUND_TIME_FOR_APP_OPEN_MS = 10_000L

        // Ad validity period: 4 hours
        private const val AD_EXPIRATION_HOURS = 4L
    }

    private val _isAdsEnabled = MutableStateFlow(true)
    val isAdsEnabled: StateFlow<Boolean> = _isAdsEnabled.asStateFlow()

    // Activity tracking for showing full-screen ads
    private var currentActivity: Activity? = null

    // State tracking to prevent duplicate displays & requests
    private var isShowingAd = false
    private var backgroundTimestamp = 0L

    // Interstitial Ad State
    private var interstitialAd: InterstitialAd? = null
    private var isInterstitialLoading = false
    private var lastInterstitialShownTime = 0L
    private var scanCountSinceLastAd = 0

    // App Open Ad State
    private var appOpenAd: AppOpenAd? = null
    private var isAppOpenLoading = false
    private var appOpenLoadTime = 0L
    private var lastAppOpenShownTime = 0L

    init {
        application.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        initializeMobileAds()
    }

    private fun initializeMobileAds() {
        try {
            MobileAds.initialize(application) { initStatus ->
                Log.d(TAG, "AdMob MobileAds SDK initialized: $initStatus")
                preloadAds()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing AdMob MobileAds", e)
        }
    }

    fun preloadAds() {
        if (!_isAdsEnabled.value) return
        loadInterstitial()
        loadAppOpenAd()
    }

    fun setPremiumStatus(isPro: Boolean) {
        _isAdsEnabled.value = !isPro
        if (isPro) {
            interstitialAd = null
            appOpenAd = null
            Log.d(TAG, "Premium enabled: AdMob ads disabled and cleared")
        } else {
            preloadAds()
        }
    }

    // =========================================================================
    // Interstitial Ad Logic
    // =========================================================================

    /**
     * Preloads the next Interstitial Ad safely.
     */
    fun loadInterstitial() {
        if (!_isAdsEnabled.value || interstitialAd != null || isInterstitialLoading) return

        isInterstitialLoading = true
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            application,
            INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isInterstitialLoading = false
                    Log.d(TAG, "AdMob Interstitial loaded successfully")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    interstitialAd = null
                    isInterstitialLoading = false
                    Log.w(TAG, "AdMob Interstitial failed to load: ${loadAdError.message} (Code: ${loadAdError.code})")
                }
            }
        )
    }

    /**
     * Records a user scan action (camera scan or gallery image decode).
     */
    fun recordScanAction() {
        scanCountSinceLastAd++
        if (interstitialAd == null && !isInterstitialLoading) {
            loadInterstitial()
        }
    }

    /**
     * Checks if natural break conditions are met for showing an interstitial ad.
     */
    fun canShowInterstitial(): Boolean {
        if (!_isAdsEnabled.value || isShowingAd) return false
        val now = System.currentTimeMillis()
        val intervalPassed = (now - lastInterstitialShownTime) >= MIN_INTERSTITIAL_INTERVAL_MS
        val enoughActions = scanCountSinceLastAd >= 1
        return intervalPassed && enoughActions && interstitialAd != null
    }

    /**
     * Shows the interstitial ad if ready, otherwise calls onDismissed immediately.
     * Never blocks or crashes the app.
     */
    fun showInterstitial(
        activity: Activity?,
        force: Boolean = false,
        onDismissed: () -> Unit = {}
    ) {
        val targetActivity = activity ?: currentActivity

        if (!_isAdsEnabled.value || targetActivity == null || isShowingAd) {
            onDismissed()
            return
        }

        val ad = interstitialAd
        val isEligible = force || canShowInterstitial()

        if (ad != null && isEligible) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    isShowingAd = true
                    Log.d(TAG, "AdMob Interstitial showed full screen")
                }

                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "AdMob Interstitial dismissed")
                    isShowingAd = false
                    interstitialAd = null
                    lastInterstitialShownTime = System.currentTimeMillis()
                    scanCountSinceLastAd = 0
                    loadInterstitial() // Automatically preload the next ad
                    onDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.w(TAG, "AdMob Interstitial failed to show: ${adError.message}")
                    isShowingAd = false
                    interstitialAd = null
                    loadInterstitial() // Reload on failure
                    onDismissed()
                }
            }
            ad.show(targetActivity)
        } else {
            // Not ready yet or throttled, trigger background load and continue seamlessly
            if (interstitialAd == null && !isInterstitialLoading) {
                loadInterstitial()
            }
            onDismissed()
        }
    }

    // =========================================================================
    // App Open Ad Logic
    // =========================================================================

    /**
     * Preloads the App Open Ad.
     */
    fun loadAppOpenAd() {
        if (!_isAdsEnabled.value || isAppOpenAdAvailable() || isAppOpenLoading) return

        isAppOpenLoading = true
        val adRequest = AdRequest.Builder().build()

        AppOpenAd.load(
            application,
            APP_OPEN_AD_UNIT_ID,
            adRequest,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    appOpenLoadTime = Date().time
                    isAppOpenLoading = false
                    Log.d(TAG, "AdMob App Open Ad loaded successfully")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    appOpenAd = null
                    isAppOpenLoading = false
                    Log.w(TAG, "AdMob App Open Ad failed to load: ${loadAdError.message} (Code: ${loadAdError.code})")
                }
            }
        )
    }

    /**
     * Check if App Open Ad is available and not expired (< 4 hours old).
     */
    fun isAppOpenAdAvailable(): Boolean {
        val ad = appOpenAd ?: return false
        val now = Date().time
        val ageMs = now - appOpenLoadTime
        val maxAgeMs = AD_EXPIRATION_HOURS * 60 * 60 * 1000L
        return ageMs < maxAgeMs
    }

    /**
     * Shows App Open Ad if conditions are met.
     */
    fun showAppOpenAdIfAvailable(activity: Activity?) {
        val targetActivity = activity ?: currentActivity ?: return

        if (!_isAdsEnabled.value || isShowingAd) {
            Log.d(TAG, "App Open Ad suppressed: Ads disabled or another ad currently showing")
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastAppOpenShownTime < MIN_APP_OPEN_INTERVAL_MS) {
            Log.d(TAG, "App Open Ad suppressed: Interval cooldown active")
            return
        }

        if (!isAppOpenAdAvailable()) {
            Log.d(TAG, "App Open Ad not ready or expired. Preloading now...")
            loadAppOpenAd()
            return
        }

        val ad = appOpenAd ?: return

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                isShowingAd = true
                Log.d(TAG, "AdMob App Open Ad showed full screen")
            }

            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "AdMob App Open Ad dismissed")
                isShowingAd = false
                appOpenAd = null
                lastAppOpenShownTime = System.currentTimeMillis()
                loadAppOpenAd() // Automatically preload the next App Open Ad
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.w(TAG, "AdMob App Open Ad failed to show: ${adError.message}")
                isShowingAd = false
                appOpenAd = null
                loadAppOpenAd()
            }
        }

        ad.show(targetActivity)
    }

    // =========================================================================
    // Process & Activity Lifecycle Callbacks
    // =========================================================================

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        val now = System.currentTimeMillis()
        val timeInBackground = if (backgroundTimestamp > 0L) now - backgroundTimestamp else 0L

        Log.d(TAG, "App foregrounded. Time spent in background: ${timeInBackground}ms")

        // Show App Open Ad when returning from background after a meaningful pause
        if (backgroundTimestamp == 0L || timeInBackground >= MIN_BACKGROUND_TIME_FOR_APP_OPEN_MS) {
            currentActivity?.let { activity ->
                showAppOpenAdIfAvailable(activity)
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        backgroundTimestamp = System.currentTimeMillis()
        Log.d(TAG, "App went to background")
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity === activity) {
            currentActivity = null
        }
    }
}
