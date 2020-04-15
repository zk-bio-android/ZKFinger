package com.zkteco.zkfingersdkdemo.fingerprint

import android.graphics.Bitmap

interface ZKFingerListener {
    fun onCapture(bitmap: Bitmap?)
    fun onExtract(tmplate: ByteArray?, templateSize: Int)
    fun onException()
}