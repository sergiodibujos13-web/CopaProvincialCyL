package com.example.futbolmanager

import android.app.Activity
import android.content.Context
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

// IDs de PRUEBA de AdMob. Sustituyelos por los tuyos antes de publicar.
const val BANNER_ID = "ca-app-pub-6910178285236368/9857266490"
const val REWARDED_ID = "ca-app-pub-6910178285236368/5614769151"

object Ads {
    fun init(ctx: Context) {
        MobileAds.initialize(ctx) {}
    }

    fun showRewarded(activity: Activity, onReward: () -> Unit, onFail: () -> Unit) {
        RewardedAd.load(activity, REWARDED_ID, AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    onFail()
                }

                override fun onAdLoaded(ad: RewardedAd) {
                    var rewarded = false
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            if (rewarded) onReward() else onFail()
                        }

                        override fun onAdFailedToShowFullScreenContent(error: AdError) {
                            onFail()
                        }
                    }
                    ad.show(activity) { rewarded = true }
                }
            })
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
