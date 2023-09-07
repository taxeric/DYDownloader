package com.lanier.dydownloader.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context


/**
 * Author: Turtledove
 * Date  : on 2023/9/6
 * Desc  :
 */
object ClipboardUtil {

    private lateinit var manager: ClipboardManager

    fun init(context: Context) {
        manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    fun clip(msg: String, unInited: () -> Unit = {}) {
        if (::manager.isInitialized) {
            val data = ClipData.newPlainText("label", msg)
            manager.setPrimaryClip(data)
        } else {
            unInited.invoke()
        }
    }
}