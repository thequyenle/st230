package com.dress.game.core.utils

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import com.dress.game.R
import com.dress.game.core.custom.layout.LayoutPresets
import com.dress.game.data.model.IntroModel
import com.dress.game.data.model.LanguageModel
import com.dress.game.data.model.custom.CustomizeModel
import com.facebook.shimmer.Shimmer
import com.dress.game.data.model.SelectedModel
import com.dress.game.data.model.custom.NavigationModel

object DataLocal {
    val shimmer =
        Shimmer.AlphaHighlightBuilder().setDuration(1800).setBaseAlpha(0.7f).setHighlightAlpha(0.6f)
            .setDirection(Shimmer.Direction.LEFT_TO_RIGHT).setAutoStart(true).build()

    var lastClickTime = 0L
    var currentDate = ""
    var isConnectInternet = MutableLiveData<Boolean>()
    var isFailBaseURL = false
    var isCallDataAlready = false

    fun getLanguageList(): ArrayList<LanguageModel> {
        return arrayListOf(
            LanguageModel("hi", "HINDI", R.drawable.ic_flag_hindi),
            LanguageModel("es", "SPANISH", R.drawable.ic_flag_spanish),
            LanguageModel("fr", "FRENCH", R.drawable.ic_flag_french),
            LanguageModel("en", "ENGLISH", R.drawable.ic_flag_english),
            LanguageModel("pt", "PORTUGEESE", R.drawable.ic_flag_portugeese),
            LanguageModel("in", "INDONESIAN", R.drawable.ic_flag_indo),
            LanguageModel("de", "GERMAN", R.drawable.ic_flag_germani),
        )
    }

    val itemIntroList = listOf(
        IntroModel(R.drawable.img_intro_1, R.string.title_1),
        IntroModel(R.drawable.img_intro_2, R.string.title_2),
        IntroModel(R.drawable.img_intro_3, R.string.title_3)
    )

    fun getBackgroundColorDefault(context: Context): ArrayList<SelectedModel> {
        return arrayListOf(
            SelectedModel(color = ContextCompat.getColor(context, R.color.color_1)),
            SelectedModel(color = ContextCompat.getColor(context, R.color.color_2)),
            SelectedModel(color = ContextCompat.getColor(context, R.color.color_3)),
            SelectedModel(color = ContextCompat.getColor(context, R.color.color_4)),
            SelectedModel(color = ContextCompat.getColor(context, R.color.color_5)),
            SelectedModel(color = ContextCompat.getColor(context, R.color.color_6)),
            SelectedModel(color = ContextCompat.getColor(context, R.color.color_7)),
            SelectedModel(color = ContextCompat.getColor(context, R.color.color_8)),
            SelectedModel(color = ContextCompat.getColor(context, R.color.color_9)),
            SelectedModel(color = ContextCompat.getColor(context, R.color.color_10)),
            SelectedModel(color = ContextCompat.getColor(context, R.color.color_11)),
            SelectedModel(color = ContextCompat.getColor(context, R.color.color_12)),
            SelectedModel(color = ContextCompat.getColor(context, R.color.color_13)),
            SelectedModel(color = ContextCompat.getColor(context, R.color.color_14)),
            SelectedModel(color = ContextCompat.getColor(context, R.color.color_15)),
            SelectedModel(color = ContextCompat.getColor(context, R.color.color_16)),
            SelectedModel(color = ContextCompat.getColor(context, R.color.color_17)),
            SelectedModel(color = ContextCompat.getColor(context, R.color.color_18)),
            SelectedModel(color = ContextCompat.getColor(context, R.color.color_19)),
        )
    }

    val bottomNavigationNotSelect = arrayListOf(
        R.drawable.ic_background,
        R.drawable.ic_sticker,
        R.drawable.ic_speech,
        R.drawable.ic_text,
    )

    val bottomNavigationSelected = arrayListOf(
        R.drawable.ic_background_selected,
        R.drawable.ic_sticker_selected,
        R.drawable.ic_speech_selected,
        R.drawable.ic_text_selected,
    )

    fun getTextFontDefault(): ArrayList<SelectedModel> {
        return arrayListOf(
            SelectedModel(color = R.font.roboto_regular),
            SelectedModel(color = R.font.aldrich),
            SelectedModel(color = R.font.brush_script),
            SelectedModel(color = R.font.nova_script),
            SelectedModel(color = R.font.carattere),
            SelectedModel(color = R.font.digital_numbers),
            SelectedModel(color = R.font.dynalight),
            SelectedModel(color = R.font.edwardian_script_itc),
            SelectedModel(color = R.font.vni_ongdo)
        )
    }

    fun getTextColorDefault(context: Context): ArrayList<SelectedModel> {
        return arrayListOf(
            SelectedModel(color = ContextCompat.getColor(context, R.color.color_9)),
            SelectedModel(color = ContextCompat.getColor(context, R.color.black)),
            SelectedModel(color = ContextCompat.getColor(context, R.color.white)),
            SelectedModel(color = ContextCompat.getColor(context, R.color.color_19)),
            SelectedModel(color = ContextCompat.getColor(context, R.color.color_2)),
            SelectedModel(color = ContextCompat.getColor(context, R.color.color_3)),
            SelectedModel(color = ContextCompat.getColor(context, R.color.color_4)),
            SelectedModel(color = ContextCompat.getColor(context, R.color.color_5)),
            SelectedModel(color = ContextCompat.getColor(context, R.color.color_6)),
            SelectedModel(color = ContextCompat.getColor(context, R.color.color_7)),
            SelectedModel(color = ContextCompat.getColor(context, R.color.color_8))
        )
    }
}