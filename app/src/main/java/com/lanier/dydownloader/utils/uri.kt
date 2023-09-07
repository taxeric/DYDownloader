package com.lanier.dydownloader.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

/**
 * Author: Turtledove
 * Date  : on 2023/9/7
 * Desc  :
 */
data class UriCase(
    val uri: Uri,
    val file: File,
)

/**
 * 获取视频媒体库uri
 *
 * @param environmentType 只能是[Environment.DIRECTORY_PICTURES]或[Environment.DIRECTORY_DCIM]
 * 或[Environment.DIRECTORY_MOVIES]
 */
fun obtainVideoMediaUri(
    context: Context,
    filename: String,
    fileTypeSuffix: String = ".mp4",
    parentPathname: String = "DouYinDownloader",
    environmentType: String = Environment.DIRECTORY_MOVIES,
): UriCase {
    val parentPath = Environment.getExternalStoragePublicDirectory(environmentType).path +
            File.separator + parentPathname + File.separator
    val realFilename = "${filename}_${System.currentTimeMillis()}$fileTypeSuffix"
    println(">>>> real $realFilename")
    val contentValues = ContentValues()
    contentValues.put(MediaStore.Video.VideoColumns.DISPLAY_NAME, realFilename)
    contentValues.put(MediaStore.Video.VideoColumns.MIME_TYPE, "video/mp4")
    contentValues.put(MediaStore.Video.VideoColumns.DATE_ADDED, System.currentTimeMillis())
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        contentValues.put(
            MediaStore.Video.VideoColumns.RELATIVE_PATH,
            environmentType + File.separator + parentPathname
        )
    } else {
        contentValues.put(MediaStore.Video.VideoColumns.DATA, parentPath + realFilename)
    }
    val newUri = try {
        context.contentResolver
            .insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
    } catch (e: Exception) {
        println(">>>> err ${e.message}")
        null
    }
    return UriCase(newUri!!, File(parentPath, realFilename))
}