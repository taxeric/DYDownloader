package com.lanier.dydownloader.utils

import android.app.Application
import com.tencent.smtt.export.external.TbsCoreSettings
import com.tencent.smtt.sdk.QbSdk
import com.tencent.smtt.sdk.QbSdk.PreInitCallback
import com.tencent.smtt.sdk.TbsListener

/**
 * Author: Turtledove
 * Date  : on 2023/9/6
 * Desc  :
 */
object X5InitUtil {

    fun init(application: Application) {
        QbSdk.setDownloadWithoutWifi(true)

        // 在调用TBS初始化、创建WebView之前进行如下配置
        val map = linkedMapOf<String, Any>()
        map[TbsCoreSettings.TBS_SETTINGS_USE_SPEEDY_CLASSLOADER] = true
        map[TbsCoreSettings.TBS_SETTINGS_USE_DEXLOADER_SERVICE] = true
        map[TbsCoreSettings.TBS_SETTINGS_USE_PRIVATE_CLASSLOADER] = true
        QbSdk.initTbsSettings(map)
        QbSdk.setTbsListener(object : TbsListener {
            override fun onDownloadFinish(i: Int) {
            }

            override fun onInstallFinish(i: Int) {
            }

            override fun onDownloadProgress(i: Int) {
            }
        })
        QbSdk.initX5Environment(application, object : PreInitCallback {
            override fun onCoreInitFinished() {
            }

            override fun onViewInitFinished(isX5: Boolean) {
            }
        })
    }
}