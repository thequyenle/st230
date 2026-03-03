package com.dress.game.dialog

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import com.dress.game.R
import com.dress.game.core.base.BaseDialog
import com.dress.game.core.extensions.invisible
import com.dress.game.core.extensions.loadImage
import com.dress.game.core.extensions.tap
import com.dress.game.core.extensions.visible
import com.dress.game.databinding.DialogSpeechBinding
import kotlin.apply
import kotlin.text.trim
import kotlin.toString
import androidx.core.graphics.createBitmap
import androidx.core.view.isVisible
import com.dress.game.core.extensions.hideSoftKeyboard
import com.dress.game.core.helper.BitmapHelper

class DialogSpeech(val context: Activity, val path: String) : BaseDialog<DialogSpeechBinding>(context, maxWidth = true, maxHeight = true) {
    override val layoutId: Int = R.layout.dialog_speech
    override val isCancelOnTouchOutside: Boolean = false
    override val isCancelableByBack: Boolean = false
    var onDoneClick: ((Bitmap?) -> Unit) = { }

    override fun initView() {
        binding.apply {
            edtSpeech.isFocusableInTouchMode = true
            edtSpeech.isFocusable = true
            edtSpeech.requestFocus()

            loadImage(context, path, imvBubble)
        }

    }

    override fun initAction() {
        binding.apply {
            edtSpeech.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                    handleDone()
                    true
                } else {
                    false
                }
            }

            layoutRoot.tap { handleDone() }

            edtSpeech.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    binding.tvGetText.text = p0.toString()
                }

                override fun afterTextChanged(p0: Editable?) {}
            })
        }
    }

    fun handleDone(){
        binding.apply {
            edtSpeech.clearFocus()
            edtSpeech.invisible()
            tvGetText.isVisible = !TextUtils.isEmpty(edtSpeech.text.toString().trim())
            val bitmap = BitmapHelper.getBitmapFromEditText(layoutBubble)
            onDoneClick.invoke(bitmap)
        }
    }

    override fun onDismissListener() {}
}