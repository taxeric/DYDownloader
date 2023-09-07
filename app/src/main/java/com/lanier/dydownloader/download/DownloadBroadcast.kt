package com.lanier.dydownloader.download

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * Author: Turtledove
 * Date  : on 2023/9/7
 * Desc  :
 */
class DownloadBroadcast: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val filename = intent.getStringExtra("filename")?: "文件"
        val successful = intent.getBooleanExtra("successful", false)
        val error = intent.getStringExtra("error")?: ""
        Toast.makeText(context, if (successful) "$filename 已下载到本地" else "$filename 下载失败, 原因:\n$error", Toast.LENGTH_SHORT).show()
    }
}