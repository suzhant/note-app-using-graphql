package com.example.tweetapp.ui.calendar

import com.example.tweetapp.model.ReminderItem

data class ReminderUiState(
    val items : List<ReminderItem> = emptyList(),
    val checkedItems : List<ReminderItem> = emptyList()
)