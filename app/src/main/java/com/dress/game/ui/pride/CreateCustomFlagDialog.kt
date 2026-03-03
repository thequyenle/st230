package com.dress.game.ui.pride

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.dress.game.R
import com.dress.game.core.base.BaseDialog
import com.dress.game.core.extensions.tap
import com.dress.game.data.model.pride.CustomFlagModel
import com.dress.game.databinding.DialogCreateCustomFlagBinding
import com.dress.game.dialog.ChooseColorDialog
import com.dress.game.ui.pride.adapter.CustomColorAdapter

class CreateCustomFlagDialog(context: Context) :
    BaseDialog<DialogCreateCustomFlagBinding>(context, maxWidth = true, maxHeight = true) {

    override val layoutId: Int = R.layout.dialog_create_custom_flag
    override val isCancelOnTouchOutside: Boolean = false
    override val isCancelableByBack: Boolean = false

    var onCreateEvent: ((CustomFlagModel) -> Unit) = {}
    var onCloseEvent: (() -> Unit) = {}
    var onDismissEvent: (() -> Unit) = {}

    private val colors = mutableListOf<Int>(Color.BLACK)
    private lateinit var colorAdapter: CustomColorAdapter

    override fun initView() {
        colorAdapter = CustomColorAdapter(
            onColorClick = { position -> openColorPicker(position) },
            onRemoveClick = { position -> removeColor(position) }
        )
        binding.rvColors.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = colorAdapter
        }
        colorAdapter.submitList(colors.toList())
        updatePreview()
        updateColorCount()

        binding.etFlagName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrEmpty()) {
                    binding.etFlagName.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        0, 0, 0, 0
                    )
                } else {
                    binding.etFlagName.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
                }
            }
        })
    }

    override fun initAction() {
        binding.apply {
            etFlagName.setOnEditorActionListener { v, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    if (v.text.toString().trim().isEmpty()) {
                        Toast.makeText(context, R.string.pride_enter_name, Toast.LENGTH_SHORT).show()
                    }
                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                    v.clearFocus()
                    true
                } else false
            }

            btnClose.tap { onCloseEvent.invoke() }

            btnAddColor.tap {
                if (colors.size < 10) {
                    val dialog = ChooseColorDialog(context)
                    dialog.show()
                    dialog.onCloseEvent = { dialog.dismiss() }
                    dialog.onDoneEvent = { color ->
                        dialog.dismiss()
                        colors.add(color)
                        colorAdapter.submitList(colors.toList())
                        updatePreview()
                        updateColorCount()
                    }
                } else {
                    Toast.makeText(context, R.string.pride_max_colors_reached, Toast.LENGTH_SHORT).show()
                }
            }

            btnCreate.tap {
                val name = etFlagName.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(context, R.string.pride_enter_name, Toast.LENGTH_SHORT).show()
                    return@tap
                }
                onCreateEvent.invoke(CustomFlagModel(name, colors.toMutableList()))
            }
        }
    }

    private fun openColorPicker(position: Int) {
        val dialog = ChooseColorDialog(context)
        dialog.show()
        dialog.onCloseEvent = { dialog.dismiss() }
        dialog.onDoneEvent = { color ->
            dialog.dismiss()
            colors[position] = color
            colorAdapter.submitList(colors.toList())
            updatePreview()
        }
    }

    private fun removeColor(position: Int) {
        if (colors.size > 1) {
            colors.removeAt(position)
            colorAdapter.submitList(colors.toList())
            updatePreview()
            updateColorCount()
        }
    }

    private fun updatePreview() {
        val view = binding.flagPreviewBar
        view.post {
            if (view.width <= 0 || view.height <= 0) return@post
            val bitmap = buildFlagPreview(view.width, view.height)
            view.setBackgroundDrawable(android.graphics.drawable.BitmapDrawable(context.resources, bitmap))
        }
    }

    private fun updateColorCount() {
        binding.tvColorCount.text = "${colors.size} color"
    }

    private fun buildFlagPreview(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        val partWidth = width.toFloat() / colors.size
        colors.forEachIndexed { index, color ->
            paint.color = color
            canvas.drawRect(
                index * partWidth, 0f,
                (index + 1) * partWidth, height.toFloat(),
                paint
            )
        }
        return bitmap
    }

    override fun onDismissListener() {
        onDismissEvent.invoke()
    }
}
