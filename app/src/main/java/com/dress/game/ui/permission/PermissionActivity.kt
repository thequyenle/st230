package com.dress.game.ui.permission

import android.content.pm.PackageManager
import android.os.Build
import android.text.TextUtils
import android.view.LayoutInflater
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.lvt.ads.callback.InterCallback
import com.lvt.ads.util.Admob
import com.dress.game.R
import com.dress.game.core.base.BaseActivity
import com.dress.game.core.extensions.checkPermissions
import com.dress.game.core.extensions.goToSettings
import com.dress.game.core.extensions.gone
import com.dress.game.core.extensions.requestPermission
import com.dress.game.core.extensions.select

import com.dress.game.core.extensions.startIntentRightToLeft
import com.dress.game.core.extensions.visible
import com.dress.game.core.helper.StringHelper
import com.dress.game.core.utils.key.RequestKey
import com.dress.game.databinding.ActivityPermissionBinding
import com.dress.game.ui.home.HomeActivity
import com.dress.game.core.extensions.tap
import kotlinx.coroutines.launch
import kotlin.compareTo

class PermissionActivity : BaseActivity<ActivityPermissionBinding>() {

    private val viewModel: PermissionViewModel by viewModels()

    private var inter: InterstitialAd? = null

    override fun setViewBinding() = ActivityPermissionBinding.inflate(LayoutInflater.from(this))

    override fun initView() {
        // Reset permission counters when activity starts (so it asks again after app restart)
        android.util.Log.d("PermissionActivity", "initView: Resetting counters to 0")
        sharePreference.setStoragePermission(0)
        sharePreference.setNotificationPermission(0)
        android.util.Log.d("PermissionActivity", "initView: Storage counter = ${sharePreference.getStoragePermission()}, Notification counter = ${sharePreference.getNotificationPermission()}")

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            binding.btnStorage.visible()
            binding.btnNotification.gone()
        } else {
            binding.btnNotification.visible()
            binding.btnStorage.gone()
        }
    }

    override fun initText() {
       // binding.actionBar.tvCenter.select()
        val textRes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) R.string.to_access_13 else R.string.to_access

        binding.txtPer.text = TextUtils.concat(
            createColoredText(R.string.allow, R.color.white),
            " ",
            createColoredText(R.string.app_name, R.color.white),
            " ",
            createColoredText(textRes, R.color.white)
        )
    }

    override fun viewListener() {
        binding.swPermission.tap { handlePermissionRequest(isStorage = true) }
        binding.swNotification.tap { handlePermissionRequest(isStorage = false) }
        binding.tvContinue.tap(1500) { handleContinue() }
    }

    override fun dataObservable() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.storageGranted.collect { granted ->
                        updatePermissionUI(granted, true)
                    }
                }

                launch {
                    viewModel.notificationGranted.collect { granted ->
                        updatePermissionUI(granted, false)
                    }
                }
            }
        }
    }

    private fun handlePermissionRequest(isStorage: Boolean) {
        val permType = if (isStorage) "Storage" else "Notification"
        val counter = if (isStorage) sharePreference.getStoragePermission() else sharePreference.getNotificationPermission()
        android.util.Log.d("PermissionActivity", "handlePermissionRequest: $permType clicked, counter = $counter")

        val perms = if (isStorage) viewModel.getStoragePermissions() else viewModel.getNotificationPermissions()

        // Check shouldShowRequestPermissionRationale for each permission
        val shouldShowRationale = perms.any { shouldShowRequestPermissionRationale(it) }
        perms.forEach { permission ->
            val shouldShow = shouldShowRequestPermissionRationale(permission)
            android.util.Log.d("PermissionActivity", "handlePermissionRequest: shouldShowRationale for $permission = $shouldShow")
        }

        if (checkPermissions(perms)) {
            android.util.Log.d("PermissionActivity", "handlePermissionRequest: $permType already granted")
            showToast(if (isStorage) R.string.granted_storage else R.string.granted_notification)
        } else if (viewModel.needGoToSettings(sharePreference, isStorage)) {
            android.util.Log.d("PermissionActivity", "handlePermissionRequest: $permType needs settings (counter > 2)")
            goToSettings()
        } else if (counter > 0 && !shouldShowRationale) {
            // User selected "Don't ask again" - shouldShowRationale is false after denying
            android.util.Log.d("PermissionActivity", "handlePermissionRequest: $permType permanently denied (Don't ask again), going to settings")
            goToSettings()
        } else {
            android.util.Log.d("PermissionActivity", "handlePermissionRequest: $permType requesting permission from system")
            android.util.Log.d("PermissionActivity", "handlePermissionRequest: Permissions to request = ${perms.joinToString()}")
            val requestCode = if (isStorage) RequestKey.STORAGE_PERMISSION_CODE else RequestKey.NOTIFICATION_PERMISSION_CODE
            requestPermission(perms, requestCode)
            android.util.Log.d("PermissionActivity", "handlePermissionRequest: requestPermission() called")
        }
    }

    private fun updatePermissionUI(granted: Boolean, isStorage: Boolean) {
        val imageView = if (isStorage) binding.swPermission else binding.swNotification
        imageView.setImageResource(if (granted) R.drawable.ic_sw_on else R.drawable.ic_sw_off)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val granted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        val permType = if (requestCode == RequestKey.STORAGE_PERMISSION_CODE) "Storage" else "Notification"

        android.util.Log.d("PermissionActivity", "onRequestPermissionsResult: $permType result = $granted")
        android.util.Log.d("PermissionActivity", "onRequestPermissionsResult: Before update - Storage counter = ${sharePreference.getStoragePermission()}, Notification counter = ${sharePreference.getNotificationPermission()}")

        when (requestCode) {
            RequestKey.STORAGE_PERMISSION_CODE -> viewModel.updateStorageGranted(sharePreference, granted)
            RequestKey.NOTIFICATION_PERMISSION_CODE -> viewModel.updateNotificationGranted(sharePreference, granted)
        }

        android.util.Log.d("PermissionActivity", "onRequestPermissionsResult: After update - Storage counter = ${sharePreference.getStoragePermission()}, Notification counter = ${sharePreference.getNotificationPermission()}")

        if (granted) {
            showToast(if (requestCode == RequestKey.STORAGE_PERMISSION_CODE) R.string.granted_storage else R.string.granted_notification)
        }
    }

    override fun onStart() {
        super.onStart()
        android.util.Log.d("PermissionActivity", "onStart: Before check - Storage counter = ${sharePreference.getStoragePermission()}, Notification counter = ${sharePreference.getNotificationPermission()}")

        // Just update UI state without incrementing counter
        val storageGranted = checkPermissions(viewModel.getStoragePermissions())
        val notificationGranted = checkPermissions(viewModel.getNotificationPermissions())
        android.util.Log.d("PermissionActivity", "onStart: Storage granted = $storageGranted, Notification granted = $notificationGranted")

        viewModel.checkAndUpdatePermissionState(storageGranted, isStorage = true)
        viewModel.checkAndUpdatePermissionState(notificationGranted, isStorage = false)

        android.util.Log.d("PermissionActivity", "onStart: After check - Storage counter = ${sharePreference.getStoragePermission()}, Notification counter = ${sharePreference.getNotificationPermission()}")
    }


    override fun initActionBar() {
//        binding.actionBar.tvCenter.apply {
//            text = getString(R.string.permission)
//            visible()
//        }
    }

    private fun createColoredText(
        @androidx.annotation.StringRes textRes: Int,
        @androidx.annotation.ColorRes colorRes: Int,
        font: Int = R.font.cherry_bomb_regular
    ) = StringHelper.changeColor(this, getString(textRes), colorRes, font)

    private fun handleContinue() {
        Admob.getInstance().showInterAds(this@PermissionActivity, inter, object : InterCallback() {
            override fun onNextAction() {
                super.onNextAction()
                sharePreference.setIsFirstPermission(false)
                startIntentRightToLeft(HomeActivity::class.java)
                finishAffinity()
            }
        })
    }

    override fun shouldPlayBackgroundMusic(): Boolean = false


//    override fun initAds() {
//        Admob.getInstance().loadInterAds(
//            this@PermissionActivity, getString(R.string.inter_per), object : InterCallback() {
//                override fun onAdLoadSuccess(interstitialAd: InterstitialAd?) {
//                    super.onAdLoadSuccess(interstitialAd)
//                    inter = interstitialAd
//                }
//            })
//
//        Admob.getInstance().loadNativeAd(
//            this@PermissionActivity,
//            getString(R.string.native_per),
//            binding.nativeAds,
//            R.layout.ads_native_big_btn_top
//        )
//    }
}