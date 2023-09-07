package com.lanier.dydownloader.download

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.ResponseBody
import java.io.File

/**
 * Author: Turtledove
 * Date  : on 2023/9/7
 * Desc  :
 */
sealed interface DownloadStatus{
    object Idle: DownloadStatus
    data class Progress(val percent: Int): DownloadStatus
    data class Complete0(val success: Boolean): DownloadStatus
}

fun ResponseBody.downloadFileWithProgress2(
    context: Context,
    uri: Uri,
    onFailure: suspend (Throwable) -> Unit = {}
): Flow<DownloadStatus> = flow {
    emit(DownloadStatus.Progress(0))
    var success: Boolean
    kotlin.runCatching {
        context.contentResolver
            .openOutputStream(uri)
            .use { outputStream ->
                byteStream().use { inputStream ->
                    val totalBytes = contentLength()
                    val buffer = ByteArray(4 * 1024)
                    var progressBytes = 0L
                    while (true) {
                        val byteCount = inputStream.read(buffer)
                        if (byteCount == -1) break
                        outputStream?.write(buffer, 0, byteCount)
                        progressBytes += byteCount
                        val percent = ((progressBytes * 100) / totalBytes).toInt()
                        emit(DownloadStatus.Progress(percent))
                    }
                    when {
                        progressBytes < totalBytes -> {
                            success = false
                            onFailure(Throwable("download failed -> missing bytes"))
                        }
                        progressBytes > totalBytes -> {
                            success = false
                            onFailure(Throwable("download failed -> too many bytes"))
                        }
                        else -> success = true
                    }
                }
            }
        emit(DownloadStatus.Complete0(success))
    }.onFailure {
        onFailure(it)
    }
}.flowOn(Dispatchers.IO).distinctUntilChanged()