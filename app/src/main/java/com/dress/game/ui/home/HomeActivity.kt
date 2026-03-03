package com.dress.game.ui.home

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.animation.AnimationUtils
import androidx.lifecycle.lifecycleScope
import com.lvt.ads.util.Admob
import com.dress.game.R
import com.dress.game.core.base.BaseActivity
import com.dress.game.core.extensions.hideNavigation
import com.dress.game.core.extensions.loadNativeCollabAds
import com.dress.game.core.extensions.rateApp
import com.dress.game.core.extensions.select
import com.dress.game.core.extensions.setImageActionBar
import com.dress.game.core.extensions.showInterAll
import com.dress.game.core.extensions.startIntentRightToLeft
import com.dress.game.core.helper.LanguageHelper
import com.dress.game.core.helper.MediaHelper
import com.dress.game.core.utils.key.ValueKey
import com.dress.game.core.utils.state.RateState
import com.dress.game.databinding.ActivityHomeBinding
import com.dress.game.ui.SettingsActivity
import com.dress.game.ui.my_creation.MyCreationActivity
import com.dress.game.ui.choose_character.ChooseCharacterActivity
import com.dress.game.core.extensions.tap
import com.dress.game.core.extensions.strings
import com.dress.game.ui.random_character.RandomCharacterActivity
import com.dress.game.ui.trending.TrendingActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.system.exitProcess

class HomeActivity : BaseActivity<ActivityHomeBinding>() {

    override fun setViewBinding(): ActivityHomeBinding {
        return ActivityHomeBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        sharePreference.setCountBack(sharePreference.getCountBack() + 1)
        deleteTempFolder()
        binding.tv1.isSelected = true
      //  binding.tvTrending.isSelected = true
        binding.tv2.isSelected = true

        // Apply elastic bounce animation to app name
        val elasticBounce = AnimationUtils.loadAnimation(this, R.anim.elastic_bounce)
        binding.imvAppName.startAnimation(elasticBounce)
    }

    override fun viewListener() {
        binding.apply {
            actionBar.btnActionBarRight.tap(800) { startIntentRightToLeft(SettingsActivity::class.java) }
            btnMaker.tap(800) { startIntentRightToLeft(ChooseCharacterActivity::class.java) }
            btnMyWork.tap(800) { showInterAll { startIntentRightToLeft(MyCreationActivity::class.java) } }
           btnRandomAll.tap(800) { showInterAll {startIntentRightToLeft(RandomCharacterActivity::class.java) }}
            btnTrending.tap(800) {  startIntentRightToLeft(TrendingActivity::class.java)}
            btnOverlay.tap(800) { startIntentRightToLeft(com.dress.game.ui.pride.PrideActivity::class.java) }

        }
    }

    override fun initText() {
        super.initText()
        binding.actionBar.tvCenter.select()
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            setImageActionBar(btnActionBarRight, R.drawable.ic_settings)
        }
    }

    // Enable background music for HomeActivity
    override fun shouldPlayBackgroundMusic(): Boolean = true

    @SuppressLint("MissingSuperCall", "GestureBackNavigation")
    override fun onBackPressed() {
        if (!sharePreference.getIsRate(this) && sharePreference.getCountBack() % 2 == 0) {
            rateApp(sharePreference) { state ->
                if (state != RateState.CANCEL) {
                    showToast(R.string.have_rated)
                }
                lifecycleScope.launch {
                    withContext(Dispatchers.Main) {
                        delay(1000)
                        exitProcess(0)
                    }
                }
            }
        } else {
            exitProcess(0)
        }
    }

    private fun deleteTempFolder() {
        lifecycleScope.launch(Dispatchers.IO) {
            val dataTemp = MediaHelper.getImageInternal(this@HomeActivity, ValueKey.RANDOM_TEMP_ALBUM)
            if (dataTemp.isNotEmpty()) {
                dataTemp.forEach {
                    val file = File(it)
                    file.delete()
                }
            }
        }
    }

    private fun updateText() {
        binding.apply {
            tv1.text = strings(R.string.pride_pfp_overlay)
            tv2.text = strings(R.string.pride_maker)
            tvTrending.text = strings(R.string.trending_tv)
            tvRandomAll.text = strings(R.string.pfp_random)
            tvMyAlbum.text = strings(R.string.my_work)
        }
    }

    override fun onRestart() {
        super.onRestart()
        deleteTempFolder()
        LanguageHelper.setLocale(this)
        updateText()
        //initNativeCollab()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
        startStaggeredAnimations()

        }
    }

    private fun startStaggeredAnimations() {
        // Card 1: Slide from right (no delay)
        val slideFromRight1 = AnimationUtils.loadAnimation(this, R.anim.slide_in_right_home)
        binding.btnMaker.startAnimation(slideFromRight1)
        binding.tv1.startAnimation(slideFromRight1)


        // Card 2: Slide from left (200ms delay)
        val slideFromLeft = AnimationUtils.loadAnimation(this, R.anim.slide_in_left_home)
        binding.btnRandomAll.postDelayed({
            binding.btnRandomAll.startAnimation(slideFromLeft)
            binding.tv2.startAnimation(slideFromLeft)
        }, 200)

        // Card 3: Slide from right (400ms delay)
        val slideFromRight2 = AnimationUtils.loadAnimation(this, R.anim.slide_in_right_home)
        binding.btnMyWork.postDelayed({
            binding.btnMyWork.startAnimation(slideFromRight2)
            binding.tvTrending.startAnimation(slideFromRight2)
        }, 400)
    }

    fun initNativeCollab() {
        Admob.getInstance().loadNativeCollapNotBanner(this,getString(R.string.native_cl_home), binding.flNativeCollab)
    }

    override fun initAds() {
        initNativeCollab()
        Admob.getInstance().loadInterAll(this, getString(R.string.inter_all))
        Admob.getInstance().loadNativeAll(this, getString(R.string.native_all))
    }
}