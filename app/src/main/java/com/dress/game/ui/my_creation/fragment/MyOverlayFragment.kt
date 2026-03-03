package com.dress.game.ui.my_creation.fragment

import android.app.ActivityOptions
import android.content.Intent
import android.view.LayoutInflater
import android.view.MotionEvent
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
import com.dress.game.core.extensions.showInterAll
import com.dress.game.core.extensions.visible
import com.dress.game.core.helper.LanguageHelper
import com.dress.game.core.utils.key.IntentKey
import com.dress.game.core.utils.key.ValueKey
import com.dress.game.databinding.FragmentMyOverlayBinding
import com.dress.game.dialog.YesNoDialog
import com.dress.game.ui.my_creation.MyCreationActivity
import com.dress.game.ui.my_creation.adapter.MyOverlayAdapter
import com.dress.game.ui.my_creation.view_model.MyCreationViewModel
import com.dress.game.ui.my_creation.view_model.MyOverlayViewModel
import com.dress.game.ui.view.ViewActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MyOverlayFragment : BaseFragment<FragmentMyOverlayBinding>() {

    private val viewModel: MyOverlayViewModel by viewModels()
    private val myCreationViewModel: MyCreationViewModel by activityViewModels()
    private val myOverlayAdapter by lazy { MyOverlayAdapter() }

    private val myAlbumActivity: MyCreationActivity
        get() = requireActivity() as MyCreationActivity

    override fun setViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentMyOverlayBinding {
        return FragmentMyOverlayBinding.inflate(inflater, container, false)
    }

    override fun initView() {
        binding.rcvMyOverlay.apply {
            adapter = myOverlayAdapter
            itemAnimator = null
            setHasFixedSize(true)
            setItemViewCacheSize(20)
            isNestedScrollingEnabled = true
        }
    }

    override fun dataObservable() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.myOverlayList.collect { list ->
                        myOverlayAdapter.submitList(list)
                        binding.layoutNoItem.isVisible = list.isEmpty()
                    }
                }
                launch {
                    myCreationViewModel.typeStatus
                        .drop(1)
                        .collect { resetData() }
                }
            }
        }
    }

    override fun viewListener() {
        binding.rcvMyOverlay.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                return if (e.action == MotionEvent.ACTION_UP && rv.findChildViewUnder(e.x, e.y) == null) {
                    resetData(); true
                } else false
            }
            override fun onRequestDisallowInterceptTouchEvent(d: Boolean) {}
            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
        })

        myOverlayAdapter.onItemClick = { path -> handleItemClick(path) }
        myOverlayAdapter.onItemTick = { position ->
            viewModel.toggleSelect(position)
            val allSelected = viewModel.myOverlayList.value.all { it.isSelected }
            myAlbumActivity.updateSelectAllIcon(allSelected)
        }
        myOverlayAdapter.onDeleteClick = { path -> handleDelete(arrayListOf(path)) }
        myOverlayAdapter.onLongClick = { position -> handleLongClick(position) }
    }

    private fun handleItemClick(path: String) {
        if (myOverlayAdapter.items.any { it.isShowSelection }) {
            resetSelectionMode(); return
        }
        val intent = Intent(myAlbumActivity, ViewActivity::class.java).apply {
            putExtra(IntentKey.INTENT_KEY, path)
            putExtra(IntentKey.TYPE_KEY, ValueKey.TYPE_VIEW)
            putExtra(IntentKey.STATUS_KEY, ValueKey.PRIDE_OVERLAY_TYPE)
        }
        val options = ActivityOptions.makeCustomAnimation(myAlbumActivity, R.anim.slide_in_right, R.anim.slide_out_left)
        myAlbumActivity.showInterAll { startActivity(intent, options.toBundle()) }
    }

    private fun handleLongClick(position: Int) {
        viewModel.showLongClick(position)
        myAlbumActivity.binding.lnlBottom.visible()
        myAlbumActivity.enterSelectionMode()
        val allSelected = viewModel.myOverlayList.value.all { it.isSelected }
        myAlbumActivity.updateSelectAllIcon(allSelected)
    }

    private fun handleDelete(pathList: ArrayList<String>) {
        if (pathList.isEmpty()) {
            myAlbumActivity.showToast(R.string.please_select_an_image); return
        }
        val dialog = YesNoDialog(myAlbumActivity, R.string.delete, R.string.are_you_sure_want_to_delete_this_item)
        LanguageHelper.setLocale(myAlbumActivity)
        dialog.show()
        dialog.onDismissClick = { dialog.dismiss(); myAlbumActivity.hideNavigation(); resetData() }
        dialog.onNoClick = { dialog.dismiss(); myAlbumActivity.hideNavigation() }
        dialog.onYesClick = {
            lifecycleScope.launch(Dispatchers.IO) {
                viewModel.deleteItem(myAlbumActivity, pathList)
                withContext(Dispatchers.Main) {
                    dialog.dismiss(); myAlbumActivity.hideNavigation(); resetData()
                }
            }
        }
    }

    private fun resetData() {
        viewModel.loadMyOverlay(myAlbumActivity)
        myAlbumActivity.binding.lnlBottom.gone()
        myAlbumActivity.exitSelectionMode()
    }

    fun deleteSelectedItems() = handleDelete(viewModel.getPathSelected())
    fun getSelectedPaths(): ArrayList<String> = viewModel.getPathSelected()
    fun selectAllItems() { viewModel.selectAll(true); myOverlayAdapter.notifyItemRangeChanged(0, myOverlayAdapter.itemCount) }
    fun deselectAllItems() { viewModel.selectAll(false); myOverlayAdapter.notifyItemRangeChanged(0, myOverlayAdapter.itemCount) }

    fun resetSelectionMode() {
        viewModel.clearSelection()
        myAlbumActivity.binding.lnlBottom.gone()
        myAlbumActivity.exitSelectionMode()
    }

    override fun onStart() {
        super.onStart()
        resetData()
    }
}
