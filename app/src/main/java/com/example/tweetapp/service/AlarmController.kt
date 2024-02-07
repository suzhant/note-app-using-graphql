package com.example.tweetapp.service

import android.app.Notification
import android.app.PendingIntent
import com.example.tweetapp.model.AlarmItem

interface AlarmController {

    fun scheduleAlarm(alarmItem: AlarmItem)

    fun cancelAlarm(alarmItem: AlarmItem)
}