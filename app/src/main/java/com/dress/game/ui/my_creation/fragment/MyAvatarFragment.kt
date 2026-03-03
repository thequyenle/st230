package com.dress.game.ui.my_creation.fragment

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
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
import kotlinx.coroutines.flow.drop
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
import com.dress.game.core.helper.MediaHelper
import com.dress.game.core.utils.key.IntentKey
import com.dress.game.core.utils.key.ValueKey
import com.dress.game.core.utils.state.HandleState
import com.dress.game.databinding.FragmentMyAvatarBinding
import com.dress.game.dialog.YesNoDialog
import com.dress.game.ui.customize.CustomizeCharacterActivity
import com.dress.game.ui.customize.CustomizeCharacterViewModel
import com.dress.game.ui.home.DataViewModel
import com.dress.game.ui.my_creation.MyCreationActivity
import com.dress.game.ui.my_creation.view_model.MyCreationViewModel
import com.dress.game.ui.my_creation.adapter.MyAvatarAdapter
import com.dress.game.ui.my_creation.view_model.MyAvatarViewModel
import com.dress.game.ui.view.ViewActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MyAvatarFragment : BaseFragment<FragmentMyAvatarBinding>() {
    private val viewModel: MyAvatarViewModel by viewModels()
    private val dataViewModel: DataViewModel by viewModels()
    private val myCreationViewModel: MyCreationViewModel by activityViewModels()
    private val myAvatarAdapter by lazy { MyAvatarAdapter(requireActivity()) }

    private val myAlbumActivity: MyCreationActivity
        get() = requireActivity() as MyCreationActivity

    override fun setViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentMyAvatarBinding {
        return FragmentMyAvatarBinding.inflate(inflater, container, false)
    }

    override fun initView() {
        initRcv()
        dataViewModel.ensureData(myAlbumActivity)
        // ✅ FIX: Removed redundant loadMyAvatar() - onStart() will handle it
        android.util.Log.d("MyAvatarFragment", "initView() - NOT loading data (onStart will do it)")
    }

    override fun dataObservable() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.myAvatarList.collect { list ->
                        myAvatarAdapter.submitList(list)
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
                    // ✅ FIX: Only reload on actual tab changes, not initial value
                    // StateFlow already has distinctUntilChanged built-in
                    myCreationViewModel.typeStatus
                        .drop(1) // Skip the first emission (initial value)
                        .collect { status ->
                            android.util.Log.d("MyAvatarFragment", "Tab switched to MyAvatar - reloading data")
                            resetData()
                        }
                }
            }
        }
    }

    override fun viewListener() {
        binding.apply {
            rcvMyAvatar.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
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
            // Action bar buttons disabled - using btnDeleteSelect instead
            // myAlbumActivity.binding.actionBar.btnActionBarRight.tap { handleSelectAll() }
            // myAlbumActivity.binding.actionBar.btnActionBarNextToRight.tap { handleDelete(viewModel.getPathSelected()) }

            // Share and Download buttons are handled in MyCreationActivity

            myAvatarAdapter.onItemClick = { pathInternal -> handleItemClick(pathInternal) }
            myAvatarAdapter.onItemTick = { position ->
                viewModel.toggleSelect(position)
                // Check if all items are now selected and update the icon
                val allSelected = viewModel.myAvatarList.value.all { it.isSelected }
                myAlbumActivity.updateSelectAllIcon(allSelected)
            }
            myAvatarAdapter.onEditClick = { pathInternal -> handleEditClick(pathInternal) }
            myAvatarAdapter.onDeleteClick = { pathInternal -> handleDelete(arrayListOf(pathInternal)) }
            myAvatarAdapter.onLongClick = { position -> handleLongClick(position) }
        }
    }

    private fun initRcv() {
        binding.apply {
            rcvMyAvatar.apply {
                adapter = myAvatarAdapter
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
            // Exit selection mode when dialog is dismissed
            resetData()
        }
        dialog.onNoClick = {
            dialog.dismiss()
            myAlbumActivity.hideNavigation()
            // Exit selection mode when user cancels
            //resetData()
        }
        dialog.onYesClick = {
            lifecycleScope.launch(Dispatchers.IO) {
                viewModel.deleteItem(myAlbumActivity, pathInternalList)
                withContext(Dispatchers.Main) {
                    dialog.dismiss()
                    myAlbumActivity.hideNavigation()
                    // Exit selection mode and reload data
                    resetData()
                }
            }
        }
    }

    private fun handleEditClick(pathInternal: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            myAlbumActivity.showLoading()
            viewModel.editItem(myAlbumActivity, pathInternal, dataViewModel.allData.value)
            withContext(Dispatchers.Main) {
                myAlbumActivity.dismissLoading()
                viewModel.checkDataInternet(myAlbumActivity) {
                    val intent = Intent(myAlbumActivity, CustomizeCharacterActivity::class.java)
                    intent.putExtra(IntentKey.INTENT_KEY, viewModel.positionCharacter)
                    intent.putExtra(IntentKey.STATUS_FROM_KEY, ValueKey.EDIT)
                    val option = ActivityOptions.makeCustomAnimation(
                        myAlbumActivity, R.anim.slide_out_left, R.anim.slide_in_right
                    )
                    myAlbumActivity.showInterAll { startActivity(intent, option.toBundle()) }
                }
            }
        }
    }

    private fun handleItemClick(pathInternal: String) {
        val intent = Intent(myAlbumActivity, ViewActivity::class.java)
        intent.putExtra(IntentKey.INTENT_KEY, pathInternal)
        intent.putExtra(IntentKey.TYPE_KEY, ValueKey.TYPE_VIEW)
        intent.putExtra(IntentKey.STATUS_KEY, ValueKey.AVATAR_TYPE)
        val options = ActivityOptions.makeCustomAnimation(myAlbumActivity, R.anim.slide_in_right, R.anim.slide_out_left)
        myAlbumActivity.showInterAll { startActivity(intent, options.toBundle()) }
    }

    private fun handleLongClick(position: Int) {
        viewModel.showLongClick(position)
        // Show deleteSection and bottom bar
        myAlbumActivity.binding.lnlBottom.visible()
        myAlbumActivity.enterSelectionMode()
        // Enable select mode margins in adapter
        myAvatarAdapter.isSelectMode = true

        // Check if all items are now selected (e.g., if there's only 1 item)
        val allSelected = viewModel.myAvatarList.value.all { it.isSelected }
        myAlbumActivity.updateSelectAllIcon(allSelected)
    }

    private fun resetData() {
        android.util.Log.d("MyAvatarFragment", "========================================")
        android.util.Log.d("MyAvatarFragment", "resetData() called")
        android.util.Log.d("MyAvatarFragment", "Current thread: ${Thread.currentThread().name}")
        android.util.Log.d("MyAvatarFragment", "Fragment state: ${lifecycle.currentState}")
        viewModel.loadMyAvatar(myAlbumActivity)
        // Hide deleteSection and bottom bar
        myAlbumActivity.binding.lnlBottom.gone()
        myAlbumActivity.exitSelectionMode()
        // Disable select mode margins in adapter
        myAvatarAdapter.isSelectMode = false
        android.util.Log.d("MyAvatarFragment", "resetData() completed")
        android.util.Log.d("MyAvatarFragment", "========================================")
    }

    fun deleteSelectedItems() {
        handleDelete(viewModel.getPathSelected())
    }

    fun getSelectedPaths(): ArrayList<String> {
        return viewModel.getPathSelected()
    }

    fun selectAllItems() {
        viewModel.selectAll(true)
        // Use notifyItemRangeChanged instead of notifyDataSetChanged for better performance
        myAvatarAdapter.notifyItemRangeChanged(0, myAvatarAdapter.itemCount)
    }

    fun deselectAllItems() {
        viewModel.selectAll(false)
        // Use notifyItemRangeChanged instead of notifyDataSetChanged for better performance
        myAvatarAdapter.notifyItemRangeChanged(0, myAvatarAdapter.itemCount)
    }

    fun exitSelectMode() {
        myAvatarAdapter.isSelectMode = false
    }

    fun resetSelectionMode() {
        viewModel.clearSelection()
        myAlbumActivity.binding.lnlBottom.gone()
        myAlbumActivity.exitSelectionMode()
        myAvatarAdapter.isSelectMode = false
        // No need to notify here - isSelectMode setter already handles it
    }

    override fun onStart() {
        super.onStart()
        android.util.Log.w("MyAvatarFragment", "🔵 onStart() called - Fragment is starting")
        resetData()
    }

    override fun onResume() {
        super.onResume()
        android.util.Log.w("MyAvatarFragment", "🟢 onResume() called - Fragment is visible")
        // onStart() already handles data loading - no need to reload here
    }

    override fun onPause() {
        super.onPause()
        android.util.Log.w("MyAvatarFragment", "🟡 onPause() called - Fragment losing focus")
    }

    override fun onStop() {
        super.onStop()
        android.util.Log.w("MyAvatarFragment", "🔴 onStop() called - Fragment no longer visible")
        android.util.Log.w("MyAvatarFragment", "Current image count: ${viewModel.myAvatarList.value.size}")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        android.util.Log.w("MyAvatarFragment", "💀 onDestroyView() called - View being destroyed")
    }
}