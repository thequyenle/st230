package com.dress.game.core.extensions

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.FontRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.FragmentActivity
import androidx.core.graphics.createBitmap
import com.dress.game.R
import com.dress.game.core.helper.RateHelper
import com.dress.game.core.helper.SharePreferenceHelper
import com.dress.game.core.helper.SoundHelper
import com.dress.game.core.utils.DataLocal
import com.dress.game.core.utils.state.RateState
import kotlin.math.roundToInt

// ----------------------------
// Visibility extensions
// ----------------------------
fun View.visible() { visibility = View.VISIBLE }
fun View.invisible() { visibility = View.INVISIBLE }
fun View.gone() { visibility = View.GONE }
fun View.select() { isSelected = true }


// ----------------------------
// Navigation / Transition
// ----------------------------
fun Activity.handleBackLeftToRight() {
    finish()
    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
}
fun Activity.handleBackRightToLeft() {
    finish()
    overridePendingTransition(R.anim.slide_out_right, R.anim.slide_in_left)
}
fun Context.handleBackFragmentFromRight() {
    if (this is FragmentActivity) {
        overridePendingTransition(R.anim.slide_out_left, R.anim.slide_in_right)
        supportFragmentManager.popBackStack()
    }
}
fun Activity.hideNavigation(isBlack: Boolean = true) {
    window?.setFlags(
        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
    )

    // Set status bar color
    window.statusBarColor = if (isBlack) {
        android.graphics.Color.TRANSPARENT
    } else {
        android.graphics.Color.BLACK
    }

    window.decorView.systemUiVisibility = if (isBlack) {
        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
    } else {
        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
    }
}
// ----------------------------
// Click utils
// ----------------------------
fun View.tap(interval: Long = 200, action: (View) -> Unit) {
    setOnClickListener {
        if (System.currentTimeMillis() - DataLocal.lastClickTime >= interval) {
            action(it)
            DataLocal.lastClickTime = System.currentTimeMillis()
        }
    }
}

fun View.tapWithSound(interval: Long = 500, action: (View) -> Unit) {
    setOnClickListener {
        if (System.currentTimeMillis() - DataLocal.lastClickTime >= interval) {
            SoundHelper.playSound(R.raw.touch)
            action(it)
            DataLocal.lastClickTime = System.currentTimeMillis()
        }
    }
}
fun Int.dp(context: Context): Int =
    (this * context.resources.displayMetrics.density).roundToInt()
fun View.setMargins(
    left: Int? = null,
    top: Int? = null,
    right: Int? = null,
    bottom: Int? = null
) {
    val params = layoutParams as ViewGroup.MarginLayoutParams
    params.setMargins(
        left ?: params.leftMargin,
        top ?: params.topMargin,
        right ?: params.rightMargin,
        bottom ?: params.bottomMargin
    )
    layoutParams = params
}
// ----------------------------
// UI Capture
// ----------------------------
@Throws(OutOfMemoryError::class)
fun createBitmapFromView(view: View): Bitmap {
    return try {
        val output = createBitmap(view.width, view.height)
        val canvas = Canvas(output)
        view.draw(canvas)
        output
    } catch (error: OutOfMemoryError) {
        throw error
    }
}

// ----------------------------
// TextView
// ----------------------------
fun TextView.setFont(@FontRes resId: Int) {
    typeface = ResourcesCompat.getFont(context, resId)
}

fun TextView.setTextContent(context: Context, resId: Int) {
    text = context.getString(resId)
}

fun Context.strings(resId: Int) : String {
    return getString(resId)
}
fun Context.strings(resId: Int, value: String) : String {
    return getString(resId, value)
}
fun setImageActionBar(imageView: ImageView, res: Int) {
    imageView.setImageResource(res)
    imageView.visible()
}

fun setTextActionBar(textView: TextView, text: String) {
    textView.text = text
    textView.visible()
    textView.visible()
}
