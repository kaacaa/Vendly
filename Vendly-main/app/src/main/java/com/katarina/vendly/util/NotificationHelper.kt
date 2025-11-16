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

    //proveravamo verziju androida, pa kreiramo kanal sa id, imenom i vaznostima
    //manager koji kreira kanal
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

    //pravimo jednostavnu notifikaciju, biramo ikonicu, tekst,naslov,prioritet
    //za prioritet gde ce da se nadje u listi na telefonu
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

    //otvara aplikaciju i vodi na odredjeni automat
    @SuppressLint("MissingPermission")
    fun showVendingNotification(
        vendingId: String,
        title: String,
        message: String,
        notificationId: Int = vendingId.hashCode()
    ) {
        //otvara mainactivity, brise sve prethodne aktivnosti, prosledjuje id automata
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("vending_id", vendingId)
        }

        //dozvola da sistem pokrene intent kada korisnik klikne na notifikaciju
        //ovaj flag kaze ako vec postoji samo ga azuriraj
        val pendingIntent = PendingIntent.getActivity(
            context,
            vendingId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        //ima klik, otvara odredjeni automat, klikom se zatvara
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(safeSmallIcon())
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        //svaki automat ima edinstvenu notifikaciju, ako je vec ima ona se samo azurira
        with(NotificationManagerCompat.from(context)) {
            notify(notificationId, builder.build())
        }
    }

    private fun safeSmallIcon(): Int {
        return try { R.drawable.ic_launcher_background } catch (_: Exception) { R.drawable.ic_launcher_foreground }
    }
}