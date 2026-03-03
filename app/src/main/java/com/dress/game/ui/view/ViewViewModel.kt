package com.dress.game.ui.view

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dress.game.core.extensions.shareImagesPaths
import com.dress.game.core.helper.MediaHelper
import com.dress.game.core.utils.key.ValueKey
import com.dress.game.core.utils.state.HandleState
import com.dress.game.data.model.custom.SuggestionModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

class ViewViewModel : ViewModel() {
    private val _pathInternal = MutableStateFlow<String>("")
    val pathInternal: StateFlow<String> = _pathInternal.asStateFlow()

    var statusFrom = ValueKey.AVATAR_TYPE

    fun setPath(path: String) {
        _pathInternal.value = path
    }

    fun deleteFile(context: Context, path: String): Flow<HandleState> = flow {
        if (statusFrom == ValueKey.MY_DESIGN_TYPE || statusFrom == ValueKey.PRIDE_OVERLAY_TYPE) {
            emitAll(MediaHelper.deleteFileByPath(arrayListOf(path)))
        } else {
            emit(HandleState.LOADING)
            try {
                val originList = MediaHelper
                    .readListFromFile<SuggestionModel>(context, ValueKey.EDIT_FILE_INTERNAL)
                    .toCollection(ArrayList())

                val editDelete = originList.first { it.pathInternalEdit == path }

                originList.remove(editDelete)

                MediaHelper.writeListToFile(context, ValueKey.EDIT_FILE_INTERNAL, originList)

                emit(HandleState.SUCCESS)
            } catch (e: Exception) {
                Log.e("nbhieu", "deleteFile: $e")
                emit(HandleState.FAIL)
            }
        }
    }

    fun shareFiles(context: Activity) {
        viewModelScope.launch {
            context.shareImagesPaths(arrayListOf(_pathInternal.value))
        }
    }

    fun downloadFiles(context: Activity): Flow<HandleState> = flow {
        emitAll(
            MediaHelper.downloadPartsToExternal(
                context, arrayListOf(_pathInternal.value)
            )
        )
    }

    fun updateStatusFrom(status: Int) {
        statusFrom = status
    }
}
