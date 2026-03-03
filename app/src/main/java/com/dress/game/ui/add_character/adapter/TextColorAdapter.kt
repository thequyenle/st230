package com.dress.game.ui.add_character.adapter

import android.annotation.SuppressLint
import android.graphics.drawable.GradientDrawable
import android.util.Log
import androidx.core.view.isVisible
import com.dress.game.R
import com.dress.game.core.base.BaseAdapter
import com.dress.game.core.extensions.gone
import com.dress.game.core.extensions.tap
import com.dress.game.core.extensions.visible
import com.dress.game.data.model.SelectedModel
import com.dress.game.databinding.ItemTextColorBinding

class TextColorAdapter : BaseAdapter<SelectedModel, ItemTextColorBinding>(ItemTextColorBinding::inflate) {
    var onChooseColorClick: (() -> Unit) = {}
    var onTextColorClick: ((Int, Int) -> Unit) = { _, _ -> }

    private var currentSelected = 1


    override fun onBind(binding: ItemTextColorBinding, item: SelectedModel, position: Int) {
        Log.d("TextColorAdapter", "onBind position=$position, color=${String.format("#%06X", 0xFFFFFF and item.color)}, isSelected=${item.isSelected}")

        binding.apply {
            vFocus.isVisible = item.isSelected
            // Ensure circular stroke for all positions (TextColorAdapter uses circles)
            vFocus.setBackgroundResource(R.drawable.bg_stroke_gradient_circle_color_text)

            if (position == 0) {
                Log.d("TextColorAdapter", "Position 0: Clearing and loading img0text_color")
                imvColor.visible()

                // Set margin to 0dp for position 0 to make it bigger
                val layoutParams = imvColor.layoutParams as android.widget.FrameLayout.LayoutParams
                layoutParams.setMargins(0, 0, 0, 0)
                imvColor.layoutParams = layoutParams

                // First clear any existing background drawable
                imvColor.background = null
                // Set background to transparent
                imvColor.setBackgroundColor(android.graphics.Color.TRANSPARENT)

                // Also clear the parent FrameLayout's background
                val parentFrame = imvColor.parent as? android.widget.FrameLayout
                parentFrame?.setBackgroundColor(android.graphics.Color.TRANSPARENT)

                Log.d("TextColorAdapter", "Position 0: Background set to TRANSPARENT (ImageView and parent), about to set image resource")
                // Now set the image resource
                imvColor.setImageResource(R.drawable.img0text_color)
                btnAddColor.visible()
                root.tap { onChooseColorClick.invoke() }
            } else {
                Log.d("TextColorAdapter", "Position $position: Setting color background")
                imvColor.visible()

                // Set margin to 2dp for other positions (keep normal size)
                val layoutParams = imvColor.layoutParams as android.widget.FrameLayout.LayoutParams
                val margin2dp = imvColor.context.resources.displayMetrics.density * 1
                layoutParams.setMargins(margin2dp.toInt(), margin2dp.toInt(), margin2dp.toInt(), margin2dp.toInt())
                imvColor.layoutParams = layoutParams

                imvColor.setImageResource(0) // Clear image resource
                btnAddColor.gone()

                // Ensure parent FrameLayout background is transparent
                val parentFrame = imvColor.parent as? android.widget.FrameLayout
                parentFrame?.setBackgroundColor(android.graphics.Color.TRANSPARENT)

                // Create circular drawable for color
                val drawable = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(item.color)
                }
                imvColor.background = drawable

                root.tap { onTextColorClick.invoke(item.color, position) }
            }
        }
    }

    fun submitItem(position: Int, list: ArrayList<SelectedModel>) {
        Log.d("TextColorAdapter", "submitItem called with position=$position")
        Log.d("TextColorAdapter", "Item at position 0: color=${String.format("#%06X", 0xFFFFFF and list[0].color)}, isSelected=${list[0].isSelected}")
        if (position == 0) {
            Log.d("TextColorAdapter", "WARNING: Position 0 was selected!")
        }

        items.clear()
        items.addAll(list)

        if (position != currentSelected) {
            Log.d("TextColorAdapter", "Notifying changes for positions $currentSelected and $position")
            notifyItemChanged(currentSelected)
            notifyItemChanged(position)
            currentSelected = position
        } else {
            Log.d("TextColorAdapter", "Notifying change for position $position")
            notifyItemChanged(position)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitListReset(list: ArrayList<SelectedModel>){
        items.clear()
        items.addAll(list)
        currentSelected = 1
        notifyDataSetChanged()
    }
}