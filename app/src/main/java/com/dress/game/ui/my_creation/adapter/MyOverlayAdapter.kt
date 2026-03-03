package com.dress.game.ui.my_creation.adapter

import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.signature.ObjectKey
import com.dress.game.R
import com.dress.game.core.base.BaseAdapter
import com.dress.game.core.extensions.gone
import com.dress.game.core.extensions.tap
import com.dress.game.core.extensions.visible
import com.dress.game.data.model.MyAlbumModel
import com.dress.game.databinding.ItemOverlayBinding
import java.io.File

class MyOverlayAdapter : BaseAdapter<MyAlbumModel, ItemOverlayBinding>(ItemOverlayBinding::inflate) {
    var onItemClick: ((String) -> Unit) = {}
    var onLongClick: ((Int) -> Unit) = {}
    var onItemTick: ((Int) -> Unit) = {}
    var onDeleteClick: ((String) -> Unit) = {}

    override fun areItemsTheSame(oldItem: MyAlbumModel, newItem: MyAlbumModel) = oldItem.path == newItem.path
    override fun areContentsTheSame(oldItem: MyAlbumModel, newItem: MyAlbumModel) = oldItem == newItem

    override fun onBind(binding: ItemOverlayBinding, item: MyAlbumModel, position: Int) {
        binding.apply {
            val file = File(item.path)
            Glide.with(root.context)
                .load(file)
                .thumbnail(0.1f)
                .override(400, 400)
                .transform(RoundedCorners(24))
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .signature(ObjectKey(file.lastModified()))
                .into(imvImage)

            if (item.isShowSelection) {
                btnSelect.visible()
                btnDelete.gone()
            } else {
                btnSelect.gone()
                btnDelete.visible()
            }

            btnSelect.setImageResource(
                if (item.isSelected) R.drawable.ic_selected else R.drawable.ic_not_select
            )

            root.tap { onItemClick.invoke(item.path) }
            root.setOnLongClickListener {
                if (items.any { it.isShowSelection }) false
                else { onLongClick.invoke(position); true }
            }
            btnDelete.tap { onDeleteClick.invoke(item.path) }
            btnSelect.tap { onItemTick.invoke(position) }
        }
    }
}
