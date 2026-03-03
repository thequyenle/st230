package com.dress.game.ui.trending

import android.animation.ObjectAnimator
import android.app.ActivityOptions
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import androidx.activity.viewModels
import androidx.core.graphics.createBitmap
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.dress.game.R
import com.dress.game.core.base.BaseActivity
import com.dress.game.core.extensions.handleBackLeftToRight
import com.dress.game.core.extensions.hideNavigation
import com.dress.game.core.extensions.setImageActionBar
import com.dress.game.core.extensions.setTextActionBar
import com.dress.game.core.extensions.showInterAll
import com.dress.game.core.extensions.tap
import com.dress.game.core.helper.InternetHelper
import com.dress.game.core.helper.MediaHelper
import com.dress.game.core.utils.key.IntentKey
import com.dress.game.core.utils.key.ValueKey
import com.dress.game.core.utils.state.SaveState
import com.dress.game.data.model.custom.SuggestionModel
import com.dress.game.databinding.ActivityTrendingBinding
import com.dress.game.dialog.YesNoDialog
import com.dress.game.ui.customize.CustomizeCharacterActivity
import com.dress.game.ui.customize.CustomizeCharacterViewModel
import com.dress.game.ui.home.DataViewModel
import com.dress.game.ui.random_character.RandomCharacterViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class TrendingActivity : BaseActivity<ActivityTrendingBinding>() {

    private val viewModel: RandomCharacterViewModel by viewModels()
    private val dataViewModel: DataViewModel by viewModels()
    private val customizeCharacterViewModel: CustomizeCharacterViewModel by viewModels()

    private var currentSuggestion: SuggestionModel? = null
    private var isAnimating = false

    override fun setViewBinding(): ActivityTrendingBinding {
        return ActivityTrendingBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        lifecycleScope.launch { showLoading() }
        dataViewModel.ensureData(this)
    }

    override fun dataObservable() {
        lifecycleScope.launch {
            dataViewModel.allData.collect { data ->
                if (data.isNotEmpty()) {
                    initData()
                }
            }
        }
    }

    override fun viewListener() {
        binding.apply {
            actionBar.btnActionBarLeft.tap { showInterAll { handleBackLeftToRight() } }
            btnGenerate.tap(0) { handleGenerate() }
            btnEdit.tap { handleEdit() }
        }
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            setImageActionBar(btnActionBarLeft, R.drawable.ic_back)
            setTextActionBar(tvCenter, getString(R.string.trending))
            tvCenter.isSelected = true
        }
    }

    private fun initData() {
        val handleExceptionCoroutine = CoroutineExceptionHandler { _, throwable ->
            CoroutineScope(Dispatchers.Main).launch {
                val dialogExit = YesNoDialog(
                    this@TrendingActivity,
                    R.string.error,
                    R.string.an_error_occurred
                )
                dialogExit.show()
                dialogExit.onNoClick = {
                    dialogExit.dismiss()
                    finish()
                }
                dialogExit.onYesClick = {
                    dialogExit.dismiss()
                    hideNavigation()
                    finish()
                }
            }
        }

        CoroutineScope(SupervisorJob() + Dispatchers.Main + handleExceptionCoroutine).launch {
            val hasInternet = withContext(Dispatchers.IO) {
                InternetHelper.isInternetAvailable(this@TrendingActivity)
            }
            val filteredData = if (hasInternet) {
                dataViewModel.allData.value
            } else {
                dataViewModel.allData.value.filter { !it.isFromAPI }
            }
            if (filteredData.isEmpty()) return@launch

            suspend fun processCharacter(data: com.dress.game.data.model.custom.CustomizeModel) {
                customizeCharacterViewModel.positionSelected =
                    dataViewModel.allData.value.indexOf(data)
                customizeCharacterViewModel.setDataCustomize(data)
                customizeCharacterViewModel.updateAvatarPath(data.avatar)
                customizeCharacterViewModel.resetDataList()
                customizeCharacterViewModel.addValueToItemNavList()
                customizeCharacterViewModel.setItemColorDefault()
                customizeCharacterViewModel.setBottomNavigationListDefault()
                for (j in 0 until ValueKey.RANDOM_QUANTITY) {
                    customizeCharacterViewModel.setClickRandomFullLayer()
                    val suggestion = customizeCharacterViewModel.getSuggestionList()
                    viewModel.updateRandomList(suggestion)
                }
            }

            // Xử lý character đầu tiên → show ngay
            withContext(Dispatchers.IO) {
                try { processCharacter(filteredData[0]) } catch (e: Exception) { e.printStackTrace() }
                viewModel.upsideDownList()
            }
            showRandomSuggestion { lifecycleScope.launch { dismissLoading() } }

            // Xử lý phần còn lại ở background
            if (filteredData.size > 1) {
                withContext(Dispatchers.IO) {
                    for (i in 1 until filteredData.size) {
                        try { processCharacter(filteredData[i]) } catch (e: Exception) { e.printStackTrace() }
                    }
                    viewModel.upsideDownList()
                }
            }
        }
    }

    private fun showRandomSuggestion(onComplete: (() -> Unit)? = null) {
        if (viewModel.randomList.isEmpty()) {
            onComplete?.invoke()
            return
        }
        val model = viewModel.randomList.random()
        currentSuggestion = model
        renderSuggestion(model, onComplete)
    }

    private fun handleGenerate() {
        if (viewModel.randomList.isEmpty()) return
        if (isAnimating) return
        isAnimating = true
        binding.btnGenerate.visibility = View.INVISIBLE
        binding.btnEdit.visibility = View.INVISIBLE

        val totalDuration = 800L

        // Show GIF while generating
        Glide.with(this).asGif().load(R.drawable.gif).into(binding.imvImage)

        // Dice: spin 3 full rounds, decelerating like a real dice roll
        val diceAnim = ObjectAnimator.ofFloat(binding.dices, "rotation", 0f, 1080f).apply {
            duration = totalDuration
            interpolator = DecelerateInterpolator(2f)
            start()
        }

        lifecycleScope.launch {
            delay(totalDuration)

            diceAnim.cancel()
            binding.dices.rotation = 0f

            // Check internet sau khi delay xong, timeout 3s để tránh hang khi mất mạng
            val hasInternet = withContext(Dispatchers.IO) {
                try {
                    withTimeout(3000) { InternetHelper.isInternetAvailable(this@TrendingActivity) }
                } catch (e: TimeoutCancellationException) {
                    false
                }
            }
            val availableList = if (hasInternet) {
                viewModel.randomList
            } else {
                viewModel.randomList.filter { model ->
                    val character = dataViewModel.allData.value.firstOrNull { it.avatar == model.avatarPath }
                    character?.isFromAPI != true
                }
            }
            val finalModel = availableList.randomOrNull() ?: run {
                isAnimating = false
                binding.btnGenerate.visibility = View.VISIBLE
                binding.btnEdit.visibility = View.VISIBLE
                return@launch
            }

            currentSuggestion = finalModel
            renderSuggestion(finalModel) {
                isAnimating = false
                binding.btnGenerate.visibility = View.VISIBLE
                binding.btnEdit.visibility = View.VISIBLE
            }
        }
    }

    private fun renderSuggestion(model: SuggestionModel, onComplete: (() -> Unit)? = null) {
        android.util.Log.d("TrendingDebug", "renderSuggestion() called | pathInternalRandom='${model.pathInternalRandom}' | pathSelectedList.size=${model.pathSelectedList.size} | avatarPath='${model.avatarPath}'")

        if (model.pathInternalRandom.isNotEmpty()) {
            android.util.Log.d("TrendingDebug", "  → pathInternalRandom không rỗng, load trực tiếp")
            Glide.with(this)
                .load(model.pathInternalRandom)
                .listener(glideListener(onComplete))
                .into(binding.imvImage)
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val paths = model.pathSelectedList.filter { it.isNotEmpty() }
                android.util.Log.d("TrendingDebug", "  → pathInternalRandom rỗng, tính từ pathSelectedList")
                android.util.Log.d("TrendingDebug", "     pathSelectedList raw (${model.pathSelectedList.size} item): ${model.pathSelectedList}")
                android.util.Log.d("TrendingDebug", "     paths sau filter notEmpty (${paths.size} item): $paths")
                if (paths.isEmpty()) {
                    android.util.Log.w("TrendingDebug", "  !! paths.isEmpty() → onComplete gọi không có ảnh nào, GIF sẽ còn quay!")
                    withContext(Dispatchers.Main) { onComplete?.invoke() }
                    return@launch
                }

                val bitmapDefault = Glide.with(this@TrendingActivity)
                    .asBitmap().load(paths.first()).submit().get()
                val width = bitmapDefault.width / 2
                val height = bitmapDefault.height / 2

                val listBitmap = ArrayList<Bitmap>()
                paths.forEach { path ->
                    listBitmap.add(
                        Glide.with(this@TrendingActivity)
                            .asBitmap().load(path).submit(width, height).get()
                    )
                }

                val combinedBitmap = createBitmap(width, height)
                val canvas = Canvas(combinedBitmap)
                for (bitmap in listBitmap) {
                    val left = (width - bitmap.width) / 2f
                    val top = (height - bitmap.height) / 2f
                    canvas.drawBitmap(bitmap, left, top, null)
                }

                MediaHelper.saveBitmapToInternalStorage(
                    this@TrendingActivity,
                    ValueKey.RANDOM_TEMP_ALBUM,
                    combinedBitmap
                ).collect { state ->
                    android.util.Log.d("TrendingDebug", "  → saveBitmapToInternalStorage state: $state")
                    if (state is SaveState.Success) {
                        model.pathInternalRandom = state.path
                        android.util.Log.d("TrendingDebug", "     Save thành công: path='${state.path}'")
                    }
                }

                android.util.Log.d("TrendingDebug", "  → Sau save: pathInternalRandom='${model.pathInternalRandom}'")
                withContext(Dispatchers.Main) {
                    if (model.pathInternalRandom.isEmpty()) {
                        android.util.Log.w("TrendingDebug", "  !! pathInternalRandom vẫn rỗng sau save → Glide.load('') sẽ fail, GIF còn quay!")
                    }
                    Glide.with(this@TrendingActivity)
                        .load(model.pathInternalRandom)
                        .listener(glideListener(onComplete))
                        .into(binding.imvImage)
                }
            } catch (e: Exception) {
                android.util.Log.e("TrendingDebug", "  !! Exception trong renderSuggestion: ${e::class.simpleName}: ${e.message}", e)
                withContext(Dispatchers.Main) { onComplete?.invoke() }
            }
        }
    }

    private fun glideListener(onComplete: (() -> Unit)?): RequestListener<android.graphics.drawable.Drawable> {
        return object : RequestListener<android.graphics.drawable.Drawable> {
            override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<android.graphics.drawable.Drawable>, isFirstResource: Boolean): Boolean {
                android.util.Log.e("TrendingDebug", "  !! Glide.onLoadFailed: model='$model' | cause=${e?.causes?.joinToString { it.message ?: it::class.simpleName ?: "?" }}")
                onComplete?.invoke()
                return false
            }
            override fun onResourceReady(resource: android.graphics.drawable.Drawable, model: Any, target: Target<android.graphics.drawable.Drawable>?, dataSource: DataSource, isFirstResource: Boolean): Boolean {
                android.util.Log.d("TrendingDebug", "  ✓ Glide.onResourceReady: model='$model' | source=$dataSource")
                onComplete?.invoke()
                return false
            }
        }
    }

    private fun handleEdit() {
        val suggestion = currentSuggestion ?: return
        customizeCharacterViewModel.positionSelected =
            dataViewModel.allData.value.indexOfFirst { it.avatar == suggestion.avatarPath }
        val selectedCharacter =
            dataViewModel.allData.value.getOrNull(customizeCharacterViewModel.positionSelected)
        viewModel.setIsDataAPI(selectedCharacter?.isFromAPI ?: false)
        viewModel.checkDataInternet(this) {
            lifecycleScope.launch {
                showLoading()
                withContext(Dispatchers.IO) {
                    MediaHelper.writeModelToFile(
                        this@TrendingActivity,
                        ValueKey.SUGGESTION_FILE_INTERNAL,
                        suggestion
                    )
                }
                val intent = Intent(this@TrendingActivity, CustomizeCharacterActivity::class.java)
                intent.putExtra(IntentKey.INTENT_KEY, customizeCharacterViewModel.positionSelected)
                intent.putExtra(IntentKey.STATUS_FROM_KEY, ValueKey.SUGGESTION)
                val option = ActivityOptions.makeCustomAnimation(
                    this@TrendingActivity,
                    R.anim.slide_out_left,
                    R.anim.slide_in_right
                )
                dismissLoading()
                showInterAll { startActivity(intent, option.toBundle()) }
            }
        }
    }

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
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }
}
