package com.lanier.dydownloader.utils

import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

/**
 * Author: Turtledove
 * Date  : on 2023/9/6
 * Desc  :
 */
object WebUtil {

    private const val UA =
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:91.0) Gecko/20100101 Firefox/91.0"

    fun getContentType(url: String): String {
        val headers: MutableMap<String, String> = HashMap()
        try {
            val mUrl = URL(url)
            //host需要随着变化不然会下载失败
            headers["Host"] = mUrl.host
        } catch (ignored: MalformedURLException) {
        }
        headers["User-Agent"] = UA
        try {
            val serverUrl = URL(url)
            val conn = serverUrl.openConnection() as HttpURLConnection
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.doInput = true
            for (entry in headers.entries) {
                conn.setRequestProperty(entry.key, entry.value)
            }
            val code = conn.responseCode
            val type = conn.contentType
            println(">>>> url-----$url")
            println(">>>> code----$code")
            println(">>>> type----$type")
            return "$type-$code"
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }
}