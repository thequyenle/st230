package com.dress.game.ui.intro

import android.content.Context
import com.dress.game.core.base.BaseAdapter
import com.dress.game.core.extensions.loadImage
import com.dress.game.core.extensions.select
import com.dress.game.core.extensions.strings
import com.dress.game.data.model.IntroModel
import com.dress.game.databinding.ItemIntroBinding

class IntroAdapter(val context: Context) : BaseAdapter<IntroModel, ItemIntroBinding>(
    ItemIntroBinding::inflate
) {
    override fun onBind(binding: ItemIntroBinding, item: IntroModel, position: Int) {
        binding.apply {
            loadImage(root, item.image, imvImage, false)
            tvContent.text = context.strings(item.content)
            tvContent.select()
        }
    }
}