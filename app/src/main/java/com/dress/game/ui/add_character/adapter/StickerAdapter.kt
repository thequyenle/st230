package com.dress.game.ui.add_character.adapter

import com.dress.game.core.base.BaseAdapter
import com.dress.game.core.extensions.loadImage
import com.dress.game.core.extensions.loadImageSticker
import com.dress.game.core.extensions.tap
import com.dress.game.data.model.SelectedModel
import com.dress.game.databinding.ItemStickerBinding

class StickerAdapter : BaseAdapter<SelectedModel, ItemStickerBinding>(ItemStickerBinding::inflate) {
    var onItemClick : ((String) -> Unit) = {}
    override fun onBind(binding: ItemStickerBinding, item: SelectedModel, position: Int) {
        binding.apply {
            loadImageSticker(root, item.path, imvSticker)
            root.tap { onItemClick.invoke(item.path) }
        }
    }
}