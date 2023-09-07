package com.lanier.dydownloader.download

import android.app.Service
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.lanier.dydownloader.utils.obtainVideoMediaUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URL
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Author: Turtledove
 * Date  : on 2023/9/7
 * Desc  :
 */
class DownloadService: Service(), CoroutineScope by MainScope() {

    private val downloadQueue = ConcurrentLinkedQueue<DownloadTask>()
    private var isDownloading = AtomicBoolean(false)

    private lateinit var client: OkHttpClient
    private val UA =
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:91.0) Gecko/20100101 Firefox/91.0"

    override fun onBind(intent: Intent?) = null

    init {
        GlobalScope.launch {
            JoinDownloadTaskFlow.collect {
                downloadQueue.offer(it)
                if (downloadQueue.isNotEmpty()) {
                    if (!isDownloading.get()) {
                        val task = downloadQueue.poll()
                        if (task != null) {
                            downloadWithUA(task)
                        }
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        println(">>>> 下载服务启动")
        client = OkHttpClient.Builder()
            .connectTimeout(10L, unit = TimeUnit.SECONDS)
            .followRedirects(false)
            .build()
    }

    private suspend fun downloadWithUA(
        task: DownloadTask
    ) {
        val host = URL(task.fileLinkUrl)
        val headers = Headers.Builder()
            .add("Host", host.host)
            .add("Connection", "keep-alive")
            .add("User-Agent", UA)
            .build()
        download(task, headers)
    }

    private suspend fun download(
        task: DownloadTask,
        headers: Headers = Headers.Builder().build()
    ) {
        withContext(Dispatchers.Default) {
            val request = Request.Builder()
                .get()
                .url(task.fileLinkUrl)
                .headers(headers)
                .build()
            val response = client.newCall(request).execute()
            val responseCode = response.code
            println(">>>> code ${response.code}")
            //重定向后重试
            if (responseCode == 302) {
                /*for (header in response.headers) {
                    println(">>>> $header")
                }*/
                val newUrl = response.headers["Location"]
                println(">>>> 重定向 url -> $newUrl")
                if (!newUrl.isNullOrEmpty()) {
                    val mTask = task.copy(
                        fileLinkUrl = newUrl
                    )
                    download(mTask)
                } else {
                    sendLocalBroadcast(task.filename, false, error = "视频无法下载, 已复制链接, 请移步浏览器")
                }
            } else if (responseCode == 200) {
                val uri = try {
                    obtainVideoMediaUri(this@DownloadService, task.filename).uri
                } catch (e: Exception) {
                    println(">>>> error ${e.message}")
                    null
                }
                if (uri != null) {
                    response.body?.downloadFileWithProgress2(
                        this@DownloadService,
                        uri,
                        onFailure = {
                            println(">>>> 下载失败 ${it.message}")
                            sendLocalBroadcast(task.filename, false, error = it.message)
                        },
                    )?.collect {
                        when (it) {
                            is DownloadStatus.Complete0 -> {
                                sendLocalBroadcast(task.filename, it.success)
                                if (downloadQueue.isNotEmpty()) {
                                    val newTask = downloadQueue.poll()
                                    if (newTask != null) {
                                        downloadWithUA(task)
                                    }
                                } else {
                                    isDownloading.set(false)
                                }
                            }

                            DownloadStatus.Idle -> {}
                            is DownloadStatus.Progress -> {
                                println(">>>> 正在下载 ${task.filename} ${it.percent}")
                            }
                        }
                    }
                } else {
                    sendLocalBroadcast(task.filename, false, error = "uri is null")
                }
            }
        }
    }

    private fun sendLocalBroadcast(
        filename: String,
        successful: Boolean,
        error: String? = null
    ) {
        val intent = Intent("dou_yin_downloader")
            .putExtra("filename", filename)
            .putExtra("successful", successful)
        if (error != null) {
            intent.putExtra("error", error)
        }
        LocalBroadcastManager
            .getInstance(this)
            .sendBroadcast(intent)
    }
}