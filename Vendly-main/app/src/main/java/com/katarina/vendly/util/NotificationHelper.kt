package com.katarina.vendly.util

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.katarina.vendly.MainActivity
import com.katarina.vendly.R

class NotificationHelper(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "vendly_notifications"
        private const val CHANNEL_NAME = "Nearby Machines"
        private const val CHANNEL_DESC = "Notifies you when vending machines or users are nearby"
    }

    init { createNotificationChannel() }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = CHANNEL_DESC }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    /** Simple info notification, no deep navigation. */
    @SuppressLint("MissingPermission")
    fun showSimpleNotification(
        title: String,
        message: String,
        notificationId: Int = System.currentTimeMillis().toInt()
    ) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(safeSmallIcon())
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            notify(notificationId, builder.build())
        }
    }

    /** Opens the app and navigates to the vending details using an extra. */
    @SuppressLint("MissingPermission")
    fun showVendingNotification(
        vendingId: String,
        title: String,
        message: String,
        notificationId: Int = vendingId.hashCode()
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("vending_id", vendingId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            vendingId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(safeSmallIcon())
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            notify(notificationId, builder.build())
        }
    }

    private fun safeSmallIcon(): Int {
        // Use your own small icon; fallback prevents crashes if ic_notification is missing.
        return try { R.drawable.ic_launcher_background } catch (_: Exception) { R.drawable.ic_launcher_foreground }
    }
}