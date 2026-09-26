package com.echo.recorder

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.echo.recorder.data.AppDatabase
import com.echo.recorder.data.PreferencesRepository
import com.echo.recorder.stt.TranscriptionManager

class EchoApplication : Application() {

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
        com.echo.recorder.util.AppLogger.init(this)
        createNotificationChannel()
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
        const val NOTIFICATION_CHANNEL_ID = "good_echo_service_channel"
        lateinit var instance: EchoApplication
            private set
    }
}
