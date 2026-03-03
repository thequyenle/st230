package com.dress.game.ui.pride.adapter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import com.dress.game.core.base.BaseAdapter
import com.dress.game.core.extensions.tap
import com.dress.game.data.model.pride.PrideFlagModel
import com.dress.game.databinding.ItemSelectedFlagChipBinding

class SelectedFlagChipAdapter(
    private val context: Context,
    private val onRemoveClick: (PrideFlagModel) -> Unit
) : BaseAdapter<PrideFlagModel, ItemSelectedFlagChipBinding>(
    ItemSelectedFlagChipBinding::inflate
) {
    private val colorCache = mutableMapOf<Int, Int>()

    override fun onBind(binding: ItemSelectedFlagChipBinding, item: PrideFlagModel, position: Int) {
        binding.apply {
            tvChipName.text = item.name

            val color = colorCache.getOrPut(item.id) { getDominantColor(item) }
            val drawable = GradientDrawable()
            drawable.shape = GradientDrawable.OVAL
            drawable.setColor(color)
            dotColor.background = drawable

            btnRemoveChip.tap { onRemoveClick(item) }
        }
    }

    private fun getDominantColor(item: PrideFlagModel): Int {
        if (!item.customColors.isNullOrEmpty()) return item.customColors[0]
        return try {
            val stream = context.assets.open(item.assetPath)
            val bitmap = BitmapFactory.decodeStream(stream)
            stream.close()
            val scaled = Bitmap.createScaledBitmap(bitmap, 1, 1, true)
            val color = scaled.getPixel(0, 0)
            bitmap.recycle()
            scaled.recycle()
            color
        } catch (e: Exception) {
            Color.GRAY
        }
    }
}
