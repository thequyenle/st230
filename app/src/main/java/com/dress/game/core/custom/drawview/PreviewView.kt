package com.dress.game.core.custom.drawview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class PreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var baseBitmap: Bitmap? = null
    var userBitmap: Bitmap? = null
    var clipRadius: Float = 0f   // in bitmap-space pixels (previewSize=400)
    var userOffsetX: Float = 0f  // in view pixels
    var userOffsetY: Float = 0f  // in view pixels
    var overlayBitmap: Bitmap? = null  // ring overlay drawn on top of user image

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val clipPath = Path()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w == 0f || h == 0f) return

        val base = baseBitmap ?: return
        canvas.drawBitmap(base, null, RectF(0f, 0f, w, h), paint)

        val user = userBitmap ?: return
        val scaleFactor = w / base.width.toFloat()
        val userW = user.width * scaleFactor
        val userH = user.height * scaleFactor
        val left = (w - userW) / 2f + userOffsetX
        val top = (h - userH) / 2f + userOffsetY

        canvas.save()
        if (clipRadius > 0f) {
            clipPath.reset()
            clipPath.addCircle(w / 2f, h / 2f, clipRadius * scaleFactor, Path.Direction.CW)
            canvas.clipPath(clipPath)
        }
        canvas.drawBitmap(user, null, RectF(left, top, left + userW, top + userH), paint)
        canvas.restore()

        val overlay = overlayBitmap
        if (overlay != null) {
            canvas.drawBitmap(overlay, null, RectF(0f, 0f, w, h), paint)
        }
    }

    fun resetOffset() {
        userOffsetX = 0f
        userOffsetY = 0f
        invalidate()
    }
}
