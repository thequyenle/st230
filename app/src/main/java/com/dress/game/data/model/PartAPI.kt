package com.dress.game.data.model

import android.os.Parcelable

data class PartAPI(
    val position: String,
    val parts: String,
    val colorArray: String,
    val quantity: Int,
    val level: Int
)

data class DataAPI(val name: String, val parts: List<PartAPI>)
