package com.elfefe.screenbrightness

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.Dispatchers

@Composable
fun MainActivity.AdsScreen() {
    var adRequest = AdRequest.Builder().build()

    var rewardedAd: RewardedAd? by remember { mutableStateOf(null) }

    LaunchedEffect(rewardedAd) {
        RewardedAd.load(this@AdsScreen,"ca-app-pub-9774733986813136/6732079908", adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                println(adError.toString())
                rewardedAd = null
            }

            override fun onAdLoaded(ad: RewardedAd) {
                println("Ad was loaded.")
                rewardedAd = ad
            }
        })
    }

    LaunchedEffect(rewardedAd) {
        rewardedAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
            override fun onAdClicked() {
                // Called when a click is recorded for an ad.
                println("Ad was clicked.")
            }

            override fun onAdDismissedFullScreenContent() {
                // Called when ad is dismissed.
                // Set the ad reference to null so you don't show the ad a second time.
                println("Ad dismissed fullscreen content.")
                rewardedAd = null
            }

            override fun onAdImpression() {
                // Called when an impression is recorded for an ad.
                println("Ad recorded an impression.")
            }

            override fun onAdShowedFullScreenContent() {
                // Called when ad is shown.
                println("Ad showed fullscreen content.")
            }
        }
    }

    LaunchedEffect(rewardedAd, Dispatchers.Main) {
        rewardedAd?.let { ad ->
            ad.show(this@AdsScreen, OnUserEarnedRewardListener { rewardItem ->
                // Handle the reward.
                val rewardAmount = rewardItem.amount
                val rewardType = rewardItem.type
                println("User earned the reward.")
            })
        } ?: run {
            println("The rewarded ad wasn't ready yet.")
        }
    }
}