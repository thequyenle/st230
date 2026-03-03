package com.dress.game.ui.pride.adapter

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.dress.game.core.base.BaseAdapter
import com.dress.game.core.extensions.tap
import com.dress.game.databinding.ItemCustomColorBinding

class CustomColorAdapter(
    private val onColorClick: (Int) -> Unit,
    private val onRemoveClick: (Int) -> Unit
) : BaseAdapter<Int, ItemCustomColorBinding>(
    ItemCustomColorBinding::inflate
) {
    override fun submitList(list: List<Int>) {
        val oldSize = items.size
        val newSize = list.size
        items.clear()
        items.addAll(list)
        when {
            newSize < oldSize -> {
                notifyItemRangeRemoved(newSize, oldSize - newSize)
                if (newSize > 0) notifyItemRangeChanged(0, newSize)
            }
            newSize > oldSize -> {
                if (oldSize > 0) notifyItemRangeChanged(0, oldSize)
                notifyItemRangeInserted(oldSize, newSize - oldSize)
            }
            else -> notifyItemRangeChanged(0, newSize)
        }
    }

    override fun onBind(binding: ItemCustomColorBinding, item: Int, position: Int) {
        binding.apply {
            tvColorLabel.text = "Color ${position + 1}"
            tvColorHex.text = String.format("#%06X", 0xFFFFFF and item)

            // Set swatch color
            val drawable = GradientDrawable()
            drawable.shape = GradientDrawable.RECTANGLE
            drawable.cornerRadius = 12f
            drawable.setColor(item)
            colorSwatch.background = drawable

            btnRemoveColor.visibility = if (itemCount == 1) View.INVISIBLE else View.VISIBLE
            colorSwatch.tap { onColorClick(position) }
            btnRemoveColor.tap { onRemoveClick(position) }
        }
    }
}
