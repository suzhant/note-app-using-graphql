package com.example.tweetapp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.navigation.NavDeepLinkBuilder
import com.example.tweetapp.R
import com.example.tweetapp.extension.toPost
import com.example.tweetapp.model.AlarmItem
import com.example.tweetapp.ui.main.MainActivity
import com.example.tweetapp.ui.main.fragments.DetailFragmentArgs


class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val alarmItem = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(ALARM_ITEM, AlarmItem::class.java)
        } else {
            intent?.getParcelableExtra(ALARM_ITEM)
        }
        Log.d("calendarData", alarmItem.toString())
        val notificationManager =
            context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = alarmItem?.let { createNotification(context, it, notificationManager) }
        notificationManager.notify(alarmItem.hashCode(), notification)
    }

    private fun createNotification(
        context: Context,
        alarmItem: AlarmItem,
        notificationManager: NotificationManager
    ): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                FCM_CHANNEL_ID,
                "Alarm Notification channel",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val pendingIntent = NavDeepLinkBuilder(context)
            .setGraph(R.navigation.nav_graph)
            .setDestination(R.id.detailFragment)
            .setArguments(DetailFragmentArgs(post = alarmItem.toPost()).toBundle())
            .setComponentName(MainActivity::class.java)
            .createPendingIntent()

        val builder = NotificationCompat.Builder(context, FCM_CHANNEL_ID)
            .setSmallIcon(R.drawable.baseline_notifications_24)
            .setContentTitle(alarmItem.title)
            .setContentText(alarmItem.message)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
            .setStyle(NotificationCompat.InboxStyle()
                .addLine(alarmItem.message.ifEmpty { "New Reminder" })
            )
            .setCategory(NotificationCompat.CATEGORY_REMINDER)

        return builder.build()
    }

    companion object {
        const val FCM_CHANNEL_ID = "alarm_notification"
        const val ALARM_ITEM = "alarm_item"
    }
}