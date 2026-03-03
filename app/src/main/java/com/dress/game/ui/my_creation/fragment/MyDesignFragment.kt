package com.dress.game.ui.my_creation.fragment

import android.app.ActivityOptions
import android.content.Intent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.dress.game.R
import com.dress.game.core.base.BaseFragment
import com.dress.game.core.extensions.gone
import com.dress.game.core.extensions.hideNavigation
import com.dress.game.core.extensions.invisible
import com.dress.game.core.extensions.showInterAll
import com.dress.game.core.extensions.tap
import com.dress.game.core.extensions.visible
import com.dress.game.core.helper.LanguageHelper
import com.dress.game.core.utils.key.IntentKey
import com.dress.game.core.utils.key.ValueKey
import com.dress.game.core.utils.state.HandleState
import com.dress.game.databinding.FragmentMyDesignBinding
import com.dress.game.dialog.YesNoDialog
import com.dress.game.ui.customize.CustomizeCharacterActivity
import com.dress.game.ui.home.DataViewModel
import com.dress.game.ui.my_creation.MyCreationActivity
import com.dress.game.ui.my_creation.view_model.MyCreationViewModel
import com.dress.game.ui.my_creation.adapter.MyAvatarAdapter
import com.dress.game.ui.my_creation.adapter.MyDesignAdapter
import com.dress.game.ui.my_creation.view_model.MyAvatarViewModel
import com.dress.game.ui.my_creation.view_model.MyDesignViewModel
import com.dress.game.ui.view.ViewActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.getValue

class MyDesignFragment : BaseFragment<FragmentMyDesignBinding>() {
    private val viewModel: MyDesignViewModel by viewModels()
    private val myCreationViewModel: MyCreationViewModel by activityViewModels()
    private val myDesignAdapter by lazy { MyDesignAdapter() }

    private val myAlbumActivity: MyCreationActivity
        get() = requireActivity() as MyCreationActivity

    override fun setViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentMyDesignBinding {
        return FragmentMyDesignBinding.inflate(inflater, container, false)
    }

    override fun initView() {
        initRcv()
        // ✅ FIX: Removed redundant load - onStart() will handle it
        android.util.Log.d("MyDesignFragment", "initView() - NOT loading data (onStart will do it)")
    }

    override fun dataObservable() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.myDesignList.collect { list ->
                        myDesignAdapter.submitList(list)
                        binding.layoutNoItem.isVisible = list.isEmpty()
                    }
                }
                // Removed - action bar buttons are disabled
                // launch {
                //     viewModel.isLastItem.collect { selectStatus ->
                //         myAlbumActivity.changeImageActionBarRight(selectStatus)
                //     }
                // }
                launch {
                    // ✅ FIX: Only reload on actual tab changes
                    myCreationViewModel.typeStatus
                        .drop(1) // Skip initial emission
                        .collect { status ->
                            android.util.Log.d("MyDesignFragment", "Tab switched to MyDesign - reloading data")
                            resetData()
                        }
                }
            }
        }
    }

    override fun viewListener() {
        binding.apply {
            rcvMyDesign.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
                override fun onInterceptTouchEvent(
                    recyclerView: RecyclerView, motionEvent: MotionEvent
                ): Boolean {
                    return when {
                        motionEvent.action != MotionEvent.ACTION_UP || recyclerView.findChildViewUnder(
                            motionEvent.x, motionEvent.y
                        ) != null -> false

                        else -> {
                            resetData()
                            true
                        }
                    }
                }

                override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
                override fun onTouchEvent(recyclerView: RecyclerView, motionEvent: MotionEvent) {}
            })
            // Action bar buttons disabled - no delete button for MyDesign tab yet
            // myAlbumActivity.binding.actionBar.btnActionBarRight.tap { handleSelectAll() }
            // myAlbumActivity.binding.actionBar.btnActionBarNextToRight.tap { handleDelete(viewModel.getPathSelected()) }

            // Share and Download buttons are handled in MyCreationActivity

            myDesignAdapter.onItemClick = { pathInternal -> handleItemClick(pathInternal) }
            myDesignAdapter.onItemTick = { position ->
                viewModel.toggleSelect(position)
                // Check if all items are now selected and update the icon
                val allSelected = viewModel.myDesignList.value.all { it.isSelected }
                myAlbumActivity.updateSelectAllIcon(allSelected)
            }
            myDesignAdapter.onDeleteClick = { pathInternal -> handleDelete(arrayListOf(pathInternal)) }
            myDesignAdapter.onLongClick = { position -> handleLongClick(position) }
        }
    }

    private fun initRcv() {
        binding.apply {
            rcvMyDesign.apply {
                adapter = myDesignAdapter
                itemAnimator = null
                // Performance optimizations
                setHasFixedSize(true) // RecyclerView size doesn't change
                setItemViewCacheSize(20) // Cache more view holders
                isNestedScrollingEnabled = true
            }
        }
    }

    private fun handleDelete(pathInternalList: ArrayList<String>) {
        if (pathInternalList.isEmpty()) {
            myAlbumActivity.showToast(R.string.please_select_an_image)
            return
        }
        val dialog = YesNoDialog(myAlbumActivity, R.string.delete, R.string.are_you_sure_want_to_delete_this_item)
        LanguageHelper.setLocale(myAlbumActivity)
        dialog.show()
        dialog.onDismissClick = {
            dialog.dismiss()
            myAlbumActivity.hideNavigation()
        }
        dialog.onNoClick = {
            dialog.dismiss()
            myAlbumActivity.hideNavigation()
        }
        dialog.onYesClick = {
            lifecycleScope.launch(Dispatchers.IO) {
                viewModel.deleteItem(myAlbumActivity, pathInternalList)
                withContext(Dispatchers.Main) {
                    dialog.dismiss()
                    myAlbumActivity.hideNavigation()
                    resetData()
                }
            }
        }
    }

    private fun handleItemClick(pathInternal: String) {
        if (myDesignAdapter.items.any { it.isShowSelection }) {
            // In selection mode - reset before navigating
            resetSelectionMode()
        }
        val intent = Intent(myAlbumActivity, ViewActivity::class.java)
        intent.putExtra(IntentKey.INTENT_KEY, pathInternal)
        intent.putExtra(IntentKey.TYPE_KEY, ValueKey.TYPE_VIEW)
        intent.putExtra(IntentKey.STATUS_KEY, ValueKey.MY_DESIGN_TYPE)
        val options = ActivityOptions.makeCustomAnimation(myAlbumActivity, R.anim.slide_in_right, R.anim.slide_out_left)
        myAlbumActivity.showInterAll { startActivity(intent, options.toBundle()) }
    }

    private fun handleLongClick(position: Int) {
        viewModel.showLongClick(position)
        // Show deleteSection and bottom bar
        myAlbumActivity.binding.lnlBottom.visible()
        myAlbumActivity.enterSelectionMode()

        // Check if all items are now selected (e.g., if there's only 1 item)
        val allSelected = viewModel.myDesignList.value.all { it.isSelected }
        myAlbumActivity.updateSelectAllIcon(allSelected)
    }

    private fun resetData() {
        android.util.Log.d("MyDesignFragment", "========================================")
        android.util.Log.d("MyDesignFragment", "resetData() called")
        android.util.Log.d("MyDesignFragment", "Loading from: ValueKey.DOWNLOAD_ALBUM")
        viewModel.loadMyDesign(myAlbumActivity)
        // Hide deleteSection and bottom bar
        myAlbumActivity.binding.lnlBottom.gone()
        myAlbumActivity.exitSelectionMode()
        android.util.Log.d("MyDesignFragment", "resetData() completed")
        android.util.Log.d("MyDesignFragment", "========================================")
    }

    fun getSelectedPaths(): ArrayList<String> {
        return viewModel.getPathSelected()
    }

    fun deleteSelectedItems() {
        handleDelete(viewModel.getPathSelected())
    }

    fun selectAllItems() {
        viewModel.selectAll(true)
        // Use notifyItemRangeChanged instead of notifyDataSetChanged for better performance
        myDesignAdapter.notifyItemRangeChanged(0, myDesignAdapter.itemCount)
    }

    fun deselectAllItems() {
        viewModel.selectAll(false)
        // Use notifyItemRangeChanged instead of notifyDataSetChanged for better performance
        myDesignAdapter.notifyItemRangeChanged(0, myDesignAdapter.itemCount)
    }

    fun resetSelectionMode() {
        viewModel.clearSelection()
        myAlbumActivity.binding.lnlBottom.gone()
        myAlbumActivity.exitSelectionMode()
        // notifyItemRangeChanged not needed - data will refresh automatically
    }

    override fun onStart() {
        super.onStart()
        android.util.Log.w("MyDesignFragment", "🔵 onStart() called - Fragment is starting")
        resetData()
    }

    override fun onResume() {
        super.onResume()
        android.util.Log.w("MyDesignFragment", "🟢 onResume() called - Fragment is visible")
    }

    override fun onPause() {
        super.onPause()
        android.util.Log.w("MyDesignFragment", "🟡 onPause() called - Fragment losing focus")
    }

    override fun onStop() {
        super.onStop()
        android.util.Log.w("MyDesignFragment", "🔴 onStop() called - Fragment no longer visible")
        android.util.Log.w("MyDesignFragment", "Current image count: ${viewModel.myDesignList.value.size}")
    }
}