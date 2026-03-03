package com.dress.game.core.extensions

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import androidx.core.app.ShareCompat
import androidx.core.net.toUri
import com.dress.game.core.helper.RateHelper
import com.dress.game.core.helper.SharePreferenceHelper
import com.dress.game.core.utils.state.RateState

fun Activity.shareApp() {
    ShareCompat.IntentBuilder.from(this).setType("text/plain").setChooserTitle("Chooser title")
        .setText("http://play.google.com/store/apps/details?id=" + (this).packageName)
        .startChooser()
}

fun Activity.policy() {
    val url = "https://sites.google.com/view/pony-oc-pony-avatar-maker/home"
    val i = Intent(Intent.ACTION_VIEW)
    i.data = url.toUri()
    startActivity(i)
}
fun Activity.rateApp(
    sharePreference: SharePreferenceHelper,
    onRateResult: (RateState) -> Unit = {}
) {
    RateHelper.showRateDialog(this, sharePreference, onRateResult)
}
