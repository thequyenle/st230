package com.dress.game.listener.listenerdraw

import android.view.MotionEvent
import com.dress.game.core.custom.drawview.DrawView


class EditEvent : DrawEvent {
    override fun onActionDown(tattooView: DrawView?, event: MotionEvent?) {}
    override fun onActionMove(tattooView: DrawView?, event: MotionEvent?) {}
    override fun onActionUp(tattooView: DrawView?, event: MotionEvent?) {
        if (!tattooView!!.isLocking()) {
            tattooView.editText()
        }
    }
}
