package com.example.tweetapp.model

import com.example.tweetapp.model.enums.ReminderDates

data class ReminderItem(
    val id : String,
    val checked : Boolean = false,
    val reminderDates: ReminderDates
)