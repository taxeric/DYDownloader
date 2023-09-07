package com.lanier.dydownloader

import android.app.Application
import com.lanier.dydownloader.utils.ClipboardUtil
import com.lanier.dydownloader.utils.X5InitUtil

/**
 * Author: Turtledove
 * Date  : on 2023/9/6
 * Desc  :
 */
class BaseApp : Application() {

    override fun onCreate() {
        super.onCreate()
        X5InitUtil.init(this)
        ClipboardUtil.init(this)
    }
}