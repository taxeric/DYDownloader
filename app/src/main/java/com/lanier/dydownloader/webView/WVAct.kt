package com.lanier.dydownloader.webView

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.View
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.lanier.dydownloader.R
import com.lanier.dydownloader.download.DownloadTask
import com.lanier.dydownloader.download.JoinDownloadTaskFlow
import com.lanier.dydownloader.utils.ClipboardUtil
import com.lanier.dydownloader.utils.WebUtil
import com.lanier.dydownloader.utils.obtainVideoMediaUri
import com.lanier.lib_js_bridge.BridgeWebView
import com.lanier.lib_js_bridge.BridgeWebViewClient
import com.tencent.smtt.export.external.interfaces.WebResourceRequest
import com.tencent.smtt.export.external.interfaces.WebResourceResponse
import com.tencent.smtt.sdk.WebChromeClient
import com.tencent.smtt.sdk.WebSettings
import com.tencent.smtt.sdk.WebView
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Author: Turtledove
 * Date  : on 2023/9/6
 * Desc  :
 */
class WVAct: AppCompatActivity() {

    private val webView by lazy {
        findViewById<BridgeWebView>(R.id.webView)
    }
    private val pb by lazy {
        findViewById<ProgressBar>(R.id.pbLoading)
    }

    private lateinit var url: String
    private var webTitle = ""
    private val hasShowDownloadDialog = AtomicBoolean(false)

    private val handler: Handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.webview_activity_main_h5)

        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN or
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
        )
        handler.postDelayed({
            try {
                //解决h5输入法遮挡问题
                window.setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or
                            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                )
            } catch (ignored: Exception) {
            }
        }, 1000)

        url = intent.getStringExtra("url")?:""
        if (TextUtils.isEmpty(url)) {
            finish()
            return
        }
        initData(url)
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        webView.removeAllViews()
        webView.destroy()
        //异常所以的信息
        handler.removeCallbacksAndMessages(null)
    }

    /**
     * 初始化数据
     */
    private fun initData(url: String) {
        webView.clearCache(true)
        val webSettings = webView.settings
        webSettings.domStorageEnabled = true
        webSettings.javaScriptEnabled = true
        webSettings.builtInZoomControls = true
        webSettings.useWideViewPort = true
        webSettings.loadWithOverviewMode = true
        webSettings.setSupportZoom(true)
        webSettings.cacheMode = WebSettings.LOAD_DEFAULT
        webSettings.setAppCacheEnabled(true)
        webSettings.savePassword = false //屏蔽提示密码保存框
        // 把内部私有缓存目录'/data/data/包名/cache/'作为WebView的AppCache的存储路径
        val cachePath = applicationContext.cacheDir.path
        webSettings.setAppCachePath(cachePath)
        webSettings.setAppCacheMaxSize((50 * 1024 * 1024).toLong())
        webSettings.displayZoomControls = true
        webView.isHorizontalScrollBarEnabled = true //滚动条水平是否显示
        webView.isVerticalScrollBarEnabled = true //滚动条垂直是否显示

        webSettings.userAgentString =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:91.0) Gecko/20100101 Firefox/91.0"
        webView.loadUrl(url)
        webView.webChromeClient = object : WebChromeClient() {
            override fun onReceivedTitle(p0: WebView?, p1: String) {
                super.onReceivedTitle(p0, p1)
                webTitle = p1
            }
        }
        //1.注意需要设置BridgeWebViewClient不然js交互无效，
        //2.自定义WebViewClient 通过webView打开链接，不调用系统浏览器
        webView.webViewClient = object : BridgeWebViewClient(webView) {
            override fun onCustomPageFinished(view: WebView, url: String) {
                super.onCustomPageFinished(view, url)
                view.loadUrl(
                    "javascript:window.local_obj.showSource('<head>'+" +
                            "document.getElementsByTagName('html')[0].innerHTML+'</head>');"
                )
            }

            override fun shouldInterceptRequest(
                webView: WebView,
                webResourceRequest: WebResourceRequest
            ): WebResourceResponse? {
                val hasShow = hasShowDownloadDialog.get()
                if (hasShow) {
                    return super.shouldInterceptRequest(webView, webResourceRequest)
                }
                val requestUrl = webResourceRequest.url.toString()
                if (requestUrl.contains("video/") && requestUrl.contains("mp4")) {
                    val type: String = WebUtil.getContentType(requestUrl)
                    if (type.startsWith("video")) {
                        handler.post {
                            pb.visibility = View.INVISIBLE
                            val codeArr = type.split("-")
                            val code = if (codeArr.size > 1) {
                                codeArr[1]
                            } else {
                                ""
                            }
                            hasShowDownloadDialog.set(true)
                            showDownloadDialog(
                                title = "完成",
                                onJoinQueue = {
                                    ClipboardUtil.clip(requestUrl)
                                    if (code == "403") {
                                        showAlertDialog(
                                            title = "Dear Smurf:",
                                            message = "视频需要重定向, 文件下载可能异常, 但链接已复制, 可以手动前往浏览器下载.\n\n点击确定继续尝试重定向下载, 点击取消什么都不做",
                                            cancelable = false,
                                            showNegativeButton = true,
                                            onPositive = {
                                                buildTask(requestUrl)
                                                finish()
                                            },
                                            onNegative = {
                                                finish()
                                            }
                                        )
                                    } else {
                                        buildTask(requestUrl)
                                        finish()
                                    }
                                },
                                onOnlyCopyLink = {
                                    ClipboardUtil.clip(requestUrl)
                                    Toast.makeText(this@WVAct, "已复制", Toast.LENGTH_SHORT)
                                        .show()
                                    finish()
                                },
                                onToBrowser = {
                                    val intent = Intent(Intent.ACTION_VIEW)
                                    val uri = Uri.parse(requestUrl)
                                    intent.data = uri
                                    startActivity(intent)
                                    finish()
                                },
                                onDoNothing = {
                                    finish()
                                }
                            )
                        }
                    }
                }
                return super.shouldInterceptRequest(webView, webResourceRequest)
            }
        }
    }

    private fun buildTask(requestUrl: String) {
        val task = DownloadTask(
            filename = webTitle.ifEmpty { "无标题" },
            fileLinkUrl = requestUrl,
        )
        Toast.makeText(
            this@WVAct,
            "已加入下载队列",
            Toast.LENGTH_SHORT
        )
            .show()
        JoinDownloadTaskFlow.tryEmit(task)
    }

    private fun showDownloadDialog(
        title: String,
        onJoinQueue: () -> Unit,
        onOnlyCopyLink: () -> Unit,
        onToBrowser: () -> Unit,
        onDoNothing: () -> Unit = {},
    ) {
        val items = arrayOf("加入下载队列", "仅复制链接", "跳转到系统浏览器下载", "什么都不做")
        val itemClickListener = DialogInterface.OnClickListener { dialog, which ->
            when (which) {
                0 -> onJoinQueue.invoke()
                1 -> onOnlyCopyLink.invoke()
                2 -> onToBrowser.invoke()
                3 -> onDoNothing.invoke()
            }
            dialog.dismiss()
        }
        AlertDialog.Builder(this)
            .setItems(items, itemClickListener)
            .setTitle(title)
            .setCancelable(false)
            .show()
    }

    private fun showAlertDialog(
        title: String = "Smurf",
        message: String,
        cancelable: Boolean = true,
        showNegativeButton: Boolean = false,
        positiveButtonText: String = "确定",
        negativeButtonText: String = "取消",
        onPositive: () -> Unit,
        onNegative: () -> Unit = {},
    ) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setCancelable(cancelable)
        builder.setPositiveButton(
            positiveButtonText
        ) { dialog, _ ->
            onPositive.invoke()
            dialog.dismiss()
        }
        if (showNegativeButton) {
            builder.setNegativeButton(
                negativeButtonText
            ) { dialog, _ ->
                onNegative.invoke()
                dialog.dismiss()
            }
        }
        builder.show()
    }
}