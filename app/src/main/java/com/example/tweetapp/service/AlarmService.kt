package com.example.tweetapp.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.example.tweetapp.model.AlarmItem


class AlarmService(private val context: Context) : AlarmController {

    private var alarmMgr: AlarmManager? = null

    init {
        alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    override fun scheduleAlarm(alarmItem: AlarmItem) {
        val notificationIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.ALARM_ITEM,alarmItem)
        }
        val alarmIntent = PendingIntent.getBroadcast(
            context,
            alarmItem.hashCode(),
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmMgr?.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            alarmItem.triggerTimeInMillis,
            alarmIntent
        )

    }

    override fun cancelAlarm(alarmItem: AlarmItem) {
        alarmMgr?.cancel(PendingIntent.getBroadcast(
            context,
            alarmItem.hashCode(),
            Intent(context,AlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        ))
    }

}