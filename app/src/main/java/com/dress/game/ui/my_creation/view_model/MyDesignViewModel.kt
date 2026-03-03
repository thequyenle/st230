package com.dress.game.ui.my_creation.view_model

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dress.game.core.helper.MediaHelper
import com.dress.game.core.utils.key.ValueKey
import com.dress.game.core.utils.state.HandleState
import com.dress.game.data.model.MyAlbumModel
import com.dress.game.data.model.custom.SuggestionModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class MyDesignViewModel : ViewModel() {
    private val _myDesignList = MutableStateFlow<ArrayList<MyAlbumModel>>(arrayListOf())
    val myDesignList = _myDesignList.asStateFlow()
    private val _isLastItem = MutableStateFlow<Boolean>(false)
    val isLastItem: StateFlow<Boolean> = _isLastItem

    fun loadMyDesign(context: Context) {
        android.util.Log.d("MyDesignViewModel", "📂 loadMyDesign() START")
        android.util.Log.d("MyDesignViewModel", "Thread: ${Thread.currentThread().name}")
        android.util.Log.d("MyDesignViewModel", "Context: ${context.javaClass.simpleName}")
        android.util.Log.d("MyDesignViewModel", "Loading from: ValueKey.DOWNLOAD_ALBUM")

        try {
            val imageList = MediaHelper.getImageInternal(context, ValueKey.DOWNLOAD_ALBUM)
            android.util.Log.d("MyDesignViewModel", "✅ Loaded ${imageList.size} items from DOWNLOAD_ALBUM")

            imageList.forEachIndexed { index, path ->
                android.util.Log.d("MyDesignViewModel", "  [$index] path: $path")
                // Check if file exists
                val file = java.io.File(path)
                val exists = file.exists()
                val size = if (exists) file.length() else 0
                android.util.Log.d("MyDesignViewModel", "  [$index] File exists: $exists, Size: $size bytes")
            }

            val albumList = imageList.map { MyAlbumModel(it) }.toCollection(ArrayList())
            _myDesignList.value = albumList

            android.util.Log.d("MyDesignViewModel", "✅ Updated myDesignList with ${albumList.size} items")
            android.util.Log.d("MyDesignViewModel", "Current myDesignList size: ${_myDesignList.value.size}")
        } catch (e: Exception) {
            android.util.Log.e("MyDesignViewModel", "❌ ERROR loading designs: ${e.message}", e)
            _myDesignList.value = arrayListOf()
        }

        checkLastItem()
        android.util.Log.d("MyDesignViewModel", "📂 loadMyDesign() END")
    }

    fun showLongClick(positionSelect: Int) {
        _myDesignList.value = _myDesignList.value.mapIndexed { position, item ->
            item.copy(isSelected = position == positionSelect, isShowSelection = true)
        }.toCollection(ArrayList())
        checkLastItem()
    }

    private fun checkLastItem() {
        _isLastItem.value = _myDesignList.value.any { !it.isSelected }
    }

    suspend fun deleteItem(context: Context, pathList: ArrayList<String>){
        MediaHelper.deleteFileByPathNotFlow(pathList)
    }

    fun toggleSelect(position: Int) {
        val list = _myDesignList.value.toMutableList()
        list[position] = list[position].copy(isSelected = !list[position].isSelected, isShowSelection = true)
        _myDesignList.value = list.toCollection(ArrayList())
        checkLastItem()
    }

    fun selectAll(shouldSelect: Boolean) {
        _myDesignList.value = _myDesignList.value.map {
            it.copy(isSelected = shouldSelect, isShowSelection = true)
        }.toCollection(ArrayList())
        checkLastItem()
    }

    fun getPathSelected() : ArrayList<String>{
        return _myDesignList.value.filter { it.isSelected }.map { it.path }.toCollection(ArrayList())
    }

    fun clearSelection() {
        _myDesignList.value = _myDesignList.value.map {
            it.copy(isSelected = false, isShowSelection = false)
        }.toCollection(ArrayList())
        checkLastItem()
    }
}