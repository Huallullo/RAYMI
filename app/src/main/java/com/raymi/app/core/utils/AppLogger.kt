package com.raymi.app.core.utils

import android.util.Log

object AppLogger {
    private const val BASE_TAG = "RAYMI"

    fun d(tag: String, message: String) {
        Log.d("$BASE_TAG-$tag", message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e("$BASE_TAG-$tag", message, throwable)
    }
}
