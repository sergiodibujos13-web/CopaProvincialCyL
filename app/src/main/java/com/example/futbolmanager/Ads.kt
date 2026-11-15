package com.example.futbolmanager

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

const val BANNER_ID = "ca-app-pub-6910178285236368/9857266490"
const val REWARDED_ID = "ca-app-pub-6910178285236368/5614769151"

object Ads {
    private var rewarded: RewardedAd? = null
    private var loading = false

    fun init(ctx: Context) {
        MobileAds.initialize(ctx) { preload(ctx) }
    }

    // Carga el vídeo con premio por adelantado para que salga al instante al pulsar el botón
    fun preload(ctx: Context) {
        if (rewarded != null || loading) return
        loading = true
        val app = ctx.applicationContext
        RewardedAd.load(app, REWARDED_ID, AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    loading = false
                    rewarded = null
                    Handler(Looper.getMainLooper()).postDelayed({ preload(app) }, 60000)
                }

                override fun onAdLoaded(ad: RewardedAd) {
                    loading = false
                    rewarded = ad
                }
            })
    }

    fun showRewarded(activity: Activity, onReward: () -> Unit, onFail: () -> Unit) {
        val ad = rewarded
        if (ad == null) {
            preload(activity)
            onFail()
            return
        }
        rewarded = null
        var earned = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                if (earned) onReward() else onFail()
                preload(activity)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                onFail()
                preload(activity)
            }
        }
        ad.show(activity) { earned = true }
    }
}

@Composable
fun BannerAd() {
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = BANNER_ID
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
