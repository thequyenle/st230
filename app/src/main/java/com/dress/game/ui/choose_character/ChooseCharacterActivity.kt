package com.dress.game.ui.choose_character

import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.lvt.ads.event.AdmobEvent
import com.lvt.ads.util.Admob
import com.dress.game.R
import com.dress.game.ui.customize.CustomizeCharacterActivity
import com.dress.game.ui.home.DataViewModel
import com.dress.game.ui.random_character.RandomCharacterActivity
import com.dress.game.core.base.BaseActivity
import com.dress.game.core.extensions.handleBackLeftToRight
import com.dress.game.core.extensions.hideNavigation
import com.dress.game.core.extensions.loadNativeCollabAds
import com.dress.game.core.extensions.select
import com.dress.game.core.extensions.setImageActionBar
import com.dress.game.core.extensions.setTextActionBar
import com.dress.game.core.extensions.showInterAll
import com.dress.game.core.extensions.tap
import com.dress.game.core.extensions.startIntentRightToLeft
import com.dress.game.core.extensions.visible
import com.dress.game.core.helper.InternetHelper
import com.dress.game.core.utils.key.IntentKey
import com.dress.game.core.utils.key.ValueKey
import com.dress.game.core.utils.state.HandleState
import com.dress.game.databinding.ActivityChooseCharacterBinding
import kotlinx.coroutines.launch

class ChooseCharacterActivity : BaseActivity<ActivityChooseCharacterBinding>() {
    private val viewModel: ChooseCharacterViewModel by viewModels()
    private val dataViewModel: DataViewModel by viewModels()
    private val chooseCharacterAdapter by lazy { ChooseCharacterAdapter() }
    private var hasCheckedInternet = false  // Flag to check internet only once
    override fun setViewBinding(): ActivityChooseCharacterBinding {
        return ActivityChooseCharacterBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        if (dataViewModel.allData.value.isEmpty()) {
            lifecycleScope.launch { showLoading() }
        }
        initRcv()
        dataViewModel.ensureData(this)
    }

    override fun dataObservable() {
        lifecycleScope.launch {
            dataViewModel.allData.collect { data ->
                if (data.isNotEmpty()) {
                    chooseCharacterAdapter.submitList(data)

                    // Dismiss loading when data is loaded
                    dismissLoading()

                    // Check if there are API characters and user has no internet
                    checkInternetForAPICharacters(data)
                }
            }
        }
    }

    private fun checkInternetForAPICharacters(data: ArrayList<com.dress.game.data.model.custom.CustomizeModel>) {
        // Only check once per activity lifecycle
        if (hasCheckedInternet) return
        hasCheckedInternet = true

        android.util.Log.d("ChooseCharacter", "========================================")
        android.util.Log.d("ChooseCharacter", "checkInternetForAPICharacters called")
        android.util.Log.d("ChooseCharacter", "Total characters in data: ${data.size}")

        // Check if API characters are already loaded
        val hasAPICharacters = data.any { it.isFromAPI }
        val apiCount = data.count { it.isFromAPI }
        val localCount = data.count { !it.isFromAPI }

        android.util.Log.d("ChooseCharacter", "API characters: $apiCount")
        android.util.Log.d("ChooseCharacter", "Local characters: $localCount")
        android.util.Log.d("ChooseCharacter", "hasAPICharacters: $hasAPICharacters")

        // Only show notification if API characters are NOT loaded yet
        if (!hasAPICharacters) {
            android.util.Log.d("ChooseCharacter", "No API characters - checking internet...")
            InternetHelper.checkInternet(this) { state ->
                android.util.Log.d("ChooseCharacter", "Internet check result: $state")
                if (state != HandleState.SUCCESS) {
                    android.util.Log.d("ChooseCharacter", "❌ No internet - SHOWING DIALOG")
                    // No internet and no API characters loaded - notify user
                    val dialog = com.dress.game.dialog.YesNoDialog(
                        this@ChooseCharacterActivity,
                        R.string.notification,
                        R.string.internet_required_for_more_characters,
                        isError = true,  // Shows only OK button
                        dialogType = com.dress.game.dialog.DialogType.INTERNET
                    )
                    dialog.show()
                    dialog.onYesClick = {
                        dialog.dismiss()
                        hideNavigation()
                    }
                } else {
                    android.util.Log.d("ChooseCharacter", "✓ Has internet - no dialog")
                }
            }
        } else {
            android.util.Log.d("ChooseCharacter", "✓ API characters already loaded - no dialog")
        }
        android.util.Log.d("ChooseCharacter", "========================================")
    }

    override fun viewListener() {
        binding.apply {
            actionBar.btnActionBarLeft.tap { showInterAll { handleBackLeftToRight() } }
        }
        chooseCharacterAdapter.onItemClick = { position ->
            AdmobEvent.logEvent(this@ChooseCharacterActivity, "click_item_$position", null)

            android.util.Log.d("ChooseCharacter", "========================================")
            android.util.Log.d("ChooseCharacter", "Item clicked: position $position")

            // ✅ FIX: Use isFromAPI flag from character data instead of position
            val selectedCharacter = dataViewModel.allData.value.getOrNull(position)
            val needsInternet = selectedCharacter?.isFromAPI ?: false



            // Log AdMob event with detailed information
            val bundle = Bundle()
            bundle.putString("character_name", selectedCharacter?.dataName ?: "unknown")
            bundle.putString("avatar_path", selectedCharacter?.avatar ?: "unknown")
            bundle.putInt("position", position)
            bundle.putBoolean("is_from_api", needsInternet)
            AdmobEvent.logEvent(this@ChooseCharacterActivity, "click_character_item", bundle)



            android.util.Log.d("ChooseCharacter", "Character isFromAPI: $needsInternet")
            android.util.Log.d("ChooseCharacter", "Character name: ${selectedCharacter?.dataName}")

            if (needsInternet) {
                android.util.Log.d("ChooseCharacter", "API character - checking internet...")
                InternetHelper.checkInternet(this) { state ->
                    if (state == HandleState.SUCCESS) {
                        showInterAll { startIntentRightToLeft(CustomizeCharacterActivity::class.java, position) }
                    } else {
                        // Show No Internet dialog
                        val dialog = com.dress.game.dialog.YesNoDialog(
                            this@ChooseCharacterActivity,
                            R.string.no_internet,
                            R.string.please_check_your_internet,
                            isError = true,
                            dialogType = com.dress.game.dialog.DialogType.INTERNET
                        )
                        dialog.show()
                        dialog.onYesClick = {
                            dialog.dismiss()
                            hideNavigation()
                        }
                    }
                }
            } else {
                android.util.Log.d("ChooseCharacter", "Local character - navigating directly")
                android.util.Log.d("ChooseCharacter", "========================================")
                showInterAll { startIntentRightToLeft(CustomizeCharacterActivity::class.java, position) }
            }
        }
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            setImageActionBar(btnActionBarLeft, R.drawable.ic_back)
            setTextActionBar(tvCenter, getString(R.string.pride_maker))
            tvCenter.select()
        }
    }

    private fun initRcv() {
        binding.rcvCharacter.apply {
            adapter = chooseCharacterAdapter
            itemAnimator = null
        }
    }

//    fun initNativeCollab() {
//        Admob.getInstance().loadNativeCollapNotBanner(this,getString(R.string.native_cl_category), binding.flNativeCollab)
//    }
//
//    override fun initAds() {
//        initNativeCollab()
//        Admob.getInstance().loadNativeAd(
//            this,
//            getString(R.string.native_category),
//            binding.nativeAds,
//            R.layout.ads_native_banner
//        )
//    }

    override fun onRestart() {
        super.onRestart()
        //initNativeCollab()
    }

}