package com.dress.game.ui.random_character

import android.app.ActivityOptions
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.lvt.ads.util.Admob
import com.dress.game.R
import com.dress.game.ui.customize.CustomizeCharacterActivity
import com.dress.game.ui.customize.CustomizeCharacterViewModel
import com.dress.game.ui.home.DataViewModel
import com.dress.game.core.base.BaseActivity
import com.dress.game.core.extensions.dLog
import com.dress.game.core.extensions.eLog
import com.dress.game.core.extensions.handleBackLeftToRight
import com.dress.game.core.extensions.hideNavigation
import com.dress.game.core.extensions.loadNativeCollabAds
import com.dress.game.core.extensions.setImageActionBar
import com.dress.game.core.extensions.setTextActionBar
import com.dress.game.core.extensions.showInterAll
import com.dress.game.core.extensions.tap
import com.dress.game.core.extensions.startIntentRightToLeft
import com.dress.game.core.extensions.visible
import com.dress.game.core.helper.InternetHelper
import com.dress.game.core.helper.MediaHelper
import com.dress.game.core.utils.key.IntentKey
import com.dress.game.core.utils.key.ValueKey
import com.dress.game.data.model.custom.SuggestionModel
import com.dress.game.databinding.ActivityRandomCharacterBinding
import com.dress.game.dialog.YesNoDialog
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.get
import kotlin.getValue
import kotlin.text.compareTo

class RandomCharacterActivity : BaseActivity<ActivityRandomCharacterBinding>() {
    private val viewModel: RandomCharacterViewModel by viewModels()
    private val dataViewModel: DataViewModel by viewModels()
    private val customizeCharacterViewModel: CustomizeCharacterViewModel by viewModels()
    private val randomCharacterAdapter by lazy { RandomCharacterAdapter(this) }

    override fun setViewBinding(): ActivityRandomCharacterBinding {
        return ActivityRandomCharacterBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        lifecycleScope.launch { showLoading() }
        dataViewModel.ensureData(this)
    }

    override fun dataObservable() {
        lifecycleScope.launch {
            lifecycleScope.launch {
                dataViewModel.allData.collect { data ->
                    if (data.isNotEmpty()) {
                        initData()
                    }
                }
            }
        }
    }

    override fun viewListener() {
        binding.apply {
            actionBar.btnActionBarLeft.tap { showInterAll{handleBackLeftToRight()} }

        }

        randomCharacterAdapter.onItemClick = { model -> handleItemClick(model)}

    }

    override fun initActionBar() {
        binding.actionBar.apply {
            setImageActionBar(btnActionBarLeft, R.drawable.ic_back)
            setTextActionBar(tvCenter, getString(R.string.trending))
            tvCenter.isSelected =true
        }
    }

    private fun initData() {
        val handleExceptionCoroutine = CoroutineExceptionHandler { _, throwable ->
            eLog("initData: ${throwable.message}")
            CoroutineScope(Dispatchers.Main).launch {
                dismissLoading()
                val dialogExit = YesNoDialog(this@RandomCharacterActivity, R.string.error, R.string.an_error_occurred)
                dialogExit.show()
                dialogExit.onNoClick = {
                    dialogExit.dismiss()
                    finish()
                }
                dialogExit.onYesClick = {
                    dialogExit.dismiss()
                    hideNavigation()
                    startIntentRightToLeft(
                        RandomCharacterActivity::class.java, customizeCharacterViewModel.positionSelected
                    )
                    finish()
                }
            }
        }

        CoroutineScope(SupervisorJob() + Dispatchers.IO + handleExceptionCoroutine).launch {
            // Get data from list
            val deferred1 = async {
                val timeStart1 = System.currentTimeMillis()
                val hasInternet = InternetHelper.isInternetAvailable(this@RandomCharacterActivity)

                // Filter data: if no internet, show only local data (isFromAPI = false)
                val filteredData = if (hasInternet) {
                    dataViewModel.allData.value
                } else {
                    dataViewModel.allData.value.filter { !it.isFromAPI }
                }

                dLog("==========================================================")
                dLog("RandomCharacter: Starting to process ${filteredData.size} characters")
                dLog("Total data available: ${dataViewModel.allData.value.size}")
                dLog("Has Internet: $hasInternet")
                dLog("Filtered to local only: ${!hasInternet}")
                dLog("==========================================================")

                for (i in 0 until filteredData.size) {
                    try {
                        dLog("---------- Processing Character $i ----------")
                        val currentData = filteredData[i]
                        customizeCharacterViewModel.positionSelected = dataViewModel.allData.value.indexOf(currentData)
                        dLog("Character name: ${currentData.dataName}")
                        dLog("Avatar path: ${currentData.avatar}")
                        dLog("Layer count: ${currentData.layerList.size}")
                        dLog("Is from API: ${currentData.isFromAPI}")

                        customizeCharacterViewModel.setDataCustomize(currentData)
                        customizeCharacterViewModel.updateAvatarPath(currentData.avatar)

                        customizeCharacterViewModel.resetDataList()
                        customizeCharacterViewModel.addValueToItemNavList()
                        customizeCharacterViewModel.setItemColorDefault()
                        customizeCharacterViewModel.setBottomNavigationListDefault()

                        for (j in 0 until ValueKey.RANDOM_QUANTITY) {
                            customizeCharacterViewModel.setClickRandomFullLayer()
                            val suggestion = customizeCharacterViewModel.getSuggestionList()
                            dLog("Generated random $j for character $i - Avatar: ${suggestion.avatarPath}")
                            viewModel.updateRandomList(suggestion)
                        }
                        dLog("✓ Character $i completed successfully")
                    } catch (e: Exception) {
                        eLog("✗ ERROR processing character $i: ${e.message}")
                        e.printStackTrace()
                    }
                }
                viewModel.upsideDownList()

                dLog("==========================================================")
                dLog("RandomCharacter: Finished processing")
                dLog("Total time: ${System.currentTimeMillis() - timeStart1}ms")
                dLog("Final random list size: ${viewModel.randomList.size}")
                dLog("==========================================================")
                return@async true
            }

            withContext(Dispatchers.Main) {
                if (deferred1.await()) {
                    dismissLoading()
                    initRcv()
                }
            }
        }
    }

    private fun initRcv() {
        binding.rcvRandomCharacter.apply {
            adapter = randomCharacterAdapter
            itemAnimator = null

            // ✅ PERFORMANCE OPTIMIZATIONS
            // Cache more ViewHolders to avoid recreating them
            setItemViewCacheSize(20)

            // Use a shared RecycledViewPool for better performance
            setRecycledViewPool(androidx.recyclerview.widget.RecyclerView.RecycledViewPool().apply {
                setMaxRecycledViews(0, 30)
            })

            // Enable drawing cache (deprecated but can help on older devices)
            isDrawingCacheEnabled = true
            setHasFixedSize(true) // All items have the same size

        }
        dLog("==========================================================")
        dLog("initRcv: Submitting ${viewModel.randomList.size} items to adapter")
        viewModel.randomList.forEachIndexed { index, item ->
            dLog("Item $index: Avatar=${item.avatarPath}, Layers=${item.pathSelectedList.size}")
        }
        dLog("==========================================================")
        randomCharacterAdapter.submitList(viewModel.randomList)
    }

    private fun handleItemClick(model: SuggestionModel) {
        customizeCharacterViewModel.positionSelected = dataViewModel.allData.value.indexOfFirst { it.avatar == model.avatarPath }
        // ✅ FIX: Use isFromAPI flag from character data instead of position
        val selectedCharacter = dataViewModel.allData.value.getOrNull(customizeCharacterViewModel.positionSelected)
        viewModel.setIsDataAPI(selectedCharacter?.isFromAPI ?: false)
        viewModel.checkDataInternet(this@RandomCharacterActivity) {
            lifecycleScope.launch {
                showLoading()
                withContext(Dispatchers.IO) {
                    MediaHelper.writeModelToFile(this@RandomCharacterActivity, ValueKey.SUGGESTION_FILE_INTERNAL, model)
                }
                val intent = Intent(this@RandomCharacterActivity, CustomizeCharacterActivity::class.java)
                intent.putExtra(IntentKey.INTENT_KEY, customizeCharacterViewModel.positionSelected)
                intent.putExtra(IntentKey.STATUS_FROM_KEY, ValueKey.SUGGESTION)
                val option = ActivityOptions.makeCustomAnimation(
                    this@RandomCharacterActivity,
                    R.anim.slide_out_left,
                    R.anim.slide_in_right
                )
                dismissLoading()
                showInterAll { startActivity(intent, option.toBundle()) }
            }
        }
    }
//
//    fun initNativeCollab() {
//    Admob.getInstance().loadNativeCollapNotBanner(this,getString(R.string.native_cl_trending), binding.flNativeCollab)
//    }

//    override fun initAds() {
//        initNativeCollab()
//    }
//
//    override fun onRestart() {
//        super.onRestart()
//        initNativeCollab()
//    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            applyUiCustomize()
            hideNavigation(true)

            window.decorView.removeCallbacks(reHideRunnable)
            window.decorView.postDelayed(reHideRunnable, 2000)
        } else {
            window.decorView.removeCallbacks(reHideRunnable)
        }
    }

    private val reHideRunnable = Runnable {
        applyUiCustomize()
        hideNavigation(true)
    }

    @Suppress("DEPRECATION")
    private fun applyUiCustomize() {
        // Cho phép app tự vẽ màu system bar
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

        // Transparent status bar
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        // Flags
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        // nếu muốn icon status bar đen thì thêm:
        // or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
    }

    override fun onDestroy() {
        super.onDestroy()
        // ✅ Cancel all pending image processing jobs to prevent memory leaks
        randomCharacterAdapter.cancelAllJobs()
    }
}