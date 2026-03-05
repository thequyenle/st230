package com.dress.game.ui.add_character.adapter

import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.dress.game.core.base.BaseAdapter
import com.dress.game.core.extensions.gone
import com.dress.game.core.extensions.loadImage
import com.dress.game.core.extensions.select
import com.dress.game.core.extensions.tap
import com.dress.game.core.extensions.visible
import com.dress.game.data.model.SelectedModel
import com.dress.game.databinding.ItemBackgroundImageBinding

class BackgroundImageAdapter :
    BaseAdapter<SelectedModel, ItemBackgroundImageBinding>(ItemBackgroundImageBinding::inflate) {
    var onAddImageClick: (() -> Unit) = {}
    var onBackgroundImageClick: ((String, Int) -> Unit) = {_,_ ->}
    var currentSelected = -1

    override fun onBind(binding: ItemBackgroundImageBinding, item: SelectedModel, position: Int) {
        binding.apply {
            vFocus.isVisible = item.isSelected
            if (position == 0) {
                lnlAddItem.visible()
                imvImage.gone()
                lnlAddItem.tap(500) { onAddImageClick.invoke() }
            } else {
                lnlAddItem.gone()
                imvImage.visible()
                // Load image with 8dp rounded corners
                val cornerRadiusPx = (2 * root.context.resources.displayMetrics.density).toInt()
                Glide.with(root)
                    .load(item.path)
                    .transform(RoundedCorners(cornerRadiusPx))
                    .into(imvImage)
                imvImage.tap { onBackgroundImageClick.invoke(item.path, position) }
            }
        }
    }

    fun submitItem(position: Int, list: ArrayList<SelectedModel>){
        if (position != currentSelected){
            items.clear()
            items.addAll(list)

            notifyItemChanged(currentSelected)
            notifyItemChanged(position)

            currentSelected = position
        }
    }
}