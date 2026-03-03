package com.dress.game.ui.my_creation.adapter

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import com.dress.game.R
import com.dress.game.core.base.BaseAdapter
import com.dress.game.core.extensions.gone
import com.dress.game.core.extensions.tap
import com.dress.game.core.extensions.visible
import com.dress.game.data.model.MyAlbumModel
import com.dress.game.databinding.ItemMyAlbumBinding
import java.io.File

class MyAvatarAdapter(val context: Context) :
    BaseAdapter<MyAlbumModel, ItemMyAlbumBinding>(ItemMyAlbumBinding::inflate) {
    var onItemClick: ((String) -> Unit) = {}
    var onLongClick: ((Int) -> Unit) = {}
    var onItemTick: ((Int) -> Unit) = {}

    var onEditClick: ((String) -> Unit) = {}
    var onDeleteClick: ((String) -> Unit) = {}

    var isSelectMode: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                // Only notify item changes if needed, not full refresh
                notifyItemRangeChanged(0, itemCount)
            }
        }

    // DiffUtil optimization
    override fun areItemsTheSame(oldItem: MyAlbumModel, newItem: MyAlbumModel): Boolean {
        return oldItem.path == newItem.path
    }

    override fun areContentsTheSame(oldItem: MyAlbumModel, newItem: MyAlbumModel): Boolean {
        return oldItem == newItem
    }

    override fun onBind(binding: ItemMyAlbumBinding, item: MyAlbumModel, position: Int) {
        binding.apply {
            // Optimized Glide loading with thumbnail, size override, and caching
            val file = File(item.path)
            Glide.with(context)
                .load(file)
                .thumbnail(0.1f) // Load 10% quality thumbnail first
                .override(400, 400) // Resize to save memory
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .signature(ObjectKey(file.lastModified())) // Cache invalidation
                .into(imvImage)

            if (item.isShowSelection) {
                btnSelect.visible()
                btnEdit.gone()
                btnDelete.gone()
            } else {
                btnSelect.gone()
                btnEdit.visible()
                btnDelete.visible()
            }

            if (item.isSelected) {
                btnSelect.setImageResource(R.drawable.ic_selected)
            } else {
                btnSelect.setImageResource(R.drawable.ic_not_select)
            }

            root.tap { onItemClick.invoke(item.path) }

            root.setOnLongClickListener {
                if (items.any { album -> album.isShowSelection }) {
                    return@setOnLongClickListener false
                } else {
                    onLongClick.invoke(position)
                    return@setOnLongClickListener true
                }
            }
            btnEdit.tap { onEditClick.invoke(item.path) }
            btnDelete.tap { onDeleteClick.invoke(item.path) }
            btnSelect.tap { onItemTick.invoke(position) }
        }
    }
}