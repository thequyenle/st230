package com.dress.game.ui.my_creation

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.PopupWindow
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.room.util.findColumnIndexBySuffix
import com.lvt.ads.util.Admob
import com.dress.game.R
import com.dress.game.core.base.BaseActivity
import com.dress.game.core.extensions.checkPermissions
import com.dress.game.core.extensions.goToSettings
import com.dress.game.core.extensions.gone
import com.dress.game.core.extensions.hideNavigation
import com.dress.game.core.extensions.invisible
import com.dress.game.core.extensions.loadNativeCollabAds
import com.dress.game.core.extensions.requestPermission
import com.dress.game.core.extensions.select
import com.dress.game.core.extensions.setImageActionBar
import com.dress.game.core.extensions.setTextActionBar
import com.dress.game.core.extensions.tap

import com.dress.game.core.extensions.startIntentWithClearTop
import com.dress.game.core.extensions.visible
import com.dress.game.core.helper.LanguageHelper
import com.dress.game.core.helper.UnitHelper
import com.dress.game.core.utils.key.IntentKey
import com.dress.game.core.utils.key.RequestKey
import com.dress.game.core.utils.key.ValueKey
import com.dress.game.core.utils.share.whatsapp.WhatsappSharingActivity
import com.dress.game.core.utils.state.HandleState
import com.dress.game.databinding.ActivityAlbumBinding
import com.dress.game.dialog.YesNoDialog
import com.dress.game.ui.home.HomeActivity
import com.dress.game.ui.view.ViewActivity
import com.dress.game.databinding.PopupMyAlbumBinding
import com.dress.game.dialog.CreateNameDialog
import com.dress.game.ui.my_creation.adapter.MyAvatarAdapter
import com.dress.game.ui.my_creation.adapter.TypeAdapter
import com.dress.game.ui.my_creation.fragment.MyAvatarFragment
import com.dress.game.ui.my_creation.fragment.MyDesignFragment
import com.dress.game.ui.my_creation.fragment.MyOverlayFragment
import com.dress.game.ui.my_creation.view_model.MyAvatarViewModel
import com.dress.game.ui.my_creation.view_model.MyCreationViewModel
import com.dress.game.ui.permission.PermissionViewModel
import kotlinx.coroutines.launch
import kotlin.text.replace

class MyCreationActivity : WhatsappSharingActivity<ActivityAlbumBinding>() {
    companion object {
        private var instanceRef: java.lang.ref.WeakReference<MyCreationActivity>? = null

        fun getInstance(): MyCreationActivity? = instanceRef?.get()
    }

    private val viewModel: MyCreationViewModel by viewModels()
    private val permissionViewModel: PermissionViewModel by viewModels()

    private var myAvatarFragment: MyAvatarFragment? = null
    private var myDesignFragment: MyDesignFragment? = null
    private var myOverlayFragment: MyOverlayFragment? = null
    private var isInSelectionMode = false
    private var isAllSelected = false
    private var pendingDownloadList: ArrayList<String>? = null

    override fun setViewBinding(): ActivityAlbumBinding {
        return ActivityAlbumBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        // Store instance reference for ViewActivity to access
        instanceRef = java.lang.ref.WeakReference(this)

        val initialTab = intent.getIntExtra(IntentKey.TAB_KEY, ValueKey.AVATAR_TYPE)
        viewModel.setTypeStatus(initialTab)
        viewModel.setStatusFrom(intent.getBooleanExtra(IntentKey.FROM_SAVE, false))

        // Hide action bar buttons by default (only show in selection mode)
        binding.actionBar.apply {
            btnActionBarRight.gone()
            btnActionBarNextRight.gone()
        }
        binding.lnlBottom.isSelected = true
    }

    override fun dataObservable() {
        binding.apply {
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.CREATED) {
                    launch {
                        viewModel.typeStatus.collect { type ->
                            if (type != -1) {
                                when (type) {
                                    ValueKey.AVATAR_TYPE -> {
                                        imvTabBackground.setImageResource(R.drawable.bg_pride_slt)
                                        setupSelectedTab(tvMyPride)
                                        setupUnselectedTab(tvMyDesign)
                                        setupUnselectedTab(tvMyOverlay)
                                        showFragment(ValueKey.AVATAR_TYPE)
                                    }
                                    ValueKey.MY_DESIGN_TYPE -> {
                                        imvTabBackground.setImageResource(R.drawable.bg_design_slt)
                                        setupUnselectedTab(tvMyPride)
                                        setupSelectedTab(tvMyDesign)
                                        setupUnselectedTab(tvMyOverlay)
                                        showFragment(ValueKey.MY_DESIGN_TYPE)
                                    }
                                    ValueKey.PRIDE_OVERLAY_TYPE -> {
                                        imvTabBackground.setImageResource(R.drawable.bg_overlay_slt)
                                        setupUnselectedTab(tvMyPride)
                                        setupUnselectedTab(tvMyDesign)
                                        setupSelectedTab(tvMyOverlay)
                                        showFragment(ValueKey.PRIDE_OVERLAY_TYPE)
                                    }
                                }
                                updateBottomButtonsVisibility()
                            }
                        }
                    }
                    launch {
                        viewModel.downloadState.collect { state ->
                            when (state) {
                                HandleState.LOADING -> {
                                    showLoading()
                                }

                                HandleState.SUCCESS -> {
                                    dismissLoading()
                                    hideNavigation()
                                    showToast(R.string.download_success)
                                    // Auto-click back button to exit selection mode
                                    binding.actionBar.btnActionBarLeft.performClick()
                                }

                                else -> {
                                    dismissLoading()
                                    hideNavigation()
                                    showToast(R.string.download_failed_please_try_again_later)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun viewListener() {
        binding.apply {
            actionBar.apply {
                btnActionBarLeft.tap {
                    if (isInSelectionMode) {
                        val avatarFragment = supportFragmentManager.findFragmentByTag("MyAvatarFragment")
                        val designFragment = supportFragmentManager.findFragmentByTag("MyDesignFragment")
                        val overlayFragment = supportFragmentManager.findFragmentByTag("MyOverlayFragment")
                        when {
                            avatarFragment is MyAvatarFragment && avatarFragment.isVisible -> avatarFragment.resetSelectionMode()
                            designFragment is MyDesignFragment && designFragment.isVisible -> designFragment.resetSelectionMode()
                            overlayFragment is MyOverlayFragment && overlayFragment.isVisible -> overlayFragment.resetSelectionMode()
                        }
                    } else {
                        startIntentWithClearTop(HomeActivity::class.java)
                    }
                }

                // Select All button
                btnActionBarRight.tap {
                    handleSelectAllFromCurrentFragment()
                }

                // Delete All button
                btnActionBarNextRight.tap {
                    handleDeleteSelectedFromCurrentFragment()
                }
            }

            btnMyPixel.tap { viewModel.setTypeStatus(ValueKey.AVATAR_TYPE) }
            btnMyDesign.tap { viewModel.setTypeStatus(ValueKey.MY_DESIGN_TYPE) }
            btnMyOverlay.tap { viewModel.setTypeStatus(ValueKey.PRIDE_OVERLAY_TYPE) }

            // WhatsApp, Telegram, and Download buttons in lnlBottom
            val layoutBottom = lnlBottom.getChildAt(0)
            layoutBottom.findViewById<View>(R.id.btnWhatsapp)?.tap(2500) {
                val selectedPaths = getSelectedPathsFromCurrentFragment()
                handleAddToWhatsApp(selectedPaths)
            }
            layoutBottom.findViewById<View>(R.id.btnTelegram)?.tap(2500) {
                val selectedPaths = getSelectedPathsFromCurrentFragment()
                handleAddToTelegram(selectedPaths)
            }
            layoutBottom.findViewById<View>(R.id.btnDownload)?.tap(2500) {
                handleDownloadFromCurrentFragment()
            }

            // Delete button in deleteSection         }
        }
    }

    private fun handleShareFromCurrentFragment() {
        val selectedPaths = getSelectedPathsFromCurrentFragment()
        handleShare(selectedPaths)
    }

    private fun handleDownloadFromCurrentFragment() {
        val selectedPaths = getSelectedPathsFromCurrentFragment()
        if (selectedPaths.isEmpty()) {
            showToast(R.string.please_select_an_image)
            return
        }
        checkStoragePermissionForDownload(selectedPaths)
    }

    private fun checkStoragePermissionForDownload(list: ArrayList<String>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ không cần quyền WRITE_EXTERNAL_STORAGE
            handleDownload(list)
        } else {
            // Android 8-9 cần check quyền
            val perms = permissionViewModel.getStoragePermissions()
            if (checkPermissions(perms)) {
                handleDownload(list)
            } else if (permissionViewModel.needGoToSettings(sharePreference, true)) {
                goToSettings()
            } else {
                // Lưu lại list để download sau khi được cấp quyền
                pendingDownloadList = list
                requestPermission(perms, RequestKey.STORAGE_PERMISSION_CODE)
            }
        }
    }

    private fun handleSelectAllFromCurrentFragment() {
        val avatarFragment = supportFragmentManager.findFragmentByTag("MyAvatarFragment")
        val designFragment = supportFragmentManager.findFragmentByTag("MyDesignFragment")
        val overlayFragment = supportFragmentManager.findFragmentByTag("MyOverlayFragment")

        val doSelect: (() -> Unit)
        val doDeselect: (() -> Unit)

        when {
            avatarFragment is MyAvatarFragment && avatarFragment.isVisible -> {
                doSelect = { avatarFragment.selectAllItems() }
                doDeselect = { avatarFragment.deselectAllItems() }
            }
            designFragment is MyDesignFragment && designFragment.isVisible -> {
                doSelect = { designFragment.selectAllItems() }
                doDeselect = { designFragment.deselectAllItems() }
            }
            overlayFragment is MyOverlayFragment && overlayFragment.isVisible -> {
                doSelect = { overlayFragment.selectAllItems() }
                doDeselect = { overlayFragment.deselectAllItems() }
            }
            else -> return
        }

        if (isAllSelected) {
            doDeselect()
            isAllSelected = false
            binding.actionBar.btnActionBarRight.setImageResource(R.drawable.ic_not_select_all)
        } else {
            doSelect()
            isAllSelected = true
            binding.actionBar.btnActionBarRight.setImageResource(R.drawable.ic_select_all)
        }
    }

    private fun handleDeleteSelectedFromCurrentFragment() {
        val avatarFragment = supportFragmentManager.findFragmentByTag("MyAvatarFragment")
        val designFragment = supportFragmentManager.findFragmentByTag("MyDesignFragment")
        val overlayFragment = supportFragmentManager.findFragmentByTag("MyOverlayFragment")

        when {
            avatarFragment is MyAvatarFragment && avatarFragment.isVisible -> avatarFragment.deleteSelectedItems()
            designFragment is MyDesignFragment && designFragment.isVisible -> designFragment.deleteSelectedItems()
            overlayFragment is MyOverlayFragment && overlayFragment.isVisible -> overlayFragment.deleteSelectedItems()
        }
    }

    private fun getSelectedPathsFromCurrentFragment(): ArrayList<String> {
        val avatarFragment = supportFragmentManager.findFragmentByTag("MyAvatarFragment")
        val designFragment = supportFragmentManager.findFragmentByTag("MyDesignFragment")
        val overlayFragment = supportFragmentManager.findFragmentByTag("MyOverlayFragment")

        return when {
            avatarFragment is MyAvatarFragment && avatarFragment.isVisible -> avatarFragment.getSelectedPaths()
            designFragment is MyDesignFragment && designFragment.isVisible -> designFragment.getSelectedPaths()
            overlayFragment is MyOverlayFragment && overlayFragment.isVisible -> overlayFragment.getSelectedPaths()
            else -> arrayListOf()
        }
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            setImageActionBar(btnActionBarLeft, R.drawable.ic_back)
            setTextActionBar(tvCenter, getString(R.string.my_work))
            tvCenter.select()

            // Select All button (btnActionBarRight) - resize to 24dp for select all icons
            val size24dp = (24 * resources.displayMetrics.density).toInt()
            val params = btnActionBarRight.layoutParams
            params.width = size24dp
            params.height = size24dp
            btnActionBarRight.layoutParams = params

            btnActionBarRight.setImageResource(R.drawable.ic_not_select_all)
            btnActionBarRight.gone()

            // Delete All button - hidden initially, only shown in selection mode
            btnActionBarNextRight.setImageResource(R.drawable.ic_delete_item)
            btnActionBarNextRight.gone()
        }
    }

    override fun initText() {
        binding.apply {
            tvMyPride.select()
            tvMyDesign.select()
            tvMyOverlay.select()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == RequestKey.STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                permissionViewModel.updateStorageGranted(sharePreference, true)
                showToast(R.string.granted_storage)
                // Thực hiện download sau khi được cấp quyền
                pendingDownloadList?.let { list ->
                    handleDownload(list)
                    pendingDownloadList = null
                }
            } else {
                permissionViewModel.updateStorageGranted(sharePreference, false)
                pendingDownloadList = null
            }
        }
    }

    fun handleShare(list: ArrayList<String>) {
        if (list.isEmpty()) {
            showToast(R.string.please_select_an_image)
            return
        }
        viewModel.shareImages(this, list)
    }

    fun handleAddToTelegram(list: ArrayList<String>) {
        if (list.isEmpty()) {
            showToast(R.string.please_select_an_image)
            return
        }
        viewModel.addToTelegram(this, list)
        // Auto-click back button to exit selection mode
        binding.actionBar.btnActionBarLeft.performClick()
    }

    fun handleAddToWhatsApp(list: ArrayList<String>) {
        if (list.size < 3) {
            showToast(R.string.limit_3_items)
            return
        }
        if (list.size > 30) {
            showToast(R.string.limit_30_items)
            return
        }

        val dialog = CreateNameDialog(this)
        LanguageHelper.setLocale(this)
        dialog.show()

        fun dismissDialog() {
            dialog.dismiss()
            hideNavigation()
        }
        dialog.onNoClick = {
            dismissDialog()
        }
        dialog.onDismissClick = {
            dismissDialog()
        }

        dialog.onYesClick = { packageName ->
            dismissDialog()
            viewModel.addToWhatsapp(this, packageName, list) { stickerPack ->
                if (stickerPack != null) {
                    addToWhatsapp(stickerPack)
                    // Auto-click back button to exit selection mode
                    binding.actionBar.btnActionBarLeft.performClick()
                }
            }
        }
    }

    private fun handleDownload(list: ArrayList<String>) {
        viewModel.downloadFiles(this, list)
    }

    private fun showFragment(type: Int) {
        val transaction = supportFragmentManager.beginTransaction()

        if (myAvatarFragment == null) {
            myAvatarFragment = MyAvatarFragment()
            transaction.add(R.id.frmList, myAvatarFragment!!, "MyAvatarFragment")
        }
        if (myDesignFragment == null) {
            myDesignFragment = MyDesignFragment()
            transaction.add(R.id.frmList, myDesignFragment!!, "MyDesignFragment")
        }
        if (myOverlayFragment == null) {
            myOverlayFragment = MyOverlayFragment()
            transaction.add(R.id.frmList, myOverlayFragment!!, "MyOverlayFragment")
        }

        when (type) {
            ValueKey.AVATAR_TYPE -> {
                myAvatarFragment?.let { transaction.show(it) }
                myDesignFragment?.let { transaction.hide(it) }
                myOverlayFragment?.let { transaction.hide(it) }
            }
            ValueKey.MY_DESIGN_TYPE -> {
                myAvatarFragment?.let { transaction.hide(it) }
                myDesignFragment?.let { transaction.show(it) }
                myOverlayFragment?.let { transaction.hide(it) }
            }
            ValueKey.PRIDE_OVERLAY_TYPE -> {
                myAvatarFragment?.let { transaction.hide(it) }
                myDesignFragment?.let { transaction.hide(it) }
                myOverlayFragment?.let { transaction.show(it) }
            }
        }

        transaction.commit()
    }

    @SuppressLint("MissingSuperCall", "GestureBackNavigation")
    override fun onBackPressed() {
        startIntentWithClearTop(HomeActivity::class.java)
    }

//    fun initNativeCollab() {
//        Admob.getInstance().loadNativeCollapNotBanner(this,getString(R.string.native_cl_myWork), binding.flNativeCollab)
//    }
//    override fun initAds() {
//        initNativeCollab()
//        Admob.getInstance().loadNativeAd(
//            this,
//            getString(R.string.native_myWork),
//            binding.nativeAds,
//            R.layout.ads_native_banner
//        )
//    }

    override fun onRestart() {
        super.onRestart()
        android.util.Log.w(
            "MyCreationActivity",
            "🔄 onRestart() called - Activity restarting after being stopped"
        )
        android.util.Log.w(
            "MyCreationActivity",
            "Current tab: ${when (viewModel.typeStatus.value) { ValueKey.AVATAR_TYPE -> "MyAvatar"; ValueKey.MY_DESIGN_TYPE -> "MyDesign"; else -> "MyOverlay" }}"
        )
        android.util.Log.w("MyCreationActivity", "Selection mode: $isInSelectionMode")

        // Check permission status
        val hasPermission =
            checkPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE))
        android.util.Log.w("MyCreationActivity", "📱 Storage permission: $hasPermission")

        // Exit selection mode when returning from another activity
        if (isInSelectionMode) {
            val avatarFragment = supportFragmentManager.findFragmentByTag("MyAvatarFragment")
            val designFragment = supportFragmentManager.findFragmentByTag("MyDesignFragment")
            val overlayFragment = supportFragmentManager.findFragmentByTag("MyOverlayFragment")

            when {
                avatarFragment is MyAvatarFragment && avatarFragment.isVisible -> avatarFragment.resetSelectionMode()
                designFragment is MyDesignFragment && designFragment.isVisible -> designFragment.resetSelectionMode()
                overlayFragment is MyOverlayFragment && overlayFragment.isVisible -> overlayFragment.resetSelectionMode()
            }
            exitSelectionMode()
        }

        // initNativeCollab()
        android.util.Log.w("MyCreationActivity", "🔄 onRestart() END")
    }

    override fun onStart() {
        super.onStart()
        android.util.Log.w("MyCreationActivity", "🔵 onStart() called - Activity becoming visible")
    }

    override fun onResume() {
        super.onResume()
        android.util.Log.w("MyCreationActivity", "🟢 onResume() called - Activity in foreground")
    }

    override fun onPause() {
        super.onPause()
        android.util.Log.w("MyCreationActivity", "🟡 onPause() called - Activity losing focus")
    }

    override fun onStop() {
        super.onStop()
        android.util.Log.w("MyCreationActivity", "🔴 onStop() called - Activity no longer visible")
    }

    fun enterSelectionMode() {
        isInSelectionMode = true
        isAllSelected = false
        binding.actionBar.apply {
            // Show select all and delete all buttons
            btnActionBarRight.setImageResource(R.drawable.ic_not_select_all)
            btnActionBarRight.visible()
            btnActionBarNextRight.visible()
        }
        updateBottomButtonsVisibility()
        android.util.Log.d("MyCreationActivity", "enterSelectionMode called - showing buttons")
    }

    fun exitSelectionMode() {
        isInSelectionMode = false
        isAllSelected = false
        binding.actionBar.apply {
            // Hide select all and delete all buttons
            btnActionBarRight.gone()
            btnActionBarNextRight.gone()
        }

        updateBottomButtonsVisibility()
        android.util.Log.d("MyCreationActivity", "exitSelectionMode called - hiding buttons")
    }

    private fun updateBottomButtonsVisibility() {
        val layoutBottom = binding.lnlBottom.getChildAt(0)
        val btnWhatsapp = layoutBottom.findViewById<View>(R.id.btnWhatsapp)
        val btnTelegram = layoutBottom.findViewById<View>(R.id.btnTelegram)
        val btnDownload = layoutBottom.findViewById<View>(R.id.btnDownload)

        if (!isInSelectionMode) {
            btnWhatsapp?.gone()
            btnTelegram?.gone()
            btnDownload?.gone()
        } else if (viewModel.typeStatus.value == ValueKey.AVATAR_TYPE) {
            // My Pony tab: show WhatsApp and Telegram
            btnWhatsapp?.visible()
            btnTelegram?.visible()
            btnDownload?.gone()
        } else {
            // My Design / Pride Overlay tab: show only Download
            btnWhatsapp?.gone()
            btnTelegram?.gone()
            btnDownload?.visible()
        }
    }

    private fun setupSelectedTab(textView: android.widget.TextView) {
        textView.setTextColor(Color.WHITE)
    }

    private fun setupUnselectedTab(textView: android.widget.TextView) {
        textView.setTextColor(Color.parseColor("#AB5BFF"))
    }

    // Public method to update select all icon based on selection state
    fun updateSelectAllIcon(allSelected: Boolean) {
        isAllSelected = allSelected
        if (allSelected) {
            binding.actionBar.btnActionBarRight.setImageResource(R.drawable.ic_select_all)
        } else {
            binding.actionBar.btnActionBarRight.setImageResource(R.drawable.ic_not_select_all)
        }
    }
}