package com.madhumangal.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.madhumangal.util.AppLogger

/**
 * Handles notification actions (e.g., Stop Recording button) via Broadcast.
 * Broadcasts are safe against Android 12+ background service start restrictions
 * and allow instantaneous cancellation of remnant/zombie notifications.
 */
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        AppLogger.i("NOTIFICATION", "Received notification action broadcast: $action")
        when (action) {
            AudioRecordingService.ACTION_STOP,
            AudioRecordingService.ACTION_NOTIFICATION_STOP -> {
                // 1. Unconditionally clear the notification from the system shade immediately
                AudioRecordingService.cancelRemnantNotification(context)

                // 2. Stop active recording service gracefully
                AudioRecordingService.stop(context)
            }
        }
    }
}
