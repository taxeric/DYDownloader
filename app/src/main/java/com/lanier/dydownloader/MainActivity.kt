package com.lanier.dydownloader

import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.lanier.dydownloader.download.DownloadBroadcast
import com.lanier.dydownloader.download.DownloadService
import com.lanier.dydownloader.webView.WVAct

class MainActivity : AppCompatActivity() {

    private val et by lazy {
        findViewById<EditText>(R.id.etUrl)
    }

    private val btn by lazy {
        findViewById<Button>(R.id.btnParse)
    }

    companion object {
        private const val PREFIX = "v.douyin.com/"
    }

    private var waitParseUrl = ""
    private val downloadBroadcastReceiver = DownloadBroadcast()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btn.setOnClickListener {
            waitParseUrl = et.text.toString()
            if (waitParseUrl.isNotEmpty()) {
                if (waitParseUrl.contains("http") && waitParseUrl.contains(PREFIX)) {
                    val last2 = waitParseUrl.lastIndexOf("/")
                    val last1 = waitParseUrl.lastIndexOf(PREFIX)
                    val realRoute = waitParseUrl.substring(last1 + PREFIX.length, last2)
                    startActivity(
                        Intent(this, WVAct::class.java)
                            .putExtra("url", "https://$PREFIX$realRoute")
                    )
                }
            }
        }

        findViewById<Button>(R.id.btnClear)
            .setOnClickListener {
                et.text = SpannableStringBuilder("")
            }

        startService(Intent(this, DownloadService::class.java))
        LocalBroadcastManager
            .getInstance(this)
            .registerReceiver(downloadBroadcastReceiver, IntentFilter("dou_yin_downloader"))
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager
            .getInstance(this)
            .unregisterReceiver(downloadBroadcastReceiver)
    }
}