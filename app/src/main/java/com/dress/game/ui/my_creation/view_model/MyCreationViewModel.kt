package com.dress.game.ui.my_creation.view_model

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dress.game.core.helper.MediaHelper
import com.dress.game.core.helper.StringHelper
import com.dress.game.core.utils.share.telegram.TelegramSharing
import com.dress.game.core.utils.share.whatsapp.IdGenerator
import com.dress.game.core.utils.share.whatsapp.StickerBook
import com.dress.game.core.utils.share.whatsapp.StickerPack
import com.dress.game.core.utils.state.HandleState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.io.File

class MyCreationViewModel : ViewModel() {
    private val _isFromSuccess = MutableStateFlow<Boolean>(false)
    val isFromSuccess: StateFlow<Boolean> = _isFromSuccess

    private val _typeStatus = MutableStateFlow<Int>(-1)
    val typeStatus = _typeStatus.asStateFlow()

    private val _downloadState = MutableSharedFlow<HandleState>()
    val downloadState: SharedFlow<HandleState> = _downloadState

    fun setStatusFrom(status: Boolean){
        _isFromSuccess.value = status
    }

    fun setTypeStatus(type: Int){
        if (type == _typeStatus.value) return
        _typeStatus.value = type
    }

    fun shareImages(context: Context, list: ArrayList<String>) {
        val uriList = getAllUrisFromList(context, list)
        if (uriList.isEmpty()) return

        val shareIntent = Intent().apply {
            action = if (uriList.size == 1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE
            type = "image/*"

            if (uriList.size == 1) {
                putExtra(Intent.EXTRA_STREAM, uriList[0])
            } else {
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uriList)
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Share images"))
    }

    fun addToTelegram(context: Context, list: ArrayList<String>){
        val uriList = getAllUrisFromList(context, list)
        TelegramSharing.importToTelegram(context, uriList)
    }
    fun addToWhatsapp(context: Activity, packageName: String, list: ArrayList<String>, onResult: (StickerPack?) -> Unit) {
        val uriList = getAllUrisFromList(context, list)
        val packId = IdGenerator.generateIdFromUrl(context, StringHelper.generateRandomString(10))
        val stickerPack = StickerPack(packId, packageName, uriList, context)
        StickerBook.addPackIfNotAlreadyAdded(stickerPack)
        onResult(stickerPack)
    }
    fun getAllUrisFromList(context: Context, shareList: ArrayList<String>): ArrayList<Uri> {
        val contentUriList = ArrayList<Uri>()
        // Tạo danh sách đường dẫn từ dataMyCat
        val listPath = arrayListOf<String>()
        shareList.forEach {
            listPath.add(it)
        }

        // Lặp qua từng đường dẫn và chuyển đổi thành Uri
        listPath.forEach { path ->
            val file = File(path)
            if (file.exists()) {
                val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                contentUriList.add(contentUri)
            }
        }

        return contentUriList
    }

    fun downloadFiles(context: Activity, pathList: ArrayList<String>) {
        viewModelScope.launch {
            MediaHelper.downloadPartsToExternal(context, pathList)
                .flowOn(Dispatchers.IO).collect { state ->
                    _downloadState.emit(state)
                }
        }
    }

}