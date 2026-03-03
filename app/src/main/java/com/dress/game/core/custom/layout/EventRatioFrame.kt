package com.dress.game.core.custom.layout

import android.widget.ImageView
import com.dress.game.core.custom.imageview.StrokeImageView

interface EventRatioFrame {
    fun onImageClick(image: StrokeImageView, btnEdit: ImageView)
}