package com.dress.game.ui.permission

import androidx.lifecycle.ViewModel
import com.dress.game.core.helper.PermissionHelper
import com.dress.game.core.helper.SharePreferenceHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.compareTo

class PermissionViewModel : ViewModel() {

    private val _storageGranted = MutableStateFlow(false)
    val storageGranted: StateFlow<Boolean> = _storageGranted

    private val _notificationGranted = MutableStateFlow(false)
    val notificationGranted: StateFlow<Boolean> = _notificationGranted

    // Just update UI state without touching counter
    fun checkAndUpdatePermissionState(granted: Boolean, isStorage: Boolean) {
        if (isStorage) {
            _storageGranted.value = granted
        } else {
            _notificationGranted.value = granted
        }
    }

    // Increment counter when user actually denies permission
    fun updateStorageGranted(sharePrefer: SharePreferenceHelper, granted: Boolean) {
        _storageGranted.value = granted
        sharePrefer.setStoragePermission(if (granted) 0 else sharePrefer.getStoragePermission() + 1)
    }

    fun updateNotificationGranted(sharePrefer: SharePreferenceHelper, granted: Boolean) {
        _notificationGranted.value = granted
        sharePrefer.setNotificationPermission(if (granted) 0 else sharePrefer.getNotificationPermission() + 1)
    }

    fun needGoToSettings(sharePrefer: SharePreferenceHelper, storage: Boolean): Boolean {
        return if (storage) {
            sharePrefer.getStoragePermission() > 2 && !_storageGranted.value
        } else {
            sharePrefer.getNotificationPermission() > 2 && !_notificationGranted.value
        }
    }

    fun getStoragePermissions() = PermissionHelper.storagePermission
    fun getNotificationPermissions() = PermissionHelper.notificationPermission
}