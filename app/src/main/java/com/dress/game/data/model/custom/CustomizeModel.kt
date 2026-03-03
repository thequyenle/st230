package com.dress.game.data.model.custom

data class CustomizeModel(
    val dataName: String = "",
    val avatar: String = "",
    val layerList: ArrayList<LayerListModel> = arrayListOf(),
    val level: Int = 100,  // Default level for local assets
    val isFromAPI: Boolean = false  // Flag to identify if data is from API
)
