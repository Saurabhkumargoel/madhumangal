package com.madhumangal

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.madhumangal.data.AppDatabase
import com.madhumangal.data.PreferencesRepository
import com.madhumangal.stt.TranscriptionManager

class MadhumangalApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    val preferencesRepository: PreferencesRepository by lazy { PreferencesRepository(this) }
    val transcriptionManager: TranscriptionManager by lazy {
        TranscriptionManager(
            this,
            database.audioSegmentDao(),
            database.transcriptionDao(),
            database.noteTaskDao()
        )
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        com.madhumangal.util.AppLogger.init(this)

        // Global crash handler: logs any unhandled crash with stack trace to persistent session log
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                com.madhumangal.util.AppLogger.e("CRASH", "FATAL UNCAUGHT CRASH on thread '${thread.name}': ${throwable.message}", throwable)
            } catch (_: Exception) {}
            defaultHandler?.uncaughtException(thread, throwable)
        }

        createNotificationChannel()
        com.madhumangal.audio.AudioRecordingService.cancelRemnantNotification(this)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Continuous Recorder Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notification for continuous background audio recording service"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "madhumangal_service_channel"
        lateinit var instance: MadhumangalApplication
            private set
    }
}
