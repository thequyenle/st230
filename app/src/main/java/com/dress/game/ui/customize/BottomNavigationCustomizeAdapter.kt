package com.dress.game.ui.customize

import android.content.Context
import android.graphics.Color
import android.graphics.Outline
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dress.game.R
import android.graphics.drawable.Drawable
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.facebook.shimmer.ShimmerDrawable
import com.dress.game.core.extensions.dp
import com.dress.game.core.extensions.setMargins
import com.dress.game.core.extensions.tap
import com.dress.game.core.utils.DataLocal
import com.dress.game.data.model.custom.NavigationModel
import com.dress.game.databinding.ItemBottomNavigationBinding

class BottomNavigationCustomizeAdapter(private val context: Context) :
    ListAdapter<NavigationModel, BottomNavigationCustomizeAdapter.BottomNavViewHolder>(DiffCallback) {
    var onItemClick: (Int) -> Unit = {}




    inner class BottomNavViewHolder(
        private val binding: ItemBottomNavigationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: NavigationModel, position: Int) = with(binding) {

            // Apply circular clipping to cvContent (so shimmer/image fills circle and doesn't overflow)
            cvContent.clipToOutline = true
            cvContent.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }

            // Apply circular clipping to imvImage (shimmer layer - fills full circle)
            imvImage.clipToOutline = true
            imvImage.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }

            // Apply circular clipping to imvImageBG (actual image layer - with margin)
            imvImageBG.clipToOutline = true
            imvImageBG.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }

            val offset = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                15f,
                cvContent.resources.displayMetrics
            )

            // Cancel any running animations to prevent jumps when recycling views
            cvContent.animate().cancel()

            if (item.isSelected) {
                imvImage.setBackgroundColor(Color.TRANSPARENT)
                cvContent.setBackgroundResource(R.drawable.bg_select_navi_shape)

                // Use translationY for visual effect without affecting layout
                cvContent.translationZ = 0f
                cvContent.translationY = -offset

            } else {
                // Use same bottom margin as selected to maintain consistent height
                binding.main.setMargins(0, 15.dp(context), 8.dp(context), 15.dp(context))

                imvImage.setBackgroundColor(Color.TRANSPARENT)
                cvContent.setBackgroundResource(R.drawable.bg_uslt_navi_shape)
                cvContent.translationZ = 0f
                cvContent.translationY = 0f
            }

            // Layer 1: imvImage - shimmer fills full circle (0dp margin)
            val shimmerDrawable = ShimmerDrawable().apply {
                setShimmer(DataLocal.shimmer)
            }
            imvImage.setImageDrawable(shimmerDrawable)
            imvImage.visibility = View.VISIBLE

            // Layer 2: imvImageBG - actual image with margin (2dp margin)
            Glide.with(root)
                .load(item.imageNavigation)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        // Hide shimmer when image loads successfully
                        imvImage.visibility = View.GONE
                        return false
                    }
                })
                .into(imvImageBG)

            root.tap { onItemClick.invoke(position) }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BottomNavViewHolder {
        val binding = ItemBottomNavigationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return BottomNavViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BottomNavViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    companion object {
        val DiffCallback = object : DiffUtil.ItemCallback<NavigationModel>() {
            override fun areItemsTheSame(oldItem: NavigationModel, newItem: NavigationModel): Boolean {
                // Nếu NavigationModel có id riêng thì nên so sánh id, ở đây tạm so sánh hình
                return oldItem.imageNavigation == newItem.imageNavigation
            }

            override fun areContentsTheSame(oldItem: NavigationModel, newItem: NavigationModel): Boolean {
                return oldItem == newItem
            }
        }
    }
}
