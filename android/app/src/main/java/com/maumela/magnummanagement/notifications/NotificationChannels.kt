package com.maumela.magnummanagement.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.maumela.magnummanagement.MainActivity

/** The notification channel (required on Android 8+) and a helper for notifications shown while the app is open. */
object NotificationChannels {
    /** Must match the default_notification_channel_id meta-data in AndroidManifest.xml. */
    const val DEFAULT_ID = "mmm_default"

    fun create(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(DEFAULT_ID, "MMM notifications", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Order updates and announcements"
        }
        manager.createNotificationChannel(channel)
    }

    /** Messages that arrive while the app is in the foreground are not shown by Android, so the app shows them. */
    fun show(context: Context, title: String, body: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = Notification.Builder(context, DEFAULT_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .build()
        context.getSystemService(NotificationManager::class.java)?.notify(System.currentTimeMillis().toInt(), notification)
    }
}
