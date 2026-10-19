package com.echo.recorder.util

import com.echo.recorder.BuildConfig

object AppConstants {
    val APP_VERSION: String get() = BuildConfig.VERSION_NAME
    val VERSION_CODE: Int get() = BuildConfig.VERSION_CODE
}
