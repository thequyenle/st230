package com.dress.game.core.helper

import android.content.Context
import android.content.SharedPreferences
import com.dress.game.core.utils.key.PermissionKey.CAMERA_KEY
import com.dress.game.core.utils.key.PermissionKey.NOTIFICATION_KEY
import com.dress.game.core.utils.key.PermissionKey.QUANTITY_UNZIPPED
import com.dress.game.core.utils.key.PermissionKey.STORAGE_KEY
import com.dress.game.core.utils.key.SharePreferenceKey
import com.dress.game.core.utils.key.SharePreferenceKey.COUNT_BACK_KEY
import com.dress.game.core.utils.key.SharePreferenceKey.FIRST_LANG_KEY
import com.dress.game.core.utils.key.SharePreferenceKey.FIRST_PERMISSION_KEY
import com.dress.game.core.utils.key.SharePreferenceKey.KEY_LANGUAGE
import com.dress.game.core.utils.key.SharePreferenceKey.MUSIC_KEY
import com.dress.game.core.utils.key.SharePreferenceKey.PRIDE_CUSTOM_FLAGS
import com.dress.game.core.utils.key.SharePreferenceKey.RATE_KEY
import com.dress.game.data.model.pride.CustomFlagModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SharePreferenceHelper(val context: Context) {
    val preferences: SharedPreferences = context.getSharedPreferences(SharePreferenceKey.SHARE_KEY, Context.MODE_PRIVATE)

    // Language
    fun getPreLanguage(): String {
        return preferences.getString(KEY_LANGUAGE, "") ?: ""
    }

    fun setPreLanguage(language: String) {
        val editor = preferences.edit()
        editor.putString(KEY_LANGUAGE, language)
        editor.apply()
    }

    // First Language
    fun getIsFirstLang(): Boolean {
        return preferences.getBoolean(FIRST_LANG_KEY, true)
    }

    fun setIsFirstLang(isFirstAccess: Boolean) {
        val editor = preferences.edit()
        editor.putBoolean(FIRST_LANG_KEY, isFirstAccess)
        editor.apply()
    }

    // Permission
    fun getIsFirstPermission(): Boolean {
        return preferences.getBoolean(FIRST_PERMISSION_KEY, true)
    }

    fun setIsFirstPermission(isFirstAccess: Boolean) {
        val editor = preferences.edit()
        editor.putBoolean(FIRST_PERMISSION_KEY, isFirstAccess)
        editor.apply()
    }

    // Rate
    fun getIsRate(context: Context): Boolean {
        return preferences.getBoolean(RATE_KEY, false)
    }

    fun setIsRate(isFirstAccess: Boolean) {
        val editor = preferences.edit()
        editor.putBoolean(RATE_KEY, isFirstAccess)
        editor.apply()
    }

    // Back
    fun setCountBack(countBack: Int) {
        val editor = preferences.edit()
        editor.putInt(COUNT_BACK_KEY, countBack)
        editor.apply()
    }

    fun getCountBack(): Int {
        return preferences.getInt(COUNT_BACK_KEY, 0)
    }

    // Storage Permission
    fun getStoragePermission(): Int {
        return preferences.getInt(STORAGE_KEY, 0)
    }

    fun setStoragePermission(count: Int) {
        val editor = preferences.edit()
        editor.putInt(STORAGE_KEY, count)
        editor.apply()
    }

    // Notification Permission
    fun getNotificationPermission(): Int {
        return preferences.getInt(NOTIFICATION_KEY, 0)
    }

    fun setNotificationPermission(count: Int) {
        val editor = preferences.edit()
        editor.putInt(NOTIFICATION_KEY, count)
        editor.apply()
    }

    // Camera Permission
    fun getCameraPermission(): Int {
        return preferences.getInt(CAMERA_KEY, 0)
    }

    fun setCameraPermission(count: Int) {
        val editor = preferences.edit()
        editor.putInt(CAMERA_KEY, count)
        editor.apply()
    }

    // Data asset
    fun getQuantityUnzipped(): MutableSet<Int> {
        val json = preferences.getString(QUANTITY_UNZIPPED, "[]")
        val type = object : TypeToken<MutableSet<Int>>(){}.type
        return Gson().fromJson(json, type)
    }

    fun setQuantityUnzipped(count: MutableSet<Int>) {
        val editor = preferences.edit()
        val json = Gson().toJson(count)
        editor.putString(QUANTITY_UNZIPPED, json)
        editor.apply()
    }

    // Pride Custom Flags
    fun getCustomFlags(): MutableList<CustomFlagModel> {
        val json = preferences.getString(PRIDE_CUSTOM_FLAGS, "[]")
        val type = object : TypeToken<MutableList<CustomFlagModel>>() {}.type
        return Gson().fromJson(json, type) ?: mutableListOf()
    }

    fun setCustomFlags(flags: List<CustomFlagModel>) {
        preferences.edit().putString(PRIDE_CUSTOM_FLAGS, Gson().toJson(flags)).apply()
    }

    // Music
    fun isMusicEnabled(): Boolean {
        return preferences.getBoolean(MUSIC_KEY, true) // Default: true (bật nhạc lần đầu)
    }

    fun setMusicEnabled(enabled: Boolean) {
        val editor = preferences.edit()
        editor.putBoolean(MUSIC_KEY, enabled)
        editor.apply()
    }
}