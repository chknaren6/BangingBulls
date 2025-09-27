package com.nc.bangingbulls

import android.content.Context
import android.media.MediaPlayer


fun playSoundFromAssets(context: Context, fileName: String) {
    try {
        val assetFileDescriptor = context.assets.openFd(fileName)
        MediaPlayer().apply {
            setDataSource(assetFileDescriptor.fileDescriptor, assetFileDescriptor.startOffset, assetFileDescriptor.length)
            prepareAsync()
            setOnPreparedListener { start() }
            setOnCompletionListener { release() }
        }
        assetFileDescriptor.close()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
