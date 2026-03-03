package com.dress.game.dialog

import android.app.Activity
import android.graphics.Color
import android.widget.LinearLayout
import com.dress.game.core.extensions.gone
import com.dress.game.core.extensions.hideNavigation
import com.dress.game.core.extensions.tap
import com.dress.game.R
import com.dress.game.core.base.BaseDialog
import com.dress.game.core.extensions.strings
import com.dress.game.databinding.DialogConfirmBinding

enum class DialogType {
    DELETE_EXIT,
    RESET,
    LOADING,
    INTERNET,
    PERMISSION
}

class YesNoDialog(
    val context: Activity,
    val title: Int,
    val description: Int,
    val isError: Boolean = false,
    val dialogType: DialogType = DialogType.DELETE_EXIT
) : BaseDialog<DialogConfirmBinding>(context, maxWidth = true, maxHeight = true) {
    override val layoutId: Int = R.layout.dialog_confirm
    override val isCancelOnTouchOutside: Boolean = false
    override val isCancelableByBack: Boolean = false

    var onNoClick: (() -> Unit) = {}
    var onYesClick: (() -> Unit) = {}
    var onDismissClick: (() -> Unit) = {}

    override fun initView() {
        initText()
        initBackground()
        if (isError) {
            binding.btnNo.gone()
        }
        context.hideNavigation()
        binding.tvTitle.isSelected = true
    }

    private fun initBackground() {
        val bgRes = when (dialogType) {
            DialogType.DELETE_EXIT -> R.drawable.bg_dialog_delete_exit
            DialogType.RESET -> R.drawable.bg_dialog_reset
            DialogType.LOADING -> R.drawable.bg_dialog_loading
            DialogType.INTERNET -> R.drawable.bg_dialog_internet
            DialogType.PERMISSION -> R.drawable.bg_dialog_loading
        }

        binding.containerDialog.setBackgroundResource(bgRes)

        val textColor = when (dialogType) {
            DialogType.DELETE_EXIT -> Color.parseColor("#FF008C")
            DialogType.RESET -> Color.parseColor("#2AABEE")
            DialogType.LOADING -> Color.parseColor("#AB5BFF")
            DialogType.INTERNET -> Color.parseColor("#FE8700")
            DialogType.PERMISSION -> Color.parseColor("#AB5BFF")
        }

        binding.tvDescription.setTextColor(textColor)
        binding.btnNo.setTextColor(textColor)

        when (dialogType) {
            DialogType.LOADING, DialogType.INTERNET -> {
                binding.btnNo.gone()
                binding.btnYes.setBackgroundResource(R.drawable.bg_btn_internet_yes)
                (binding.btnYes.layoutParams as LinearLayout.LayoutParams).marginStart = 0
            }
            DialogType.PERMISSION -> {
                // Set custom backgrounds for PERMISSION dialog buttons
                binding.btnNo.setBackgroundResource(R.drawable.bg_btn_permission_no)
                binding.btnYes.setBackgroundResource(R.drawable.bg_btn_permission_yes)
                // Set Yes button text color to white
                binding.btnYes.setTextColor(Color.parseColor("#FFFFFF"))
                // Set same padding for both buttons to have equal height
                val paddingVertical = (9 * context.resources.displayMetrics.density).toInt()
                binding.btnNo.setPadding(0, paddingVertical, 0, paddingVertical)
                binding.btnYes.setPadding(0, paddingVertical, 0, paddingVertical)
            }
            DialogType.RESET -> {
                // Set custom backgrounds for PERMISSION dialog buttons
                binding.btnNo.setBackgroundResource(R.drawable.bg_btn_reset_no)
                binding.btnYes.setBackgroundResource(R.drawable.bg_btn_reset_yes)
                // Set Yes button text color to white
                binding.btnYes.setTextColor(Color.parseColor("#FFFFFF"))
                // Set same padding for both buttons to have equal height
                val paddingVertical = (9 * context.resources.displayMetrics.density).toInt()
                binding.btnNo.setPadding(0, paddingVertical, 0, paddingVertical)
                binding.btnYes.setPadding(0, paddingVertical, 0, paddingVertical)
            }

            DialogType.DELETE_EXIT -> {
                // Set custom backgrounds for PERMISSION dialog buttons
                binding.btnNo.setBackgroundResource(R.drawable.bg_btn_delete_no)
                binding.btnYes.setBackgroundResource(R.drawable.bg_btn_delete_yes)
                // Set Yes button text color to white
                binding.btnYes.setTextColor(Color.parseColor("#FFFFFF"))
                // Set same padding for both buttons to have equal height
                val paddingVertical = (9 * context.resources.displayMetrics.density).toInt()
                binding.btnNo.setPadding(0, paddingVertical, 0, paddingVertical)
                binding.btnYes.setPadding(0, paddingVertical, 0, paddingVertical)
            }


            else -> {}
        }
    }

    override fun initAction() {
        binding.apply {
            btnNo.tap { onNoClick.invoke() }
            btnYes.tap { onYesClick.invoke() }
            flOutSide.tap { onDismissClick.invoke() }
        }
    }

    override fun onDismissListener() {

    }

    private fun initText() {
        binding.apply {
            tvTitle.text = context.strings(title)
            tvDescription.text = context.strings(description)
            // Use "Ok" text for error dialogs (like internet check)
            if (isError) {
                btnYes.text = context.strings(R.string.ok)
            }
        }
    }
}