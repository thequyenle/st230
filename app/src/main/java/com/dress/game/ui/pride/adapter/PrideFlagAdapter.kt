package com.dress.game.ui.pride.adapter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import com.dress.game.core.base.BaseAdapter
import com.dress.game.core.extensions.gone
import com.dress.game.core.extensions.visible
import com.dress.game.data.model.pride.PrideFlagModel
import com.dress.game.databinding.ItemPrideFlagBinding

class PrideFlagAdapter(
    private val context: Context,
    private val onFlagClick: (PrideFlagModel) -> Unit
) : BaseAdapter<PrideFlagModel, ItemPrideFlagBinding>(
    ItemPrideFlagBinding::inflate
) {

    private var maxReached = false

    fun setMaxReached(reached: Boolean) {
        maxReached = reached
        notifyDataSetChanged()
    }

    override fun onBind(binding: ItemPrideFlagBinding, item: PrideFlagModel, position: Int) {
        binding.apply {
            tvFlagName.text = item.name

            // Load flag image: from assets or from custom colors
            if (item.assetPath.isNotEmpty()) {
                try {
                    val inputStream = context.assets.open(item.assetPath)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    imgFlag.setImageBitmap(bitmap)
                    inputStream.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else if (!item.customColors.isNullOrEmpty()) {
                imgFlag.setImageBitmap(buildCustomFlagBitmap(item.customColors))
            }

            // Checkbox state
            imgCheckbox.setImageResource(
                if (item.isSelected) com.dress.game.R.drawable.ic_selected
                else com.dress.game.R.drawable.ic_not_select
            )

            // Card background
            cardFlag.setCardBackgroundColor(
                if (item.isSelected)
                    android.graphics.Color.parseColor("#F3EEFF")
                else
                    android.graphics.Color.WHITE
            )

            // Dim overlay when max reached and not selected
            if (maxReached && !item.isSelected) {
                dimOverlay.visible()
            } else {
                dimOverlay.gone()
            }

            root.setOnClickListener {
                if (!maxReached || item.isSelected) {
                    onFlagClick(item)
                }
            }
        }
    }

    private fun buildCustomFlagBitmap(colors: List<Int>): Bitmap {
        val size = 200
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        val stripeHeight = size.toFloat() / colors.size
        colors.forEachIndexed { i, color ->
            paint.color = color
            canvas.drawRect(0f, i * stripeHeight, size.toFloat(), (i + 1) * stripeHeight, paint)
        }
        return bitmap
    }
}
