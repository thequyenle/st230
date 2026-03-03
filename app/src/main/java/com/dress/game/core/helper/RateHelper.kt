package com.dress.game.core.helper

import android.app.Activity
import com.dress.game.R
import com.dress.game.core.extensions.hideNavigation

import com.dress.game.core.utils.state.RateState
import com.dress.game.dialog.RateDialog
import com.google.android.gms.tasks.Task
import com.google.android.play.core.review.ReviewManagerFactory
import java.lang.Void
import kotlin.system.exitProcess

object RateHelper {

    fun showRateDialog(
        activity: Activity,
        preference: SharePreferenceHelper,
        onRateResult: (RateState) -> Unit = {}
    ) {
        val dialogRate = RateDialog(activity)
        LanguageHelper.setLocale(activity)
        dialogRate.show()

        dialogRate.onRateLess3 = {
            preference.setIsRate(true)
            dialogRate.dismiss()
            activity.hideNavigation()
            onRateResult(RateState.LESS3)
        }

        dialogRate.onRateGreater3 = {
            preference.setIsRate(true)
            reviewApp(activity, false)
            dialogRate.dismiss()
            activity.hideNavigation()
            onRateResult(RateState.GREATER3)
        }

        dialogRate.onCancel = {
            dialogRate.dismiss()
            activity.hideNavigation()
            onRateResult(RateState.CANCEL)
        }
    }

    fun reviewApp(context: Activity, isBackPress: Boolean) {
        val manager = ReviewManagerFactory.create(context)
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                val flow = (context as Activity?)?.let { manager.launchReviewFlow(it, reviewInfo) }
                flow?.addOnCompleteListener { task2: Task<Void> ->
                    if (isBackPress) {
                        exitProcess(0)
                    }
                }
            } else {
                if (isBackPress) {
                    exitProcess(0)
                }
            }
        }
    }
}