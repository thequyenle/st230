package com.dress.game.ui.add_character.adapter

import android.util.Log
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.dress.game.R
import com.dress.game.core.base.BaseAdapter
import com.dress.game.core.extensions.tap
import com.dress.game.data.model.SelectedModel
import com.dress.game.databinding.ItemBackgroundColorBinding

class BackgroundColorAdapter :
    BaseAdapter<SelectedModel, ItemBackgroundColorBinding>(ItemBackgroundColorBinding::inflate) {
    var onChooseColorClick: (() -> Unit) = {}
    var onBackgroundColorClick: ((Int, Int) -> Unit) = {_,_ ->}

    var currentSelected = -1
    override fun onBind(binding: ItemBackgroundColorBinding, item: SelectedModel, position: Int) {
        Log.d("BackgroundColorAdapter", "onBind position=$position, color=${String.format("#%06X", 0xFFFFFF and item.color)}, isSelected=${item.isSelected}, path=${item.path}")

        binding.apply {
            vFocus.isVisible = item.isSelected
            // Set circular stroke for position 0, regular stroke for others
            if (position == 0) {
                vFocus.setBackgroundResource(R.drawable.bg_stroke_gradient_circle)
            } else {
                vFocus.setBackgroundResource(R.drawable.bg_stroke_gradient_circle)
            }

            if (position == 0) {
                Log.d("BackgroundColorAdapter", "Position 0: Clearing and loading img.png")
                // First clear Glide to stop any pending loads
                Glide.with(root.context).clear(imvColor)
                // Clear any existing drawable
                imvColor.setImageDrawable(null)
                // Clear background completely
                imvColor.background = null
                // Set background to transparent
                imvColor.setBackgroundColor(android.graphics.Color.TRANSPARENT)

                // Also clear the parent MaterialCardView's background color
                val cardView = imvColor.parent as? com.google.android.material.card.MaterialCardView
                cardView?.setCardBackgroundColor(android.graphics.Color.TRANSPARENT)

                Log.d("BackgroundColorAdapter", "Position 0: Background set to TRANSPARENT (ImageView and CardView), about to load image")
                // Now load the image
                val radiusPx = (4 * root.context.resources.displayMetrics.density).toInt()
                Glide.with(root.context)
                    .load(R.drawable.img)
                    .apply(RequestOptions.bitmapTransform(RoundedCorners(radiusPx)))
                    .into(imvColor)
                root.tap { onChooseColorClick.invoke() }
            } else {
                Log.d("BackgroundColorAdapter", "Position $position: Setting color background")
                // Clear Glide image and set background color on ImageView
                Glide.with(root.context).clear(imvColor)
                imvColor.setImageDrawable(null)
                imvColor.setBackgroundColor(item.color)

                // Ensure CardView background is also transparent for color positions
                val cardView = imvColor.parent as? com.google.android.material.card.MaterialCardView
                cardView?.setCardBackgroundColor(android.graphics.Color.TRANSPARENT)

                root.tap { onBackgroundColorClick.invoke(item.color, position) }
            }
        }
    }

    fun submitItem(position: Int, list: ArrayList<SelectedModel>){
        Log.d("BackgroundColorAdapter", "submitItem called with position=$position")
        Log.d("BackgroundColorAdapter", "Item at position 0: color=${String.format("#%06X", 0xFFFFFF and list[0].color)}, isSelected=${list[0].isSelected}")
        if (position == 0) {
            Log.d("BackgroundColorAdapter", "WARNING: Position 0 was selected!")
        }

        items.clear()
        items.addAll(list)

        if (position != currentSelected){
            Log.d("BackgroundColorAdapter", "Notifying changes for positions $currentSelected and $position")
            notifyItemChanged(currentSelected)
            notifyItemChanged(position)
            currentSelected = position
        } else {
            Log.d("BackgroundColorAdapter", "Notifying change for position $position")
            notifyItemChanged(position)
        }
    }
}