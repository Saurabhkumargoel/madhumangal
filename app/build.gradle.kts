plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.ksp)
}

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val buildTimestamp = SimpleDateFormat("yyyy.MM.dd.HHmm", Locale.US).format(Date())

android {
    namespace = "com.echo.recorder"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.madhumangal"
        minSdk = 26
        targetSdk = 34
        versionCode = (System.currentTimeMillis() / 1000).toInt()
        versionName = "0.0.$buildTimestamp"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.vosk.android) { artifact { type = "aar" } }
    implementation(libs.jna) { artifact { type = "aar" } }

    debugImplementation(libs.androidx.ui.tooling)
    testImplementation("junit:junit:4.13.2")
}

tasks.register("exportApkToShared") {
    description = "Exports the built APK to .shared directory in the project root"
    doLast {
        val sharedDir = File(rootDir, ".shared")
        if (!sharedDir.exists()) {
            sharedDir.mkdirs()
        }
        val versionName = android.defaultConfig.versionName ?: "unknown"
        val apkSource = layout.buildDirectory.file("outputs/apk/debug/app-debug.apk").get().asFile
        if (apkSource.exists()) {
            val destFile = File(sharedDir, "Madhumangal_v${versionName}.apk")
            apkSource.copyTo(destFile, overwrite = true)
            println("✅ [EXPORT] Successfully exported APK to: ${destFile.absolutePath} (${destFile.length() / 1024} KB)")
        } else {
            println("⚠️ [EXPORT] APK source not found at: ${apkSource.absolutePath}")
        }
    }
}

tasks.matching { it.name == "assembleDebug" }.configureEach {
    finalizedBy("exportApkToShared")
}

