package com.dress.game.listener.listenerdraw

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import androidx.annotation.IntDef
import com.dress.game.core.custom.drawview.DrawView
import com.dress.game.core.utils.key.DrawKey
import com.dress.game.data.model.draw.DrawableDraw
import androidx.core.graphics.toColorInt

class BitmapDrawIcon(drawable: Drawable?, @Gravity gravity: Int) : DrawableDraw(drawable!!, "nbhieu"),
    DrawEvent {
    @IntDef(*[DrawKey.TOP_LEFT, DrawKey.RIGHT_TOP, DrawKey.LEFT_BOTTOM, DrawKey.RIGHT_BOTTOM])
    @Retention(AnnotationRetention.SOURCE)
    annotation class Gravity

    var radius = DrawKey.DEFAULT_RADIUS
    var x = 0f
    var y = 0f

    @get:Gravity
    @Gravity
    var positionDefault = DrawKey.TOP_LEFT
    var event: DrawEvent? = null

    init {
        positionDefault = gravity
    }

    override fun onActionDown(tattooView: DrawView?, event: MotionEvent?) {
        if (this.event != null) {
            this.event!!.onActionDown(tattooView, event)
        }
    }

    override fun onActionMove(tattooView: DrawView?, event: MotionEvent?) {
        if (this.event != null) {
            this.event!!.onActionMove(tattooView, event)
        }
    }

    override fun onActionUp(tattooView: DrawView?, event: MotionEvent?) {
        if (this.event != null) {
            this.event!!.onActionUp(tattooView, event)
        }
    }


    fun draw(canvas: Canvas, paint: Paint) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 5f
        paint.color = "#4EABDA".toColorInt()
        super.draw(canvas)
    }

}

