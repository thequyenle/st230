package com.dress.game.ui.pride

import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import com.dress.game.core.custom.drawview.PreviewView
import android.net.Uri
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.SeekBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.dress.game.R
import com.dress.game.core.base.BaseActivity
import com.dress.game.core.extensions.gone
import com.dress.game.core.extensions.select
import com.dress.game.core.extensions.tap
import com.dress.game.core.extensions.visible
import com.dress.game.core.helper.MediaHelper
import com.dress.game.data.model.pride.CustomFlagModel
import com.dress.game.data.model.pride.LayoutStyle
import com.dress.game.data.model.pride.PrideFlagData
import com.dress.game.data.model.pride.PrideFlagModel
import com.dress.game.databinding.ActivityPrideBinding
import com.dress.game.ui.my_creation.MyCreationActivity
import com.dress.game.ui.pride.adapter.PrideFlagAdapter
import com.dress.game.ui.pride.adapter.SelectedFlagChipAdapter
import com.dress.game.core.extensions.startIntentRightToLeft
import com.dress.game.core.utils.key.IntentKey
import com.dress.game.core.utils.key.ValueKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PrideActivity : BaseActivity<ActivityPrideBinding>() {

    private var currentStep = 1
    private val totalSteps = 6

    // State
    private var selectedImageBitmap: Bitmap? = null
    private var croppedBitmap: Bitmap? = null
    private var selectedLayout = LayoutStyle.CIRCLE
    private var imageZoom = 0.5f
    private var ringScale = 0.3f
    private var flagModeRing = true
    private var hasCropResized = false
    private var resultBitmap: Bitmap? = null
    private var imageOffsetX = 0f
    private var imageOffsetY = 0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var previewJob: Job? = null
    private var cachedFlagBitmap: Bitmap? = null

    private val allFlags = PrideFlagData.getFlags().toMutableList()
    private val selectedFlags = mutableListOf<PrideFlagModel>()
    private val customFlags = mutableListOf<CustomFlagModel>()
    private val customFlagItems = mutableListOf<PrideFlagModel>()

    private lateinit var flagAdapter: PrideFlagAdapter
    private lateinit var customFlagAdapter: PrideFlagAdapter
    private lateinit var chipAdapter: SelectedFlagChipAdapter

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { loadImage(it) }
    }

    override fun setViewBinding() = ActivityPrideBinding.inflate(LayoutInflater.from(this))

    override fun initView() {
        val saved = sharePreference.getCustomFlags()
        customFlags.addAll(saved)
        saved.forEachIndexed { index, customFlag ->
            customFlagItems.add(PrideFlagModel(
                id = 100 + index + 1,
                name = customFlag.name,
                assetPath = "",
                isSelected = false,
                customColors = customFlag.colors.toList()
            ))
        }
        setupFlagAdapter()
        setupChipAdapter()
    }

    override fun viewListener() {
        binding.apply {
            // Upload box tap (step 1)
            uploadBox.tap { pickImageLauncher.launch("image/*") }
            btnClearImage.tap { clearSelectedImage() }
            btnChangeImage.tap { pickImageLauncher.launch("image/*") }

            // Step 2 crop buttons
            btnCropReset.tap {
                cropView.resetPoints()
                hasCropResized = false
                setStep2ButtonsEnabled(false)
            }
            btnCropCenter.tap {
                cropView.centerPoints()
                if (!hasCropResized) {
                    setStep2ButtonsEnabled(false)
                } else {
                    binding.btnCropCenter.isEnabled = false
                    binding.btnCropCenter.alpha = 0.4f
                }
            }

            // Step 3 custom flag
            btnAddCustomFlag.tap { openCreateCustomFlagDialog() }

            // Step 4 layout options
            optionCircle.tap { selectLayoutOption(LayoutStyle.CIRCLE) }
            optionSquare.tap { selectLayoutOption(LayoutStyle.SQUARE) }
            optionBackground.tap { selectLayoutOption(LayoutStyle.BACKGROUND) }

            // Step 5 sliders
            seekImageZoom.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    imageZoom = progress / 100f
                    updatePreview()
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
            seekRingScale.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    ringScale = progress / 100f
                    updatePreview()
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
            btnDefaultZoom.tap {
                seekImageZoom.progress = 50
                imageZoom = 0.5f
                updatePreview()
            }
            btnDefaultRing.tap {
                seekRingScale.progress = 30
                ringScale = 0.3f
                updatePreview()
            }
            btnCenterImage.tap {
                imageOffsetX = 0f
                imageOffsetY = 0f
                imgPreview.resetOffset()
            }
            imgPreview.setOnTouchListener { view, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        lastTouchX = event.x
                        lastTouchY = event.y
                        view.parent.requestDisallowInterceptTouchEvent(true)
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.x - lastTouchX
                        val dy = event.y - lastTouchY
                        lastTouchX = event.x
                        lastTouchY = event.y
                        imgPreview.userOffsetX += dx
                        imgPreview.userOffsetY += dy
                        imgPreview.invalidate()
                        val scale = 400f / view.width
                        imageOffsetX += dx * scale
                        imageOffsetY += dy * scale
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        view.parent.requestDisallowInterceptTouchEvent(false)
                        true
                    }
                    else -> false
                }
            }
            switchFlagMode.tap {
                flagModeRing = !flagModeRing
                switchFlagMode.setImageResource(
                    if (flagModeRing) R.drawable.ic_sw_on else R.drawable.ic_sw_off
                )
                updatePreview()
            }

            // Step 6 buttons
            btnDownload.tap { downloadResult() }
            btnCreateAnother.tap {
                finish()
                startActivity(intent)
                overridePendingTransition(0, 0)
            }
            btnMyWork.tap {
                startIntentRightToLeft(MyCreationActivity::class.java, IntentKey.TAB_KEY, ValueKey.PRIDE_OVERLAY_TYPE)
            }

            // Bottom nav
            btnPrevious.tap { goToPreviousStep() }
            btnStartOver.tap { resetToStep1() }
            btnContinue.tap { goToNextStep() }
        }
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            btnActionBarLeft.setImageResource(R.drawable.ic_back)
            btnActionBarLeft.visible()
            btnActionBarLeft.tap { handleBackLeftToRight() }
            tvCenter.text = getString(R.string.pride_pfp_overlay)
            tvCenter.visible()
            tvCenter.select()
            btnActionBarRight.setImageResource(R.drawable.ic_home)
            btnActionBarRight.tap { handleBackLeftToRight() }
        }
    }

    override fun dataObservable() {}

    // ==================== Navigation ====================

    private fun goToNextStep() {
        if (currentStep == 1 && selectedImageBitmap == null) {
            binding.errorNoImage.visible()
            return
        }
        if (currentStep == 2) {
            performCrop()
        }
        if (currentStep == 3 && selectedFlags.isEmpty()) {
            showToast(R.string.pride_select_at_least_one)
            return
        }
        if (currentStep < totalSteps) {
            if (currentStep == 4) {
                imageOffsetX = 0f
                imageOffsetY = 0f
                generatePreviewBitmap()
                applyPreviewCorner()
            }
            if (currentStep == 5) {
                resultBitmap = generateFinalBitmap()
                binding.imgResult.setImageBitmap(resultBitmap)
                // Auto-save to internal PRIDE_ALBUM for My Work tab 3
                resultBitmap?.let { bmp ->
                    lifecycleScope.launch {
                        MediaHelper.saveBitmapToInternalStorage(
                            this@PrideActivity,
                            ValueKey.PRIDE_ALBUM,
                            bmp.copy(bmp.config ?: Bitmap.Config.ARGB_8888, false)
                        ).collect {}
                    }
                }
            }
            currentStep++
            updateStep()
            // Set CropView image right after switching to step 2
            if (currentStep == 2) {
                selectedImageBitmap?.let { binding.cropView.setImageBitmap(it) }
            }
        }
    }

    private fun goToPreviousStep() {
        if (currentStep > 1) {
            currentStep--
            updateStep()
        }
    }

    private fun resetToStep1() {
        currentStep = 1
        selectedImageBitmap = null
        croppedBitmap = null
        hasCropResized = false
        selectedFlags.clear()
        allFlags.forEach { it.isSelected = false }
        customFlags.clear()
        customFlagItems.clear()
        selectedLayout = LayoutStyle.CIRCLE
        imageZoom = 0.5f
        ringScale = 0.3f
        flagModeRing = true
        resultBitmap = null
        imageOffsetX = 0f
        imageOffsetY = 0f
        cachedFlagBitmap = null
        binding.seekImageZoom.progress = 50
        binding.seekRingScale.progress = 30
        binding.switchFlagMode.setImageResource(R.drawable.ic_sw_on)
        clearSelectedImage()
        flagAdapter.setMaxReached(false)
        customFlagAdapter.setMaxReached(false)
        flagAdapter.submitList(allFlags.toList())
        chipAdapter.submitList(emptyList())
        updateStep()
    }

    private fun updateStep() {
        val steps = listOf(
            binding.step1Layout,
            binding.step2Layout,
            binding.step3Layout,
            binding.step4Layout,
            binding.step5Layout,
            binding.step6Layout
        )
        steps.forEachIndexed { index, view ->
            view.visibility = if (index + 1 == currentStep) View.VISIBLE else View.GONE
        }
        if (currentStep == 2) {
            setStep2ButtonsEnabled(hasCropResized)
            binding.cropView.onChanged = { isResize ->
                hasCropResized = true
                setStep2ButtonsEnabled(true)
            }
        }
        binding.actionBar.btnActionBarRight.visibility =
            if (currentStep == 6) View.VISIBLE else View.GONE
        updateDots()
        updateStepLabel()
        updateBottomNav()
    }

    private fun setStep2ButtonsEnabled(enabled: Boolean) {
        binding.btnCropReset.isEnabled = enabled
        binding.btnCropCenter.isEnabled = enabled
        binding.btnCropReset.alpha = if (enabled) 1f else 0.4f
        binding.btnCropCenter.alpha = if (enabled) 1f else 0.4f
    }

    private fun updateDots() {
        val dots = listOf(
            binding.dot1, binding.dot2, binding.dot3,
            binding.dot4, binding.dot5, binding.dot6
        )
        dots.forEachIndexed { index, dot ->
            val stepNum = index + 1
            val bg = when {
                stepNum < currentStep -> R.drawable.bg_pride_step_done
                stepNum == currentStep -> R.drawable.bg_pride_step_active
                else -> R.drawable.bg_pride_step_inactive
            }
            dot.setBackgroundResource(bg)
        }
    }

    private fun updateStepLabel() {
        binding.tvStepCounter.text = "$currentStep/$totalSteps"
        val label = when (currentStep) {
            1 -> R.string.pride_choose_image
            2 -> R.string.pride_crop_image
            3 -> R.string.pride_choose_flags
            4 -> R.string.pride_choose_layout
            5 -> R.string.pride_choose_design
            6 -> R.string.pride_result_title
            else -> R.string.pride_choose_image
        }
        binding.tvStepLabel.setText(label)
    }

    private fun updateBottomNav() {
        binding.layoutBottomNav.visibility =
            if (currentStep == 6) View.GONE else View.VISIBLE
        val previousColor = if (currentStep == 1)
            getColor(R.color.pride_btn_inactive)
        else
            getColor(R.color.pride_btn_active)
        binding.btnPrevious.setTextColor(previousColor)
        binding.btnPrevious.compoundDrawableTintList = ColorStateList.valueOf(previousColor)
        updateStartOverButton()
    }

    private fun updateStartOverButton() {
        val hasImage = selectedImageBitmap != null || currentStep > 1
        val color = if (hasImage)
            getColor(R.color.pride_btn_active)
        else
            getColor(R.color.pride_btn_inactive)
        binding.btnStartOver.setTextColor(color)
        binding.btnStartOver.compoundDrawableTintList = ColorStateList.valueOf(color)
        binding.btnStartOver.isEnabled = hasImage
    }

    // ==================== Step 1: Choose Image ====================

    private fun loadImage(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            selectedImageBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            hasCropResized = false
            binding.imgSelectedPreview.setImageBitmap(selectedImageBitmap)
            binding.emptyUploadState.gone()
            binding.loadedImageState.visible()
            binding.btnClearImage.visible()
            binding.btnChangeImage.visible()
            binding.errorNoImage.gone()
            updateStartOverButton()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun clearSelectedImage() {
        selectedImageBitmap = null
        binding.emptyUploadState.visible()
        binding.loadedImageState.gone()
        binding.btnClearImage.gone()
        binding.btnChangeImage.gone()
        binding.errorNoImage.gone()
        updateStartOverButton()
    }

    // ==================== Step 2: Crop ====================

    private fun performCrop() {
        val bytes = binding.cropView.getCroppedImage()
        if (bytes != null) {
            croppedBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } else {
            croppedBitmap = selectedImageBitmap
        }
    }


    // ==================== Step 3: Choose Flags ====================

    private fun setupFlagAdapter() {
        val onFlagClick: (PrideFlagModel) -> Unit = { flag ->
            cachedFlagBitmap = null
            if (flag.isSelected) {
                flag.isSelected = false
                selectedFlags.remove(flag)
            } else {
                if (selectedFlags.size < 4) {
                    flag.isSelected = true
                    selectedFlags.add(flag)
                }
            }
            val maxReached = selectedFlags.size >= 4
            flagAdapter.setMaxReached(maxReached)
            customFlagAdapter.setMaxReached(maxReached)
            chipAdapter.submitList(selectedFlags.toList())
            binding.selectedFlagsBar.visibility =
                if (selectedFlags.isNotEmpty()) View.VISIBLE else View.GONE
            flagAdapter.submitList(allFlags.toList())
            customFlagAdapter.submitList(customFlagItems.toList())
        }

        flagAdapter = PrideFlagAdapter(this, onFlagClick)
        binding.rvFlags.apply {
            layoutManager = GridLayoutManager(this@PrideActivity, 3)
            adapter = flagAdapter
        }
        flagAdapter.submitList(allFlags.toList())

        customFlagAdapter = PrideFlagAdapter(this, onFlagClick)
        binding.rvCustomFlags.apply {
            layoutManager = GridLayoutManager(this@PrideActivity, 3)
            adapter = customFlagAdapter
        }
        customFlagAdapter.submitList(customFlagItems.toList())
    }

    private fun setupChipAdapter() {
        chipAdapter = SelectedFlagChipAdapter(this) { flag ->
            cachedFlagBitmap = null
            flag.isSelected = false
            selectedFlags.remove(flag)
            val maxReached = selectedFlags.size >= 4
            flagAdapter.setMaxReached(maxReached)
            customFlagAdapter.setMaxReached(maxReached)
            chipAdapter.submitList(selectedFlags.toList())
            binding.selectedFlagsBar.visibility =
                if (selectedFlags.isNotEmpty()) View.VISIBLE else View.GONE
            flagAdapter.submitList(allFlags.toList())
            customFlagAdapter.submitList(customFlagItems.toList())
        }
        binding.rvSelectedChips.apply {
            layoutManager = LinearLayoutManager(this@PrideActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = chipAdapter
        }
    }

    private fun openCreateCustomFlagDialog() {
        val dialog = CreateCustomFlagDialog(this)
        dialog.show()
        dialog.onCloseEvent = { dialog.dismiss() }
        dialog.onCreateEvent = { customFlag ->
            dialog.dismiss()
            customFlags.add(0, customFlag)
            sharePreference.setCustomFlags(customFlags)
            val newFlag = PrideFlagModel(
                id = 100 + customFlags.size,
                name = customFlag.name,
                assetPath = "",
                isSelected = false,
                customColors = customFlag.colors.toList()
            )
            customFlagItems.add(0, newFlag)
            customFlagAdapter.submitList(customFlagItems.toList())
        }
    }

    // ==================== Step 4: Layout Style ====================

    private fun selectLayoutOption(style: LayoutStyle) {
        selectedLayout = style
        binding.optionCircle.setBackgroundResource(
            if (style == LayoutStyle.CIRCLE) R.drawable.bg_pride_layout_option_selected
            else R.drawable.bg_pride_layout_option
        )
        binding.optionSquare.setBackgroundResource(
            if (style == LayoutStyle.SQUARE) R.drawable.bg_pride_layout_option_selected
            else R.drawable.bg_pride_layout_option
        )
        binding.optionBackground.setBackgroundResource(
            if (style == LayoutStyle.BACKGROUND) R.drawable.bg_pride_layout_option_selected
            else R.drawable.bg_pride_layout_option
        )
    }

    // ==================== Step 5: Design Style ====================

    private fun generatePreviewBitmap() {
        updatePreview()
    }

    private fun applyPreviewCorner() {
        val needRound = selectedLayout == LayoutStyle.CIRCLE
                || selectedLayout == LayoutStyle.BACKGROUND
                || selectedLayout == LayoutStyle.SQUARE
        val radius = when (selectedLayout) {
            LayoutStyle.CIRCLE -> 12f * resources.displayMetrics.density
            else -> 14f * resources.displayMetrics.density
        }
        binding.imgPreview.apply {
            if (needRound) {
                outlineProvider = object : android.view.ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: android.graphics.Outline) {
                        outline.setRoundRect(0, 0, view.width, view.height, radius)
                    }
                }
                clipToOutline = true
            } else {
                clipToOutline = false
                outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
            }
        }
    }

    private fun updatePreview() {
        previewJob?.cancel()
        previewJob = lifecycleScope.launch {
            val source = croppedBitmap ?: selectedImageBitmap ?: return@launch
            var baseBmp: Bitmap? = null
            var userBmp: Bitmap? = null
            var clipR = 0f
            var overlayBmp: Bitmap? = null
            withContext(Dispatchers.Default) {
                val flagBmp = buildFlagBitmap(400, 400)
                baseBmp = buildBaseLayer(flagBmp, 400)
                val user = if (flagModeRing) buildUserLayer(source, 400) else null
                userBmp = user?.first
                clipR = user?.second ?: 0f
                overlayBmp = buildRingOverlay(flagBmp, 400)
            }
            if (!isActive) return@launch
            binding.imgPreview.baseBitmap = baseBmp
            binding.imgPreview.userBitmap = userBmp
            binding.imgPreview.clipRadius = clipR
            binding.imgPreview.overlayBitmap = overlayBmp
            val vw = binding.imgPreview.width.toFloat().takeIf { it > 0f } ?: 400f
            binding.imgPreview.userOffsetX = imageOffsetX * (vw / 400f)
            binding.imgPreview.userOffsetY = imageOffsetY * (vw / 400f)
            binding.imgPreview.invalidate()
        }
    }

    private fun buildBaseLayer(flagBitmap: Bitmap, size: Int): Bitmap {
        val result = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        return when (selectedLayout) {
            LayoutStyle.CIRCLE -> {
                val cornerRadius = size * 0.06f
                val path = Path()
                path.addRoundRect(RectF(0f, 0f, size.toFloat(), size.toFloat()), cornerRadius, cornerRadius, Path.Direction.CW)
                canvas.save()
                canvas.clipPath(path)
                canvas.drawBitmap(flagBitmap, null, RectF(0f, 0f, size.toFloat(), size.toFloat()), paint)
                canvas.restore()
                result
            }
            LayoutStyle.SQUARE, LayoutStyle.BACKGROUND -> {
                canvas.drawBitmap(flagBitmap, null, RectF(0f, 0f, size.toFloat(), size.toFloat()), paint)
                result
            }
        }
    }

    private fun buildRingOverlay(flagBitmap: Bitmap, size: Int): Bitmap? {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }
        return when (selectedLayout) {
            LayoutStyle.SQUARE -> {
                val overlay = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                val c = Canvas(overlay)
                c.drawBitmap(flagBitmap, null, RectF(0f, 0f, size.toFloat(), size.toFloat()), paint)
                val thickness = size * (0.05f + ringScale * 0.2f)
                c.drawRect(thickness, thickness, size - thickness, size - thickness, clearPaint)
                overlay
            }
            LayoutStyle.BACKGROUND -> {
                val overlay = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                val c = Canvas(overlay)
                c.drawBitmap(flagBitmap, null, RectF(0f, 0f, size.toFloat(), size.toFloat()), paint)
                val padding = size * (0.05f + ringScale * 0.2f)
                val innerSize = size - padding * 2
                val holeRadius = innerSize * 0.08f
                c.drawRoundRect(RectF(padding, padding, size - padding, size - padding), holeRadius, holeRadius, clearPaint)
                overlay
            }
            else -> null
        }
    }

    private fun buildUserLayer(source: Bitmap, size: Int): Pair<Bitmap, Float> {
        return when (selectedLayout) {
            LayoutStyle.CIRCLE -> {
                val ringThickness = size * (0.1f + ringScale * 0.3f)
                val innerRadius = size / 2f - ringThickness
                val scaledSize = ((innerRadius * 2) * (0.5f + imageZoom * 0.8f)).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(source, scaledSize, scaledSize, true) to innerRadius
            }
            LayoutStyle.SQUARE -> {
                val ringThickness = size * (0.05f + ringScale * 0.2f)
                val innerSize = size - ringThickness * 2
                val scaledSize = (innerSize * (0.5f + imageZoom * 0.8f)).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(source, scaledSize, scaledSize, true) to 0f
            }
            LayoutStyle.BACKGROUND -> {
                val padding = size * (0.05f + ringScale * 0.2f)
                val availableSize = size - padding * 2
                val scaledSize = (availableSize * (0.3f + imageZoom * 0.7f)).toInt().coerceAtLeast(1)
                val scaled = Bitmap.createScaledBitmap(source, scaledSize, scaledSize, true)
                val rounded = Bitmap.createBitmap(scaledSize, scaledSize, Bitmap.Config.ARGB_8888)
                val c = Canvas(rounded)
                val p = Paint(Paint.ANTI_ALIAS_FLAG)
                val radius = scaledSize * 0.12f
                val path = Path()
                path.addRoundRect(RectF(0f, 0f, scaledSize.toFloat(), scaledSize.toFloat()), radius, radius, Path.Direction.CW)
                c.clipPath(path)
                c.drawBitmap(scaled, 0f, 0f, p)
                scaled.recycle()
                rounded to 0f
            }
        }
    }

    // ==================== Rendering ====================

    private fun generateFinalBitmap(previewSize: Int = 800): Bitmap {
        val source = croppedBitmap ?: selectedImageBitmap
        ?: Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)

        val size = previewSize
        val flagBitmap = buildFlagBitmap(size, size)

        return when (selectedLayout) {
            LayoutStyle.CIRCLE -> renderCircle(source, flagBitmap, size)
            LayoutStyle.SQUARE -> renderSquare(source, flagBitmap, size)
            LayoutStyle.BACKGROUND -> renderBackground(source, flagBitmap, size)
        }
    }

    private fun buildFlagBitmap(width: Int, height: Int): Bitmap {
        if (width == 400 && height == 400) {
            cachedFlagBitmap?.let { return it }
        }
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val flags = if (selectedFlags.isNotEmpty()) selectedFlags else
            listOf(PrideFlagModel(1, "Default", "flag/1.jpg"))

        val partWidth = width.toFloat() / flags.size

        flags.forEachIndexed { index, flag ->
            if (flag.assetPath.isNotEmpty()) {
                try {
                    val stream = assets.open(flag.assetPath)
                    val flagBmp = BitmapFactory.decodeStream(stream)
                    stream.close()
                    val dst = RectF(
                        index * partWidth, 0f,
                        (index + 1) * partWidth, height.toFloat()
                    )
                    canvas.drawBitmap(flagBmp, null, dst, paint)
                    flagBmp.recycle()
                } catch (e: Exception) {
                    // Custom flag - find in customFlags and draw colors
                    val customFlag = customFlags.find { it.name == flag.name }
                    if (customFlag != null) {
                        drawCustomFlagSlice(canvas, customFlag, index * partWidth, 0f,
                            (index + 1) * partWidth, height.toFloat())
                    }
                }
            } else {
                // Custom flag
                val customFlag = customFlags.find { it.name == flag.name }
                if (customFlag != null) {
                    drawCustomFlagSlice(canvas, customFlag, index * partWidth, 0f,
                        (index + 1) * partWidth, height.toFloat())
                }
            }
        }
        if (width == 400 && height == 400) cachedFlagBitmap = result
        return result
    }

    private fun drawCustomFlagSlice(
        canvas: Canvas, flag: CustomFlagModel,
        left: Float, top: Float, right: Float, bottom: Float
    ) {
        val paint = Paint()
        val sliceHeight = (bottom - top) / flag.colors.size
        flag.colors.forEachIndexed { i, color ->
            paint.color = color
            canvas.drawRect(left, top + i * sliceHeight, right, top + (i + 1) * sliceHeight, paint)
        }
    }

    private fun renderCircle(source: Bitmap, flagBitmap: Bitmap, size: Int): Bitmap {
        val result = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Draw flag clipped to rounded square (12dp proportional)
        val cornerRadius = size * 0.06f
        val path = Path()
        path.addRoundRect(RectF(0f, 0f, size.toFloat(), size.toFloat()), cornerRadius, cornerRadius, Path.Direction.CW)
        canvas.save()
        canvas.clipPath(path)
        canvas.drawBitmap(flagBitmap, null, RectF(0f, 0f, size.toFloat(), size.toFloat()), paint)
        canvas.restore()

        if (flagModeRing) {
            // Draw user image in center (keep same ring thickness formula for seekbar behavior)
            val ringThickness = size * (0.1f + ringScale * 0.3f)
            val innerSize = size - ringThickness * 2
            val cx = size / 2f
            val cy = size / 2f

            val zoom = 0.5f + imageZoom * 0.8f
            val scaledSize = (innerSize * zoom).toInt().coerceAtLeast(1)
            val scaledUser = Bitmap.createScaledBitmap(source, scaledSize, scaledSize, true)
            val scaleFactor = size / 400f
            val offsetX = cx - scaledSize / 2f + imageOffsetX * scaleFactor
            val offsetY = cy - scaledSize / 2f + imageOffsetY * scaleFactor
            canvas.drawBitmap(scaledUser, offsetX, offsetY, paint)
        }

        return result
    }

    private fun renderSquare(source: Bitmap, flagBitmap: Bitmap, size: Int): Bitmap {
        val result = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Draw flag as background
        canvas.drawBitmap(flagBitmap, null, RectF(0f, 0f, size.toFloat(), size.toFloat()), paint)

        if (flagModeRing) {
            // Draw user image in center square
            val ringThickness = size * (0.05f + ringScale * 0.2f)
            val innerSize = size - ringThickness * 2

            val zoom = 0.5f + imageZoom * 0.8f
            val scaledSize = (innerSize * zoom).toInt().coerceAtLeast(1)
            val scaledUser = Bitmap.createScaledBitmap(source, scaledSize, scaledSize, true)
            val scaleFactor = size / 400f
            val offsetX = (size - scaledSize) / 2f + imageOffsetX * scaleFactor
            val offsetY = (size - scaledSize) / 2f + imageOffsetY * scaleFactor
            canvas.drawBitmap(scaledUser, offsetX, offsetY, paint)
        }

        // Draw ring overlay on top (flag with center hole)
        val ringOverlay = buildRingOverlay(flagBitmap, size)
        if (ringOverlay != null) canvas.drawBitmap(ringOverlay, 0f, 0f, paint)

        return result
    }

    private fun renderBackground(source: Bitmap, flagBitmap: Bitmap, size: Int): Bitmap {
        val result = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Draw flag as full background
        canvas.drawBitmap(flagBitmap, null, RectF(0f, 0f, size.toFloat(), size.toFloat()), paint)

        // Draw user image centered on top with rounded corners
        val padding = size * (0.05f + ringScale * 0.2f)
        val availableSize = size - padding * 2
        val zoom = 0.3f + imageZoom * 0.7f
        val scaledSize = (availableSize * zoom).toInt().coerceAtLeast(1)
        val scaledUser = Bitmap.createScaledBitmap(source, scaledSize, scaledSize, true)
        val scaleFactor = size / 400f
        val offsetX = (size - scaledSize) / 2f + imageOffsetX * scaleFactor
        val offsetY = (size - scaledSize) / 2f + imageOffsetY * scaleFactor
        val radius = scaledSize * 0.12f

        canvas.save()
        val clipPath = Path().apply {
            addRoundRect(
                RectF(offsetX, offsetY, offsetX + scaledSize, offsetY + scaledSize),
                radius, radius,
                Path.Direction.CW
            )
        }
        canvas.clipPath(clipPath)
        canvas.drawBitmap(scaledUser, offsetX, offsetY, paint)
        canvas.restore()

        // Draw ring overlay on top (flag with rounded center hole)
        val ringOverlay = buildRingOverlay(flagBitmap, size)
        if (ringOverlay != null) canvas.drawBitmap(ringOverlay, 0f, 0f, paint)

        return result
    }

    // ==================== Step 6: Result ====================

    private fun downloadResult() {
        resultBitmap?.let { bitmap ->
            lifecycleScope.launch {
                showLoading()
                MediaHelper.saveBitmapToExternal(this@PrideActivity, bitmap).collect { state ->
                    when (state) {
                        com.dress.game.core.utils.state.HandleState.SUCCESS -> {
                            dismissLoading()
                            showToast(R.string.download_success)
                        }
                        com.dress.game.core.utils.state.HandleState.FAIL -> {
                            dismissLoading()
                            showToast(R.string.download_failed_please_try_again_later)
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun handleBackLeftToRight() {
        finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
