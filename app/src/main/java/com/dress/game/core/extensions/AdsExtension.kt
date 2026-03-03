package com.dress.game.core.extensions

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.nativead.NativeAd
import com.lvt.ads.callback.InterCallback
import com.lvt.ads.callback.NativeCallback
import com.lvt.ads.event.AdmobEvent
import com.lvt.ads.util.Admob
import com.dress.game.core.helper.UnitHelper

private const val SHOW_ADS = false

fun Activity.showInterAll(onFinishInter: () -> Unit) {
    if (!SHOW_ADS) { onFinishInter.invoke(); return }
    Admob.getInstance().showInterAll(this, object : InterCallback() {
        override fun onNextAction() {
            super.onNextAction()
            onFinishInter.invoke()
        }
    })
}

fun Activity.showInterAll() {
    if (!SHOW_ADS) return
    Admob.getInstance().showInterAll(this, object : InterCallback() {
        override fun onNextAction() {
            super.onNextAction()

        }
    })
}

fun Activity.showInterAllWait(onFinishInter: () -> Unit) {
    if (!SHOW_ADS) { onFinishInter.invoke(); return }
    Admob.getInstance().setOpenActivityAfterShowInterAds(false)
    Admob.getInstance().showInterAll(this, object : InterCallback() {
        override fun onNextAction() {
            super.onNextAction()
            Admob.getInstance().setOpenActivityAfterShowInterAds(true)
            onFinishInter.invoke()
        }
    })
}

fun Activity.loadNativeAds(id: String, nativeValue: ((NativeAd?) -> Unit) = {}){
    if (!SHOW_ADS) { nativeValue.invoke(null); return }
    Admob.getInstance().loadNativeAd(this, id, object : NativeCallback() {
        override fun onNativeAdLoaded(nativeAd: NativeAd?) {
            super.onNativeAdLoaded(nativeAd)
            nativeValue.invoke(nativeAd)
        }
    })
}
fun Activity.loadNativeCollabAds(id: String, layout: FrameLayout) {
    if (!SHOW_ADS) return
    Admob.getInstance().loadNativeCollap(this, id, layout)
}

fun Activity.loadNativeCollabAds(id: Int, layout: FrameLayout) {
    if (!SHOW_ADS) return
    Admob.getInstance().loadNativeCollap(this, getString(id), layout)
}

fun Activity.loadNativeCollabAds(
    id: Any,
    layout: FrameLayout,
    view: View,
    bottomFailed: Int = 0,
    bottomLoadSuccess: Int = 82,
) {
    if (!SHOW_ADS) return
    val marginBottom = UnitHelper.pxToDpInt(this, bottomFailed)
    val marginBottomLoadSuccess = UnitHelper.pxToDpInt(this, bottomLoadSuccess)
    val adsId = when(id){
        is String -> id
        is Int -> getString(id)
        else -> return
    }
    val params = view.layoutParams as ViewGroup.MarginLayoutParams

    Admob.getInstance().loadNativeCollap(this, adsId, layout, object : NativeCallback() {
        override fun onAdFailedToLoad() {
            super.onAdFailedToLoad()
            params.bottomMargin = marginBottom
            view.layoutParams = params
        }

        override fun onNativeAdLoaded(nativeAd: NativeAd?) {
            super.onNativeAdLoaded(nativeAd)
            params.bottomMargin = marginBottomLoadSuccess
            view.layoutParams = params
        }

    })
}

fun Activity.loadNativeCollabAds(
    id: Int,
    layout: FrameLayout,
    view: View,
    bottomFailed: Int = 0,
    bottomLoadSuccess: Int = 82,
) {
    if (!SHOW_ADS) return
    val marginBottom = UnitHelper.pxToDpInt(this, bottomFailed)
    val marginBottomLoadSuccess = UnitHelper.pxToDpInt(this, bottomLoadSuccess)

    Admob.getInstance().loadNativeCollap(this, getString(id), layout, object : NativeCallback() {
        override fun onAdFailedToLoad() {
            super.onAdFailedToLoad()
            val params = view.layoutParams as ViewGroup.MarginLayoutParams
            params.bottomMargin = marginBottom
            view.layoutParams = params
        }

        override fun onNativeAdLoaded(nativeAd: NativeAd?) {
            super.onNativeAdLoaded(nativeAd)
            val params = view.layoutParams as ViewGroup.MarginLayoutParams
            params.bottomMargin = marginBottomLoadSuccess
            view.layoutParams = params
        }

    })
}

fun AppCompatActivity.loadSplashInterAds(id: String, timeOut: Long, timeDelay: Long, interCallback: InterCallback?) {
    if (!SHOW_ADS) { interCallback?.onNextAction(); return }
    Admob.getInstance().loadSplashInterAds(this, id, timeOut, timeDelay, interCallback)
}

fun AppCompatActivity.checkShowSplashWhenFail(interCallback: InterCallback?, delay: Int) {
    if (!SHOW_ADS) return
    Admob.getInstance().onCheckShowSplashWhenFail(this, interCallback, delay)
}

fun Activity.logEvent(nameEvent: String){
    AdmobEvent.logEvent(this, nameEvent, null)
}