package com.dress.game.ui.my_creation.view_model

import android.content.Context
import androidx.lifecycle.ViewModel
import com.dress.game.core.helper.MediaHelper
import com.dress.game.core.utils.key.ValueKey
import com.dress.game.data.model.MyAlbumModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MyOverlayViewModel : ViewModel() {

    private val _myOverlayList = MutableStateFlow<ArrayList<MyAlbumModel>>(arrayListOf())
    val myOverlayList = _myOverlayList.asStateFlow()

    private val _isLastItem = MutableStateFlow(false)
    val isLastItem: StateFlow<Boolean> = _isLastItem

    fun loadMyOverlay(context: Context) {
        try {
            val imageList = MediaHelper.getImageInternal(context, ValueKey.PRIDE_ALBUM)
            val albumList = imageList.map { MyAlbumModel(it) }.toCollection(ArrayList())
            _myOverlayList.value = albumList
        } catch (e: Exception) {
            _myOverlayList.value = arrayListOf()
        }
        checkLastItem()
    }

    fun showLongClick(positionSelect: Int) {
        _myOverlayList.value = _myOverlayList.value.mapIndexed { position, item ->
            item.copy(isSelected = position == positionSelect, isShowSelection = true)
        }.toCollection(ArrayList())
        checkLastItem()
    }

    private fun checkLastItem() {
        _isLastItem.value = _myOverlayList.value.any { !it.isSelected }
    }

    suspend fun deleteItem(context: Context, pathList: ArrayList<String>) {
        MediaHelper.deleteFileByPathNotFlow(pathList)
    }

    fun toggleSelect(position: Int) {
        val list = _myOverlayList.value.toMutableList()
        list[position] = list[position].copy(isSelected = !list[position].isSelected, isShowSelection = true)
        _myOverlayList.value = list.toCollection(ArrayList())
        checkLastItem()
    }

    fun selectAll(shouldSelect: Boolean) {
        _myOverlayList.value = _myOverlayList.value.map {
            it.copy(isSelected = shouldSelect, isShowSelection = true)
        }.toCollection(ArrayList())
        checkLastItem()
    }

    fun getPathSelected(): ArrayList<String> {
        return _myOverlayList.value.filter { it.isSelected }.map { it.path }.toCollection(ArrayList())
    }

    fun clearSelection() {
        _myOverlayList.value = _myOverlayList.value.map {
            it.copy(isSelected = false, isShowSelection = false)
        }.toCollection(ArrayList())
        checkLastItem()
    }
}
