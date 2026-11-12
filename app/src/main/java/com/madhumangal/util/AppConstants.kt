package com.madhumangal.util

import com.madhumangal.BuildConfig

object AppConstants {
    val APP_VERSION: String get() = BuildConfig.VERSION_NAME
    val VERSION_CODE: Int get() = BuildConfig.VERSION_CODE
}
