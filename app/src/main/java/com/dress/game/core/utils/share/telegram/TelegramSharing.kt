package com.dress.game.core.utils.share.telegram

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.dress.game.R

object TelegramSharing {
    fun importToTelegram(context: Context, uriList: List<Uri>) {
        val list: ArrayList<Uri> = ArrayList(uriList)
        val it = list.iterator()
        while (it.hasNext()) {
            context.grantUriPermission("org.telegram.messenger", it.next(), Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val intent = Intent("org.telegram.messenger.CREATE_STICKER_PACK")
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, list)
        intent.putExtra("IMPORTER", context.packageName)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        intent.type = "image/*"

        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, context.getString(R.string.no_app_found_to_handle_this_action), Toast.LENGTH_SHORT).show()
        }
    }
}