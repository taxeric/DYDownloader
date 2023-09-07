package com.lanier.dydownloader.download

import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Author: Turtledove
 * Date  : on 2023/9/7
 * Desc  :
 */

val JoinDownloadTaskFlow = MutableSharedFlow<DownloadTask>(extraBufferCapacity = 1)